package com.onpositive.dldemos;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.onpositive.dldemos.data.AppDatabase;
import com.onpositive.dldemos.data.ContentType;
import com.onpositive.dldemos.data.RecyclerViewAdapter;
import com.onpositive.dldemos.data.ResultItem;
import com.onpositive.dldemos.data.ResultItemDao;
import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.data.TFLiteItemDao;
import com.onpositive.dldemos.data.TFModelType;
import com.onpositive.dldemos.segmentation.Segmentator;
import com.onpositive.dldemos.segmentation.VideoSegmentator;
import com.onpositive.dldemos.tools.Logger;
import com.onpositive.dldemos.tools.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;


public class MainActivity extends AppCompatActivity {

    private static String currentPhotoPath;
    private static String currentVideoPath;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.container)
    ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private Logger log = new Logger(this.getClass());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        tabLayout.setupWithViewPager(mViewPager);
        //TODO fix progress showing
        //TODO model properties image size(input/output)
        //TODO fragment model for image classification(find test model for this task)
        //TODO remove models tabs
        log.log("onCreate executed");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class TFliteAddFragment extends Fragment {

        public static final String ARG_SECTION_NUMBER = "section_number";
        private static final int READ_REQUEST_CODE = 5;
        @BindView(R.id.tflite_add_title)
        TextView tfliteAddTitelTV;
        @BindView(R.id.model_type_rg)
        RadioGroup modelTypeRG;
        @BindView(R.id.size_x)
        TextView modelInputWidth;
        @BindView(R.id.size_y)
        TextView modelInputHeight;
        @BindView(R.id.btn_tf_select)
        Button btnTFSelect;
        @BindView(R.id.tflite_progress_bar)
        ProgressBar tfliteAddProgress;
        @BindView(R.id.tflite_add_status)
        TextView tfliteAddStatus;
        TFLiteDownloaderAT tfldat;
        private TFModelType tfModelType = null;
        private int size_x = 0;
        private int size_y = 0;

        public TFliteAddFragment() {
        }
        private Logger logger = new Logger(this.getClass());

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static TFliteAddFragment newInstance(int sectionNumber) {
            TFliteAddFragment fragment = new TFliteAddFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
//            log.log("Created fragment on position: " + sectionNumber);
            return fragment;
        }

        @OnCheckedChanged({R.id.segmentation_rb, R.id.classification_rb})
        public void onRadioButtonCheckChanged(RadioButton radioButton, boolean checked) {
            if (checked) {
                switch (radioButton.getId()) {
                    case R.id.segmentation_rb:
                        tfModelType = TFModelType.SEGMENTATION;
                        break;
                    case R.id.classification_rb:
                        tfModelType = TFModelType.CLASSIFICATION;
                        break;
                }
            }
        }

        @OnTextChanged(value = R.id.size_x, callback = AFTER_TEXT_CHANGED)
        public void widthTextChanged(Editable s) {
            if (!s.toString().isEmpty())
                size_x = Integer.valueOf(s.toString());
        }

        @OnTextChanged(value = R.id.size_y, callback = AFTER_TEXT_CHANGED)
        public void heightTextChanged(Editable s) {
            if (!s.toString().isEmpty())
                size_y = Integer.valueOf(s.toString());
        }

        @OnClick(R.id.btn_tf_select)
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_tf_select:
                    String errMsg = "";
                    boolean hasErr = false;
                    if (tfliteAddTitelTV.getText().length() < 1) {
                        errMsg += getContext().getString(R.string.tflite_add_empty_title_msg) + "\n";
                        hasErr = true;
                    }
                    if (tfModelType == null) {
                        errMsg += getContext().getString(R.string.tflite_add_empty_type_msg) + "\n";
                        hasErr = true;
                    }
                    if (modelInputWidth.getText().length() < 1 || modelInputHeight.getText().length() < 1) {
                        errMsg += getContext().getString(R.string.tflite_add_empty_size_msg) + "\n";
                        hasErr = true;
                    }
                    if (hasErr) {
                        Toast.makeText(getContext(), errMsg, Toast.LENGTH_LONG).show();
                        return;
                    }
                    logger.log("Select tflite button pressed.");
                    performFileSearch();
                    break;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                String fileName = Utils.getFileName(getActivity(), uri);
                logger.log("Selected file: " + fileName);
                if (!fileName.endsWith(".tflite")) {
                    Toast.makeText(getContext(), R.string.tflite_add_wrong_msg, Toast.LENGTH_LONG).show();
                    logger.log("Selected file is not tflite.");
                    return;
                }
                tfldat = new TFLiteDownloaderAT(this);
                tfldat.execute(uri);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tflite_add, container, false);
            ButterKnife.bind(this, rootView);
            return rootView;
        }

        public void performFileSearch() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            startActivityForResult(intent, READ_REQUEST_CODE);
        }

        public void showDownloading(boolean isDownloading) {
            if (isDownloading) {
                tfliteAddProgress.setVisibility(View.VISIBLE);
                tfliteAddStatus.setVisibility(View.VISIBLE);
                tfliteAddStatus.setText(R.string.tflite_add_downloading);
            } else {
                tfliteAddProgress.setVisibility(View.INVISIBLE);
                tfliteAddStatus.setVisibility(View.INVISIBLE);
                tfliteAddStatus.setText("");
            }
        }
    }

    public static class SegmentationFragment extends Fragment {

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
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.model_fragment_menu, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.remove_current_tab:
                    showRemoveModelDialog();
                    return true;
                default:
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

            try {
                ResultItemDao resultItemDao = MLDemoApp.getInstance().getDatabase().resultItemDao();
                segResultList = resultItemDao.getAllByParentTF(tfLiteItem.getTfFilePath());
                Collections.sort(segResultList);
                Collections.reverse(segResultList);
                segRvAdapter = new RecyclerViewAdapter(this.getContext(), segResultList);
                segmentationRV.setAdapter(segRvAdapter);
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
            } catch (IOException e) {
                e.printStackTrace();
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

            ((MainActivity) getActivity()).mSectionsPagerAdapter.refreshDataSet();
            ((MainActivity) getActivity()).mSectionsPagerAdapter.notifyDataSetChanged();
        }
    }

    public static class SegmentationAsyncTask extends AsyncTask<ContentType, Integer, List<ResultItem>> {

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
                        segmentator.getSegmentedImage(currentPhotoPath)
                                .compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        stream.flush();
                        stream.close();
                        String thumbnailPath = Utils.createThumbnail(fragment.getActivity(), resultImageFile.getAbsolutePath(), ContentType.IMAGE);
                        segmentationResults.add(new ResultItem(resultImageFile.getAbsolutePath(), ContentType.IMAGE, thumbnailPath));
                        deleteFile(new File(currentPhotoPath));
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
                        String segmentedVideoPath = segmentator.getSegmentedVideoPath(currentVideoPath);
                        if (isCanceled) {
                            log.log("Segmentation canceled. Segmented video was deleted. Is file exist: " + new File(segmentedVideoPath).exists());
                        } else {
                            String thumbnailPath = Utils.createThumbnail(fragment.getActivity(), segmentedVideoPath, ContentType.VIDEO);
                            segmentationResults.add(new ResultItem(segmentedVideoPath, ContentType.VIDEO, thumbnailPath));
                            deleteFile(new File(currentVideoPath));
                            log.log("Video segmented successfully. Video file path:" + segmentedVideoPath);
                        }
                    } catch (Exception e) {
                        log.log("Failed video segmentation AsyncTask:\n" + e.getMessage());
                    }
                    break;
            }
            saveSegmentationResults(segmentationResults);
            return segmentationResults;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            fragment.progressBar.setVisibility(View.VISIBLE);
            fragment.progressStatusTV.setText(R.string.calculating);
            fragment.progressStatusTV.setVisibility(View.VISIBLE);
            fragment.cancelBtn.setVisibility(View.VISIBLE);
            log.log("Segmentation Async Task onPreExecute");
        }

        @Override
        protected void onPostExecute(List<ResultItem> resultItemList) {
            super.onPostExecute(resultItemList);
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
                resultItem.setTfLiteParentFile(fragment.tfLiteItem.getTfFilePath());
                resultItemDao.insert(resultItem);
            }
        }
    }

    public static class TFLiteDownloaderAT extends AsyncTask<Uri, Integer, Void> {

        private TFliteAddFragment fragment;
        private Logger logger = new Logger(TFLiteDownloaderAT.class);

        public TFLiteDownloaderAT(TFliteAddFragment fragment) {
            this.fragment = fragment;
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            downloadFile(uris[0]);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            fragment.showDownloading(true);
        }

        @Override
        protected void onPostExecute(Void v) {
            fragment.showDownloading(false);
            ((MainActivity) fragment.getActivity()).mSectionsPagerAdapter.refreshDataSet();
            ((MainActivity) fragment.getActivity()).mSectionsPagerAdapter.notifyDataSetChanged();
        }

        private void downloadFile(Uri uri) {
            File path = fragment.getContext().getExternalFilesDir("TFLite");
            path.mkdirs();
            File outFile = new File(path, Utils.getFileName(fragment.getContext(), uri));
            try {
                InputStream is = fragment.getContext().getContentResolver().openInputStream(uri);
                OutputStream os = new FileOutputStream(outFile);
                byte[] data = new byte[is.available()];
                is.read(data);
                os.write(data);
                is.close();
                os.close();

                TFLiteItemDao tfLiteDao = MLDemoApp.getInstance().getDatabase().tfLiteItemDao();
                tfLiteDao.insert(new TFLiteItem(
                        outFile.getAbsolutePath(),
                        fragment.tfliteAddTitelTV.getText().toString(),
                        fragment.tfModelType,
                        fragment.size_x,
                        fragment.size_y));
                logger.log("Downloaded file path: " + outFile);
            } catch (IOException e) {
                logger.log("File downloading failed. Error: " + e);
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private AppDatabase db;
        private TFLiteItemDao tfLiteItemDao;
        private List<TFLiteItem> tfLiteItems;
        private boolean hasTFLadd = false;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            db = MLDemoApp.getInstance().getDatabase();
            tfLiteItemDao = db.tfLiteItemDao();
            tfLiteItems = tfLiteItemDao.getAll();
        }

        @Override
        public Fragment getItem(int position) {
            if (position + 1 == getCount() && !hasTFLadd) {
                hasTFLadd = true;
                log.log("Loaded fragment for adding a tflite file");
                return TFliteAddFragment.newInstance(position);
            } else if (position + 1 == getCount() && hasTFLadd) {
                return SegmentationFragment.newInstance(tfLiteItems.get(position - 1), position);
            } else {
                return SegmentationFragment.newInstance(tfLiteItems.get(position), position);
            }
        }

        @Override
        public int getCount() {
            return tfLiteItems.size() + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position < getCount() - 1) {
                return tfLiteItems.get(position).getTitle();
            } else if (position == getCount() - 1) {
                return getResources().getString(R.string.add_new);
            } else
                return null;
        }

        public void refreshDataSet() {
            tfLiteItems = tfLiteItemDao.getAll();
        }
    }
}
