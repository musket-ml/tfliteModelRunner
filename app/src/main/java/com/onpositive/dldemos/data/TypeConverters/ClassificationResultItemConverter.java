package com.onpositive.dldemos.data.TypeConverters;

import androidx.room.TypeConverter;

import com.onpositive.dldemos.interpreter.ImageClassifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ClassificationResultItemConverter {
    @TypeConverter
    public String fromClassificationResultList(List<ImageClassifier.Prediction> list) {
        String json = "";
        JSONArray jsonArray = new JSONArray();
        try {
            for (ImageClassifier.Prediction prediction : list) {
                JSONObject object = new JSONObject();
                object.put(prediction.getTitle(), prediction.getConfidence());
                jsonArray.put(object);
            }
            json = jsonArray.toString();
        } catch (JSONException e) {
        }
        return json;
    }

    @TypeConverter
    public List<ImageClassifier.Prediction> toClassificationResultList(String str) {
        List<ImageClassifier.Prediction> predictionList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(str);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String key = object.keys().next();
                Float value = Float.valueOf(object.getString(key));
                predictionList.add(new ImageClassifier.Prediction(key, value));

            }
        } catch (JSONException e) {

        }
        return predictionList;
    }
}
