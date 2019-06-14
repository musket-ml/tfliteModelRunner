package com.onpositive.dldemos.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface ResultItemDao {
    @Query("SELECT * FROM ResultItem")
    List<ResultItem> getAll();

    @Query("SELECT * FROM ResultItem WHERE tfLiteParentFile = :parentTFfile")
    List<ResultItem> getAllByParentTF(String parentTFfile);

    @Query("SELECT * FROM ResultItem WHERE filePath = :filePath")
    ResultItem getById(long filePath);

    @Insert
    void insert(ResultItem resultItem);

    @Update
    void update(ResultItem resultItem);

    @Delete
    void delete(ResultItem resultItem);
}
