package com.onpositive.dldemos.data.TypeConverters;

import androidx.room.TypeConverter;

import com.onpositive.dldemos.data.TFModelType;

public class TFLiteItemConverter {

    @TypeConverter
    public String fromModelType(TFModelType modelType) {
        switch (modelType) {
            case SEGMENTATION:
                return "SEGMENTATION";
            case CLASSIFICATION:
                return "CLASSIFICATION";
        }
        return null;
    }

    @TypeConverter
    public TFModelType toModelType(String modelType) {
        switch (modelType) {
            case "SEGMENTATION":
                return TFModelType.SEGMENTATION;
            case "CLASSIFICATION":
                return TFModelType.CLASSIFICATION;
        }
        return null;
    }
}
