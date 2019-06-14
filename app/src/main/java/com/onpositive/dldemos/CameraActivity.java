package com.onpositive.dldemos;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.camerakit.CameraKitView;
import com.onpositive.dldemos.camera.MediaContainer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraActivity extends AppCompatActivity {

    private CameraKitView cameraKitView;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    static final int REQUEST_IMAGE_CAPTURE = 2;


    @BindView(R.id.toggleBtn)
    public Button toggleCameraBtn;
    @BindView(R.id.photoBtn)
    public Button makePhotoBtn;
    @BindView(R.id.videoBtn)
    public Button captureVideoBtn;

    @OnClick({R.id.photoBtn, R.id.videoBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.photoBtn:
                dispatchTakePictureIntent();
                break;
            case R.id.videoBtn:
                dispatchTakeVideoIntent();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);
        ButterKnife.bind(this);
        cameraKitView = findViewById(R.id.camera);
    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraKitView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraKitView.onResume();
    }

    @Override
    protected void onPause() {
        cameraKitView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        cameraKitView.onStop();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void makeImage(CameraKitView cameraKitView, byte[] bytes) {
        MediaContainer.dispose();
        MediaContainer.setImage(bytes);
        MediaContainer.setNativeCaptureSize(new CameraKitView.Size(cameraKitView.getWidth(), cameraKitView.getHeight()));

        Intent intent = new Intent(this.getBaseContext(), PreviewActivity.class);
        this.getBaseContext().startActivity(intent);
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

}
