package com.sigma.openfashion.ui.order;

import android.graphics.drawable.Drawable;
import android.icu.text.DecimalFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sigma.openfashion.R;
import com.sigma.openfashion.SharedPrefHelper;
import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.ui.BaseFragment;
import com.sigma.openfashion.ui.HeaderConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurView;

public class OrderFragment extends BaseFragment implements CartAdapter.OnQuantityChangeListener {
    private SupabaseService supabaseService;
    private SharedPrefHelper prefs;
    private BlurView errorBlurView;
    private MaterialButton buttonOrder;
    private RecyclerView cardItemsRecyclerView;
    private List<CartItem> cartItems = new ArrayList<>();
    private CartAdapter cartAdapter;
    private TextView textViewEmptyCart, textViewPrice;
    private View view;

    public OrderFragment() {
        super(R.layout.fragment_order);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_order, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setHeaderVisibility(true);
        setupHeader(HeaderConfig.BUTTON_MENU | HeaderConfig.BUTTON_SEARCH);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        errorBlurView         = view.findViewById(R.id.errorBlurView);
        buttonOrder           = view.findViewById(R.id.buttonOrdera);
        cardItemsRecyclerView = view.findViewById(R.id.cardItemsRecyclerView);
        textViewEmptyCart     = view.findViewById(R.id.textViewEmptyCart);
        textViewPrice         = view.findViewById(R.id.textViewPrice);

        ViewGroup rootView        = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();
        supabaseService           = new SupabaseService();
        prefs                     = SharedPrefHelper.getInstance(requireContext());

        errorBlurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurEnabled(true)
                .setBlurAutoUpdate(true)
                .setBlurRadius(10f);

        supabaseService.setJwtToken(prefs.getJwtToken());

        cardItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartAdapter = new CartAdapter(new ArrayList<>(), this);
        cardItemsRecyclerView.setAdapter(cartAdapter);

        loadCartItems();
    }

    private void showProgress(boolean show) {
        errorBlurView.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonOrder.setEnabled(!show);
    }

    private void showError(String message) {
        showProgress(false);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void loadCartItems() {
        showProgress(true);
        supabaseService.getCartItems(prefs.getUserId(), new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUi(() -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<CartItem> items = new ArrayList<>();

                        double priceAll = 0;
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject itemObj = array.getJSONObject(i);
                            JSONObject productObj = itemObj.getJSONObject("product");

                            double price = productObj.getDouble("price");
                            int quantity = itemObj.getInt("quantity");
                            items.add(new CartItem(
                                    itemObj.getInt("id"),
                                    itemObj.getInt("product_id"),
                                    itemObj.getString("user_id"),
                                    quantity,
                                    itemObj.getString("selected_size"),
                                    itemObj.getString("selected_color"),
                                    itemObj.getString("added_at"),
                                    itemObj.getString("updated_at"),
                                    productObj.getString("name"),
                                    price,
                                    productObj.getString("currency"),
                                    productObj.getString("preview_image_url")
                            ));

                            priceAll += price * quantity;
                        }

                        cartItems.clear();
                        cartItems.addAll(items);
                        cartAdapter.updateData(cartItems);
                        showProgress(false);
                        textViewEmptyCart.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                        buttonOrder.setEnabled(!items.isEmpty());

                        if (!items.isEmpty()){
                            DecimalFormat df = new DecimalFormat("#0.00");
                            textViewPrice.setText(String.valueOf(df.format(priceAll) + " " + items.get(0).getCurrency()));
                        }
                        else textViewPrice.setText("-");
                    } catch (JSONException e) {
                        showError(e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUi(() -> showError(error));
            }
        });
    }

    @Override
    public void onQuantityChanged(int itemId, int newQuantity) {
        supabaseService.updateCartItemQuantity(itemId, newQuantity, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUi(() -> loadCartItems());
            }

            @Override
            public void onError(String error) {
                runOnUi(() -> showError(error));
            }
        });
    }

    @Override
    public void onRemoveItem(int itemId) {
        supabaseService.deleteCartItem(itemId, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUi(() -> loadCartItems());
            }

            @Override
            public void onError(String error) {
                runOnUi(() -> showError(error));
            }
        });
    }

    @Override
    public void onClick(int product_id) {
        runOnUi(() -> {
            Bundle bundle = new Bundle();
            bundle.putInt("productId", product_id);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_order_to_product, bundle);
        });
    }
}