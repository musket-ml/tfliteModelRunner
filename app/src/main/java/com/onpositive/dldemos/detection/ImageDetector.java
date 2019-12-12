package com.onpositive.dldemos.detection;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Trace;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageDetector {
    private static final Logger log = new Logger(ImageDetector.class);
    private static final int MAX_RESULTS = 10;
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    protected ByteBuffer imgData = null;
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;
    private int SIZE_X;
    private int SIZE_Y;
    private Interpreter interpreter;
    private MappedByteBuffer tfliteModel;
    private List<String> labels;
    private GpuDelegate gpuDelegate = null;
    private byte[][] labelProbArray = null;
    private TFLiteItem tfLiteItem;

    public ImageDetector(Activity activity, TFLiteItem tfLiteItem) throws IOException {
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

    public List<ObjectDetection> recognizeImage(final Bitmap bitmap) {
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, SIZE_X, SIZE_Y, true);
        convertBitmapToByteBuffer(resizedImage);
        Trace.endSection();

        Trace.beginSection("runDetection");
        long startTime = SystemClock.uptimeMillis();
        runDetection();
        long endTime = SystemClock.uptimeMillis();
        Trace.endSection();
        log.log("Time cost to run model inference: " + (endTime - startTime));

        final ArrayList<ObjectDetection> recognitions = new ArrayList<>(MAX_RESULTS);
        for (int i = 0; i < MAX_RESULTS; ++i) {
            final RectF detection =
                    new RectF(
                            outputLocations[0][i][1] * bitmap.getWidth(),
                            outputLocations[0][i][0] * bitmap.getHeight(),
                            outputLocations[0][i][3] * bitmap.getWidth(),
                            outputLocations[0][i][2] * bitmap.getHeight());
            // SSD Mobilenet V1 Model assumes class 0 is background class
            // in label file and class labels start from 1 to number_of_classes+1,
            // while outputClasses correspond to class index from 0 to number_of_classes
            int labelOffset = 0;
            recognitions.add(
                    new ObjectDetection(
                            labels.get((int) outputClasses[0][i] + labelOffset),
                            outputScores[0][i],
                            detection));
        }
        Trace.endSection(); // "recognizeImage"
        log.log("ObjectDetection successful. Results count: " + recognitions.size());
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

    protected void runDetection() {
        outputLocations = new float[1][MAX_RESULTS][4];
        outputClasses = new float[1][MAX_RESULTS];
        outputScores = new float[1][MAX_RESULTS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);

        interpreter.runForMultipleInputsOutputs(inputArray, outputMap);
    }

    protected int getNumLabels() {
        return labels.size();
    }

    public static class ObjectDetection implements Comparable<ObjectDetection> {

        private final String title;
        private final Float confidence;
        private RectF location;

        public ObjectDetection(final String title, final Float confidence, final RectF location) {
            this.title = title;
            this.confidence = confidence;
            this.location = location;
            log.log("ObjectDetection created: " + this.toString());
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
        public int compareTo(ObjectDetection objectDetection) {
            return this.confidence.compareTo(objectDetection.confidence);
        }
    }
}