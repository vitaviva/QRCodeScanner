
package com.vitaviva.qrcodescanner.core;

import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class BarcodeCameraConfig {

    private Camera camera;

    public BarcodeCameraConfig(Camera c) {
        camera = c;
    }

    @SuppressLint("NewApi")
    public BarcodeCameraConfig configDisplayOrientation(int rotation) {
        int degree = 90 * rotation;
        int result = (360 + 90 - degree) % 360;

        try {
            camera.setDisplayOrientation(result);
        } catch (Exception e) {

        }
        return this;
    }

    public BarcodeCameraConfig configFocusMode() {
        try {
            Parameters parameters = camera.getParameters();
            String focusMode = findValue(parameters.getSupportedFocusModes(),
                    Parameters.FOCUS_MODE_AUTO,
                    Parameters.FOCUS_MODE_MACRO);

            if (focusMode != null) {
                parameters.setFocusMode(focusMode);
            }

            camera.setParameters(parameters);
        } catch (Exception e) {
        }

        return this;
    }

    private static String findValue(Collection<String> supportedValues,
                                    String... desiredValues) {
        String result = null;
        if (supportedValues != null) {
            for (String desiredValue : desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    result = desiredValue;
                    break;
                }
            }
        }
        return result;
    }

    private static CameraSizeComparator sizeComparator = new CameraSizeComparator();

    private static class CameraSizeComparator implements Comparator<Size> {
        // desc order
        public int compare(Size lhs, Size rhs) {
            // TODO Auto-generated method stub
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return -1;
            } else {
                return 1;
            }
        }

    }

    public BarcodeCameraConfig configPreviewSize2(int viewWidth, int viewHeight) {
        Parameters params = camera.getParameters();
        List<Size> sizes = params.getSupportedPreviewSizes();
        if (sizes == null || sizes.size() <= 0) {
            return this;
        }

        Size bestSize = null;
        int diff = Integer.MAX_VALUE;

        for (Size tmpSize : sizes) {
            int newDiff = Math.abs(tmpSize.width - viewWidth) + Math.abs(tmpSize.height - viewHeight);
            if (newDiff == 0) {
                bestSize = tmpSize;
                break;
            } else if (newDiff < diff) {
                bestSize = tmpSize;
                diff = newDiff;
            }
        }
        params.setPreviewSize(bestSize.width, bestSize.height);
        camera.setParameters(params);
        return this;
    }

    public BarcodeCameraConfig configFlashlight(boolean openFlashLight) {
        Parameters params = camera.getParameters();
        params.setFlashMode(openFlashLight ? Parameters.FLASH_MODE_TORCH
                : Parameters.FLASH_MODE_OFF);
        camera.setParameters(params);
        return this;
    }

    public Camera getCamera() {
        return camera;
    }
}
