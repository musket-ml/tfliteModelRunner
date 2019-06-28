package com.onpositive.dldemos.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.TypeConverters;

import com.onpositive.dldemos.classification.ImageClassifier;
import com.onpositive.dldemos.data.TypeConverters.ClassificationResultItemConverter;
import com.onpositive.dldemos.data.TypeConverters.ResultItemConverter;

import java.util.ArrayList;
import java.util.List;

@Entity
@TypeConverters({ClassificationResultItemConverter.class, ResultItemConverter.class})
public class ClassificationResultItem extends ResultItem {

    private List<ImageClassifier.Classification> classificationResultList = new ArrayList<>();

    public ClassificationResultItem() {
    }

    public ClassificationResultItem(String filePath, ContentType ct, String thumbnailPath) {
        super(filePath, ct, thumbnailPath);
    }

    public List<ImageClassifier.Classification> getClassificationResultList() {
        return classificationResultList;
    }

    public void setClassificationResultList(List<ImageClassifier.Classification> classificationResultList) {
        this.classificationResultList = classificationResultList;
    }
}
