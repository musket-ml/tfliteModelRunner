package com.onpositive.dldemos.classification;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;

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

    public List<Prediction> recognizeImage(final Bitmap bitmap) {
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

        PriorityQueue<Prediction> pq =
                new PriorityQueue<Prediction>(
                        3,
                        new Comparator<Prediction>() {
                            @Override
                            public int compare(Prediction lhs, Prediction rhs) {
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });
        for (int i = 0; i < labels.size(); ++i) {
            pq.add(
                    new Prediction(
                            labels.size() > i ? labels.get(i) : "unknown",
                            getNormalizedProbability(i)));
        }
        final ArrayList<Prediction> recognitions = new ArrayList<Prediction>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        Trace.endSection();
        log.log("Prediction successful. Results count: " + recognitions.size());
        return recognitions;
    }

    protected void runInference() {
        interpreter.run(imgData, labelProbArray);
    }
}
