package com.onpositive.dldemos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.onpositive.dldemos.camera.MediaContainer;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PreviewActivity extends AppCompatActivity {
    @BindView(R.id.image)
    ImageView imageView;
    @BindView(R.id.video)
    VideoView videoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_activity);
        ButterKnife.bind(this);

        byte[] jpeg = MediaContainer.getImage();
        File video = MediaContainer.getVideo();

        if (jpeg != null) {
            imageView.setVisibility(View.VISIBLE);

            Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

            if (bitmap == null) {
                finish();
                return;
            }

            imageView.setImageBitmap(bitmap);
        } else if (video != null) {
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(Uri.parse(video.getAbsolutePath()));
            MediaController mediaController = new MediaController(this);
            mediaController.setVisibility(View.GONE);
            videoView.setMediaController(mediaController);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                    mp.start();

                    float multiplier = (float) videoView.getWidth() / (float) mp.getVideoWidth();
                    videoView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (mp.getVideoHeight() * multiplier)));
                }
            });
            //videoView.start();
        } else {
            finish();
        }
    }
}
