package com.sigma.openfashion;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * SharedPrefHelper: хранение/чтение JWT‑токена и user_id в SharedPreferences.
 */
public class SharedPrefHelper {
    private static final String PREF_NAME = "com.sigma.openfashion.PREFS";
    private static final String KEY_JWT     = "KEY_JWT";
    private static final String KEY_USER_ID = "KEY_USER_ID";

    private static SharedPrefHelper instance;
    private final SharedPreferences prefs;

    /** Вызывается один раз в MyApplication.onCreate() */
    public static void initialize(Context context) {
        if (instance == null) {
            instance = new SharedPrefHelper(context.getApplicationContext());
        }
    }

    /** Получить экземпляр */
    public static SharedPrefHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefHelper(context.getApplicationContext());
        }
        return instance;
    }

    private SharedPrefHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Сохранить JWT (после входа) */
    public void saveJwtToken(String jwt) {
        prefs.edit().putString(KEY_JWT, jwt).apply();
    }

    /** Получить JWT или null */
    @Nullable
    public String getJwtToken() {
        return prefs.getString(KEY_JWT, null);
    }

    /** Удалить JWT */
    public void clearJwtToken() {
        prefs.edit().remove(KEY_JWT).apply();
    }

    /** Сохранить userId (UUID, после регистрации или входа) */
    public void saveUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    /** Получить userId или null */
    @Nullable
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /** Удалить userId */
    public void clearUserId() {
        prefs.edit().remove(KEY_USER_ID).apply();
    }
}