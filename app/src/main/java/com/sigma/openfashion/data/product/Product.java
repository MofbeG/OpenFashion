package com.sigma.openfashion.data.product;

import android.icu.text.DecimalFormat;

public class Product {
    public int id;
    public int category;
    public String name;
    public double price;
    public String previewImageUrl;
    public String currency;
    public String description;
    private Gender gender;

    public Product(int id, int category, Gender gender, String name, double price, String previewImageUrl, String currency, String description) {
        this.id = id;
        this.category = category;
        this.gender = gender;
        this.name = name;
        this.price = price;
        this.previewImageUrl = previewImageUrl;
        this.currency = currency;
        this.description = description;
    }

    public String getGenderStr() {
        switch (gender){
            case MALE:
                return "муж";
            case FEMALE:
                return "жен";
            default:
                return "унисекс";
        }
    }
    public String getPriceStr() {
        DecimalFormat df = new DecimalFormat("#0.00");
        return df.format(this.price) + " " + this.currency;
    }
}