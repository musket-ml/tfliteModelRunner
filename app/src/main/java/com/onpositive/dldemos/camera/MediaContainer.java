package com.onpositive.dldemos.camera;

import android.support.annotation.Nullable;

import com.camerakit.CameraKitView.Size;

import java.io.File;

public class MediaContainer {
    private static byte[] image;
    private static File video;
    private static Size nativeCaptureSize;

    @Nullable
    public static byte[] getImage() {
        return image;
    }

    public static void setImage(@Nullable byte[] image) {
        MediaContainer.image = image;
    }

    @Nullable
    public static File getVideo() {
        return video;
    }

    public static void setVideo(@Nullable File video) {
        MediaContainer.video = video;
    }

    @Nullable
    public static Size getNativeCaptureSize() {
        return nativeCaptureSize;
    }

    public static void setNativeCaptureSize(@Nullable Size nativeCaptureSize) {
        MediaContainer.nativeCaptureSize = nativeCaptureSize;
    }

    public static void dispose() {
        setImage(null);
        setNativeCaptureSize(null);
    }

}
