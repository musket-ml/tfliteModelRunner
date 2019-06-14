package com.onpositive.dldemos.segmentation;

import android.graphics.Bitmap;

import java.util.concurrent.Callable;

public class ImageSegmentatorCall implements Callable<Bitmap> {
    private Segmentator segmentator;
    private Bitmap inputBitmap;

    public ImageSegmentatorCall(Segmentator segmentator, Bitmap inputBitmap) {
        this.segmentator = segmentator;
        this.inputBitmap = inputBitmap;
    }

    @Override
    public Bitmap call() throws Exception {
        return segmentator.getSegmentedImage(inputBitmap);
    }
}
