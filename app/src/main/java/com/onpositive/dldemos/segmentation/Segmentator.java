package com.onpositive.dldemos.segmentation;

import android.graphics.Bitmap;

import com.onpositive.dldemos.ProgressListener;

import java.util.List;

public interface Segmentator {
    Bitmap getSegmentedImage(Bitmap inputImage);

    Bitmap getSegmentedImage(String photoPath);

    List<Bitmap> getSegmentedImageList(List<Bitmap> inputImageList);

    Bitmap getSegmentationMask(Bitmap inputImage);

    List<Bitmap> getSegmentationMaskList(List<Bitmap> inputImagesList);

    void setProgressListener(ProgressListener progressListener);

    String getSegmentedVideoPath(String videoFilePath);
}
