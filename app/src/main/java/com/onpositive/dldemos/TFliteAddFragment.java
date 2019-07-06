package com.onpositive.dldemos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.onpositive.dldemos.data.TFModelType;
import com.onpositive.dldemos.tools.Logger;
import com.onpositive.dldemos.tools.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static android.app.Activity.RESULT_OK;
import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class TFliteAddFragment extends Fragment {

    public static final String ARG_SECTION_NUMBER = "section_number";
    private static final int READ_REQUEST_CODE = 5;
    private static Logger logger = new Logger(TFliteAddFragment.class);
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

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static TFliteAddFragment newInstance(int sectionNumber) {
        TFliteAddFragment fragment = new TFliteAddFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        logger.log("Created fragment on position: " + sectionNumber);
        return fragment;
    }

    @OnCheckedChanged({R.id.segmentation_rb, R.id.classification_rb})
    public void onRadioButtonCheckChanged(RadioButton radioButton, boolean checked) {
        if (checked) {
            switch (radioButton.getId()) {
                case R.id.segmentation_rb:
                    tfModelType = TFModelType.SEGMENTATION;
                    logger.log("Selected TFModelType is: " + TFModelType.SEGMENTATION.toString());
                    break;
                case R.id.classification_rb:
                    tfModelType = TFModelType.CLASSIFICATION;
                    logger.log("Selected TFModelType is: " + TFModelType.CLASSIFICATION.toString());
                    break;
            }
        }
    }

    @OnTextChanged(value = R.id.size_x, callback = AFTER_TEXT_CHANGED)
    public void widthTextChanged(Editable s) {
        if (!s.toString().isEmpty()) {
            size_x = Integer.valueOf(s.toString());
            logger.log("User input size_x: " + size_x);
        }
    }

    @OnTextChanged(value = R.id.size_y, callback = AFTER_TEXT_CHANGED)
    public void heightTextChanged(Editable s) {
        if (!s.toString().isEmpty()) {
            size_y = Integer.valueOf(s.toString());
            logger.log("User input size_y: " + size_y);
        }
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
                    logger.log("Wrong settings for tflite file: " + errMsg);
                    Toast.makeText(getContext(), errMsg, Toast.LENGTH_LONG).show();
                    return;
                }
                logger.log("Select interpreter button pressed.");
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
                logger.log("Selected file is not interpreter.");
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
        logger.log("onCreateView executed");
        return rootView;
    }

    public void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
        logger.log("Started ACTION_OPEN_DOCUMENT with READ_REQUEST_CODE");
    }

    public void showDownloading(boolean isDownloading) {
        if (isDownloading) {
            tfliteAddProgress.setVisibility(View.VISIBLE);
            tfliteAddStatus.setVisibility(View.VISIBLE);
            tfliteAddStatus.setText(R.string.tflite_add_downloading);
            logger.log("Download status views are visible");
        } else {
            tfliteAddProgress.setVisibility(View.INVISIBLE);
            tfliteAddStatus.setVisibility(View.INVISIBLE);
            tfliteAddStatus.setText("");
            logger.log("Download status views are invisible");
        }
    }

    public TFModelType getTfModelType() {
        return tfModelType;
    }

    public void setTfModelType(TFModelType tfModelType) {
        this.tfModelType = tfModelType;
    }

    public int getSize_x() {
        return size_x;
    }

    public void setSize_x(int size_x) {
        this.size_x = size_x;
    }

    public int getSize_y() {
        return size_y;
    }

    public void setSize_y(int size_y) {
        this.size_y = size_y;
    }
}