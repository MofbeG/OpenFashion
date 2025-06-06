package com.sigma.openfashion.data.product;

import com.google.gson.annotations.SerializedName;

public enum Gender {
    @SerializedName("male")
    MALE,

    @SerializedName("female")
    FEMALE,

    @SerializedName("unisex")
    UNISEX;

    public static Gender fromString(String value) {
        for (Gender gender : Gender.values()) {
            if (gender.name().equalsIgnoreCase(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Неизвестный гендер: " + value);
    }
}
