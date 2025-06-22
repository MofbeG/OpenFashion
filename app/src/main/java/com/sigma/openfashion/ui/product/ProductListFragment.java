package com.sigma.openfashion.ui.product;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import android.widget.Button;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.data.local.ProductEntity;
import com.sigma.openfashion.data.product.Gender;
import com.sigma.openfashion.data.product.ProductsRepository;
import com.sigma.openfashion.ui.BaseFragment;
import com.sigma.openfashion.ui.HeaderConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurView;

public class ProductListFragment extends BaseFragment {
    private FlexboxLayout flexboxProducts;
    private ProductsRepository productsRepository;
    private List<ProductEntity> productEntityList = new ArrayList<>();
    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;
    private MaterialButton buttonFullClose, buttonFilter;
    private ImageButton buttonNextPage, buttonBackPage, buttonShowFilter;
    private TextView textViewCountPages, textViewProductsCount;
    private RadioGroup radioGroupCategories, radioGroupGender;
    private View view;
    private BlurView blurViewFilter;
    private ScrollView scrollView;

    private int currentPage = 1;
    private int totalPages = 1;
    private int totalItems = 0;
    private final int itemsPerPage = 8; // или другое значение
    private LinearLayout paginationContainer;

    private Gender genderFilter = null;
    private String name;
    private int categoryFilter = -1;

    public ProductListFragment() {
        super(R.layout.fragment_productlist);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_productlist, container, false);
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        flexboxProducts        = view.findViewById(R.id.flexboxProducts);
        buttonNextPage         = view.findViewById(R.id.buttonNextPage);
        buttonBackPage         = view.findViewById(R.id.buttonBackPage);
        paginationContainer    = view.findViewById(R.id.paginationContainer);
        textViewCountPages     = view.findViewById(R.id.textViewCountPages);
        textViewProductsCount  = view.findViewById(R.id.textViewProductsCount);
        blurViewFilter         = view.findViewById(R.id.blurViewFilter);
        buttonFullClose        = view.findViewById(R.id.buttonFullClose);
        buttonShowFilter       = view.findViewById(R.id.buttonShowFilter);
        buttonFilter           = view.findViewById(R.id.buttonApply);
        radioGroupGender       = view.findViewById(R.id.radioGroupGender);
        radioGroupCategories   = view.findViewById(R.id.radioGroupCategories);
        scrollView             = view.findViewById(R.id.scrollView);

        ViewGroup rootView        = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();
        supabaseService           = new SupabaseService();
        prefs                     = SharedPrefHelper.getInstance(requireContext());
        productsRepository        = new ProductsRepository(requireContext(), supabaseService);

        Bundle args = getArguments();
        if (args != null) {
            String genderStr = args.getString("gender", null);
            genderFilter = genderStr == null ? null : Gender.valueOf(genderStr);
            categoryFilter = args.getInt("categoryFilter", -1);
            name = args.getString("name", null);
        }

        blurViewFilter.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurEnabled(true)
                .setBlurAutoUpdate(true)
                .setBlurRadius(5f);

        String[] genders = { getString(R.string.man), getString(R.string.female), getString(R.string.unisex)};
        createRadioGroup(radioGroupGender, genders);

        buttonFullClose.setOnClickListener(view1 -> {
            blurViewFilter.setVisibility(View.GONE);
            scrollView.setActivated(true);
        });
        buttonShowFilter.setOnClickListener(view1 -> {
            supabaseService.getCategories(new SupabaseService.QueryCallback() {
                @Override
                public void onSuccess(String jsonResponse) {
                    runOnUi(() -> {
                        try {
                            JSONArray array = new JSONArray(jsonResponse);

                            String[] categories = new String[array.length()];
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                categories[i] = obj.getString("name");
                            }

                            createRadioGroup(radioGroupCategories, categories);

                            blurViewFilter.setVisibility(View.VISIBLE);
                            scrollView.setActivated(false);
                        } catch (JSONException e) {
                            showError(e.getMessage());
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    showError(errorMessage);
                }
            });
        });
        buttonFilter.setOnClickListener(view1 -> {
            int selectedId = radioGroupCategories.getCheckedRadioButtonId();
            if (selectedId != -1) {
                int selectedIndex = radioGroupCategories.indexOfChild(view.findViewById(selectedId));
                categoryFilter = selectedIndex + 1;
            } else {
                categoryFilter = -1;
            }

            if (radioGroupGender.getCheckedRadioButtonId() == -1){
                genderFilter = null;
            } else {
                int selectedGenderIndex = radioGroupGender.indexOfChild(view.findViewById(radioGroupGender.getCheckedRadioButtonId()));

                switch (selectedGenderIndex){
                    case 0:
                        genderFilter = Gender.MALE;
                        break;
                    case 1:
                        genderFilter = Gender.FEMALE;
                        break;
                    case 2:
                        genderFilter = Gender.UNISEX;
                        break;
                    default:
                        genderFilter = null;
                        break;
                }
            }
            radioGroupCategories.clearCheck();
            radioGroupGender.clearCheck();

            totalItems = 0;
            currentPage = 1;
            setupPaginationButtons();
            loadProductsWithCache(currentPage);

            blurViewFilter.setVisibility(View.GONE);
            scrollView.setActivated(true);
        });

        setupPaginationButtons();
        loadProductsWithCache(currentPage);
    }

    private void setupPaginationButtons() {
        buttonNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadProductsWithCache(currentPage);
            }
        });

        buttonBackPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadProductsWithCache(currentPage);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setHeaderVisibility(true);
        setupHeader(HeaderConfig.BUTTON_MENU | HeaderConfig.BUTTON_SEARCH | HeaderConfig.BUTTON_ORDER);
    }

    private void loadProductsWithCache(int page) {
        int offset = (page - 1) * itemsPerPage;

        productsRepository.getProductsWithCount(
                categoryFilter,
                genderFilter,
                name,
                itemsPerPage,
                offset,
                new ProductsRepository.LoadCallbackWithCount() {
                    @Override
                    public void onLoaded(List<ProductEntity> products, int totalCount) {
                        runOnUi(() -> {
                            productEntityList.clear();
                            productEntityList.addAll(products);
                            loadProductsPreviewFromEntities(productEntityList);

                            // Обновляем общее количество товаров
                            if (totalCount > 0) {
                                totalItems = totalCount;
                                totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
                            }

                            textViewProductsCount.setText(String.format("%d " + getString(R.string.Apparel), totalItems));

                            updatePaginationUI();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showError(errorMessage);
                    }
                }
        );
    }

    private void updatePaginationUI() {
        // Очищаем предыдущие кнопки страниц
        for (int i = paginationContainer.getChildCount() - 1; i >= 0; i--) {
            View child = paginationContainer.getChildAt(i);
            if (child != buttonNextPage && child != buttonBackPage) {
                paginationContainer.removeViewAt(i);
            }
        }

        // Рассчитываем диапазон отображаемых страниц
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);

        // Добавляем кнопки страниц
        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = new Button(requireContext());
            pageButton.setId(View.generateViewId());
            pageButton.setText(String.valueOf(i));
            pageButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

            // Параметры размера
            int size = dpToPx(50);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (currentPage > 1 && i == 1)
                params.setMargins(dpToPx(10), 0, 0, 0);
            pageButton.setLayoutParams(params);

            // Стилизация
            if (i == currentPage) {
                pageButton.setBackgroundResource(R.drawable.active_page_button_bg);
                pageButton.setTextColor(getResources().getColor(R.color.white));
            } else {
                pageButton.setBackgroundResource(R.drawable.inactive_page_button_bg);
                pageButton.setTextColor(getResources().getColor(R.color.Body));
            }

            final int page = i;
            pageButton.setOnClickListener(v -> {
                currentPage = page;
                loadProductsWithCache(currentPage);
            });

            // Вставляем перед кнопкой "Вперед"
            paginationContainer.addView(pageButton, paginationContainer.getChildCount() - 1);
        }

        // Обновляем состояние кнопок
        buttonBackPage.setVisibility(currentPage > 1 ? View.VISIBLE : View.GONE);
        buttonNextPage.setVisibility(currentPage < totalPages ? View.VISIBLE : View.GONE);

        // Визуальная обратная связь
        buttonBackPage.setAlpha(currentPage > 1 ? 1.0f : 0.5f);
        buttonNextPage.setAlpha(currentPage < totalPages ? 1.0f : 0.5f);

        // Показываем информацию о страницах
        textViewCountPages.setText(String.format("%d / %d", currentPage, totalPages));
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void createRadioGroup(RadioGroup radioGroup, String[] options) {
        radioGroup.clearCheck();
        radioGroup.removeAllViews();

        for (int i = 0; i < options.length; i++) {
            RadioButton radioButton = new RadioButton(
                    requireContext(),
                    null,
                    0,
                    R.style.Widget_OpenFashion_RadioButton);

            int size = dpToPx(40);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, size);
            params.setMargins(dpToPx(10), 0, 0, 0);

            radioButton.setLayoutParams(params);
            radioButton.setText(options[i]);
            radioGroup.addView(radioButton);
        }
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
                        .navigate(R.id.action_productlist_to_product, bundle);
            });

            flexboxProducts.addView(itemView);
        }
    }

    private void showError(String message) {
        runOnUi(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        );
    }
}
