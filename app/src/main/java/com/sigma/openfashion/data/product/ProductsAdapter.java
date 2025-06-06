package com.sigma.openfashion.data.product;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sigma.openfashion.R;

import java.util.List;

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private List<Product> products;
    private OnProductClickListener listener;

    public ProductsAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPreview;
        TextView textViewTitle;
        TextView textViewPrice;

        public ProductViewHolder(View itemView) {
            super(itemView);
            imageViewPreview = itemView.findViewById(R.id.imageViewPreview);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
        }

        public void bind(Product product, OnProductClickListener listener) {
            textViewTitle.setText(product.name);
            textViewPrice.setText(product.getPriceStr());

            Glide.with(imageViewPreview.getContext())
                    .load(product.previewImageUrl)
                    .placeholder(R.color.Body)
                    .into(imageViewPreview);

            itemView.setOnClickListener(v -> listener.onProductClick(product));
        }
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_item, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(products.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }
}

