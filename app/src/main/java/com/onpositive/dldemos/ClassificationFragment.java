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

import com.onpositive.dldemos.classification.ImageClassifier;
import com.onpositive.dldemos.data.ClassificationRVAdapter;
import com.onpositive.dldemos.data.ClassificationResultItem;
import com.onpositive.dldemos.data.ClassificationResultItemDao;
import com.onpositive.dldemos.data.ContentType;
import com.onpositive.dldemos.data.ResultItem;
import com.onpositive.dldemos.data.TFLiteItem;
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

public class ClassificationFragment extends Fragment {

    public static final String ARG_SECTION_NUMBER = "section_number";
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int PHOTO_PERMISSION_REQUEST_CODE = 3;
    private static Logger log = new Logger(ClassificationFragment.class);
    @BindView(R.id.photoBtn)
    public Button makePhotoBtn;
    @BindView(R.id.classification_rv)
    RecyclerView classificationRV;
    RecyclerView.Adapter classifyRvAdapter;
    List<ClassificationResultItem> classifyResultList;
    private String currentPhotoPath;
    private ClassificationAsyncTask cat;
    private TFLiteItem tfLiteItem;

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
                log.log("onOptionsItemSelected super.onOptionsItemSelected");
                return super.onOptionsItemSelected(item);
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
            log.error("Segmentation initialization failed:\n" + e.getMessage());
        }
        log.log("SegmentationFragment created");
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
        log.log("Image file created");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoURI = FileProvider.getUriForFile(getActivity(),
                "com.onpositive.dldemos.fileprovider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            log.log("Starting takePictureIntent");
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
        log.log("TFLite Model deleted with its files");
    }

    public TFLiteItem getTfLiteItem() {
        return tfLiteItem;
    }

    public void setTfLiteItem(TFLiteItem tfLiteItem) {
        this.tfLiteItem = tfLiteItem;
    }

    public String getCurrentPhotoPath() {
        return currentPhotoPath;
    }

    public void setCurrentPhotoPath(String currentPhotoPath) {
        this.currentPhotoPath = currentPhotoPath;
    }
}