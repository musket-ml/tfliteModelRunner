package com.onpositive.dldemos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.onpositive.dldemos.classification.ImageClassifier;
import com.onpositive.dldemos.data.ClassificationResultItem;
import com.onpositive.dldemos.data.ClassificationResultItemDao;
import com.onpositive.dldemos.data.ContentType;
import com.onpositive.dldemos.tools.Logger;
import com.onpositive.dldemos.tools.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

public class ClassificationAsyncTask extends AsyncTask<ContentType, Integer, ClassificationResultItem> {

    private static Logger log = new Logger(MainActivity.SegmentationAsyncTask.class);
    ClassificationResultItem classificationRI = null;
    ContentType contentType;
    private boolean isCanceled = false;
    private ClassificationFragment fragment;
    @Nullable
    private ImageClassifier classifier;

    public ClassificationAsyncTask(ImageClassifier classifier, ClassificationFragment fragment) {
        this.classifier = classifier;
        this.fragment = fragment;
    }

    @Override
    protected ClassificationResultItem doInBackground(ContentType... contentTypes) {
        switch (contentTypes[0]) {
            case IMAGE:
                contentType = ContentType.IMAGE;
                File resultImageFile = null;
                try {
                    resultImageFile = Utils.createImageFile(fragment.getActivity());
                    FileOutputStream stream = new FileOutputStream(resultImageFile);
                    List<ImageClassifier.Classification> classificationList = classifier.recognizeImage(BitmapFactory.decodeFile(fragment.getCurrentPhotoPath()));
                    BitmapFactory.decodeFile(fragment.getCurrentPhotoPath()).compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    stream.flush();
                    stream.close();
                    String thumbnailPath = Utils.createThumbnail(fragment.getActivity(), resultImageFile.getAbsolutePath(), ContentType.IMAGE);
                    classificationRI = new ClassificationResultItem(resultImageFile.getAbsolutePath(), ContentType.IMAGE, thumbnailPath);
                    Collections.sort(classificationList);
                    classificationRI.setClassificationResultList(classificationList);
                    deleteFile(new File(fragment.getCurrentPhotoPath()));
                    log.log("Image classified successfully. Image file path:" + resultImageFile.getAbsolutePath());
                } catch (Exception e) {
                    log.log("Failed image classification AsyncTask:\n" + e.getMessage());
                }
                break;
        }
        if (null != classificationRI)
            saveClassificationResults(classificationRI);
        return classificationRI;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        log.log("Segmentation Async Task onPreExecute");
    }

    @Override
    protected void onPostExecute(ClassificationResultItem classificationResultItem) {
        super.onPostExecute(classificationResultItem);

        fragment.classifyResultList.add(classificationResultItem);
        Collections.sort(fragment.classifyResultList);
        Collections.reverse(fragment.classifyResultList);
        fragment.classifyRvAdapter.notifyDataSetChanged();
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

    private void saveClassificationResults(ClassificationResultItem classificationResultItem) {
        ClassificationResultItemDao resultItemDao = MLDemoApp.getInstance().getDatabase().classificationResultItemDao();
        classificationResultItem.setTfLiteParentFile(fragment.getTfLiteItem().getTfFilePath());
        resultItemDao.insert(classificationResultItem);
        log.log("ClassificationResults are saved");
    }
}