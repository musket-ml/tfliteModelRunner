package com.onpositive.dldemos.segmentation;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.onpositive.dldemos.R;
import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.tools.Logger;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public abstract class ImageSegmentator implements Segmentator {
    private static final int INPUT_SIZE_X = 320;
    private static final int INPUT_SIZE_Y = 320;
    private static final int OUTPUT_SIZE_X = 320;
    private static final int OUTPUT_SIZE_Y = 320;
    private static final int PIXEL_SIZE = 3;
    private static final double GOOD_PROB_THRESHOLD = 0.5;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    protected volatile Interpreter interpreter;
    protected Logger log = new Logger(this.getClass());
    private MappedByteBuffer tfliteModel;
    private Delegate gpuDelegate = null;
    private Activity activity;
    private int batchSize = 1;
    private TFLiteItem tfLiteItem;

    public ImageSegmentator(Activity activity, @NonNull TFLiteItem tfLiteItem) throws IOException {
        this.activity = activity;
        this.tfLiteItem = tfLiteItem;
        this.tfliteModel = loadModelFile(activity.getAssets(), tfLiteItem);
        interpreter = new Interpreter(tfliteModel, tfliteOptions);

        log.log(this.getClass().getSimpleName() + " initialized.");
    }

    public Bitmap getSegmentedImage(Bitmap inputImage) {
        return (overlay(inputImage, getSegmentationMask(inputImage)));
    }

    public Bitmap getSegmentedImage(String photoPath) {
        Bitmap imageBitmap = BitmapFactory.decodeFile(photoPath);
        return (overlay(imageBitmap, getSegmentationMask(imageBitmap)));
    }

    public List<Bitmap> getSegmentedImageList(List<Bitmap> inputImageList) {
        List<Bitmap> segmantationImageList = new ArrayList<>();
        List<Bitmap> segmantationMaskList = getSegmentationMaskList(inputImageList);
        for (int i = 0; i < inputImageList.size(); i++) {
            segmantationImageList.add(
                    overlay(inputImageList.get(i), segmantationMaskList.get(i))
            );
        }
        log.log("Images batch segmented");
        return segmantationImageList;
    }

    public Bitmap getSegmentationMask(Bitmap inputImage) {
        List<Bitmap> bitmapList = new ArrayList<>();
        bitmapList.add(inputImage);
        return getSegmentationMaskList(bitmapList).get(0);
    }

    public List<Bitmap> getSegmentationMaskList(List<Bitmap> inputImagesList) {
        batchSize = inputImagesList.size();
        List<Bitmap> resizedImagesList = scaleBitmaps(inputImagesList);
        ByteBuffer segmentationMaskBB = ByteBuffer.allocate(INPUT_SIZE_X * INPUT_SIZE_Y * 4 * batchSize);
        int[] dimensions = {batchSize, INPUT_SIZE_X, INPUT_SIZE_Y, PIXEL_SIZE};
        interpreter.resizeInput(0, dimensions);
        interpreter.run(convertBitmapListToByteBuffer(resizedImagesList), segmentationMaskBB);
        log.log("Interpreter returned segmentation mask prediction");
        return convertByteBufferToBitmapList(segmentationMaskBB, inputImagesList.get(0).getWidth(), inputImagesList.get(0).getHeight());
    }

    public List<Bitmap> scaleBitmaps(List<Bitmap> inputImagesList) {
        List<Bitmap> scaledBitmapsList = new ArrayList<>();
        for (Bitmap bitmap : inputImagesList) {
            Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE_X, INPUT_SIZE_Y, true);
            scaledBitmapsList.add(resizedImage);
        }
        log.log("Input images are scaled for the Interpreter input");
        return scaledBitmapsList;
    }

    /**
     * Memory-map model file from Assets.
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
     * Converts ByteBuffer with segmentation mask to the Bitmap
     *
     * @param byteBuffer Output ByteBuffer from Interpreter.run
     * @return Mono color Bitmap mask
     */
    private List<Bitmap> convertByteBufferToBitmapList(ByteBuffer byteBuffer, int width, int height) {
        List<Bitmap> bitmapMaskList = new ArrayList<>();
        byteBuffer.rewind();
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[OUTPUT_SIZE_X * OUTPUT_SIZE_Y];
        int maskColor = ContextCompat.getColor(activity.getApplicationContext(), R.color.segmentationMask);
        for (int c = 0; c < batchSize; c++) {
            for (int i = 0; i < OUTPUT_SIZE_X * OUTPUT_SIZE_Y; i++)
                if (byteBuffer.getFloat() > GOOD_PROB_THRESHOLD)
                    pixels[i] = maskColor;
                else
                    pixels[i] = Color.argb(0, 0, 0, 0);
            Bitmap bitmap = Bitmap.createBitmap(OUTPUT_SIZE_X, OUTPUT_SIZE_Y, Bitmap.Config.ARGB_4444);
            bitmap.setPixels(pixels, 0, OUTPUT_SIZE_X, 0, 0, OUTPUT_SIZE_X, OUTPUT_SIZE_Y);

            Bitmap segmentationMask = Bitmap.createScaledBitmap(bitmap, width, height, true);
            bitmapMaskList.add(segmentationMask);
        }
        log.log("Byte buffer converted to the Bitmaps. (Segmentation masks are converted to the Bitmaps)");
        return bitmapMaskList;
    }

    private ByteBuffer convertBitmapListToByteBuffer(List<Bitmap> bitmapList) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * batchSize * INPUT_SIZE_X * INPUT_SIZE_Y * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_SIZE_X * INPUT_SIZE_Y];
        try {
            for (Bitmap bitmap : bitmapList) {
                bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                int pixel = 0;
                for (int i = 0; i < INPUT_SIZE_X; ++i) {
                    for (int j = 0; j < INPUT_SIZE_Y; ++j) {
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

    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        log.log("Overlay created");
        return bmOverlay;
    }

    public void close() {
        interpreter.close();
        interpreter = null;
        tfliteModel = null;
        log.log("Session closed");
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
                gpuDelegate = GpuDelegateHelper.createGpuDelegate();
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

}
