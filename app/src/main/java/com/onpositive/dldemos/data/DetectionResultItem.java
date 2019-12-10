package com.onpositive.dldemos.data;

import androidx.room.Entity;
import androidx.room.TypeConverters;

import com.onpositive.dldemos.data.TypeConverters.ResultItemConverter;

@Entity
@TypeConverters({ResultItemConverter.class})
public class DetectionResultItem extends ResultItem {

    public DetectionResultItem() {
    }

    public DetectionResultItem(String filePath, ContentType ct, String thumbnailPath) {
        super(filePath, ct, thumbnailPath);
    }
}
