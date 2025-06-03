package com.sigma.openfashion.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * SupabaseService: все запросы к Supabase REST API с помощью OkHttp.
 * Поддерживает таблицы: profiles, categories, products, cart_items, orders, order_items.
 * Также проверка сетевого состояния и доступности сервера.
 */
public class SupabaseService {

    private static final String SUPABASE_URL = "https://bdeeqmuzkarbifypuniz.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJkZWVxbXV6a2FyYmlmeXB1bml6Iiwicm9sZSI6ImFub3giLCJpYXQiOjE3NDg4ODY1ODgsImV4cCI6MjA2NDQ2MjU4OH0.ZUXI8b2UiiDUBzkqky4TpMRv0e_JzBB6yHIX4ZOX-1A";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private String jwtToken; // Bearer token для авторизованных запросов

    public SupabaseService() {
        client = new OkHttpClient();
    }

    /**
     * Установить JWT (после успешного входа), чтобы включать Authorization заголовок.
     */
    public void setJwtToken(@Nullable String jwtToken) {
        this.jwtToken = jwtToken;
    }

    /**
     * Очистить сохранённый JWT (после выхода).
     */
    public void clearJwtToken() {
        this.jwtToken = null;
    }

    /**
     * Проверка наличия интернет‑соединения.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    /**
     * Проверка доступности Supabase сервера.
     * Выполняет GET /rest/v1/categories?select=id&limit=1.
     */
    public void checkServerConnection(QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/categories?select=id&limit=1";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("OK");
                } else {
                    callback.onError("Server error: " + response.code());
                }
                response.close();
            }
        });
    }

    /* ==================== PROFILES ==================== */

    /**
     * Получить профиль текущего пользователя.
     * Применяется RLS, поэтому JWT должен быть установлен.
     */
    public void getProfile(String userId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/profiles?id=eq.%s&select=*", userId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Создать или обновить профиль (UPSERT).
     * При выполнении сначала проверяется, есть ли профиль: если нет — вставка, иначе — обновление.
     * Используется POST с заголовком Prefer: return=representation для выполнения UPSERT.
     */
    public void upsertProfile(String userId, String name, String phone, String address, QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/profiles";
        JSONObject json = new JSONObject();
        try {
            json.put("id", userId);
            json.put("name", name);
            json.put("phone", phone);
            json.put("address", address);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getAuthHeadersBuilder()
                        .add("Prefer", "resolution=merge-duplicates") // UPSERT
                        .build())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== CATEGORIES ==================== */

    /**
     * Получить все категории.
     */
    public void getCategories(QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/categories?select=*";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== PRODUCTS ==================== */

    /**
     * Получить все товары.
     */
    public void getAllProducts(QueryCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/products?select=*";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Получить товары по категории.
     */
    public void getProductsByCategory(int categoryId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/products?category_id=eq.%d&select=*", categoryId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Получить один товар по ID.
     */
    public void getProduct(int productId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/products?id=eq.%d&select=*", productId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getCommonHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== CART ITEMS ==================== */

    /**
     * Получить все элементы корзины для пользователя.
     */
    public void getCartItems(String userId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/cart_items?user_id=eq.%s&select=*", userId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Добавить элемент в корзину.
     */
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
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Обновить количество товара в корзине.
     */
    public void updateCartItemQuantity(int cartItemId, int newQuantity, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/cart_items?id=eq.%d", cartItemId);
        JSONObject json = new JSONObject();
        try {
            json.put("quantity", newQuantity);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Удалить элемент из корзины по ID.
     */
    public void deleteCartItem(int cartItemId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/cart_items?id=eq.%d", cartItemId);
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== ORDERS ==================== */

    /**
     * Создать новый заказ (orders).
     * Затем нужно отдельно добавлять позиции (order_items).
     */
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
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Получить все заказы пользователя.
     */
    public void getOrders(String userId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/orders?user_id=eq.%s&select=*", userId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Обновить статус заказа.
     */
    public void updateOrderStatus(int orderId, String newStatus, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/orders?id=eq.%d", orderId);
        JSONObject json = new JSONObject();
        try {
            json.put("status", newStatus);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== ORDER ITEMS ==================== */

    /**
     * Добавить позицию к заказу.
     */
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
            callback.onError("JSON error: " + e.getMessage());
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /**
     * Получить все позиции конкретного заказа.
     */
    public void getOrderItems(int orderId, QueryCallback callback) {
        String url = String.format(Locale.US,
                SUPABASE_URL + "/rest/v1/order_items?order_id=eq.%d&select=*", orderId);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(getAuthHeaders())
                .build();
        client.newCall(request).enqueue(openCallback(callback));
    }

    /* ==================== HELPERS ==================== */

    /**
     * Общие заголовки для запросов (только apikey).
     */
    private okhttp3.Headers getCommonHeaders() {
        return new okhttp3.Headers.Builder()
                .add("apikey", API_KEY)
                .add("Accept", "application/json")
                .build();
    }

    /**
     * Заголовки для авторизованных запросов (apikey + Authorization).
     */
    private okhttp3.Headers getAuthHeaders() {
        okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder()
                .add("apikey", API_KEY)
                .add("Accept", "application/json");
        if (jwtToken != null && !jwtToken.isEmpty()) {
            builder.add("Authorization", "Bearer " + jwtToken);
        }
        return builder.build();
    }

    /**
     * Заголовки Builder для модификаций (например, добавление Prefer).
     */
    private okhttp3.Headers.Builder getAuthHeadersBuilder() {
        okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder()
                .add("apikey", API_KEY)
                .add("Accept", "application/json");
        if (jwtToken != null && !jwtToken.isEmpty()) {
            builder.add("Authorization", "Bearer " + jwtToken);
        }
        return builder;
    }

    /**
     * Универсальный Callback-обработчик для OkHttp, отдающий результат в QueryCallback.
     */
    private Callback openCallback(final QueryCallback callback) {
        return new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful()) {
                        String body = response.body() != null ? response.body().string() : "";
                        callback.onSuccess(body);
                    } else {
                        callback.onError("HTTP " + response.code() + ": " + response.message());
                    }
                } catch (IOException e) {
                    callback.onError(e.getMessage());
                } finally {
                    response.close();
                }
            }
        };
    }

    /**
     * Callback-интерфейс для получения ответа сервера.
     */
    public interface QueryCallback {
        void onSuccess(String jsonResponse);
        void onError(String errorMessage);
    }
}
