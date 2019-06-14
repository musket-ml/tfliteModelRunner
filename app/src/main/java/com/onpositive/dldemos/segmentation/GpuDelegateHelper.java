package com.onpositive.dldemos.segmentation;

import org.tensorflow.lite.Delegate;

public class GpuDelegateHelper {
    /**
     * Checks whether {@code GpuDelegate} is available.
     */
    public static boolean isGpuDelegateAvailable() {
        try {
            Class.forName("org.tensorflow.lite.experimental.GpuDelegate");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns an instance of {@code GpuDelegate} if available.
     */
    public static Delegate createGpuDelegate() {
        try {
            return Class.forName("org.tensorflow.lite.experimental.GpuDelegate")
                    .asSubclass(Delegate.class)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
