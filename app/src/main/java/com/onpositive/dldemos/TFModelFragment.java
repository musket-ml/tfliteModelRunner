package com.onpositive.dldemos;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.onpositive.dldemos.data.ResultItem;
import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.tools.Logger;
import com.onpositive.dldemos.tools.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class TFModelFragment extends Fragment {
    public static final String ARG_SECTION_NUMBER = "section_number";
    protected static final int REQUEST_VIDEO_CAPTURE = 1;
    protected static final int REQUEST_IMAGE_CAPTURE = 2;
    protected static final int PHOTO_PERMISSION_REQUEST_CODE = 3;
    protected static final int VIDEO_PERMISSION_REQUEST_CODE = 4;
    protected String currentPhotoPath;
    protected String currentVideoPath;
    protected TFLiteItem tfLiteItem;
    private Logger log = new Logger(this.getClass());

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
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
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

    void dispatchTakeVideoIntent() {
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

    void dispatchTakePictureIntent() {
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
