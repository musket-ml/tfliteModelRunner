package com.onpositive.dldemos.data;


import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class TFLiteItem {

    public TFLiteItem(String tfFilePath, String title) {
        this.tfFilePath = tfFilePath;
        this.title = title;
    }

    @PrimaryKey
    @NonNull
    private String tfFilePath;
    private String title;

    public String getTfFilePath() {
        return tfFilePath;
    }

    public void setTfFilePath(String tfFilePath) {
        this.tfFilePath = tfFilePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
