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
            case OBJECT_DETECTION:
                return "OBJECT_DETECTION";
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
            case "OBJECT_DETECTION":
                return TFModelType.OBJECT_DETECTION;
        }
        return null;
    }
}
