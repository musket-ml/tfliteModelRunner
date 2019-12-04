package com.onpositive.dldemos.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {TFLiteItem.class, ResultItem.class, ClassificationResultItem.class, DetectionResultItem.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ResultItemDao resultItemDao();

    public abstract ClassificationResultItemDao classificationResultItemDao();

    public abstract DetectionResultItemDao detectionResultItemDao();

    public abstract TFLiteItemDao tfLiteItemDao();
}
