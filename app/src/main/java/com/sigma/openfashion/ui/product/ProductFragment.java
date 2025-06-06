package com.sigma.openfashion.ui.product;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.data.image.ImageAdapter;
import com.sigma.openfashion.data.product.Gender;
import com.sigma.openfashion.data.product.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eightbitlab.com.blurview.BlurView;

public class ProductFragment extends Fragment {
    private ImageButton buttonBack;
    private TextView textViewTitle, textViewPrice, textViewDescription, textViewCategory;
    private ViewPager2 imageViewPhotos, imageViewPhotosFull;
    private LinearLayout indicators, indicatorsFull;
    private MaterialButton buttonOrderProduct, buttonHeart, buttonFullClose;
    private ImageButton buttonOpenFull;
    private ImageAdapter adapter;
    private Product product;
    private RadioGroup radioGroupSize, radioGroupColor;
    private List<String> images = new ArrayList<>();
    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;

    public ProductFragment() {
        super(R.layout.fragment_product);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BlurView blurView     = view.findViewById(R.id.blurView);
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
        buttonBack            = view.findViewById(R.id.buttonBack);
        radioGroupColor       = view.findViewById(R.id.radioGroupColor);
        radioGroupSize        = view.findViewById(R.id.radioGroupSize);
        buttonHeart           = view.findViewById(R.id.buttonHeart);
        buttonFullClose       = view.findViewById(R.id.buttonFullClose);
        buttonOpenFull        = view.findViewById(R.id.buttonOpenFull);
        buttonOrderProduct    = view.findViewById(R.id.buttonOrderProduct);

        ViewGroup rootView        = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();
        supabaseService           = new SupabaseService();
        prefs                     = SharedPrefHelper.getInstance(requireContext());

        supabaseService.setJwtToken(prefs.getJwtToken());

        Bundle args = getArguments();
        int id = args != null ? args.getInt("productId", 0) : 0;

        blurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurEnabled(true)
                .setBlurAutoUpdate(true)
                .setBlurRadius(15f);
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

        blurView3.setVisibility(View.GONE);

        supabaseService.getProduct(id, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUi(() -> {
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

                        for (int i = 0; i < radioGroupSize.getChildCount(); i++) {
                            RadioButton radioButton = (RadioButton) radioGroupSize.getChildAt(i);
                            String buttonText = radioButton.getText().toString().trim().toLowerCase();

                            boolean existsInArray = Arrays.stream(sizes)
                                    .anyMatch(size -> size.trim().toLowerCase().equals(buttonText));

                            radioButton.setEnabled(existsInArray);
                        }

                        createRadioGroup(radioGroupColor, colors);

                        supabaseService.getCategoryById(product.category, new SupabaseService.QueryCallback() {
                            @Override
                            public void onSuccess(String jsonResponse) {
                                runOnUi(() -> {
                                    try {
                                        JSONArray array = new JSONArray(jsonResponse);
                                        JSONObject obj = array.getJSONObject(0);
                                        textViewCategory.setText(obj.getString("name") + " - " + product.getGenderStr());
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
        supabaseService.getProductImages(id, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUi(() -> {
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

        buttonBack.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigateUp();
        });

        buttonFullClose.setOnClickListener(view1 -> {
            blurView3.setVisibility(View.GONE);
        });
        buttonOpenFull.setOnClickListener(view1 -> {
            blurView3.setVisibility(View.VISIBLE);
        });

        radioGroupSize.setOnCheckedChangeListener((group, checkedId) -> updateButtonState());
        radioGroupColor.setOnCheckedChangeListener((group, checkedId) -> updateButtonState());
        updateButtonState();
    }

    private void updateButtonState() {
        boolean hasSelection1 = radioGroupSize.getCheckedRadioButtonId() != -1;
        boolean hasSelection2 = radioGroupColor.getCheckedRadioButtonId() != -1;

        buttonOrderProduct.setEnabled(hasSelection1 && hasSelection2);
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

    }

    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    private void runOnUi(Runnable block) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(block);
        }
    }
}
