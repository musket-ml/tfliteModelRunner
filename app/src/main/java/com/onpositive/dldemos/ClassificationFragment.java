package com.onpositive.dldemos;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.onpositive.dldemos.data.ClassificationRVAdapter;
import com.onpositive.dldemos.data.ClassificationResultItem;
import com.onpositive.dldemos.data.ClassificationResultItemDao;
import com.onpositive.dldemos.data.ContentType;
import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.interpreter.ImageClassifier;
import com.onpositive.dldemos.tools.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

public class ClassificationFragment extends TFModelFragment {

    private static Logger log = new Logger(ClassificationFragment.class);
    @BindView(R.id.photoBtn)
    public Button makePhotoBtn;
    @BindView(R.id.classificationProgressBar)
    public ProgressBar progressBar;
    @BindView(R.id.classificationStatusTV)
    public TextView progressStatusTV;
    @BindView(R.id.classification_rv)
    RecyclerView classificationRV;
    RecyclerView.Adapter classifyRvAdapter;
    List<ClassificationResultItem> classifyResultList;
    private ClassificationAsyncTask cat;

    public static ClassificationFragment newInstance(TFLiteItem tfLiteItem, int sectionNumber) {
        ClassificationFragment fragment = new ClassificationFragment();
        fragment.tfLiteItem = tfLiteItem;
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        log.log("Created fragment on position: " + sectionNumber);
        return fragment;
    }

    @OnClick({R.id.photoBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.photoBtn:
                log.log("Photo button pressed");
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PHOTO_PERMISSION_REQUEST_CODE);
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_classification, container, false);
        ButterKnife.bind(this, rootView);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        classificationRV.setLayoutManager(new LinearLayoutManager(this.getContext()));
        classificationRV.setItemAnimator(itemAnimator);
        log.log("onCreateView");
        try {
            ClassificationResultItemDao resultItemDao = MLDemoApp.getInstance().getDatabase().classificationResultItemDao();
            classifyResultList = resultItemDao.getAllByParentTF(tfLiteItem.getTfFilePath());
            Collections.sort(classifyResultList);
            Collections.reverse(classifyResultList);
            classifyRvAdapter = new ClassificationRVAdapter(this.getContext(), classifyResultList);
            classificationRV.setAdapter(classifyRvAdapter);
            log.log("Classification Recycler View content uploaded");
        } catch (Exception e) {
            log.error("Classification initialization failed:\n" + e.getMessage());
        }
        log.log("ClassificationFragment created");
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        log.log("onActivityResult. Request code: " + requestCode + ", resultCode: " + resultCode);
        ImageClassifier classifier = null;
        try {
            classifier = new ImageClassifier(this.getActivity(), tfLiteItem);
        } catch (IOException e) {
            log.log("ImageClassifier creation failed: " + e.getMessage());
        }
        if (null != classifier) {
            log.log("ImageClassifier created. ");
            cat = new ClassificationAsyncTask(classifier, this);
        } else
            log.log("Classification failed. ImageClassifier object is null.");
        if (resultCode != RESULT_OK) {
            log.log("onActivityResult failed");
            return;
        }
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                log.log(" got result from camera. currentPhotoPath: " + currentPhotoPath);
                try {
                    if (null != cat) {
                        cat.execute(ContentType.IMAGE);
                        log.log("Classification Async Task execution started");
                    } else
                        log.log("Classification failed. ClassificationAsyncTask object is null.");
                } catch (Exception e) {
                    log.log("Photo classification failed: " + e.getMessage());
                }
                break;
        }
    }
}
