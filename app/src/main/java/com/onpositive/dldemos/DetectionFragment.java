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

import com.onpositive.dldemos.data.ContentType;
import com.onpositive.dldemos.data.DetectionRVAdapter;
import com.onpositive.dldemos.data.DetectionResultItem;
import com.onpositive.dldemos.data.DetectionResultItemDao;
import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.interpreter.ImageDetector;
import com.onpositive.dldemos.tools.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

public class DetectionFragment extends TFModelFragment {

    private static Logger log = new Logger(DetectionFragment.class);
    @BindView(R.id.photoBtn)
    public Button makePhotoBtn;
    @BindView(R.id.detectionProgressBar)
    public ProgressBar progressBar;
    @BindView(R.id.detectionStatusTV)
    public TextView progressStatusTV;
    @BindView(R.id.detection_rv)
    RecyclerView detectionRV;
    RecyclerView.Adapter detectionRvAdapter;
    List<DetectionResultItem> detectionResultList;
    private DetectionAsyncTask dat;

    public static DetectionFragment newInstance(TFLiteItem tfLiteItem, int sectionNumber) {
        DetectionFragment fragment = new DetectionFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_detection, container, false);
        ButterKnife.bind(this, rootView);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        detectionRV.setLayoutManager(new LinearLayoutManager(this.getContext()));
        detectionRV.setItemAnimator(itemAnimator);
        log.log("onCreateView");
        try {
            DetectionResultItemDao resultItemDao = MLDemoApp.getInstance().getDatabase().detectionResultItemDao();
            detectionResultList = resultItemDao.getAllByParentTF(tfLiteItem.getTfFilePath());
            Collections.sort(detectionResultList);
            Collections.reverse(detectionResultList);
            detectionRvAdapter = new DetectionRVAdapter(this.getContext(), detectionResultList);
            detectionRV.setAdapter(detectionRvAdapter);
            log.log("Detection Recycler View content uploaded");
        } catch (Exception e) {
            log.error("Detection initialization failed:\n" + e.getMessage());
        }
        log.log("DetectionFragment created");
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        log.log("onActivityResult. Request code: " + requestCode + ", resultCode: " + resultCode);
        ImageDetector detector = null;
        try {
            detector = new ImageDetector(this.getActivity(), tfLiteItem);
        } catch (IOException e) {
            log.log("ImageDetector creation failed: " + e.getMessage());
        }
        if (null != detector) {
            log.log("ImageDetector created. ");
            dat = new DetectionAsyncTask(detector, this);
        } else
            log.log("Object detection failed. ImageDetector object is null.");
        if (resultCode != RESULT_OK) {
            log.log("onActivityResult failed");
            return;
        }
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                log.log(" got result from camera. currentPhotoPath: " + currentPhotoPath);
                try {
                    if (null != dat) {
                        dat.execute(ContentType.IMAGE);
                        log.log("Detection Async Task execution started");
                    } else
                        log.log("Detection failed. ClassificationAsyncTask object is null.");
                } catch (Exception e) {
                    log.log("Object detection failed: " + e.getMessage());
                }
                break;
        }
    }
}