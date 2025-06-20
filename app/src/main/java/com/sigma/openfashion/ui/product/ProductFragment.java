package com.sigma.openfashion.ui.product;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.data.image.ImageAdapter;
import com.sigma.openfashion.data.product.Gender;
import com.sigma.openfashion.data.product.Product;
import com.sigma.openfashion.ui.BaseFragment;
import com.sigma.openfashion.ui.HeaderConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eightbitlab.com.blurview.BlurView;

public class ProductFragment extends BaseFragment {
    private TextView textViewTitle, textViewPrice, textViewDescription, textViewCategory;
    private ViewPager2 imageViewPhotos, imageViewPhotosFull;
    private LinearLayout indicators, indicatorsFull;
    private MaterialButton buttonOrderProduct, buttonHeart, buttonFullClose;
    private ImageButton buttonOpenFull;
    private ImageAdapter adapter;
    private Product product;
    private ProgressBar errorProgress;
    private BlurView errorBlurView;
    private ConstraintLayout viewPhotoFull;
    private RadioGroup radioGroupSize, radioGroupColor;
    private List<String> images = new ArrayList<>();
    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;
    private View view;
    private int currentProductId;

    public ProductFragment() {
        super(R.layout.fragment_product);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_product, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setHeaderVisibility(true);
        setupHeader(HeaderConfig.BUTTON_BACK | HeaderConfig.BUTTON_SEARCH | HeaderConfig.BUTTON_ORDER);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BlurView blurView2    = view.findViewById(R.id.blurView2);
        BlurView blurView3    = view.findViewById(R.id.blurView3);
        imageViewPhotos       = view.findViewById(R.id.imageViewPhotos);
        imageViewPhotosFull   = view.findViewById(R.id.imageViewPhotosFull);
        indicators            = view.findViewById(R.id.indicatorsPhotos);
        indicatorsFull        = view.findViewById(R.id.indicatorsPhotosFull);
        textViewDescription   = view.findViewById(R.id.textViewDescription);
        textViewCategory      = view.findViewById(R.id.textViewCategory);
        textViewPrice         = view.findViewById(R.id.textViewPrice);
        textViewTitle         = view.findViewById(R.id.textViewTitle);
        radioGroupColor       = view.findViewById(R.id.radioGroupColor);
        radioGroupSize        = view.findViewById(R.id.radioGroupSize);
        buttonHeart           = view.findViewById(R.id.buttonHeart);
        buttonFullClose       = view.findViewById(R.id.buttonFullClose);
        buttonOpenFull        = view.findViewById(R.id.buttonOpenFull);
        buttonOrderProduct    = view.findViewById(R.id.buttonOrderProduct);
        errorProgress         = view.findViewById(R.id.errorProgress);
        errorBlurView         = view.findViewById(R.id.errorBlurView);

        ViewGroup rootView        = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();
        supabaseService           = new SupabaseService();
        prefs                     = SharedPrefHelper.getInstance(requireContext());

        supabaseService.setJwtToken(prefs.getJwtToken());

        Bundle args = getArguments();
        currentProductId = args != null ? args.getInt("productId", 0) : 0;

        blurView2.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurEnabled(true)
                .setBlurAutoUpdate(true)
                .setBlurRadius(5f);
        blurView3.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurEnabled(true)
                .setBlurAutoUpdate(true)
                .setBlurRadius(3f);
        errorBlurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurEnabled(true)
                .setBlurAutoUpdate(true)
                .setBlurRadius(10f);

        showProgress(true);
        supabaseService.getProduct(currentProductId, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUi(() -> {
                    showProgress(false);
                    try {
                        JSONArray array = new JSONArray(jsonResponse);
                        JSONObject obj = array.getJSONObject(0);
                        product = new Product(
                                obj.getInt("id"),
                                obj.getInt("category_id"),
                                Gender.fromString(obj.getString("gender")),
                                obj.getString("name"),
                                obj.getDouble("price"),
                                obj.optString("preview_image_url", ""),
                                obj.optString("currency", "RUB"),
                                obj.optString("description")
                        );

                        textViewDescription.setText(Html.fromHtml(product.description, Html.FROM_HTML_MODE_LEGACY));
                        textViewTitle.setText(product.name);
                        textViewPrice.setText(product.getPriceStr());

                        JSONArray sizesArray = obj.getJSONArray("available_sizes");
                        JSONArray colorsArray = obj.getJSONArray("available_colors");

                        String[] sizes = new String[sizesArray.length()];
                        for (int i = 0; i < sizesArray.length(); i++) {
                            sizes[i] = sizesArray.getString(i);
                        }

                        String[] colors = new String[colorsArray.length()];
                        for (int i = 0; i < colorsArray.length(); i++) {
                            colors[i] = colorsArray.getString(i);
                        }

                        boolean atLeastOneEnabled = false;

                        for (int i = 0; i < radioGroupSize.getChildCount(); i++) {
                            RadioButton radioButton = (RadioButton) radioGroupSize.getChildAt(i);
                            String buttonText = radioButton.getText().toString().trim().toLowerCase();

                            boolean existsInArray = Arrays.stream(sizes)
                                    .anyMatch(size -> size.trim().toLowerCase().equals(buttonText));

                            radioButton.setEnabled(existsInArray);

                            if (existsInArray) {
                                atLeastOneEnabled = true;
                            }
                        }

                        if (!atLeastOneEnabled){
                            for (int i = 0; i < radioGroupSize.getChildCount(); i++) {
                                RadioButton radioButton = (RadioButton) radioGroupSize.getChildAt(i);
                                if (!sizesArray.isNull(i)){
                                    radioButton.setText(sizesArray.getString(i));
                                    radioButton.setEnabled(true);
                                } else {
                                    radioButton.setVisibility(View.GONE);
                                }
                            }
                        }

                        createRadioGroup(radioGroupColor, colors);

                        showProgress(true);
                        supabaseService.getCategoryById(product.category, new SupabaseService.QueryCallback() {
                            @Override
                            public void onSuccess(String jsonResponse) {
                                runOnUi(() -> {
                                    showProgress(false);
                                    try {
                                        JSONArray array = new JSONArray(jsonResponse);
                                        JSONObject obj = array.getJSONObject(0);

                                        String gender;
                                        switch (product.gender){
                                            case MALE:
                                                gender = getString(R.string.man);
                                                break;
                                            case FEMALE:
                                                gender = getString(R.string.female);
                                                break;
                                            default:
                                                gender = getString(R.string.unisex);
                                                break;
                                        }

                                        textViewCategory.setText(obj.getString("name") + " - " + gender);
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

        showProgress(true);
        supabaseService.getProductImages(currentProductId, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUi(() -> {
                    showProgress(false);
                    try {
                        JSONArray array = new JSONArray(jsonResponse);
                        images.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            images.add(obj.getString("image_url"));
                        }

                        adapter = new ImageAdapter(requireContext(), images);
                        imageViewPhotos.setAdapter(adapter);
                        imageViewPhotosFull.setAdapter(adapter);

                        createIndicators();

                        imageViewPhotos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                            @Override
                            public void onPageSelected(int position) {
                                super.onPageSelected(position);
                                imageViewPhotosFull.setCurrentItem(position);
                                updateIndicators(position);
                            }
                        });
                        imageViewPhotosFull.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                            @Override
                            public void onPageSelected(int position) {
                                super.onPageSelected(position);
                                imageViewPhotos.setCurrentItem(position);
                                updateIndicators(position);
                            }
                        });
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

        buttonFullClose.setOnClickListener(view1 -> {
            blurView3.setVisibility(View.GONE);
            setHeaderVisibility(true);
        });
        buttonOpenFull.setOnClickListener(view1 -> {
            blurView3.setVisibility(View.VISIBLE);
            setHeaderVisibility(false);
        });

        radioGroupSize.setOnCheckedChangeListener((group, checkedId) -> bindButtonOrderProduct());
        radioGroupColor.setOnCheckedChangeListener((group, checkedId) -> bindButtonOrderProduct());
        bindButtonOrderProduct();
    }

    private void bindButtonOrderProduct() {
        String selectedColor = null;
        String selectedSize = null;

        int selectedColorId = radioGroupColor.getCheckedRadioButtonId();
        if (selectedColorId != -1) {
            RadioButton selectedColorButton = view.findViewById(selectedColorId);
            selectedColor = selectedColorButton.getText().toString();
        }

        int selectedSizeId = radioGroupSize.getCheckedRadioButtonId();
        if (selectedSizeId != -1) {
            RadioButton selectedSizeButton = view.findViewById(selectedSizeId);
            selectedSize = selectedSizeButton.getText().toString();
        }

        if (selectedSizeId == -1 || selectedColorId == -1){
            buttonOrderProduct.setEnabled(false);
            return;
        }

        showProgress(true);
        supabaseService.isExistCartItem(prefs.getUserId(), currentProductId, selectedSize, selectedColor,  new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUi(() -> {
                    showProgress(false);
                    if (jsonResponse != null && !jsonResponse.equals("[]")) {
                        try {
                            JSONArray array = new JSONArray(jsonResponse);
                            JSONObject obj = null;
                            obj = array.getJSONObject(0);
                            int id = obj.optInt("id");
                            buttonOrderProduct.setBackgroundTintList(
                                    ColorStateList.valueOf(getResources().getColor(R.color.colorError))
                            );
                            buttonOrderProduct.setText(getString(R.string.remove_basket));
                            buttonOrderProduct.setOnClickListener(view1 -> removeBasked(id));
                            buttonOrderProduct.setIconResource(R.drawable.minus);
                            buttonOrderProduct.setEnabled(true);
                        } catch (JSONException e) {
                            showError(getString(R.string.Parse_Error) + e.getMessage());
                        }
                    } else {
                        buttonOrderProduct.setBackgroundTintList(
                            ColorStateList.valueOf(getResources().getColor(R.color.TitleActive))
                        );
                        buttonOrderProduct.setIconResource(R.drawable.plus);
                        buttonOrderProduct.setText(getString(R.string.add_to_basket));
                        buttonOrderProduct.setOnClickListener(view1 -> addBasked());
                        buttonOrderProduct.setEnabled(true);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void addBasked() {
        String selectedColor = null;
        String selectedSize = null;

        int selectedColorId = radioGroupColor.getCheckedRadioButtonId();
        if (selectedColorId != -1) {
            RadioButton selectedColorButton = view.findViewById(selectedColorId);
            selectedColor = selectedColorButton.getText().toString();
        }

        int selectedSizeId = radioGroupSize.getCheckedRadioButtonId();
        if (selectedSizeId != -1) {
            RadioButton selectedSizeButton = view.findViewById(selectedSizeId);
            selectedSize = selectedSizeButton.getText().toString();
        }

        showProgress(true);
        supabaseService.addCartItem(prefs.getUserId(), currentProductId, selectedSize, selectedColor, 1, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUi(() -> {
                    showProgress(false);
                    NavHostFragment.findNavController(ProductFragment.this)
                            .navigate(R.id.action_product_to_basked);
                });
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void removeBasked(int cardItemId) {
        showProgress(true);
        supabaseService.deleteCartItem(cardItemId, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUi(() -> {
                    showProgress(false);
                    bindButtonOrderProduct();
                });
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void createIndicators() {
        indicators.removeAllViews();
        indicatorsFull.removeAllViews();
        for (int i = 0; i < images.size(); i++) {
            View indicator = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40, 40);
            params.setMargins(6, 0, 6, 0);
            indicator.setLayoutParams(params);
            indicators.addView(indicator);
            View indicator2 = new View(requireContext());
            params.setMargins(0, 6, 0, 6);
            indicator2.setLayoutParams(params);
            indicatorsFull.addView(indicator2);
        }
        updateIndicators(0);
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.getChildCount(); i++) {
            View child = indicators.getChildAt(i);
            Drawable drawable = ContextCompat.getDrawable(requireContext(), (i == position ? R.drawable.rectangle_fill : R.drawable.rectangle));
            if (drawable != null) {
                drawable.setTint(ContextCompat.getColor(requireContext(), R.color.Placeholder));
                child.setBackground(drawable);
            }
        }
        for (int i = 0; i < indicatorsFull.getChildCount(); i++) {
            View child = indicatorsFull.getChildAt(i);
            Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.rectangle_fill);
            if (drawable != null) {
                drawable.setTint(ContextCompat.getColor(requireContext(), (i == position ? R.color.Primary : R.color.white)));
                child.setBackground(drawable);
            }
        }
    }
    private void createRadioGroup(RadioGroup radioGroup, String[] options) {
        radioGroup.clearCheck();

        for (int i = 0; i < options.length; i++) {
            RadioButton radioButton = new RadioButton(
                    requireContext(),
                    null,
                    0,
                    R.style.Widget_OpenFashion_RadioButton);

            radioButton.setText(options[i]);
            radioGroup.addView(radioButton);
        }
    }

    private void showProgress(boolean show) {
        errorBlurView.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonHeart.setEnabled(!show);
    }

    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }
}
