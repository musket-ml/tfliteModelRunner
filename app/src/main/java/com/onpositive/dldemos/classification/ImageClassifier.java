package com.onpositive.dldemos.classification;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;
import android.support.annotation.NonNull;

import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.segmentation.GpuDelegateHelper;
import com.onpositive.dldemos.tools.Logger;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class ImageClassifier {
    private static final Logger log = new Logger(ImageClassifier.class);
    private static final int MAX_RESULTS = 10;
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    protected ByteBuffer imgData = null;
    private int SIZE_X;
    private int SIZE_Y;
    private Interpreter interpreter;
    private MappedByteBuffer tfliteModel;
    private List<String> labels;
    private GpuDelegate gpuDelegate = null;
    private byte[][] labelProbArray = null;
    private TFLiteItem tfLiteItem;

    public ImageClassifier(Activity activity, TFLiteItem tfLiteItem) throws IOException {
        this.tfLiteItem = tfLiteItem;
        SIZE_X = tfLiteItem.getSize_x();
        SIZE_Y = tfLiteItem.getSize_y();
        tfliteModel = loadModelFile(activity.getAssets(), tfLiteItem);
        interpreter = new Interpreter(tfliteModel, tfliteOptions);
        labels = loadLabelList(activity);
        imgData =
                ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE
                                * SIZE_X
                                * SIZE_Y
                                * DIM_PIXEL_SIZE
                                * getNumBytesPerChannel());
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new byte[1][getNumLabels()];
        log.log(this.getClass().getSimpleName() + " initialized.");
    }

    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labels = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(tfLiteItem.getLabelsPath())));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        log.log("Labels load completed");
        return labels;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, TFLiteItem model) throws IOException {
        MappedByteBuffer byteBuffer;
        FileInputStream inputStream;
        if (model.isAsset()) {
            AssetFileDescriptor fileDescriptor = assetManager.openFd(model.getTfFilePath());
            inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            FileChannel fileChannel = inputStream.getChannel();
            byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } else {
            inputStream = new FileInputStream(model.getTfFilePath());
            FileChannel fileChannel = inputStream.getChannel();
            byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        }
        log.info("Model: " + model.getTfFilePath() + " loaded");
        return byteBuffer;
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        int[] intValues = new int[SIZE_X * SIZE_Y];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < SIZE_X; ++i) {
            for (int j = 0; j < SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        log.log("Timecost to put values into ByteBuffer: " + (endTime - startTime));
    }

    public List<Classification> recognizeImage(final Bitmap bitmap) {
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, SIZE_X, SIZE_Y, true);
        convertBitmapToByteBuffer(resizedImage);
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

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        tfliteModel = null;
        log.log("Closed. Interpreter and model is null");
    }

    private void recreateInterpreter() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = new Interpreter(tfliteModel, tfliteOptions);
            log.log("interpeter recreated");
        }
        log.log("interpreter is undefined. Recreation impossible");
    }

    public void useGPU() {
        if (gpuDelegate == null && GpuDelegateHelper.isGpuDelegateAvailable()) {
            try {
                gpuDelegate = new GpuDelegate();
                tfliteOptions.addDelegate(gpuDelegate);
                recreateInterpreter();
                log.log("GPU enabled");
            } catch (Exception e) {
                log.log("Enabling GPU failed.\n" + e.getMessage());
            }
        } else
            log.log("Enabling GPU failed. Gpu delegate is not available");
    }

    public void useCPU() {
        tfliteOptions.setUseNNAPI(false);
        recreateInterpreter();
        log.log("CPU enabled");
    }

    public void useNNAPI() {
        tfliteOptions.setUseNNAPI(true);
        recreateInterpreter();
        log.log("NNAPI enabled");
    }

    protected String getModelPath() {
        return tfLiteItem.getTfFilePath();
    }

    protected int getNumBytesPerChannel() {
        return 1;
    }

    protected void addPixelValue(int pixelValue) {
        imgData.put((byte) ((pixelValue >> 16) & 0xFF));
        imgData.put((byte) ((pixelValue >> 8) & 0xFF));
        imgData.put((byte) (pixelValue & 0xFF));
    }

    protected float getProbability(int labelIndex) {
        return labelProbArray[0][labelIndex];
    }

    protected void setProbability(int labelIndex, Number value) {
        labelProbArray[0][labelIndex] = value.byteValue();
    }

    protected float getNormalizedProbability(int labelIndex) {
        return (labelProbArray[0][labelIndex] & 0xff) / 255.0f;
    }

    protected void runInference() {
        interpreter.run(imgData, labelProbArray);
    }

    protected int getNumLabels() {
        return labels.size();
    }

    public static class Classification implements Comparable<Classification> {

        private final String title;
        private final Float confidence;

        public Classification(final String title, final Float confidence) {
            this.title = title;
            this.confidence = confidence;
            log.log("Classification created: " + this.toString());
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
