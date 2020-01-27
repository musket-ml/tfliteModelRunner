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
import com.onpositive.dldemos.data.RecyclerViewAdapter;
import com.onpositive.dldemos.data.ResultItem;
import com.onpositive.dldemos.data.ResultItemDao;
import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.interpreter.Segmentator;
import com.onpositive.dldemos.interpreter.VideoSegmentator;
import com.onpositive.dldemos.tools.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

public class SegmentationFragment extends TFModelFragment {

    private static Logger log = new Logger(SegmentationFragment.class);
    @BindView(R.id.photoBtn)
    public Button makePhotoBtn;
    @BindView(R.id.videoBtn)
    public Button captureVideoBtn;
    @BindView(R.id.cancelBtn)
    public Button cancelBtn;
    @BindView(R.id.segmentationProgressBar)
    public ProgressBar progressBar;
    @BindView(R.id.progerssStatusTV)
    public TextView progressStatusTV;
    @BindView(R.id.segmentationRV)
    RecyclerView segmentationRV;
    RecyclerView.Adapter segRvAdapter;
    List<ResultItem> segResultList;
    private SegmentationAsyncTask sat;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SegmentationFragment newInstance(TFLiteItem tfLiteItem, int sectionNumber) {
        SegmentationFragment fragment = new SegmentationFragment();
        fragment.tfLiteItem = tfLiteItem;
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        log.log("Created fragment on position: " + sectionNumber);
        return fragment;
    }

    @OnClick({R.id.photoBtn, R.id.videoBtn, R.id.cancelBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.photoBtn:
                log.log("Photo button pressed");
//                if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) //TODO add TOAST with message that this function works only with CAMERA, if camera not found.
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PHOTO_PERMISSION_REQUEST_CODE);
                break;
            case R.id.videoBtn:
                //TODO add TOAST with message that this function works only with CAMERA, if camera not found.
                log.log("Video button pressed");
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        VIDEO_PERMISSION_REQUEST_CODE);
                break;
            case R.id.cancelBtn:
                moveRVDown(segmentationRV);
                log.log("Cancel button pressed");
                if (sat == null)
                    return;
                sat.cancel(true);
                progressStatusTV.setText(R.string.canceling);
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_segmentation, container, false);
        ButterKnife.bind(this, rootView);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        segmentationRV.setLayoutManager(new LinearLayoutManager(this.getContext()));
        segmentationRV.setItemAnimator(itemAnimator);
        log.log("onCreateView");
        try {
            ResultItemDao resultItemDao = MLDemoApp.getInstance().getDatabase().resultItemDao();
            segResultList = resultItemDao.getAllByParentTF(tfLiteItem.getTfFilePath());
            Collections.sort(segResultList);
            Collections.reverse(segResultList);
            segRvAdapter = new RecyclerViewAdapter(this.getContext(), segResultList);
            segmentationRV.setAdapter(segRvAdapter);
            log.log("Segmentation Recycler View content upload successful");
        } catch (Exception e) {
            log.error("Segmentation initialization failed:\n" + e.getMessage());
        }
        log.log("SegmentationFragment created");
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            log.log("onActivityResult failed");
            return;
        }
        Segmentator segmentator = null;
        try {
            segmentator = new VideoSegmentator(this.getActivity(), tfLiteItem);
            log.log("VideoSegmentator initialization successful");
        } catch (IOException e) {
            log.log("VideoSegmentator initialization failed " + e.getMessage());
        }
        sat = new SegmentationAsyncTask(segmentator, this);
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                log.log(" got result from camera. currentPhotoPath: " + currentPhotoPath);
                try {
                    sat.execute(ContentType.IMAGE);
                    log.log("SegmentationAsyncTask started");
                } catch (Exception e) {
                    log.log("Photo segmentation failed: " + e.getMessage());
                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                }
                break;
            case REQUEST_VIDEO_CAPTURE:
                log.log(" got result from camera. currentVideoPath: " + currentVideoPath);
                try {
                    sat.execute(ContentType.VIDEO);
                } catch (Exception e) {
                    log.log("Video segmentation failed: \n" + e.getMessage());
                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                }
                break;
        }
    }
}