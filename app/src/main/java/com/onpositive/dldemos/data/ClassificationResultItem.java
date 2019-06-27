package com.onpositive.dldemos.data;

import android.arch.persistence.room.Entity;

import com.onpositive.dldemos.classification.ImageClassifier;

import java.util.ArrayList;
import java.util.List;

@Entity
public class ClassificationResultItem extends ResultItem {

    private List<ImageClassifier.Recognition> classificationResultList = new ArrayList<>();

    public ClassificationResultItem(String filePath, ContentType ct, String thumbnailPath) {
        super(filePath, ct, thumbnailPath);
    }

    public List<ImageClassifier.Recognition> getClassificationResultList() {
        return classificationResultList;
    }

    public void setClassificationResultList(List<ImageClassifier.Recognition> classificationResultList) {
        this.classificationResultList = classificationResultList;
    }
}
