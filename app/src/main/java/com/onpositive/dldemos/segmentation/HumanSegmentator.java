package com.onpositive.dldemos.segmentation;

import android.app.Activity;

import java.io.IOException;

public class HumanSegmentator extends VideoSegmentator implements Segmentator {

    public HumanSegmentator(Activity activity) throws IOException {
        super(activity);
    }

    @Override
    protected String getModelPath() {
        return "segmentation_model_0.2_relu-1.13.0rc2.tflite";
    }
}
