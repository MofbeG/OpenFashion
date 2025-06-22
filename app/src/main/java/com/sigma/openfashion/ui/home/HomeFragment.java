// res/java/com/sigma/openfashion/ui/home/HomeFragment.java
package com.sigma.openfashion.ui.home;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
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
import com.google.android.material.navigation.NavigationView;
import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.data.local.ProductEntity;
import com.sigma.openfashion.data.product.Gender;
import com.sigma.openfashion.data.product.Product;
import com.sigma.openfashion.data.product.ProductsAdapter;
import com.sigma.openfashion.data.product.ProductsRepository;
import com.sigma.openfashion.ui.BaseFragment;
import com.sigma.openfashion.ui.HeaderConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurView;


public class HomeFragment extends BaseFragment {

    private ConstraintLayout constaintLayoutBanner;
    private FlexboxLayout flexboxProducts;
    private ProductsRepository productsRepository;
    private Button buttonShowAll;
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

        ImageView imageView   = view.findViewById(R.id.imageViewBanner);
        constaintLayoutBanner = view.findViewById(R.id.constaintLayoutBanner);
        flexboxProducts       = view.findViewById(R.id.flexboxProducts);
        buttonShowAll         = view.findViewById(R.id.buttonShowAll);

        supabaseService           = new SupabaseService();
        prefs                     = SharedPrefHelper.getInstance(requireContext());

        supabaseService.setJwtToken(prefs.getJwtToken());
        productsRepository = new ProductsRepository(requireContext(), supabaseService);

        Glide.with(this)
                .asBitmap()
                .load("https://hufhfmqquczxpxpmtzbf.supabase.co/storage/v1/object/public/images//Banner%20(1).png")
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

        buttonShowAll.setOnClickListener(view1 -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_home_to_productlist);
        });

        loadProductsWithCache();
    }

    @Override
    public void onResume() {
        super.onResume();
        setHeaderVisibility(true);
        setupHeader(HeaderConfig.BUTTON_MENU | HeaderConfig.BUTTON_SEARCH | HeaderConfig.BUTTON_ORDER);
    }

    private void loadProductsWithCache() {
        // Запрашиваем из репозитория (он вернёт сначала локальные, потом сетевые)
        productsRepository.getProducts(-1, null, 4, 0, new ProductsRepository.LoadCallback() {
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
}
