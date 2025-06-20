package com.sigma.openfashion.ui.order;

import android.icu.text.DecimalFormat;

public class CartItem {
    private int id;
    private int productId;
    private String userId;
    private int quantity;
    private String selectedSize;
    private String selectedColor;
    private String addedAt;
    private String updatedAt;
    private String productTitle;
    private double price;
    private String currency;
    private String imageUrl;

    public CartItem(int id, int productId, String userId, int quantity,
                    String selectedSize, String selectedColor,
                    String addedAt, String updatedAt,
                    String productTitle, double price, String currency, String imageUrl) {
        this.id = id;
        this.productId = productId;
        this.userId = userId;
        this.quantity = quantity;
        this.selectedSize = selectedSize;
        this.selectedColor = selectedColor;
        this.addedAt = addedAt;
        this.updatedAt = updatedAt;
        this.productTitle = productTitle;
        this.price = price;
        this.currency = currency;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public int getProductId() { return productId; }
    public String getUserId() { return userId; }
    public int getQuantity() { return quantity; }
    public String getSelectedSize() { return selectedSize; }
    public String getSelectedColor() { return selectedColor; }
    public String getAddedAt() { return addedAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getProductTitle() { return productTitle; }
    public String getPriceStr() {
        DecimalFormat df = new DecimalFormat("#0.00");
        return df.format(this.price) + " " + this.currency;
    }
    public double getPrice() {
        return this.price;
    }
    public String getCurrency() {
        return this.currency;
    }
    public String getImageUrl() { return imageUrl; }
}