// app/src/main/java/com/sigma/openfashion/data/local/ProductEntity.java
package com.sigma.openfashion.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {

    @PrimaryKey
    private int id;

    private int categoryId;

    @NonNull
    private String gender;

    @NonNull
    private String name;

    private double price;

    @NonNull
    private String previewImageUrl;

    @NonNull
    private String currency;

    public ProductEntity(int id,
                         int categoryId,
                         @NonNull String gender,
                         @NonNull String name,
                         double price,
                         @NonNull String previewImageUrl,
                         @NonNull String currency) {
        this.id = id;
        this.categoryId = categoryId;
        this.gender = gender;
        this.name = name;
        this.price = price;
        this.previewImageUrl = previewImageUrl;
        this.currency = currency;
    }

    // Геттеры/сеттеры (либо только геттеры, если не планируете менять поля)

    public int getId() {
        return id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    @NonNull
    public String getGender() {
        return gender;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    @NonNull
    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    @NonNull
    public String getCurrency() {
        return currency;
    }
}
