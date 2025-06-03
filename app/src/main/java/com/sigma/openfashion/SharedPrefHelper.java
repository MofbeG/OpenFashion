package com.sigma.openfashion;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class SharedPrefHelper {

    private static final String PREF_NAME      = "com.sigma.openfashion.PREFS";
    private static final String KEY_JWT        = "KEY_JWT";
    private static final String KEY_USER_ID    = "KEY_USER_ID";
    private static final String KEY_USER_EMAIL = "KEY_USER_EMAIL";
    private static final String KEY_USER_PIN   = "KEY_USER_PIN";

    private static SharedPrefHelper instance;
    private final SharedPreferences prefs;

    public static void initialize(Context context) {
        if (instance == null) {
            instance = new SharedPrefHelper(context.getApplicationContext());
        }
    }

    public static SharedPrefHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefHelper(context.getApplicationContext());
        }
        return instance;
    }

    private SharedPrefHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveJwtToken(String jwt) {
        prefs.edit().putString(KEY_JWT, jwt).apply();
    }

    @Nullable
    public String getJwtToken() {
        return prefs.getString(KEY_JWT, null);
    }

    public void saveUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    @Nullable
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public void saveUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    @Nullable
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public void saveUserPin(String pin) {
        prefs.edit().putString(KEY_USER_PIN, pin).apply();
    }

    @Nullable
    public String getUserPin() {
        return prefs.getString(KEY_USER_PIN, null);
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
