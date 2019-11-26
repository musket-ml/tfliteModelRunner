package com.onpositive.dldemos;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.onpositive.dldemos.data.ContentType;
import com.onpositive.dldemos.data.RecyclerViewAdapter;
import com.onpositive.dldemos.data.ResultItem;
import com.onpositive.dldemos.data.ResultItemDao;
import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.segmentation.Segmentator;
import com.onpositive.dldemos.segmentation.VideoSegmentator;
import com.onpositive.dldemos.tools.Logger;
import com.onpositive.dldemos.tools.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

public class SegmentationFragment extends Fragment {

    public static final String ARG_SECTION_NUMBER = "section_number";
    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int PHOTO_PERMISSION_REQUEST_CODE = 3;
    private static final int VIDEO_PERMISSION_REQUEST_CODE = 4;
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
    private String currentPhotoPath;
    private String currentVideoPath;
    private SegmentationAsyncTask sat;
    private TFLiteItem tfLiteItem;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        log.log("onCreate executed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.model_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        log.log("onCreateOptionsMenu executed");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.remove_current_tab:
                log.log("onOptionsItemSelected. showRemoveModelDialog");
                showRemoveModelDialog();
                return true;
            default:
                log.log("super.onOptionsItemSelected");
                return super.onOptionsItemSelected(item);
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
        Segmentator segmentator = null;
        try {
            segmentator = new VideoSegmentator(this.getActivity(), tfLiteItem);
            log.log("VideoSegmentator initialization successful");
        } catch (IOException e) {
            log.log("VideoSegmentator initialization failed " + e.getMessage());
        }
        sat = new SegmentationAsyncTask(segmentator, this);
        if (resultCode != RESULT_OK) {
            log.log("onActivityResult failed");
            return;
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PHOTO_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    log.log("Photo permission granted");
                    dispatchTakePictureIntent();
                }
                break;
            }
            case VIDEO_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    log.log("Video permission granted");
                    dispatchTakeVideoIntent();
                }
                break;
            }
        }
    }

    private void dispatchTakeVideoIntent() {
        File videoFile = null;
        try {
            videoFile = Utils.createVideoFile(getActivity());
            currentVideoPath = videoFile.getAbsolutePath();
        } catch (IOException ex) {
            log.log("Video file creation failed");
        }
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        Uri videoURI = FileProvider.getUriForFile(getActivity(),
                "com.onpositive.dldemos.fileprovider",
                videoFile);
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);

        if (takeVideoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            log.log("Started intent: takeVideoIntent");
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    private void dispatchTakePictureIntent() {
        File photoFile = null;
        try {
            photoFile = Utils.createImageFile(getActivity());
            currentPhotoPath = photoFile.getAbsolutePath();
        } catch (IOException ex) {
            log.log("Image file creation failed");
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoURI = FileProvider.getUriForFile(getActivity(),
                "com.onpositive.dldemos.fileprovider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            log.log("Started intent: takeVideoIntent");
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void showRemoveModelDialog() {
        new AlertDialog.Builder(this.getActivity())
                .setTitle(getResources().getString(R.string.remove_alert_title))
                .setMessage(
                        getResources().getString(R.string.remove_alert_message))
                .setPositiveButton(
                        getResources().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                removeModel(tfLiteItem);
                            }
                        })
                .setNegativeButton(
                        getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }
                        }).show();
        log.log("RemoveModelDialog is active");
    }

    private void removeModel(TFLiteItem tfLiteItem) {
        List<ResultItem> segmentItemList = MLDemoApp.getInstance().getDatabase().resultItemDao()
                .getAllByParentTF(tfLiteItem.getTfFilePath());
        for (ResultItem resultItem : segmentItemList) {
            File resultItemFile = new File(resultItem.getFilePath());
            if (resultItemFile.exists())
                resultItemFile.delete();
            MLDemoApp.getInstance().getDatabase().resultItemDao().delete(resultItem);
        }

        File modelFile = new File(tfLiteItem.getTfFilePath());
        if (modelFile.exists())
            modelFile.delete();
        MLDemoApp.getInstance().getDatabase().tfLiteItemDao().delete(tfLiteItem);

        ((MainActivity) getActivity()).getSectionsPagerAdapter().refreshDataSet();
        ((MainActivity) getActivity()).getSectionsPagerAdapter().notifyDataSetChanged();
        Intent intent = this.getActivity().getIntent();
        this.getActivity().finish();
        this.getActivity().startActivity(intent);
        log.log("TFLite Model deleted with its files");
    }

    public void moveRVDown(RecyclerView rv) {
        setMargins(rv, 8, 56, 8, 0);
        log.log("Recycler View moved down");
    }

    public void moveRVUp(RecyclerView rv) {
        setMargins(rv, 8, 8, 8, 0);
        log.log("Recycler View moved up");
    }

    private void setMargins(View v, int l, int t, int r, int b) {
        final float scale = this.getActivity().getBaseContext().getResources().getDisplayMetrics().density;
        int left = (int) (l * scale + 0.5f);
        int right = (int) (r * scale + 0.5f);
        int top = (int) (t * scale + 0.5f);
        int bottom = (int) (b * scale + 0.5f);
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            v.requestLayout();
        }
        log.log("Recycler View top marging changed to: " + top + "dp");
    }

    public String getCurrentPhotoPath() {
        return currentPhotoPath;
    }

    public void setCurrentPhotoPath(String currentPhotoPath) {
        this.currentPhotoPath = currentPhotoPath;
    }

    public String getCurrentVideoPath() {
        return currentVideoPath;
    }

    public void setCurrentVideoPath(String currentVideoPath) {
        this.currentVideoPath = currentVideoPath;
    }

    public TFLiteItem getTfLiteItem() {
        return tfLiteItem;
    }

    public void setTfLiteItem(TFLiteItem tfLiteItem) {
        this.tfLiteItem = tfLiteItem;
    }
}