package com.onpositive.dldemos.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface TFLiteItemDao {
    @Query("SELECT * FROM TFLiteItem")
    List<TFLiteItem> getAll();

    @Query("SELECT * FROM TFLiteItem WHERE tfFilePath = :filePath")
    TFLiteItem getById(long filePath);

    @Insert
    void insert(TFLiteItem tfLiteItem);

    @Update
    void update(TFLiteItem tfLiteItem);

    @Delete
    void delete(TFLiteItem tfLiteItem);
}
