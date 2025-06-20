package com.sigma.openfashion.ui.order;

import android.icu.text.DecimalFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sigma.openfashion.R;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private OnQuantityChangeListener onQuantityChangeListener;

    public interface OnQuantityChangeListener {
        void onQuantityChanged(int itemId, int newQuantity);
        void onRemoveItem(int itemId);
        void onClick(int product_id);
    }

    public CartAdapter(List<CartItem> cartItems, OnQuantityChangeListener listener) {
        this.cartItems = cartItems;
        this.onQuantityChangeListener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_order, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        // Установка данных
        holder.textViewTitle.setText(item.getProductTitle());
        holder.textViewBody.setText(item.getSelectedColor() + ", " + item.getSelectedSize());

        DecimalFormat df = new DecimalFormat("#0.00");
        holder.textViewPrice.setText(String.valueOf(df.format(item.getPrice() * item.getQuantity()) + " " + item.getCurrency()));
        if (item.getQuantity() > 1)
            holder.textViewPrice1.setVisibility(View.VISIBLE);
        holder.textViewPrice1.setText(item.getPriceStr());
        holder.textViewCount.setText(String.valueOf(item.getQuantity()));

        Glide.with(holder.imageViewPreview.getContext())
                .load(item.getImageUrl())
                .into(holder.imageViewPreview);

        // Обработка кликов
        holder.buttonPlus.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() + 1;
            if (newQuantity <= 100) {
                onQuantityChangeListener.onQuantityChanged(item.getId(), newQuantity);
                holder.textViewPrice1.setVisibility(View.VISIBLE);
            }
        });

        holder.buttonMinus.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() - 1;

            if (newQuantity > 1)
                holder.textViewPrice1.setVisibility(View.VISIBLE);
            else
                holder.textViewPrice1.setVisibility(View.GONE);

            if (newQuantity >= 1) {
                onQuantityChangeListener.onQuantityChanged(item.getId(), newQuantity);
            } else {
                onQuantityChangeListener.onRemoveItem(item.getId());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            onQuantityChangeListener.onClick(item.getProductId());
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void updateData(List<CartItem> newItems) {
        cartItems.clear();
        cartItems.addAll(newItems);
        notifyDataSetChanged();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPreview;
        TextView textViewCount, textViewTitle, textViewBody, textViewPrice, textViewPrice1;
        ImageButton buttonPlus, buttonMinus;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);

            imageViewPreview = itemView.findViewById(R.id.imageViewPreview);
            textViewCount = itemView.findViewById(R.id.textViewCount);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewBody = itemView.findViewById(R.id.textViewBody);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewPrice1 = itemView.findViewById(R.id.textViewPrice1);
            buttonPlus = itemView.findViewById(R.id.buttonPlus);
            buttonMinus = itemView.findViewById(R.id.buttonMinus);
        }
    }
}
