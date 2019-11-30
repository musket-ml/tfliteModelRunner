package com.onpositive.dldemos.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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
