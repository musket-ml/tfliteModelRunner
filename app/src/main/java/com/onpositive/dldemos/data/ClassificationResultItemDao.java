package com.onpositive.dldemos.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface ClassificationResultItemDao {
    @Query("SELECT * FROM ClassificationResultItem")
    List<ClassificationResultItem> getAll();

    @Query("SELECT * FROM ClassificationResultItem WHERE tfLiteParentFile = :parentTFfile")
    List<ClassificationResultItem> getAllByParentTF(String parentTFfile);

    @Query("SELECT * FROM ClassificationResultItem WHERE filePath = :filePath")
    ClassificationResultItem getById(long filePath);

    @Insert
    void insert(ClassificationResultItem classificationResultItem);

    @Update
    void update(ClassificationResultItem classificationResultItem);

    @Delete
    void delete(ClassificationResultItem classificationResultItem);
}
