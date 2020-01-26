package com.onpositive.dldemos.data;

import androidx.room.Entity;
import androidx.room.TypeConverters;

import com.onpositive.dldemos.data.TypeConverters.ClassificationResultItemConverter;
import com.onpositive.dldemos.data.TypeConverters.ResultItemConverter;
import com.onpositive.dldemos.interpreter.ImageClassifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@TypeConverters({ClassificationResultItemConverter.class, ResultItemConverter.class})
public class ClassificationResultItem extends ResultItem {

    private List<ImageClassifier.Prediction> predictionResultList = new ArrayList<>();

    public ClassificationResultItem() {
    }

    public ClassificationResultItem(String filePath, ContentType ct, String thumbnailPath) {
        super(filePath, ct, thumbnailPath);
    }

    public List<ImageClassifier.Prediction> getPredictionResultList() {
        Collections.sort(predictionResultList);
        Collections.reverse(predictionResultList);
        return predictionResultList;
    }

    public void setPredictionResultList(List<ImageClassifier.Prediction> predictionResultList) {
        this.predictionResultList = predictionResultList;
    }
}
