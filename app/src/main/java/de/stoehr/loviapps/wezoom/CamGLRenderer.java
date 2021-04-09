package de.stoehr.loviapps.wezoom;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CamGLRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "CamGLRenderer";
    private boolean bitmapCreationInProgress = false;
    public CamSurfaceView camSurfaceView;
    public Camera camera;
    private Bitmap currentBitmap;
    private int currentSurfaceHeight;
    private int currentSurfaceWidth;
    public int enableMoreColorDetails = 1;
    private String fssFileName = "accessibility_filter_fss";
    private int glesProgramHandle;
    private SurfaceTexture glesSsurfaceTexture;
    private int[] glesTextureHandle;
    public boolean isContinuousFocus = true;
    public boolean isZoomSupportedByCamera = false;
    protected boolean newCameraFrameAvailable = false;
    public Camera.Parameters paramResumeHolder;
    public boolean pauseRendering = false;
    public AccessibilityMode selectedAccessibilityMode = AccessibilityMode.UNFILTERED;
    private boolean takePhotoFromPreview = false;
    private FloatBuffer texCoordBuffer;
    public float threshold = 0.5f;
    private FloatBuffer vertexBuffer;
    private String vssFileName = "standard_vss";
    public float zoomFactor = 1.0f;
    public float zoomOffsetX = 0.0f;
    public int zoomOffsetXLimit = 0;
    public float zoomOffsetY = 0.0f;
    public int zoomOffsetYLimit = 0;

    CamGLRenderer(CamSurfaceView camSurfaceView2) {
        this.camSurfaceView = camSurfaceView2;
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(camSurfaceView2.getContext());
        float[] fArr = {1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f};
        float[] fArr2 = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
        float[] fArr3 = {1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
        boolean isCameraSensorMountedUpsideDown = isCameraSensorMountedUpsideDown();
        this.vertexBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
        if (defaultSharedPreferences.getBoolean("ENABLE_LEFTHANDED_MODE", false)) {
            if (!isCameraSensorMountedUpsideDown) {
                this.vertexBuffer.put(fArr2);
            } else {
                this.vertexBuffer.put(fArr);
            }
        } else if (!isCameraSensorMountedUpsideDown) {
            this.vertexBuffer.put(fArr);
        } else {
            this.vertexBuffer.put(fArr2);
        }
        this.vertexBuffer.position(0);
        FloatBuffer asFloatBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.texCoordBuffer = asFloatBuffer;
        asFloatBuffer.put(fArr3);
        this.texCoordBuffer.position(0);
    }

    public void switchAccessibilityMode(AccessibilityMode accessibilityMode) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.camSurfaceView.getContext());
        if (accessibilityMode == null) {
            int i = this.selectedAccessibilityMode.value;
            if (i < AccessibilityMode.values().length) {
                int i2 = i + 1;
                AccessibilityMode[] values = AccessibilityMode.values();
                int length = values.length;
                int i3 = 0;
                while (true) {
                    if (i3 >= length) {
                        break;
                    }
                    AccessibilityMode accessibilityMode2 = values[i3];
                    if (accessibilityMode2.value == i2) {
                        if (!defaultSharedPreferences.getBoolean(accessibilityMode2.toString(), true)) {
                            if (i2 >= AccessibilityMode.values().length) {
                                this.selectedAccessibilityMode = AccessibilityMode.UNFILTERED;
                                break;
                            }
                            i2++;
                        } else {
                            this.selectedAccessibilityMode = accessibilityMode2;
                            break;
                        }
                    }
                    i3++;
                }
            } else {
                this.selectedAccessibilityMode = AccessibilityMode.UNFILTERED;
            }
        } else {
            this.selectedAccessibilityMode = accessibilityMode;
        }
        this.camSurfaceView.requestRender();
    }

    public boolean isCameraSensorMountedUpsideDown() {
        try {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.camSurfaceView.getContext());
            int numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == 0) {
                    Log.d(TAG, "CameraSeonsorOrientation: " + cameraInfo.orientation);
                    defaultSharedPreferences.edit().putInt("CAMERA_SENSOR_MOUNT_ORIENTATION", cameraInfo.orientation).apply();
                    if (Build.MODEL.startsWith("Nexus")) {
                        Log.d(TAG, "CameraSeonsorOrientation Device Name: " + Build.MODEL);
                        if (cameraInfo.orientation == 270) {
                            Log.d(TAG, "CameraSeonsorOrientation is upsidedown: " + cameraInfo.orientation);
                            defaultSharedPreferences.edit().putBoolean("IS_CAMERA_SENSOR_MOUNTED_UPSIDEDOWN", true).apply();
                            return true;
                        }
                    } else {
                        continue;
                    }
                }
            }
            defaultSharedPreferences.edit().putBoolean("IS_CAMERA_SENSOR_MOUNTED_UPSIDEDOWN", false).apply();
        } catch (Exception e) {
            Log.d(TAG, "CameraSeonsorOrientation detection error: " + e.getMessage());
        }
        return false;
    }

    public void startCamera() {
        Camera.Parameters parameters;
        if (this.camera == null) {
            this.camera = Camera.open();
        }
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.camSurfaceView.getContext());
        boolean z = defaultSharedPreferences.getBoolean("ENABLE_CONTINUOUS_FOCUS", false);
        boolean z2 = defaultSharedPreferences.getBoolean("ENABLE_CAMERA_ZOOM_RATIOS", true);
        String string = defaultSharedPreferences.getString("MAX_ZOOM_LEVEL_EXTENSION", "0");
        if (defaultSharedPreferences.getBoolean("ENABLE_MORE_COLOR_DETAILS", true)) {
            this.enableMoreColorDetails = 1;
        } else {
            this.enableMoreColorDetails = 0;
        }
        float floatValue = Float.valueOf(string).floatValue();
        this.camSurfaceView.setMaxZoomFactorExtension(floatValue);
        try {
            if (this.paramResumeHolder != null) {
                parameters = this.paramResumeHolder;
            } else {
                parameters = this.camera.getParameters();
                parameters.set("orientation", "landscape");
                parameters.setAutoExposureLock(false);
                parameters.setAutoWhiteBalanceLock(false);
            }
            try {
                if (!defaultSharedPreferences.contains("CAM_DEFAULT_FOCUS_MODE")) {
                    defaultSharedPreferences.edit().putString("CAM_DEFAULT_FOCUS_MODE", parameters.getFocusMode()).apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error when saving default focus mode", e);
            }
            Log.d(TAG, "NewZoom: Is zoom supported: " + parameters.isZoomSupported());
            if (parameters.isZoomSupported() && z2) {
                Log.d(TAG, "NewZoom: Max Zoom: " + parameters.getMaxZoom());
                Log.d(TAG, "NewZoom: Zoom steps: " + parameters.getZoomRatios().toString());
                this.isZoomSupportedByCamera = true;
                float intValue = ((float) parameters.getZoomRatios().get(parameters.getMaxZoom()).intValue()) / 100.0f;
                if (intValue >= this.camSurfaceView.getMaxZoomFactorOverall()) {
                    this.camSurfaceView.setMaxZoomFactorGL(floatValue + 1.0f);
                    this.camSurfaceView.setMaxZoomFactorCam(intValue);
                    this.camSurfaceView.setMaxZoomFactorOverall(intValue);
                } else {
                    this.camSurfaceView.setMaxZoomFactorGL(((this.camSurfaceView.getMaxZoomFactorOverall() + floatValue) - intValue) + 1.0f);
                    this.camSurfaceView.setMaxZoomFactorCam(intValue);
                }
            } else if (!parameters.isZoomSupported() || z2) {
                this.isZoomSupportedByCamera = false;
                this.camSurfaceView.setMaxZoomFactorGL(this.camSurfaceView.getMaxZoomFactorOverall() + floatValue);
            } else {
                if (parameters.getZoomRatios().size() > 0) {
                    parameters.setZoom(0);
                }
                this.isZoomSupportedByCamera = false;
                this.camSurfaceView.setMaxZoomFactorGL(this.camSurfaceView.getMaxZoomFactorOverall() + floatValue);
                this.camSurfaceView.resetZoomFactorCam();
            }
            this.camSurfaceView.resetScaleFactor();
            if (z) {
                ((MainActivity) this.camSurfaceView.getContext()).cbContinuousFocus.setChecked(true);
                setContinuousFocusMode(parameters);
            } else {
                ((MainActivity) this.camSurfaceView.getContext()).cbContinuousFocus.setChecked(false);
                setManualFocusMode(parameters);
            }
            this.camera.setParameters(parameters);
            try {
                defaultSharedPreferences.edit().putString("CAM_CURRENT_FOCUS_MODE", parameters.getFocusMode()).apply();
                List<String> supportedFocusModes = parameters.getSupportedFocusModes();
                StringBuilder sb = new StringBuilder("LVM");
                for (int i = 0; i < supportedFocusModes.size(); i++) {
                    sb.append(supportedFocusModes.get(i));
                    sb.append("\n");
                }
                defaultSharedPreferences.edit().putString("CAM_SUPPORTED_FOCUS_MODES", sb.toString()).apply();
                defaultSharedPreferences.edit().putString("CAM_MAX_FOCUS_AREAS", Integer.toString(parameters.getMaxNumFocusAreas())).apply();
                defaultSharedPreferences.edit().putString("CAM_MAX_METERING_AREAS", Integer.toString(parameters.getMaxNumMeteringAreas())).apply();
            } catch (Exception e2) {
                Log.d(TAG, "Could not save focus information for AppInfo", e2);
            }
        } catch (Exception e3) {
            Log.e(TAG, "Error when setting initial cam params", e3);
        }
        Log.i(TAG, "Camera started ... ");
        try {
            if (this.glesSsurfaceTexture != null) {
                this.camera.setPreviewTexture(this.glesSsurfaceTexture);
                Log.i(TAG, "Camera preview texture linked with glesSurfaceTexture");
            }
        } catch (IOException e4) {
            Log.e(TAG, "Could not link camera preview texture with gles surface texture", e4);
        }
    }

    public void releaseCamera() {
        if (this.camera != null) {
            saveCameraParams();
            try {
                this.camera.cancelAutoFocus();
            } catch (Exception e) {
                Log.w(TAG, "Unexpected exception while cancelling auto focus", e);
            }
            this.camera.stopPreview();
            this.camera.release();
            this.camera = null;
        }
    }

    public void saveCameraParams() {
        Camera camera2 = this.camera;
        if (camera2 != null) {
            this.paramResumeHolder = camera2.getParameters();
        }
    }

    public void setContinuousFocusMode(Camera.Parameters parameters) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.camSurfaceView.getContext());
        String string = defaultSharedPreferences.getString("CAM_DEFAULT_FOCUS_MODE", "NO_DEFAULT_FOCUS_MODE_SET");
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if (supportedFocusModes != null) {
            if (supportedFocusModes.contains("continuous-video")) {
                parameters.setFocusMode("continuous-video");
                this.isContinuousFocus = true;
            } else if (supportedFocusModes.contains("continuous-picture")) {
                parameters.setFocusMode("continuous-picture");
                this.isContinuousFocus = true;
            } else if (supportedFocusModes.contains(string) && !string.equalsIgnoreCase("auto") && !string.equalsIgnoreCase("macro")) {
                parameters.setFocusMode(string);
                this.isContinuousFocus = true;
            } else if (parameters.getFocusMode().equalsIgnoreCase("auto") || parameters.getFocusMode().equalsIgnoreCase("macro")) {
                this.isContinuousFocus = false;
            }
            if (this.isContinuousFocus) {
                if (parameters.getMaxNumFocusAreas() > 0) {
                    parameters.setFocusAreas(null);
                }
                if (parameters.getMaxNumMeteringAreas() > 0) {
                    parameters.setMeteringAreas(null);
                }
            }
        }
        defaultSharedPreferences.edit().putString("CAM_CURRENT_FOCUS_MODE", parameters.getFocusMode()).apply();
    }

    public void setManualFocusMode(Camera.Parameters parameters) {
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if (supportedFocusModes != null) {
            if (supportedFocusModes.contains("auto")) {
                parameters.setFocusMode("auto");
                this.isContinuousFocus = false;
            } else if (supportedFocusModes.contains("macro")) {
                parameters.setFocusMode("macro");
                this.isContinuousFocus = false;
            }
        }
        PreferenceManager.getDefaultSharedPreferences(this.camSurfaceView.getContext()).edit().putString("CAM_CURRENT_FOCUS_MODE", parameters.getFocusMode()).apply();
    }

    public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
        String glGetString = GLES20.glGetString(7939);
        Log.d(TAG, "Available GLES20 extensions: " + glGetString);
        initAndBindTexture();
        SurfaceTexture surfaceTexture = new SurfaceTexture(this.glesTextureHandle[0]);
        this.glesSsurfaceTexture = surfaceTexture;
        surfaceTexture.setOnFrameAvailableListener(this);
        Log.d(TAG, "Trigger camera start in onSurfaceCreated");
        startCamera();
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        setGlesProgram(this.vssFileName, this.fssFileName);
    }

    private void initAndBindTexture() {
        int[] iArr = new int[1];
        this.glesTextureHandle = iArr;
        GLES20.glGenTextures(1, iArr, 0);
        GLES20.glBindTexture(36197, this.glesTextureHandle[0]);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        GLES20.glTexParameteri(36197, 10241, 9728);
        GLES20.glTexParameteri(36197, 10240, 9728);
    }

    public void setGlesProgram(String str, String str2) {
        this.vssFileName = str;
        this.fssFileName = str2;
        Context context = this.camSurfaceView.getContext();
        this.glesProgramHandle = compileAndAddShaderProgram(getShaderFileContent(context, context.getResources().getIdentifier(this.vssFileName, "raw", context.getPackageName())), getShaderFileContent(context, context.getResources().getIdentifier(this.fssFileName, "raw", context.getPackageName())));
    }

    public void onSurfaceChanged(GL10 gl10, int i, int i2) {
        String str;
        boolean z;
        String str2;
        SharedPreferences sharedPreferences;
        boolean z2;
        boolean z3;
        String str3;
        if (this.camera != null) {
            GLES20.glViewport(0, 0, i, i2);
            Log.d(TAG, "Entering CamGLRenderer onSurfaceChanged");
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.camSurfaceView.getContext());
            boolean z4 = defaultSharedPreferences.getBoolean("ENABLE_HIGHEST_PREVIEW_SIZE", true);
            String string = defaultSharedPreferences.getString("CAM_CURRENT_PREVIEW_SIZE_FROM_SETTINGS", "LVM");
            Log.d(TAG, "Camera preview size - enableHighestPreviewSize: " + z4);
            Log.d(TAG, "Camera preview size - currentPreviewSizeFromSettings: " + string);
            Camera.Parameters parameters = this.camera.getParameters();
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            parameters.getSupportedPictureSizes();
            if (supportedPreviewSizes.size() > 0) {
                int i3 = 0;
                while (true) {
                    str = " - ";
                    if (i3 >= supportedPreviewSizes.size()) {
                        break;
                    }
                    Log.d(TAG, "Camera preview size DEFAULT (" + i3 + "): " + supportedPreviewSizes.get(i3).width + str + supportedPreviewSizes.get(i3).height);
                    i3++;
                }
                Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
                    /* class de.stoehr.loviapps.wezoom.CamGLRenderer.AnonymousClass1 */

                    public int compare(Camera.Size size, Camera.Size size2) {
                        return (size2.width * size2.height) - (size.width * size.height);
                    }
                });
                StringBuilder sb = new StringBuilder("LVM");
                int i4 = 0;
                while (i4 < supportedPreviewSizes.size()) {
                    Log.d(TAG, "Camera preview size DESC (" + i4 + "): " + supportedPreviewSizes.get(i4).width + str + supportedPreviewSizes.get(i4).height);
                    sb.append(supportedPreviewSizes.get(i4).width);
                    sb.append(str);
                    sb.append(supportedPreviewSizes.get(i4).height);
                    sb.append("\n");
                    i4++;
                }
                String sb2 = sb.toString();
                Log.d(TAG, "--- Start searching for best preview size ---");
                Log.d(TAG, "Search for preview size nearest gles surface size: " + i + str + i2);
                String str4 = "x";
                if (z4) {
                    Log.d(TAG, "Highest Quality Match: Choosing camera preview size: " + supportedPreviewSizes.get(0).width + str + supportedPreviewSizes.get(0).height);
                    z = false;
                    i4 = 0;
                } else {
                    if (string != null && !string.equalsIgnoreCase("LVM")) {
                        String[] split = string.split(str4);
                        i4 = 0;
                        while (true) {
                            if (i4 >= supportedPreviewSizes.size()) {
                                break;
                            }
                            if (supportedPreviewSizes.get(i4).width == Integer.valueOf(split[0]).intValue()) {
                                if (supportedPreviewSizes.get(i4).height == Integer.valueOf(split[1]).intValue()) {
                                    Log.d(TAG, "Choosing camera preview size from user settings: " + supportedPreviewSizes.get(i4).width + str + supportedPreviewSizes.get(i4).height);
                                    z = true;
                                    break;
                                }
                            }
                            i4++;
                        }
                    }
                    z = false;
                }
                if (z4 || z) {
                    sharedPreferences = defaultSharedPreferences;
                    str2 = str4;
                } else {
                    int i5 = 0;
                    while (true) {
                        if (i5 < supportedPreviewSizes.size()) {
                            if (supportedPreviewSizes.get(i5).width == i && supportedPreviewSizes.get(i5).height == i2) {
                                Log.d(TAG, "Exact Match: Choosing camera preview size: " + supportedPreviewSizes.get(i5).width + str + supportedPreviewSizes.get(i5).height);
                                z2 = true;
                                break;
                            }
                            i5++;
                        } else {
                            z2 = false;
                            break;
                        }
                    }
                    if (!z2) {
                        i5 = 0;
                        while (true) {
                            if (i5 >= supportedPreviewSizes.size()) {
                                break;
                            }
                            double d = (double) supportedPreviewSizes.get(i5).width;
                            double d2 = (double) supportedPreviewSizes.get(i5).height;
                            Double.isNaN(d);
                            Double.isNaN(d2);
                            double d3 = d / d2;
                            double d4 = (double) i;
                            sharedPreferences = defaultSharedPreferences;
                            str2 = str4;
                            double d5 = (double) i2;
                            Double.isNaN(d4);
                            Double.isNaN(d5);
                            if (d3 == d4 / d5) {
                                StringBuilder sb3 = new StringBuilder();
                                sb3.append("Ratio Size Match: Choosing camera preview size: ");
                                sb3.append(supportedPreviewSizes.get(i5).width);
                                str3 = str;
                                sb3.append(str3);
                                sb3.append(supportedPreviewSizes.get(i5).height);
                                Log.d(TAG, sb3.toString());
                                i4 = i5;
                                z3 = true;
                                break;
                            }
                            i5++;
                            str = str;
                            defaultSharedPreferences = sharedPreferences;
                            str4 = str2;
                        }
                    }
                    sharedPreferences = defaultSharedPreferences;
                    str2 = str4;
                    str3 = str;
                    i4 = i5;
                    z3 = false;
                    if (!z2 && !z3) {
                        int i6 = 0;
                        while (true) {
                            if (i6 >= supportedPreviewSizes.size()) {
                                break;
                            } else if (supportedPreviewSizes.get(i6).width < i || supportedPreviewSizes.get(i6).height < i2) {
                                Log.d(TAG, "Nearest Size Match: Choosing camera preview size: " + supportedPreviewSizes.get(i6).width + str3 + supportedPreviewSizes.get(i6).height);
                            } else {
                                i6++;
                            }
                        }
                        Log.d(TAG, "Nearest Size Match: Choosing camera preview size: " + supportedPreviewSizes.get(i6).width + str3 + supportedPreviewSizes.get(i6).height);
                        i4 = i6 > 0 ? i6 - 1 : i6;
                    }
                }
                if (!(parameters.getPreviewSize().width == supportedPreviewSizes.get(i4).width && parameters.getPreviewSize().height == supportedPreviewSizes.get(i4).height)) {
                    parameters.setPreviewSize(supportedPreviewSizes.get(i4).width, supportedPreviewSizes.get(i4).height);
                }
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putString("CAM_CURRENT_PREVIEW_SIZE", parameters.getPreviewSize().width + str2 + parameters.getPreviewSize().height).apply();
                sharedPreferences.edit().putString("CAM_PREVIEW_SIZE_LIST_ORDERED", sb2).apply();
                this.zoomOffsetXLimit = supportedPreviewSizes.get(i4).width / 2;
                this.zoomOffsetYLimit = supportedPreviewSizes.get(i4).height / 2;
            }
            this.camera.setParameters(parameters);
            this.camera.startPreview();
            Log.d(TAG, "Camera preview started in onSurfaceChange");
            this.currentSurfaceWidth = i;
            this.currentSurfaceHeight = i2;
            this.camSurfaceView.updateSurfaceViewSize();
        }
    }

    public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.newCameraFrameAvailable = true;
        this.camSurfaceView.requestRender();
    }

    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(16384);
        synchronized (this) {
            if (this.newCameraFrameAvailable && !this.pauseRendering) {
                this.glesSsurfaceTexture.updateTexImage();
                this.newCameraFrameAvailable = false;
            }
        }
        GLES20.glUseProgram(this.glesProgramHandle);
        int glGetAttribLocation = GLES20.glGetAttribLocation(this.glesProgramHandle, "vPosition");
        int glGetAttribLocation2 = GLES20.glGetAttribLocation(this.glesProgramHandle, "vTexCoord");
        int glGetUniformLocation = GLES20.glGetUniformLocation(this.glesProgramHandle, "sTexture");
        int glGetUniformLocation2 = GLES20.glGetUniformLocation(this.glesProgramHandle, "selectedAccessibilityMode");
        int glGetUniformLocation3 = GLES20.glGetUniformLocation(this.glesProgramHandle, "enableMoreColorDetails");
        int glGetUniformLocation4 = GLES20.glGetUniformLocation(this.glesProgramHandle, "threshold");
        int glGetUniformLocation5 = GLES20.glGetUniformLocation(this.glesProgramHandle, "zoomFactor");
        int glGetUniformLocation6 = GLES20.glGetUniformLocation(this.glesProgramHandle, "zoomOffsetX");
        int glGetUniformLocation7 = GLES20.glGetUniformLocation(this.glesProgramHandle, "zoomOffsetY");
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, this.glesTextureHandle[0]);
        GLES20.glUniform1i(glGetUniformLocation, 0);
        GLES20.glUniform1i(glGetUniformLocation2, this.selectedAccessibilityMode.value);
        GLES20.glUniform1i(glGetUniformLocation3, this.enableMoreColorDetails);
        GLES20.glUniform1f(glGetUniformLocation4, this.threshold);
        GLES20.glUniform1f(glGetUniformLocation5, this.zoomFactor);
        GLES20.glUniform1f(glGetUniformLocation6, this.zoomOffsetX);
        GLES20.glUniform1f(glGetUniformLocation7, this.zoomOffsetY);
        GLES20.glVertexAttribPointer(glGetAttribLocation, 2, 5126, false, 8, (Buffer) this.vertexBuffer);
        GLES20.glVertexAttribPointer(glGetAttribLocation2, 2, 5126, false, 8, (Buffer) this.texCoordBuffer);
        GLES20.glEnableVertexAttribArray(glGetAttribLocation);
        GLES20.glEnableVertexAttribArray(glGetAttribLocation2);
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glFlush();
        if (this.takePhotoFromPreview) {
            this.takePhotoFromPreview = false;
            Log.d(TAG, "TakePictrue currentSurfaceWidth " + this.currentSurfaceWidth + "currentSurfaceHeight" + this.currentSurfaceHeight);
            this.currentBitmap = createBitmapFromGLSurface(0, 0, this.currentSurfaceWidth, this.currentSurfaceHeight, gl10);
            this.bitmapCreationInProgress = false;
        }
    }

    public Bitmap getBitmapFromSurfaceView() {
        Log.d(TAG, "Start transforming current surface view to bitmap ...");
        boolean z = true;
        this.takePhotoFromPreview = true;
        this.bitmapCreationInProgress = true;
        if (this.camSurfaceView.camGLRenderer.pauseRendering) {
            this.camSurfaceView.requestRender();
        }
        int i = 40;
        boolean z2 = false;
        while (true) {
            if (!this.bitmapCreationInProgress) {
                z2 = z;
                break;
            }
            try {
                Thread.sleep(100);
                i--;
                if (i == 0) {
                    break;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Error waiting for bitmap creation: " + e.getMessage());
                z = false;
            }
        }
        if (z2) {
            return this.currentBitmap;
        }
        return null;
    }

    private Bitmap createBitmapFromGLSurface(int i, int i2, int i3, int i4, GL10 gl10) throws OutOfMemoryError {
        int i5 = i3 * i4;
        int[] iArr = new int[i5];
        int[] iArr2 = new int[i5];
        IntBuffer wrap = IntBuffer.wrap(iArr);
        wrap.position(0);
        try {
            GLES20.glReadPixels(i, i2, i3, i4, 6408, 5121, wrap);
            for (int i6 = 0; i6 < i4; i6++) {
                int i7 = i6 * i3;
                int i8 = ((i4 - i6) - 1) * i3;
                for (int i9 = 0; i9 < i3; i9++) {
                    int i10 = iArr[i7 + i9];
                    iArr2[i8 + i9] = (i10 & -16711936) | ((i10 << 16) & 16711680) | ((i10 >> 16) & 255);
                }
            }
            return Bitmap.createBitmap(iArr2, i3, i4, Bitmap.Config.ARGB_8888);
        } catch (GLException e) {
            Log.e(TAG, "Cannot transform gles surface to bitmap", e);
            return null;
        }
    }

    private void deleteTexture() {
        GLES20.glDeleteTextures(1, this.glesTextureHandle, 0);
    }

    private static int compileAndAddShaderProgram(String str, String str2) {
        int glCreateShader = GLES20.glCreateShader(35633);
        GLES20.glShaderSource(glCreateShader, str);
        GLES20.glCompileShader(glCreateShader);
        int[] iArr = new int[1];
        int i = 0;
        GLES20.glGetShaderiv(glCreateShader, 35713, iArr, 0);
        if (iArr[0] == 0) {
            Log.e(TAG, "Error when compiling vssHeader");
            Log.v(TAG, "Error when compiling vssHeader:" + GLES20.glGetShaderInfoLog(glCreateShader));
            GLES20.glDeleteShader(glCreateShader);
            glCreateShader = 0;
        }
        int glCreateShader2 = GLES20.glCreateShader(35632);
        GLES20.glShaderSource(glCreateShader2, str2);
        GLES20.glCompileShader(glCreateShader2);
        GLES20.glGetShaderiv(glCreateShader2, 35713, iArr, 0);
        if (iArr[0] == 0) {
            Log.e(TAG, "Error when compiling fssHeader");
            Log.v(TAG, "Error when compiling fssHeader:" + GLES20.glGetShaderInfoLog(glCreateShader2));
            GLES20.glDeleteShader(glCreateShader2);
        } else {
            i = glCreateShader2;
        }
        int glCreateProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(glCreateProgram, glCreateShader);
        GLES20.glAttachShader(glCreateProgram, i);
        GLES20.glLinkProgram(glCreateProgram);
        return glCreateProgram;
    }

    public static String getShaderFileContent(Context context, int i) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(i)));
        StringBuilder sb = new StringBuilder();
        while (true) {
            try {
                String readLine = bufferedReader.readLine();
                if (readLine == null) {
                    return sb.toString();
                }
                sb.append(readLine);
                sb.append('\n');
            } catch (IOException unused) {
                return null;
            }
        }
    }
}
