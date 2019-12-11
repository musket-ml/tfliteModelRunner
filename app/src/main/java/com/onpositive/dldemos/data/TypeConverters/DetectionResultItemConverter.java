package com.onpositive.dldemos.data.TypeConverters;

import android.graphics.RectF;

import androidx.room.TypeConverter;

import com.onpositive.dldemos.detection.ImageDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DetectionResultItemConverter {
    public static final String TOP = "top";
    public static final String RIGHT = "right";
    public static final String BOTTOM = "bottom";
    public static final String LEFT = "left";

    @TypeConverter
    public String fromRecognitionList(List<ImageDetector.ObjectDetection> list) {
        String json = "";
        JSONArray jsonArray = new JSONArray();
        try {
            for (ImageDetector.ObjectDetection recognition : list) {
                JSONObject object = new JSONObject();
                object.put(recognition.getTitle(), recognition.getConfidence());
                object.put(LEFT, recognition.getLocation().left);
                object.put(TOP, recognition.getLocation().top);
                object.put(RIGHT, recognition.getLocation().right);
                object.put(BOTTOM, recognition.getLocation().bottom);
                jsonArray.put(object);
            }
            json = jsonArray.toString();
        } catch (JSONException e) {
        }
        return json;
    }

    @TypeConverter
    public List<ImageDetector.ObjectDetection> toRecognitionList(String str) {
        List<ImageDetector.ObjectDetection> recognitions = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(str);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String key = object.keys().next();
                Float value = Float.valueOf(object.getString(key));
                RectF location = new RectF(
                        (float) object.getDouble(LEFT),
                        (float) object.getDouble(TOP),
                        (float) object.getDouble(RIGHT),
                        (float) object.getDouble(BOTTOM)
                );
                recognitions.add(new ImageDetector.ObjectDetection(key, value, location));

            }
        } catch (JSONException e) {

        }
        return recognitions;
    }
}
