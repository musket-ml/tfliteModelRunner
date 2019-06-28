package com.onpositive.dldemos.data.TypeConverters;

import android.arch.persistence.room.TypeConverter;

import com.onpositive.dldemos.classification.ImageClassifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ClassificationResultItemConverter {
    @TypeConverter
    public String fromClassificationResultList(List<ImageClassifier.Classification> list) {
        String json = "";
        JSONArray jsonArray = new JSONArray();
        try {
            for (ImageClassifier.Classification classification : list) {
                JSONObject object = new JSONObject();
                object.put(classification.getTitle(), classification.getConfidence());
                jsonArray.put(object);
            }
            json = jsonArray.toString();
        } catch (JSONException e) {
        }
        return json;
    }

    @TypeConverter
    public List<ImageClassifier.Classification> toClassificationResultList(String str) {
        List<ImageClassifier.Classification> classificationList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(str);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String key = object.keys().next();
                Float value = Float.valueOf(object.getString(key));
                classificationList.add(new ImageClassifier.Classification(key, value));

            }
        } catch (JSONException e) {

        }
        return classificationList;
    }
}
