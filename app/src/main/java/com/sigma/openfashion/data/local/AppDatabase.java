// app/src/main/java/com/sigma/openfashion/data/local/AppDatabase.java
package com.sigma.openfashion.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Единственный класс для инициализации базы Room.
 */
@Database(entities = {ProductEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "openfashion_db";

    private static volatile AppDatabase INSTANCE;

    public abstract ProductDao productDao();

    /**
     * Singleton-паттерн: если INSTANCE == null, создаём,
     * иначе возвращаем уже существующую.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            // .fallbackToDestructiveMigration() // если меняете схему, даёт пересоздание
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
