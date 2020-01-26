package com.onpositive.dldemos.interpreter;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;

import androidx.annotation.NonNull;

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
import java.util.List;

public abstract class BaseInterpreter {
    protected static final int MAX_RESULTS = 10;
    protected static final int PIXEL_SIZE = 3;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    protected int BATCH_SIZE = 1;
    protected int SIZE_X;
    protected int SIZE_Y;
    protected Logger log = new Logger(this.getClass());
    protected Activity activity;
    protected TFLiteItem tfLiteItem;
    protected volatile Interpreter interpreter;
    protected List<String> labels;
    protected byte[][] labelProbArray = null;
    private MappedByteBuffer tfliteModel;
    private GpuDelegate gpuDelegate = null;

    public BaseInterpreter(Activity activity, @NonNull TFLiteItem tfLiteItem) throws IOException {
        this.activity = activity;
        this.tfLiteItem = tfLiteItem;
        SIZE_X = tfLiteItem.getSize_x();
        SIZE_Y = tfLiteItem.getSize_y();
        this.tfliteModel = loadModelFile(activity.getAssets(), tfLiteItem);
        if (tfLiteItem.hasLabels()) {
            labels = loadLabelList();
            labelProbArray = new byte[1][getNumLabels()];
        }
        useGPU();
        tfliteOptions.setNumThreads(Runtime.getRuntime().availableProcessors() + 1);
        interpreter = new Interpreter(tfliteModel, tfliteOptions);
        log.log(this.getClass().getSimpleName() + " initialized.");
    }

    /**
     * Converts Bitmap to the ByteBuffer
     *
     * @param bitmap Bitmap for prediction.
     * @return Input ByteBuffer for Interpreter
     */
    protected ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(BATCH_SIZE * SIZE_X * SIZE_Y * PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        imgData.rewind();
        int[] intValues = new int[SIZE_X * SIZE_Y];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < SIZE_X; ++i) {
            for (int j = 0; j < SIZE_Y; ++j) {
                final int pixelValue = intValues[pixel++];
                imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                imgData.put((byte) (pixelValue & 0xFF));
            }
        }
        long endTime = SystemClock.uptimeMillis();
        log.log("Timecost to put values into ByteBuffer: " + (endTime - startTime));
        return imgData;
    }

    /**
     * Converts Bitmap List to the ByteBuffer
     *
     * @param bitmapList List of Bitmap for prediction.
     * @return Input ByteBuffer for Interpreter
     */
    protected ByteBuffer convertBitmapListToByteBuffer(List<Bitmap> bitmapList) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * BATCH_SIZE * SIZE_X * SIZE_Y * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[SIZE_X * SIZE_Y];
        try {
            for (Bitmap bitmap : bitmapList) {
                bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                int pixel = 0;
                for (int i = 0; i < SIZE_X; ++i) {
                    for (int j = 0; j < SIZE_Y; ++j) {
                        final int val = intValues[pixel++];
                        byteBuffer.putFloat((val >> 16) & 0xFF);
                        byteBuffer.putFloat((val >> 8) & 0xFF);
                        byteBuffer.putFloat((val) & 0xFF);
                    }
                }
            }
        } catch (Exception e) {
            log.error("convertBitmapListToByteBuffer failed:\n" + e.getMessage());
        }
        log.log("Bitmap List converted to the ByteBuffer");
        return byteBuffer;
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

    protected int getNumLabels() {
        return labels.size();
    }

    /**
     * Loads tensorflow-lite model file from assets or from the disk.
     *
     * @param model TFLiteItem with a model data
     * @return ByteBuffer for Interpreter
     */
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

    /**
     * Load model labels for predictions.
     *
     * @return List of labels for predictions.
     * @throws IOException on file read
     */
    private List<String> loadLabelList() throws IOException {
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

    private void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        tfliteModel = null;
    }

    private void recreateInterpreter() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = new Interpreter(tfliteModel, tfliteOptions);
            log.log("interpeter recreated");
        }
        log.log("interpreter is undefined. Recreation impossible");
    }

    private void useGPU() {
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

    private void useCPU() {
        tfliteOptions.setUseNNAPI(false);
        recreateInterpreter();
        log.log("CPU enabled");
    }

    private void useNNAPI() {
        tfliteOptions.setUseNNAPI(true);
        recreateInterpreter();
        log.log("NNAPI enabled");
    }

    /**
     * Stores prediction result item.
     */
    public static class Prediction implements Comparable<Prediction> {

        private final String title;
        private final Float confidence;
        private RectF location;

        public Prediction(final String title, final Float confidence) {
            this.title = title;
            this.confidence = confidence;
        }

        public Prediction(final String title, final Float confidence, final RectF location) {
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
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
        public int compareTo(Prediction prediction) {
            return this.confidence.compareTo(prediction.confidence);
        }
    }
}
