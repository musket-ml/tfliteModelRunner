package com.onpositive.dldemos;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;

import androidx.annotation.Nullable;

import com.onpositive.dldemos.data.ContentType;
import com.onpositive.dldemos.data.ResultItem;
import com.onpositive.dldemos.data.ResultItemDao;
import com.onpositive.dldemos.segmentation.Segmentator;
import com.onpositive.dldemos.tools.Logger;
import com.onpositive.dldemos.tools.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SegmentationAsyncTask extends AsyncTask<ContentType, Integer, List<ResultItem>> {

    private static Logger log = new Logger(SegmentationAsyncTask.class);
    List<ResultItem> segmentationResults = new ArrayList<>();
    ContentType contentType;
    private boolean isCanceled = false;
    private SegmentationFragment fragment;
    @Nullable
    private Segmentator segmentator;

    public SegmentationAsyncTask(Segmentator segmentator, SegmentationFragment fragment) {
        this.segmentator = segmentator;
        this.fragment = fragment;
        log.log("SegmentationAsyncTask object created");
    }

    @Override
    protected List<ResultItem> doInBackground(ContentType... contentTypes) {
        switch (contentTypes[0]) {
            case IMAGE:
                contentType = ContentType.IMAGE;
                File resultImageFile = null;
                try {
                    resultImageFile = Utils.createImageFile(fragment.getActivity());
                    FileOutputStream stream = new FileOutputStream(resultImageFile);
                    segmentator.getSegmentedImage(fragment.getCurrentPhotoPath())
                            .compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    stream.flush();
                    stream.close();
                    String thumbnailPath = Utils.createThumbnail(fragment.getActivity(), resultImageFile.getAbsolutePath(), ContentType.IMAGE);
                    segmentationResults.add(new ResultItem(resultImageFile.getAbsolutePath(), ContentType.IMAGE, thumbnailPath));
                    deleteFile(new File(fragment.getCurrentPhotoPath()));
                    log.log("Image segmented successfully. Image file path:" + resultImageFile.getAbsolutePath());
                } catch (Exception e) {
                    log.log("Failed image segmentation AsyncTask:\n" + e.getMessage());
                }
                break;
            case VIDEO:
                contentType = ContentType.VIDEO;
                try {
                    assert segmentator != null;
                    segmentator.setProgressListener(new ProgressListener() {
                        public void updateProgress(ProgressEvent progressEvent) {
                            progressEvent.setCanceled(isCanceled);
                            publishProgress(progressEvent.getProgressInPercent(), progressEvent.getExpectedMinutes());
                        }
                    });
                    String segmentedVideoPath = segmentator.getSegmentedVideoPath(fragment.getCurrentVideoPath());
                    if (isCanceled) {
                        log.log("Segmentation canceled. Segmented video was deleted. Is file exist: " + new File(segmentedVideoPath).exists());
                    } else {
                        String thumbnailPath = Utils.createThumbnail(fragment.getActivity(), segmentedVideoPath, ContentType.VIDEO);
                        segmentationResults.add(new ResultItem(segmentedVideoPath, ContentType.VIDEO, thumbnailPath));
                        deleteFile(new File(fragment.getCurrentVideoPath()));
                        log.log("Video segmented successfully. Video file path:" + segmentedVideoPath);
                    }
                } catch (Exception e) {
                    log.log("Failed video segmentation AsyncTask:\n" + e.getMessage());
                }
                break;
        }
        saveSegmentationResults(segmentationResults);
        log.log("Segmentation successful. Result saved");
        return segmentationResults;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        fragment.moveRVDown(fragment.segmentationRV);
        fragment.progressBar.setVisibility(View.VISIBLE);
        fragment.progressStatusTV.setText(R.string.calculating);
        fragment.progressStatusTV.setVisibility(View.VISIBLE);
        fragment.cancelBtn.setVisibility(View.VISIBLE);
        log.log("Segmentation Async Task onPreExecute");
    }

    @Override
    protected void onPostExecute(List<ResultItem> resultItemList) {
        super.onPostExecute(resultItemList);
        fragment.moveRVUp(fragment.segmentationRV);
        switch (contentType) {
            case IMAGE:
                if (resultItemList.size() > 0 && null != resultItemList.get(0).getFilePath()) {
                } else
                    log.error("Image segmentation failed. Missing segmented image path.");
                fragment.progressBar.setVisibility(View.INVISIBLE);
                fragment.progressStatusTV.setVisibility(View.INVISIBLE);
                fragment.cancelBtn.setVisibility(View.INVISIBLE);
                break;
            case VIDEO:
                fragment.progressBar.setVisibility(View.INVISIBLE);
                fragment.progressStatusTV.setVisibility(View.INVISIBLE);
                fragment.cancelBtn.setVisibility(View.INVISIBLE);
                break;
        }

        fragment.segResultList.addAll(segmentationResults);
        Collections.sort(fragment.segResultList);
        Collections.reverse(fragment.segResultList);
        fragment.segRvAdapter.notifyDataSetChanged();
        log.log("'onPostExecute' executed");
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        String status = fragment.getActivity().getApplicationContext().getResources().getString(R.string.segmentation_status, values[0], values[1]);
        fragment.progressStatusTV.setText(status);
        log.log("Progress updated to: " + status);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        fragment.moveRVUp(fragment.segmentationRV);
        isCanceled = isCancelled();
        fragment.progressBar.setVisibility(View.INVISIBLE);
        fragment.progressStatusTV.setVisibility(View.INVISIBLE);
        fragment.cancelBtn.setVisibility(View.INVISIBLE);
        log.log("AT method 'onCancelled' executed");
    }

    private void deleteFile(File file) {
        if (null != file && file.exists())
            file.delete();
        log.log("File was deleted. Is file exist: " + file.exists() +
                "\nDeleted file path: " + file.getAbsolutePath());
    }

    private void saveSegmentationResults(List<ResultItem> segmentationResults) {
        ResultItemDao resultItemDao = MLDemoApp.getInstance().getDatabase().resultItemDao();
        for (ResultItem resultItem : segmentationResults) {
            resultItem.setTfLiteParentFile(fragment.getTfLiteItem().getTfFilePath());
            resultItemDao.insert(resultItem);
        }
        log.log("Segmentation results are saved");
    }
}