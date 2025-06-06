package com.sigma.openfashion.data.product;

import android.content.Context;

import com.sigma.openfashion.data.SupabaseService;
import com.sigma.openfashion.data.local.AppDatabase;
import com.sigma.openfashion.data.local.ProductDao;
import com.sigma.openfashion.data.local.ProductEntity;
import com.sigma.openfashion.data.product.Gender;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * В этом классе собрана логика:
 * 1) получить из локальной БД (Room);
 * 2) если локально пусто (или нужен фреш), сделать сетевой вызов к Supabase;
 * 3) сохранить в локальную БД;
 * 4) вернуть результат через колбэки.
 */
public class ProductsRepository {

    private final ProductDao productDao;
    private final SupabaseService supabaseService;

    public interface LoadCallback {
        void onLoaded(List<ProductEntity> products);
        void onError(String errorMessage);
    }

    public ProductsRepository(Context context, SupabaseService supabaseService) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.productDao = db.productDao();
        this.supabaseService = supabaseService;
    }

    /**
     * Шаг 1: читаем из локальной базы (если записи есть → сразу возвращаем их через onLoaded).
     * Шаг 2: одновременно вызываем supabaseService.getProductsPreview и
     *         по полученному ответу обновляем локальную БД и снова возвращаем через onLoaded.
     *
     * @param categoryId  = -1 (или другой)
     * @param limit       = 4 (или другой)
     * @param offset      = 0 (или другой)
     * @param callback    колбэк, в который отправим сначала локальный список (если есть),
     *                    а потом сетевой.
     */
    public void getProducts(final int categoryId, final int limit, final int offset, final LoadCallback callback) {
        // 1) Читаем из локальной БД в фоновом потоке
        new Thread(() -> {
            List<ProductEntity> cached = productDao.getProductsPreview(limit, offset);
            if (cached != null && !cached.isEmpty()) {
                // Отправляем «локальный» результат на UI
                callback.onLoaded(cached);
            }
            // 2) Даже если локально что-то есть, всё равно будем обновлять из сети
            fetchFromNetwork(categoryId, limit, offset, callback);
        }).start();
    }

    /**
     * Вызывается в фоновом потоке (Thread в getProducts).
     * Делает сетевой запрос к Supabase, парсит JSON, сохраняет в БД и
     * отдаёт результат обратно через callback.onLoaded(...) в UI-потоке.
     */
    private void fetchFromNetwork(int categoryId, int limit, int offset, LoadCallback callback) {
        supabaseService.getProductsPreview(categoryId, limit, offset, new SupabaseService.QueryCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                // Парсим JSON в List<ProductEntity>
                try {
                    JSONArray array = new JSONArray(jsonResponse);
                    List<ProductEntity> list = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        ProductEntity p = new ProductEntity(
                                obj.getInt("id"),
                                obj.getInt("category_id"),
                                obj.getString("gender"),
                                obj.getString("name"),
                                obj.getDouble("price"),
                                obj.optString("preview_image_url", ""),
                                obj.optString("currency", "RUB")
                        );
                        list.add(p);
                    }

                    // 3) Сохраняем в локальную БД (в фоне)
                    new Thread(() -> {
                        productDao.insertAll(list);
                        // 4) Возвращаем полученные данные в UI (колбэк)
                        callback.onLoaded(list);
                    }).start();

                } catch (JSONException e) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }
}