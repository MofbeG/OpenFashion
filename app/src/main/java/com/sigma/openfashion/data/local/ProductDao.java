// app/src/main/java/com/sigma/openfashion/data/local/ProductDao.java
package com.sigma.openfashion.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ProductDao {

    @Query("SELECT * FROM products ORDER BY id LIMIT :limit OFFSET :offset")
    List<ProductEntity> getProductsPreview(int limit, int offset);
    @Query("SELECT * FROM products WHERE " +
            "(:categoryId = -1 OR categoryId = :categoryId) AND " +
            "(:gender IS NULL OR gender = :gender) AND " +
            "(:query IS NULL OR name LIKE '%' || :query || '%') " +
            "ORDER BY id LIMIT :limit OFFSET :offset")
    List<ProductEntity> searchProducts(
            int categoryId,
            String gender,
            String query,
            int limit,
            int offset
    );

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductEntity> products);

    /**
     * Полностью очищает таблицу (если нужно обновить весь кэш).
     */
    @Query("DELETE FROM products")
    void clearAll();

    /**
     * Удаляет продукты по списку ID.
     */
    @Query("DELETE FROM products WHERE id IN (:ids)")
    void deleteByIds(List<Integer> ids);
}