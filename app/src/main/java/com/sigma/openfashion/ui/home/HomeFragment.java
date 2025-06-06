// res/java/com/sigma/openfashion/ui/home/HomeFragment.java
package com.sigma.openfashion.ui.home;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.Drawable;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.google.android.flexbox.FlexboxLayout;
import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.data.local.ProductEntity;
import com.sigma.openfashion.data.product.Gender;
import com.sigma.openfashion.data.product.Product;
import com.sigma.openfashion.data.product.ProductsAdapter;
import com.sigma.openfashion.data.product.ProductsRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurView;


public class HomeFragment extends Fragment {

    private ConstraintLayout constaintLayoutBanner;
    private FlexboxLayout flexboxProducts;
    private ProductsRepository productsRepository;
    private List<ProductEntity> productEntityList = new ArrayList<>();

    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BlurView blurView     = view.findViewById(R.id.blurView);
        ImageView imageView   = view.findViewById(R.id.imageViewBanner);
        constaintLayoutBanner = view.findViewById(R.id.constaintLayoutBanner);
        flexboxProducts       = view.findViewById(R.id.flexboxProducts);

        ViewGroup rootView        = (ViewGroup) requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();
        supabaseService           = new SupabaseService();
        prefs                     = SharedPrefHelper.getInstance(requireContext());

        supabaseService.setJwtToken(prefs.getJwtToken());
        productsRepository = new ProductsRepository(requireContext(), supabaseService);

        blurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurEnabled(true)
                .setBlurAutoUpdate(true)
                .setBlurRadius(20f);

        Glide.with(this)
                .asBitmap()
                .load("https://hufhfmqquczxpxpmtzbf.supabase.co/storage/v1/object/public/images//image%2010.png")
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(bitmap);
                        extractDominantColor(bitmap);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });

        loadProductsWithCache();
    }
    /**
     * Получаем список сначала из локального кэша (Room), затем (независимо) из сети,
     * и обновляем UI по каждому «пакету» данных.
     */
    private void loadProductsWithCache() {
        // Запрашиваем из репозитория (он вернёт сначала локальные, потом сетевые)
        productsRepository.getProducts(-1, 4, 0, new ProductsRepository.LoadCallback() {
            @Override
            public void onLoaded(List<ProductEntity> products) {
                // В этом колбэке могут прийти:
                // 1) Сначала — локальные из БД (если они есть);
                // 2) Затем — свежие из сети (обновлённый список).
                // Надо обязательно делать UI-обновление в UI-потоке:
                runOnUi(() -> {
                    // Если список пустой и пришёл из сети —
                    // просто обновляем список.
                    // Если список непустой (из БД) — покажем сразу.
                    productEntityList.clear();
                    productEntityList.addAll(products);
                    loadProductsPreviewFromEntities(productEntityList);
                });
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    /**
     * Заполняем FlexboxLayout товарами
     * (раньше у вас это был List<Product>, теперь — List<ProductEntity>).
     */
    private void loadProductsPreviewFromEntities(List<ProductEntity> products) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        flexboxProducts.removeAllViews();

        for (ProductEntity product : products) {
            View itemView = inflater.inflate(R.layout.product_item, flexboxProducts, false);

            TextView textTitle = itemView.findViewById(R.id.textViewTitle);
            TextView textPrice = itemView.findViewById(R.id.textViewPrice);
            ImageView imagePreview = itemView.findViewById(R.id.imageViewPreview);

            textTitle.setText(product.getName());
            textPrice.setText(String.format("%s %s", product.getPrice(), product.getCurrency()));
            Glide.with(this)
                    .load(product.getPreviewImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // кэширование картинки
                    .into(imagePreview);

            itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putInt("productId", product.getId());
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_home_to_product, bundle);
            });

            flexboxProducts.addView(itemView);
        }
    }

    private void extractDominantColor(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int dominantColor = palette.getDominantColor(Color.BLACK);
                constaintLayoutBanner.setBackgroundColor(dominantColor);
            }
        });
    }

    private void showError(String message) {
        runOnUi(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        );
    }

    private void runOnUi(Runnable block) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(block);
        }
    }
}
