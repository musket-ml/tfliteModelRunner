package com.onpositive.dldemos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.onpositive.dldemos.data.ContentType;
import com.onpositive.dldemos.data.DetectionResultItem;
import com.onpositive.dldemos.data.DetectionResultItemDao;
import com.onpositive.dldemos.detection.ImageDetector;
import com.onpositive.dldemos.tools.Logger;
import com.onpositive.dldemos.tools.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

public class DetectionAsyncTask extends AsyncTask<ContentType, Integer, DetectionResultItem> {

    private static final float FILTER_VALUE = 0.5f;
    private static final float TEXT_SIZE_DIP = 18;
    private static final int[] COLORS = {
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"),
            Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
    };
    private static Logger log = new Logger(DetectionAsyncTask.class);
    DetectionResultItem detectionRI = null;
    ContentType contentType;
    private boolean isCanceled = false;
    private DetectionFragment fragment;
    @Nullable
    private ImageDetector detector;

    public DetectionAsyncTask(ImageDetector detector, DetectionFragment fragment) {
        this.detector = detector;
        this.fragment = fragment;
    }

    @Override
    protected DetectionResultItem doInBackground(ContentType... contentTypes) {
        switch (contentTypes[0]) {
            case IMAGE:
                contentType = ContentType.IMAGE;
                File resultImageFile = null;
                try {
                    List<ImageDetector.Prediction> recognitions = detector.recognizeImage(BitmapFactory.decodeFile(fragment.getCurrentPhotoPath()));
                    resultImageFile = Utils.createImageFile(fragment.getActivity());
                    FileOutputStream stream = new FileOutputStream(resultImageFile);
                    drawRecognitions(BitmapFactory.decodeFile(fragment.getCurrentPhotoPath()), recognitions)
                            .compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    ;
                    stream.flush();
                    stream.close();
                    String thumbnailPath = Utils.createThumbnail(fragment.getActivity(), resultImageFile.getAbsolutePath(), ContentType.IMAGE);
                    detectionRI = new DetectionResultItem(resultImageFile.getAbsolutePath(), ContentType.IMAGE, thumbnailPath);
                    detectionRI.setRecognitionResultList(recognitions);
                    deleteFile(new File(fragment.getCurrentPhotoPath()));
                    log.log("Image segmented successfully. Image file path:" + resultImageFile.getAbsolutePath());
                } catch (Exception e) {
                    log.log("Failed image segmentation AsyncTask:\n" + e.getMessage());
                }
                break;
        }
        if (null != detectionRI)
            saveDetectionResults(detectionRI);
        return detectionRI;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        fragment.moveRVDown(fragment.detectionRV);
        fragment.progressBar.setVisibility(View.VISIBLE);
        fragment.progressStatusTV.setText(R.string.calculating);
        fragment.progressStatusTV.setVisibility(View.VISIBLE);
        log.log("Object Detection Async Task onPreExecute");
    }

    @Override
    protected void onPostExecute(DetectionResultItem detectionResultItem) {
        super.onPostExecute(detectionResultItem);
        fragment.moveRVUp(fragment.detectionRV);
        fragment.progressBar.setVisibility(View.INVISIBLE);
        fragment.progressStatusTV.setVisibility(View.INVISIBLE);

        fragment.detectionResultList.add(detectionResultItem);
        Collections.sort(fragment.detectionResultList);
        Collections.reverse(fragment.detectionResultList);
        fragment.detectionRvAdapter.notifyDataSetChanged();
        log.log("'onPostExecute' executed");
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        log.log("onProgressUpdate called");
        super.onProgressUpdate(values);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        isCanceled = isCancelled();
        log.log("AT method 'onCancelled' executed");
    }

    private void deleteFile(File file) {
        if (null != file && file.exists())
            file.delete();
        log.log("File was deleted. Is file exist: " + file.exists() +
                "\nDeleted file path: " + file.getAbsolutePath());
    }

    private void saveDetectionResults(DetectionResultItem detectionResultItem) {
        DetectionResultItemDao resultItemDao = MLDemoApp.getInstance().getDatabase().detectionResultItemDao();
        detectionResultItem.setTfLiteParentFile(fragment.getTfLiteItem().getTfFilePath());
        resultItemDao.insert(detectionResultItem);
        log.log("Object Detection results are saved");
    }

    private Bitmap drawRecognitions(Bitmap bitmap, List<ImageDetector.Prediction> recognitions) {
        Bitmap recognitionBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(recognitionBitmap);
        float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, fragment.getResources().getDisplayMetrics());
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10.0f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeMiter(100);

        Paint textPaint = new Paint();
        textPaint.setTextSize(textSizePx);
        textPaint.setColor(Color.RED);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(false);
        textPaint.setAlpha(255);

        for (ImageDetector.Prediction recognition : recognitions) {
            int colorNumber = recognitions.indexOf(recognition) % COLORS.length;
            int color = COLORS[colorNumber];
            paint.setColor(color);
            textPaint.setColor(color);
            if (FILTER_VALUE > recognition.getConfidence()) {
                continue;
            }
            if (recognition.getTitle().length() > 0) {
                canvas.drawText(recognition.toString(),
                        recognition.getLocation().left + textSizePx,
                        recognition.getLocation().top + textSizePx,
                        textPaint);
            }
            canvas.drawRect(recognition.getLocation(), paint);
        }
        return recognitionBitmap;
    }
}
