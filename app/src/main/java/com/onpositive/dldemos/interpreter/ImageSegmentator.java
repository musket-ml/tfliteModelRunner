package com.onpositive.dldemos.interpreter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.onpositive.dldemos.R;
import com.onpositive.dldemos.data.TFLiteItem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public abstract class ImageSegmentator extends BaseInterpreter implements Segmentator {
    public static final int[] colors = {
            Color.parseColor("#88FFB300"), // Vivid Yellow
            Color.parseColor("#88803E75"), // Strong Purple
            Color.parseColor("#88FF6800"), // Vivid Orange
            Color.parseColor("#88A6BDD7"), // Very Light Blue
            Color.parseColor("#88C10020"), // Vivid Red
            Color.parseColor("#88CEA262"), // Grayish Yellow
            Color.parseColor("#88817066"), // Medium Gray
            Color.parseColor("#88007D34"), // Vivid Green
            Color.parseColor("#88F6768E"), // Strong Purplish Pink
            Color.parseColor("#8800538A"), // Strong Blue
            Color.parseColor("#88FF7A5C"), // Strong Yellowish Pink
            Color.parseColor("#8853377A"), // Strong Violet
            Color.parseColor("#88FF8E00"), // Vivid Orange Yellow
            Color.parseColor("#88B32851"), // Strong Purplish Red
            Color.parseColor("#88F4C800"), // Vivid Greenish Yellow
            Color.parseColor("#887F180D"), // Strong Reddish Brown
            Color.parseColor("#8893AA00"), // Vivid Yellowish Green
            Color.parseColor("#88593315"), // Deep Yellowish Brown
            Color.parseColor("#88F13A13"), // Vivid Reddish Orange
            Color.parseColor("#88232C16"), // Dark Olive Green
            Color.parseColor("#8800A1C2")  // Vivid Blue
    };
    private final double GOOD_PROB_THRESHOLD = 0.5;
    private int OUTPUT_CLASS_COUNT = 1;

    public ImageSegmentator(Activity activity, @NonNull TFLiteItem tfLiteItem) throws IOException {
        super(activity, tfLiteItem);
        if (interpreter.getOutputTensor(0).shape().length > 3)
            OUTPUT_CLASS_COUNT = interpreter.getOutputTensor(0).shape()[3];
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
        BATCH_SIZE = inputImagesList.size();
        List<Bitmap> resizedImagesList = scaleBitmaps(inputImagesList);
        ByteBuffer segmentationMaskBB = ByteBuffer.allocate(SIZE_X * SIZE_Y * 4 * OUTPUT_CLASS_COUNT * BATCH_SIZE);
        int[] dimensions = {BATCH_SIZE, SIZE_X, SIZE_Y, PIXEL_SIZE};
        interpreter.resizeInput(0, dimensions);
        interpreter.run(convertBitmapListToByteBuffer(resizedImagesList), segmentationMaskBB);
        log.log("Interpreter returned segmentation mask prediction");
        return convertByteBufferToColorBitmapList(segmentationMaskBB, inputImagesList.get(0).getWidth(), inputImagesList.get(0).getHeight());
    }

    private List<Bitmap> scaleBitmaps(List<Bitmap> inputImagesList) {
        List<Bitmap> scaledBitmapsList = new ArrayList<>();
        for (Bitmap bitmap : inputImagesList) {
            Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, SIZE_X, SIZE_Y, true);
            scaledBitmapsList.add(resizedImage);
        }
        log.log("Input images are scaled for the Interpreter input");
        return scaledBitmapsList;
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
        int[] pixels = new int[SIZE_X * SIZE_Y];
        int maskColor = ContextCompat.getColor(activity.getApplicationContext(), R.color.segmentationMask);
        for (int c = 0; c < BATCH_SIZE; c++) {
            for (int i = 0; i < SIZE_X * SIZE_Y; i++)
                if (byteBuffer.getFloat() > GOOD_PROB_THRESHOLD)
                    pixels[i] = maskColor;
                else
                    pixels[i] = Color.argb(0, 0, 0, 0);
            Bitmap bitmap = Bitmap.createBitmap(SIZE_X, SIZE_Y, Bitmap.Config.ARGB_4444);
            bitmap.setPixels(pixels, 0, SIZE_X, 0, 0, SIZE_X, SIZE_Y);

            Bitmap segmentationMask = Bitmap.createScaledBitmap(bitmap, width, height, true);
            bitmapMaskList.add(segmentationMask);
        }
        log.log("Byte buffer converted to the Bitmaps. (Segmentation masks are converted to the Bitmaps)");
        return bitmapMaskList;
    }

    private List<Bitmap> convertByteBufferToColorBitmapList(ByteBuffer byteBuffer, int width, int height) {
        List<Bitmap> bitmapMaskList = new ArrayList<>();
        byteBuffer.rewind();
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[SIZE_X * SIZE_Y];
        for (int c = 0; c < BATCH_SIZE; c++) {
            for (int i = 0; i < SIZE_X * SIZE_Y; i++) {
                float maxColorProbability = 0;
                int pixelColor = Color.argb(0, 0, 0, 0);
                for (int z = 0; z < OUTPUT_CLASS_COUNT; z++) {
                    float colorProbability = byteBuffer.getFloat();
                    if (colorProbability > maxColorProbability && colorProbability > GOOD_PROB_THRESHOLD) {
                        maxColorProbability = colorProbability;
                        pixelColor = colors[z % colors.length];
                    }
                }
                pixels[i] = pixelColor;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(SIZE_X, SIZE_Y, Bitmap.Config.ARGB_4444);
        bitmap.setPixels(pixels, 0, SIZE_X, 0, 0, SIZE_X, SIZE_Y);

        Bitmap segmentationMask = Bitmap.createScaledBitmap(bitmap, width, height, true);
        bitmapMaskList.add(segmentationMask);

        log.log("Byte buffer converted to the Bitmaps. (Segmentation masks are converted to the Bitmaps)");
        return bitmapMaskList;
    }

    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        log.log("Overlay created");
        return bmOverlay;
    }
}
