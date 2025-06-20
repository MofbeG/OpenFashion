package com.sigma.openfashion.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sigma.openfashion.data.product.Gender;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * SupabaseService: все запросы к Supabase REST API с помощью OkHttp.
 * Добавлены логи для отладки (Logcat).
 */
public class SupabaseService {

    private static final String TAG = "SupabaseService";
    private static final String SUPABASE_URL = "https://hufhfmqquczxpxpmtzbf.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh1ZmhmbXFxdWN6eHB4cG10emJmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg5NjMwMjIsImV4cCI6MjA2NDUzOTAyMn0.yNIiBC7Xx4Jz-b-XqdCPPUqlHP0ZGy2bgog8tabaA-k";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private String jwtToken; // Bearer token для авторизованных запросов

    public SupabaseService() {
        client = new OkHttpClient();
    }

    /** Установить JWT (после успешного входа), чтобы включать Authorization заголовок */
    public void setJwtToken(@Nullable String jwtToken) {
        this.jwtToken = jwtToken;
        Log.d(TAG, "JWT token set: " + (jwtToken != null ? "[REDACTED]" : "null"));
    }

    /** Очистить сохранённый JWT (после выхода) */
    public void clearJwtToken() {
        this.jwtToken = null;
        Log.d(TAG, "JWT token cleared");
    }

    /** Проверка наличия интернет‑соединения */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Log.d(TAG, "ConnectivityManager is null");
            return false;
        }
        NetworkInfo ni = cm.getActiveNetworkInfo();
        boolean available = ni != null && ni.isConnected();
        Log.d(TAG, "Network available: " + available);
        return available;
    }

    /**
     * Проверка доступности Supabase сервера.
     * Выполняет GET /rest/v1/categories?select=id&limit=1.
     */
    public void checkServerConnection(QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/categories?select=id&limit=1";
        Log.d(TAG, "checkServerConnection URL: " + url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * JWT Token Update
     * Выполняет POST /auth/v1/token?grant_type=refresh_token
     */
    public void refreshSession(String refreshToken, QueryCallback callback) {
        String url = SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token";
        JSONObject json = new JSONObject();
        try {
            json.put("refresh_token", refreshToken);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** 1. Регистрация **/
    public void signUp(String email, String password, QueryCallback callback) {
        String url = SUPABASE_URL + "/auth/v1/signup";
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** 2. Вход **/
    public void signIn(String email, String password, QueryCallback callback) {
        String url = SUPABASE_URL + "/auth/v1/token?grant_type=password";
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Вход JWT **/
    public void signIn(QueryCallback callback) {
        String url = SUPABASE_URL + "/auth/v1/user";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Выход **/
    public void logout(String refreshToken, QueryCallback callback) {
        String url = SUPABASE_URL + "/auth/v1/logout";
        JSONObject json = new JSONObject();
        try {
            json.put("refresh_token", refreshToken);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** 3. Запрос OTP‑кода для восстановления пароля **/
    public void requestOtp(String email, QueryCallback callback) {
        // Предполагаем, что на сервере заведён таблица otp_requests или Edge Function
        String url = SUPABASE_URL + "/auth/v1/otp";
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** 4. Проверка OTP‑кода **/
    public void verifyOtp(String email, String otpCode, QueryCallback callback) {
        String url = SUPABASE_URL + "/auth/v1/verify";
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("token", otpCode);
            json.put("type", "email");
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** 5. Сброс пароля после подтверждения OTP **/
    public void resetPassword(String newPassword, QueryCallback callback) {
        String url = SUPABASE_URL + "/auth/v1/user";
        JSONObject json = new JSONObject();
        try {
            json.put("password", newPassword);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** 6. Загрузка аватара в Supabase Storage **/
    public void uploadAvatar(String userId, byte[] imageData, String mimeType, QueryCallback callback) {
        // Пример: в Supabase Storage есть бакет "avatars" с публичным доступом
        // Пользовательский файл кладём по пути "avatars/<userId>.jpg"
        String url = SUPABASE_URL + "/storage/v1/object/avatars/" + userId + ".jpg";
        RequestBody body = RequestBody.create(imageData, MediaType.get(mimeType));
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + jwtToken)
                .addHeader("x-upsert", "true")
                .build();
        logRequest(request);
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError("Upload failed: " + e.getMessage());
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (response.isSuccessful()) {
                        callback.onSuccess("URL: https://bdeeqmuzkarbifypuniz.supabase.co/storage/v1/object/public/avatars/" + userId + ".jpg");
                    } else {
                        callback.onError("Upload error: " + response.code() + ": " + response.message());
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    /* ==================== PROFILES ==================== */

    /** Получить профиль текущего пользователя. */
    public void getProfile(String userId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/profiles?id=eq.%s&select=*", userId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Создать или обновить профиль (UPSERT). */
    public void upsertProfile(String userId, String first_name, String last_name, String email, String phone, String address, QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/profiles";
        JSONObject json = new JSONObject();
        try {
            json.put("id", userId);
            json.put("first_name", first_name);
            json.put("last_name", last_name);
            json.put("email", email);
            json.put("phone", phone);
            json.put("address", address);
        } catch (JSONException e) {
            Log.e(TAG, "upsertProfile JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        String bodyStr = json.toString();
        Log.d(TAG, "upsertProfile payload: " + bodyStr);
        RequestBody body = RequestBody.create(bodyStr, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getAuthHeadersBuilder()
                        .add("Content-Type", "application/json")
                        .add("Prefer", "resolution=merge-duplicates")
                        .build())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== CATEGORIES ==================== */

    /** Получить все категории. */
    public void getCategories(QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/categories?select=*";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    public void getCategoryById(int id, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/categories?id=eq.%d&select=*", id);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== PRODUCTS ==================== */

    /**
     * Получить товары с возможностью фильтрации по категории и постраничной загрузкой.
     *
     * @param categoryId ID категории (или -1 если не фильтровать)
     * @param limit Кол-во товаров на странице
     * @param offset Смещение для постраничной загрузки
     * @param callback Обработчик результата
     */
    public void getProducts(Integer categoryId, Integer limit, Integer offset, QueryCallback callback) {
        StringBuilder urlBuilder = new StringBuilder(SUPABASE_URL + "/rest/v1/products?select=*");

        if (categoryId != null && categoryId >= 0) {
            urlBuilder.append("&category_id=eq.").append(categoryId);
        }

        if (limit != null) {
            urlBuilder.append("&limit=").append(limit);
        }

        if (offset != null) {
            urlBuilder.append("&offset=").append(offset);
        }

        String url = urlBuilder.toString();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();

        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Получить список товаров с превью (id, category_id, gender, name, price, currency, preview_image).
     *
     * @param categoryId ID категории (или null если не фильтровать)
     * @param gender Гендер male, female, unisex (или null если не фильтровать)
     * @param limit Кол-во товаров (можно null)
     * @param offset Смещение (можно null)
     * @param callback Обработчик результата
     */
    public void getProductsPreview(Integer categoryId, Gender gender, Integer limit, Integer offset, QueryCallback callback) {
        StringBuilder urlBuilder = new StringBuilder(SUPABASE_URL + "/rest/v1/products?select=id,category_id,gender,name,price,currency,preview_image_url");

        if (categoryId != null && categoryId >= 0) {
            urlBuilder.append("&category_id=eq.").append(categoryId);
        }

        if (gender != null) {
            urlBuilder.append("&gender=eq.").append(gender.name().toLowerCase());
        }

        if (limit != null) {
            urlBuilder.append("&limit=").append(limit);
        }

        if (offset != null) {
            urlBuilder.append("&offset=").append(offset);
        }

        String url = urlBuilder.toString();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();

        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    public void getProductsPreviewWithCount(
            Integer categoryId,
            Gender gender,
            Integer limit,
            Integer offset,
            QueryCallbackWithCount callback
    ) {
        StringBuilder urlBuilder = new StringBuilder(SUPABASE_URL + "/rest/v1/products?select=id,category_id,gender,name,price,currency,preview_image_url");


        if (categoryId != null && categoryId >= 0) {
            urlBuilder.append("&category_id=eq.").append(categoryId);
        }

        if (gender != null) {
            urlBuilder.append("&gender=eq.").append(gender.name().toLowerCase());
        }

        if (limit != null) {
            urlBuilder.append("&limit=").append(limit);
        }

        if (offset != null) {
            urlBuilder.append("&offset=").append(offset);
        }

        String url = urlBuilder.toString();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .addHeader("Prefer", "count=exact")
                .build();

        logRequest(request);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (response) {
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        Log.d(TAG, "Response body: " + json);
                        int totalCount = 0;

                        // Извлекаем общее количество из заголовка
                        String contentRange = response.header("Content-Range");
                        if (contentRange != null) {
                            String[] parts = contentRange.split("/");
                            if (parts.length > 1) {
                                try {
                                    totalCount = Integer.parseInt(parts[1]);
                                } catch (NumberFormatException e) {
                                    // Оставляем 0
                                }
                            }
                        }

                        callback.onSuccess(json, totalCount);
                    } else {
                        callback.onError(response.message());
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading response: " + e.getMessage());
                    callback.onError(e.getMessage());
                }

            }
        });
    }

    public interface QueryCallbackWithCount {
        void onSuccess(String jsonResponse, int totalCount);
        void onError(String errorMessage);
    }

    /** Получить один товар по ID. */
    public void getProduct(int productId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/products?id=eq.%d&select=*", productId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Получить изображения по товар ID. */
    public void getProductImages(int productId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/product_images?product_id=eq.%d&select=image_url", productId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== CART ITEMS ==================== */

    /** Получить все элементы корзины для пользователя. */
    public void getCartItems(String userId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/cart_items?user_id=eq.%s&select=*,product:products(id,name,price,currency,preview_image_url)", userId);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();

        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Получить все элементы корзины для пользователя. */
    public void isExistCartItem(String userId, int productId,
                                String selectedSize, String selectedColor,
                                QueryCallback callback) {
        // Добавляем все условия в URL как параметры фильтрации
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/cart_items?" +
                        "user_id=eq.%s&" +
                        "product_id=eq.%d&" +
                        "selected_size=eq.%s&" +
                        "selected_color=eq.%s&" +
                        "select=*",
                userId, productId, selectedSize, selectedColor);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();

        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Добавить элемент в корзину. */
    public void addCartItem(String userId, int productId,
                            String selectedSize, String selectedColor, int quantity,
                            QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/cart_items";
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("product_id", productId);
            json.put("selected_size", selectedSize);
            json.put("selected_color", selectedColor);
            json.put("quantity", quantity);
        } catch (JSONException e) {
            Log.e(TAG, "addCartItem JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        String bodyStr = json.toString();
        RequestBody body = RequestBody.create(bodyStr, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Обновить количество товара в корзине. */
    public void updateCartItemQuantity(int cartItemId, int newQuantity, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/cart_items?id=eq.%d", cartItemId);
        JSONObject json = new JSONObject();
        try {
            json.put("quantity", newQuantity);
        } catch (JSONException e) {
            Log.e(TAG, "updateCartItemQuantity JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        String bodyStr = json.toString();
        Log.d(TAG, "updateCartItemQuantity payload: " + bodyStr);
        RequestBody body = RequestBody.create(bodyStr, JSON);
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Удалить элемент из корзины по ID. */
    public void deleteCartItem(int cartItemId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/cart_items?id=eq.%d", cartItemId);
        Log.d(TAG, "deleteCartItem URL: " + url);
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== ORDERS ==================== */

    /** Создать новый заказ (orders). */
    public void createOrder(String userId,
                            String address,
                            String deliveryType,
                            String paymentType,
                            double totalAmount,
                            String currency,
                            QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/orders";
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("address", address);
            json.put("delivery_type", deliveryType);
            json.put("payment_type", paymentType);
            json.put("total_amount", totalAmount);
            json.put("currency", currency);
        } catch (JSONException e) {
            Log.e(TAG, "createOrder JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        String bodyStr = json.toString();
        Log.d(TAG, "createOrder payload: " + bodyStr);
        RequestBody body = RequestBody.create(bodyStr, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Получить все заказы пользователя. */
    public void getOrders(String userId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/orders?user_id=eq.%s&select=*", userId);
        Log.d(TAG, "getOrders URL: " + url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Обновить статус заказа. */
    public void updateOrderStatus(int orderId, String newStatus, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/orders?id=eq.%d", orderId);
        JSONObject json = new JSONObject();
        try {
            json.put("status", newStatus);
        } catch (JSONException e) {
            Log.e(TAG, "updateOrderStatus JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        String bodyStr = json.toString();
        Log.d(TAG, "updateOrderStatus payload: " + bodyStr);
        RequestBody body = RequestBody.create(bodyStr, JSON);
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== ORDER ITEMS ==================== */

    /** Добавить позицию к заказу. */
    public void addOrderItem(int orderId,
                             int productId,
                             int quantity,
                             String selectedSize,
                             String selectedColor,
                             double priceAtOrder,
                             QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/order_items";
        JSONObject json = new JSONObject();
        try {
            json.put("order_id", orderId);
            json.put("product_id", productId);
            json.put("quantity", quantity);
            json.put("selected_size", selectedSize);
            json.put("selected_color", selectedColor);
            json.put("price_at_order", priceAtOrder);
        } catch (JSONException e) {
            Log.e(TAG, "addOrderItem JSON error: " + e.getMessage());
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        String bodyStr = json.toString();
        Log.d(TAG, "addOrderItem payload: " + bodyStr);
        RequestBody body = RequestBody.create(bodyStr, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /** Получить все позиции конкретного заказа. */
    public void getOrderItems(int orderId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/order_items?order_id=eq.%d&select=*", orderId);
        Log.d(TAG, "getOrderItems URL: " + url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();
        logRequest(request);
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== HELPERS ==================== */

    /** Общие заголовки для запросов (только apikey). */
    private Headers getCommonHeaders() {
        Headers headers = new Headers.Builder()
                .add("apikey", API_KEY)
                .add("Accept", "application/json")
                .build();
        Log.d(TAG, "Common headers: " + headers);
        return headers;
    }

    /** Заголовки для авторизованных запросов (apikey + Authorization). */
    private Headers getAuthHeaders() {
        Headers.Builder builder = new Headers.Builder()
                .add("apikey", API_KEY)
                .add("Accept", "application/json");
        if (jwtToken != null && !jwtToken.isEmpty()) {
            builder.add("Authorization", "Bearer " + jwtToken);
            builder.add("Content-Type", "application/json");
        }
        Headers headers = builder.build();
        Log.d(TAG, "Auth headers: " + headers);
        return headers;
    }

    /** Заголовки Builder для модификаций (например, добавление Prefer). */
    private Headers.Builder getAuthHeadersBuilder() {
        Headers.Builder builder = new Headers.Builder()
                .add("apikey", API_KEY)
                .add("Accept", "application/json");
        if (jwtToken != null && !jwtToken.isEmpty()) {
            builder.add("Authorization", "Bearer " + jwtToken);
        }
        return builder;
    }

    /** Универсальный Callback-обработчик для OkHttp, отдающий результат в QueryCallback. */
    private Callback openCallback(final QueryCallback callback) {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    Log.d(TAG, "Response received: code=" + response.code());
                    if (response.isSuccessful()) {
                        String body = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "Response body: " + body);
                        callback.onSuccess(body);
                    } else {
                        String errorMsg = "HTTP " + response.code() + ": " + response.message();
                        Log.e(TAG, errorMsg);
                        callback.onError(errorMsg);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading response: " + e.getMessage());
                    callback.onError(e.getMessage());
                }
            }
        };
    }

    /** Логирование запроса (URL, метод, заголовки, тело) */
    private void logRequest(Request request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request → ")
                .append(request.method())
                .append(" ")
                .append(request.url())
                .append("\nHeaders: ").append(request.headers());

        if (request.body() != null) {
            try {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                sb.append("\nBody: ").append(buffer.readUtf8());
            } catch (IOException ignored) {
            }
        }
        Log.d(TAG, sb.toString());
    }

    /** Callback-интерфейс для получения ответа сервера. */
    public interface QueryCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }
}
