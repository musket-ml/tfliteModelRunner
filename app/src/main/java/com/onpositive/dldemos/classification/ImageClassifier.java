package com.onpositive.dldemos.classification;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;

import androidx.annotation.NonNull;

import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.interpreter.BaseInterpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class ImageClassifier extends BaseInterpreter {

    private ByteBuffer imgData = null;

    public ImageClassifier(Activity activity, TFLiteItem tfLiteItem) throws IOException {
        super(activity, tfLiteItem);
        log.log(this.getClass().getSimpleName() + " initialized.");
    }

    public List<Classification> recognizeImage(final Bitmap bitmap) {
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, SIZE_X, SIZE_Y, true);
        imgData = convertBitmapToByteBuffer(resizedImage);
        Trace.endSection();

        Trace.beginSection("runInference");
        long startTime = SystemClock.uptimeMillis();
        runInference();
        long endTime = SystemClock.uptimeMillis();
        Trace.endSection();
        log.log("Timecost to run model inference: " + (endTime - startTime));

        PriorityQueue<Classification> pq =
                new PriorityQueue<Classification>(
                        3,
                        new Comparator<Classification>() {
                            @Override
                            public int compare(Classification lhs, Classification rhs) {
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });
        for (int i = 0; i < labels.size(); ++i) {
            pq.add(
                    new Classification(
                            labels.size() > i ? labels.get(i) : "unknown",
                            getNormalizedProbability(i)));
        }
        final ArrayList<Classification> recognitions = new ArrayList<Classification>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        Trace.endSection();
        log.log("Classification successful. Results count: " + recognitions.size());
        return recognitions;
    }

    protected void runInference() {
        interpreter.run(imgData, labelProbArray);
    }

    public static class Classification implements Comparable<Classification> {

        private final String title;
        private final Float confidence;

        public Classification(final String title, final Float confidence) {
            this.title = title;
            this.confidence = confidence;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        @Override
        public String toString() {
            String resultString = "";

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            return resultString.trim();
        }

        @Override
        public int compareTo(@NonNull Classification o) {
            return this.confidence.compareTo(o.confidence);
        }
    }
}
