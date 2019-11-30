package com.onpositive.dldemos.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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
