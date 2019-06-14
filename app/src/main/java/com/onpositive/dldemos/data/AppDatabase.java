package com.onpositive.dldemos.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {TFLiteItem.class, ResultItem.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ResultItemDao resultItemDao();

    public abstract TFLiteItemDao tfLiteItemDao();
}
