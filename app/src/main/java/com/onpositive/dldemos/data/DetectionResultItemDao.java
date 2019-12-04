package com.onpositive.dldemos.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DetectionResultItemDao {
    @Query("SELECT * FROM DetectionResultItem")
    List<DetectionResultItem> getAll();

    @Query("SELECT * FROM DetectionResultItem WHERE tfLiteParentFile = :parentTFfile")
    List<DetectionResultItem> getAllByParentTF(String parentTFfile);

    @Query("SELECT * FROM DetectionResultItem WHERE filePath = :filePath")
    DetectionResultItem getById(long filePath);

    @Insert
    void insert(DetectionResultItem detectionResultItem);

    @Update
    void update(DetectionResultItem detectionResultItem);

    @Delete
    void delete(DetectionResultItem detectionResultItem);
}
