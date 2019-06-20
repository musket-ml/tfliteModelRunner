package com.onpositive.dldemos.data;


import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class TFLiteItem {

    @PrimaryKey
    @NonNull
    private String tfFilePath;
    private String title;
    private boolean isAsset;

    public TFLiteItem(String tfFilePath, String title) {
        this(tfFilePath, title, false);
    }

    @Ignore
    public TFLiteItem(String tfFilePath, String title, boolean isAsset) {
        this.tfFilePath = tfFilePath;
        this.title = title;
        this.isAsset = isAsset;
    }

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

    public boolean isAsset() {
        return isAsset;
    }

    public void setAsset(boolean asset) {
        isAsset = asset;
    }
}
