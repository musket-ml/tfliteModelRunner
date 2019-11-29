package com.onpositive.dldemos.data;


import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.onpositive.dldemos.data.TypeConverters.TFLiteItemConverter;

@Entity
@TypeConverters({TFLiteItemConverter.class})
public class TFLiteItem {

    @PrimaryKey
    @NonNull
    private String tfFilePath;
    private String title;
    private boolean isAsset;
    private TFModelType modelType;
    private int size_x;
    private int size_y;
    private String labelsPath = null;

    public TFLiteItem() {
    }

    public TFLiteItem(String tfFilePath, String title, TFModelType tfModelType, int width, int height) {
        this(tfFilePath, title, false, tfModelType, width, height);
    }

    public TFLiteItem(String tfFilePath, String title, TFModelType tfModelType, int width, int height, String labelsPath) {
        this(tfFilePath, title, false, tfModelType, width, height);
        this.labelsPath = labelsPath;
    }

    @Ignore
    public TFLiteItem(String tfFilePath, String title, boolean isAsset, TFModelType tfModelType, int width, int height) {
        this.tfFilePath = tfFilePath;
        this.title = title;
        this.isAsset = isAsset;
        this.modelType = tfModelType;
        this.size_x = width;
        this.size_y = height;
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

    public TFModelType getModelType() {
        return modelType;
    }

    public void setModelType(TFModelType modelType) {
        this.modelType = modelType;
    }

    public int getSize_x() {
        return size_x;
    }

    public void setSize_x(int size_x) {
        this.size_x = size_x;
    }

    public int getSize_y() {
        return size_y;
    }

    public void setSize_y(int size_y) {
        this.size_y = size_y;
    }

    public String getLabelsPath() {
        return labelsPath;
    }

    public void setLabelsPath(String labelsPath) {
        this.labelsPath = labelsPath;
    }
}
