package com.onpositive.dldemos.data;

import androidx.room.Entity;
import androidx.room.TypeConverters;

import com.onpositive.dldemos.data.TypeConverters.DetectionResultItemConverter;
import com.onpositive.dldemos.data.TypeConverters.ResultItemConverter;
import com.onpositive.dldemos.detection.ImageDetector;

import java.util.Collections;
import java.util.List;

@Entity
@TypeConverters({DetectionResultItemConverter.class, ResultItemConverter.class})
public class DetectionResultItem extends ResultItem {

    private List<ImageDetector.ObjectDetection> recognitionResultList;

    public DetectionResultItem() {
    }

    public DetectionResultItem(String filePath, ContentType ct, String thumbnailPath) {
        super(filePath, ct, thumbnailPath);
    }

    public List<ImageDetector.ObjectDetection> getRecognitionResultList() {
        Collections.sort(recognitionResultList);
        Collections.reverse(recognitionResultList);
        return this.recognitionResultList;
    }

    public void setRecognitionResultList(List<ImageDetector.ObjectDetection> recognitionResultList) {
        this.recognitionResultList = recognitionResultList;
    }
}
