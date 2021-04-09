package de.stoehr.loviapps.wezoom;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public static int PAUSE_CAMERA_DELAY = 125;
    public static final int REQUEST_CODE_ASK_STORAGE_PERMISSIONS = 5678;
    private static final String TAG = "MainActivity";
    private float[] accelerationData = new float[3];
    private float azimuth;
    private ImageButton btnCloseAdvancedOptions;
    public ImageButton btnFlashlight;
    public ImageButton btnPauseRendering;
    public ImageButton btnResetAdvancedOptions;
    private ImageButton btnShowAdvancedOptions;
    private ImageButton btnShowAppInfo;
    private ImageButton btnShowSettings;
    public ImageButton btnSwitchAccessibilityMode;
    public ImageButton btnTakePicture;
    public ImageButton btnToggleFullscreen;
    public CamSurfaceView camSurfaceView;
    public LinearLayout camSurfaceViewFrame;
    public LinearLayout camSurfaceViewHolder;
    public CheckBox cbContinuousFocus;
    private int currentExposureValue = 0;
    public float currentRotation = 0.0f;
    public float defaultRotationBasedOnScreenOrientation = 0.0f;
    public boolean flashlightPausedByPausedCamera = false;
    public FocusRectView focusRectView;
    public float glRotationOffset = 180.0f;
    private float[] gravity;
    private boolean hasCamera = false;
    public boolean hasExposureCompensation = false;
    public boolean hasFlashlight = false;
    public float image_orientation_degree = 0.0f;
    private ImageView imgContinuousFocus;
    private ImageView imgExposureCompensation;
    private ImageView imgThreshold;
    boolean isSensorTypeAccelerometerSupported;
    boolean isSensorTypeMagneticFieldSupported;
    boolean isSensorTypeRotationVectorSupported;
    private FrameLayout layoutAdvancedOptions;
    private LinearLayout layoutContinuousFocus;
    private LinearLayout layoutExposureCompensation;
    private FrameLayout layoutMainButtons;
    private float[] mRotationMatrix = new float[16];
    private float[] magnetic;
    private float[] magneticFieldData = new float[3];
    private int maxExposureValue = 0;
    private int minExposureValue = 0;
    private int nrOfPermissionsNeeded = 1;
    private float pitch;
    private float roll;
    private float[] rotationValues = new float[3];
    public SeekBar seekBarExposureCompensation;
    public SeekBar seekBarThreshold;
    private SensorManager sensorManager;
    private TextView tvZoomFactor;

    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    /* access modifiers changed from: protected */
    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.SupportActivity, android.support.v4.app.FragmentActivity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG, "On Create called");
        getWindow().setFlags(128, 128);
        this.sensorManager = (SensorManager) getSystemService("sensor");
        this.hasCamera = getPackageManager().hasSystemFeature("android.hardware.camera");
        Log.i(TAG, "Camera available: " + this.hasCamera);
        this.hasFlashlight = getPackageManager().hasSystemFeature("android.hardware.camera.flash");
        Log.i(TAG, "Flashlight Feature available: " + this.hasFlashlight);
        if (!this.hasCamera) {
            new AlertDialog.Builder(this).setTitle(R.string.startup_dialog_title).setMessage(R.string.startup_dialog_description).setNeutralButton(R.string.startup_dialog_button_ok, new DialogInterface.OnClickListener() {
                /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass1 */

                public void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.finishAffinity();
                }
            }).show();
        }
        setupViewBasedOnLayout();
        deleteShareDir();
        AppRater.appHasLaunched(this);
        Log.i(TAG, "MainActivity successfully created");
    }

    public void setupViewBasedOnLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("ENABLE_LEFTHANDED_MODE", false)) {
            this.currentRotation = 180.0f;
            this.defaultRotationBasedOnScreenOrientation = 180.0f;
            setContentView(R.layout.activity_main_lefthanded);
            setRequestedOrientation(8);
        } else {
            this.currentRotation = 0.0f;
            this.defaultRotationBasedOnScreenOrientation = 0.0f;
            setContentView(R.layout.activity_main);
            setRequestedOrientation(0);
        }
        this.camSurfaceViewHolder = (LinearLayout) findViewById(R.id.camSurfaceViewHolder);
        this.camSurfaceView = (CamSurfaceView) findViewById(R.id.camSurfaceViewID);
        this.camSurfaceViewFrame = (LinearLayout) findViewById(R.id.camSurfaceViewFrame);
        this.focusRectView = (FocusRectView) findViewById(R.id.focusPaintViewID);
        this.layoutMainButtons = (FrameLayout) findViewById(R.id.layoutMainButtons);
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.layoutAdvancedOptions);
        this.layoutAdvancedOptions = frameLayout;
        frameLayout.setVisibility(8);
        ImageButton imageButton = (ImageButton) findViewById(R.id.btnTakePicture);
        this.btnTakePicture = imageButton;
        imageButton.setVisibility(8);
        initMainButtonListeners();
        initAdvancedOptionsListeners();
        initFurtherListeners();
    }

    @Override // android.support.v4.app.FragmentActivity
    public void onPause() {
        super.onPause();
        try {
            Log.d(TAG, "On Pause called");
            this.sensorManager.unregisterListener(this);
            this.camSurfaceView.camGLRenderer.saveCameraParams();
        } catch (Exception e) {
            Log.e(TAG, "Error on pausing activity.", e);
        }
    }

    /* access modifiers changed from: protected */
    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity
    public void onStop() {
        super.onStop();
        try {
            Log.d(TAG, "On Stop called");
            this.camSurfaceView.camGLRenderer.releaseCamera();
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            defaultSharedPreferences.edit().putInt("CACHED_ACCESSIBILITY_MODE", this.camSurfaceView.camGLRenderer.selectedAccessibilityMode.value).apply();
            defaultSharedPreferences.edit().putInt("CACHED_THRESHOLD", this.seekBarThreshold.getProgress()).apply();
            if (this.hasExposureCompensation) {
                defaultSharedPreferences.edit().putInt("CACHED_EXPOSURE_COMPENSATION", this.seekBarExposureCompensation.getProgress()).apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on stopping activity.", e);
        }
    }

    private boolean deleteShareDir() {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("KEEP_SAVED_IMAGES", false)) {
                Log.d(TAG, "Share Dir cannot be deleted due to user preference");
            } else {
                Log.d(TAG, "Start deletion of share dir");
                if (ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
                    Log.d(TAG, "Share Dir cannot be deleted due to missing permissions");
                } else {
                    File externalStorageDirectory = Environment.getExternalStorageDirectory();
                    File file = new File(externalStorageDirectory.getAbsolutePath() + "/" + getString(R.string.app_name));
                    StringBuilder sb = new StringBuilder();
                    sb.append("Share Dir: ");
                    sb.append(file);
                    Log.d(TAG, sb.toString());
                    if (!file.exists() || !file.isDirectory()) {
                        return false;
                    }
                    Log.d(TAG, "Share Dir exists an is directory - starting deletion.");
                    boolean deleteFolderRecursive = deleteFolderRecursive(file);
                    Log.d(TAG, "Share Dir deleted: " + deleteFolderRecursive);
                    return deleteFolderRecursive;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on delete share dir.", e);
        }
        return false;
    }

    private boolean deleteFolderRecursive(File file) {
        if (file.isDirectory()) {
            for (File file2 : file.listFiles()) {
                deleteFolderRecursive(file2);
            }
        }
        return file.delete();
    }

    @Override // android.support.v4.app.FragmentActivity
    public void onResume() {
        super.onResume();
        try {
            Log.d(TAG, "On Resume called");
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (((MainApplication) getApplication()).getLeftHandedModeChangedInSettings()) {
                ((MainApplication) getApplication()).setLeftHandedModeChangedInSettings(false);
                setupViewBasedOnLayout();
            }
            if (((MainApplication) getApplication()).getShowZoomStatusChangedInSettings()) {
                ((MainApplication) getApplication()).setShowZoomStatusChangedInSettings(false);
                updateTvZoomFactorVisibility();
            }
            this.isSensorTypeRotationVectorSupported = this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(11), 3);
            this.isSensorTypeAccelerometerSupported = this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(1), 3);
            this.isSensorTypeMagneticFieldSupported = this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(2), 3);
            this.camSurfaceView.camGLRenderer.startCamera();
            if (((MainApplication) getApplication()).getPreviewSizeChangedInSettings() && this.camSurfaceView.camGLRenderer.pauseRendering) {
                this.btnPauseRendering.performClick();
                ((MainApplication) getApplication()).setPreviewSizeChangedInSettings(false);
            }
            if (defaultSharedPreferences.getBoolean("FULLSCREEN_TOGGLE_ON", false)) {
                if (!defaultSharedPreferences.getBoolean("ALLOW_FULLSCREEN_LOCK", false) || !defaultSharedPreferences.getBoolean("FULLSCREEN_TOGGLE_LOCKED", false)) {
                    this.btnToggleFullscreen.setImageResource(R.drawable.ic_twotone_toggle_on_24px);
                } else {
                    this.btnToggleFullscreen.setImageResource(R.drawable.ic_twotone_toggle_on_locked_24px);
                }
                setViewElementsVisibilityForToggleFullscreen(8);
            }
            checkAndSetupExposureCompensation();
            int i = defaultSharedPreferences.getInt("CACHED_ACCESSIBILITY_MODE", AccessibilityMode.UNFILTERED.value);
            this.camSurfaceView.camGLRenderer.selectedAccessibilityMode = AccessibilityMode.get(Integer.valueOf(i));
            this.seekBarThreshold.setProgress(defaultSharedPreferences.getInt("CACHED_THRESHOLD", 50));
            if (this.hasExposureCompensation) {
                this.seekBarExposureCompensation.setProgress(defaultSharedPreferences.getInt("CACHED_EXPOSURE_COMPENSATION", this.seekBarExposureCompensation.getProgress()));
            }
            updateTheme();
            if (this.hasFlashlight && defaultSharedPreferences.getBoolean("ENABLE_FLASHLIGHT_ON_STARTUP", false) && this.camSurfaceView.camGLRenderer.camera.getParameters().getFlashMode().equals("off")) {
                this.btnFlashlight.performClick();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on resuming camera.", e);
        }
    }

    @Override // android.support.v4.app.FragmentActivity
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x00b5  */
    /* JADX WARNING: Removed duplicated region for block: B:46:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onSensorChanged(android.hardware.SensorEvent var1) {
        /*
        // Method dump skipped, instructions count: 247
        */
        //throw new UnsupportedOperationException("Method not decompiled: de.stoehr.loviapps.wezoom.MainActivity.onSensorChanged(android.hardware.SensorEvent):void");

        int var3;
        int var4;
        boolean var5;
        label84: {
            label83: {
                boolean var6 = this.isSensorTypeRotationVectorSupported;
                var5 = false;
                float[] var10;
                if (var6) {
                    if (var1.sensor.getType() != 11) {
                        break label83;
                    }

                    SensorManager.getRotationMatrixFromVector(this.mRotationMatrix, var1.values);
                    var10 = this.mRotationMatrix;
                    SensorManager.remapCoordinateSystem(var10, 1, 3, var10);
                    var10 = new float[3];
                    SensorManager.getOrientation(this.mRotationMatrix, var10);
                    var3 = (int)Math.toDegrees((double)var10[1]);
                    var4 = (int)Math.toDegrees((double)var10[2]);
                } else {
                    if (!this.isSensorTypeAccelerometerSupported || !this.isSensorTypeMagneticFieldSupported) {
                        this.currentRotation = 270.0F;
                        this.rotateViewElements();
                        break label83;
                    }

                    var3 = var1.sensor.getType();
                    if (var3 != 1) {
                        if (var3 == 2) {
                            this.magneticFieldData = (float[])var1.values.clone();
                        }
                    } else {
                        this.accelerationData = (float[])var1.values.clone();
                    }

                    var10 = this.magneticFieldData;
                    if (var10 == null) {
                        break label83;
                    }

                    float[] var7 = this.accelerationData;
                    if (var7 == null) {
                        break label83;
                    }

                    float[] var8 = new float[9];
                    this.gravity = var8;
                    float[] var9 = new float[9];
                    this.magnetic = var9;
                    SensorManager.getRotationMatrix(var8, var9, var7, var10);
                    var10 = new float[9];
                    SensorManager.remapCoordinateSystem(this.gravity, 1, 3, var10);
                    SensorManager.getOrientation(var10, this.rotationValues);
                    var10 = this.rotationValues;
                    this.azimuth = var10[0] * 57.29578F;
                    float var2 = var10[1] * 57.29578F;
                    this.pitch = var2;
                    this.roll = var10[2] * 57.29578F;
                    this.magneticFieldData = null;
                    this.accelerationData = null;
                    var3 = Math.round(var2);
                    var4 = Math.round(this.roll);
                }

                var5 = true;
                break label84;
            }

            var3 = 0;
            var4 = 0;
        }

        if (var5) {
            if (-45 < var3 && var3 < 45 && -45 < var4 && var4 < 45) {
                this.currentRotation = 270.0F;
                this.rotateViewElements();
            }

            if (-45 < var3 && var3 < 45 && (var4 < -135 || 135 < var4)) {
                this.currentRotation = 90.0F;
                this.rotateViewElements();
            }

            if (-45 < var3 && var3 < 45 && -135 < var4 && var4 < -45) {
                this.currentRotation = 0.0F;
                this.rotateViewElements();
            }

            if (-45 < var3 && var3 < 45 && 45 < var4 && var4 < 135) {
                this.currentRotation = 180.0F;
                this.rotateViewElements();
            }
        }
    }

    private void initMainButtonListeners() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ImageButton imageButton = (ImageButton) findViewById(R.id.btnSwitchAccessibilityMode);
        this.btnSwitchAccessibilityMode = imageButton;
        imageButton.setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass2 */

            public void onClick(View view) {
                MainActivity.this.camSurfaceView.camGLRenderer.switchAccessibilityMode(null);
                MainActivity.this.updateAdvancedOptionItemsEnabled();
            }
        });
        this.btnSwitchAccessibilityMode.setOnLongClickListener(new View.OnLongClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass3 */

            public boolean onLongClick(View view) {
                MainActivity.this.camSurfaceView.camGLRenderer.switchAccessibilityMode(AccessibilityMode.UNFILTERED);
                MainActivity.this.updateAdvancedOptionItemsEnabled();
                return true;
            }
        });
        ImageButton imageButton2 = (ImageButton) findViewById(R.id.btnFlashlight);
        this.btnFlashlight = imageButton2;
        if (this.hasFlashlight) {
            imageButton2.setOnClickListener(new View.OnClickListener() {
                /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass4 */

                public void onClick(View view) {
                    try {
                        Camera.Parameters parameters = MainActivity.this.camSurfaceView.camGLRenderer.camera.getParameters();
                        if (parameters.getFlashMode().equals("off")) {
                            parameters.setFlashMode("torch");
                            MainActivity.this.btnFlashlight.setColorFilter(ContextCompat.getColor(view.getContext(), R.color.md_yellow_500));
                        } else {
                            parameters.setFlashMode("off");
                            MainActivity.this.btnFlashlight.setColorFilter(ContextCompat.getColor(view.getContext(), R.color.white));
                        }
                        MainActivity.this.camSurfaceView.camGLRenderer.camera.setParameters(parameters);
                    } catch (Exception e) {
                        Log.e(MainActivity.TAG, "Cannot switch flashlight on/off.", e);
                    }
                }
            });
        } else {
            imageButton2.setVisibility(8);
        }
        ImageButton imageButton3 = (ImageButton) findViewById(R.id.btnPauseRendering);
        this.btnPauseRendering = imageButton3;
        imageButton3.setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass5 */

            public void onClick(View view) {
                try {
                    if (!MainActivity.this.camSurfaceView.camGLRenderer.pauseRendering) {
                        new Handler().postDelayed(new Runnable() {
                            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass5.AnonymousClass1 */

                            public void run() {
                                MainActivity.this.camSurfaceView.camGLRenderer.pauseRendering = true;
                                MainActivity.this.btnPauseRendering.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                                if (!defaultSharedPreferences.getBoolean("FULLSCREEN_TOGGLE_ON", false)) {
                                    MainActivity.this.btnFlashlight.setVisibility(8);
                                    MainActivity.this.btnTakePicture.setVisibility(0);
                                }
                                MainActivity.this.updateAdvancedOptionItemsEnabled();
                                MainActivity.this.puaseFlashlightOnPauseCamera();
                            }
                        }, (long) MainActivity.PAUSE_CAMERA_DELAY);
                        return;
                    }
                    MainActivity.this.camSurfaceView.camGLRenderer.pauseRendering = false;
                    MainActivity.this.btnPauseRendering.setImageResource(R.drawable.ic_pause_black_24dp);
                    MainActivity.this.camSurfaceView.updateZoomLevel();
                    if (!defaultSharedPreferences.getBoolean("FULLSCREEN_TOGGLE_ON", false)) {
                        MainActivity.this.btnTakePicture.setVisibility(8);
                        MainActivity.this.btnFlashlight.setVisibility(0);
                    }
                    MainActivity.this.updateAdvancedOptionItemsEnabled();
                    MainActivity.this.camSurfaceView.camGLRenderer.zoomOffsetX = 0.0f;
                    MainActivity.this.camSurfaceView.camGLRenderer.zoomOffsetY = 0.0f;
                    MainActivity.this.camSurfaceView.requestRender();
                    MainActivity.this.resumeFlashlightOnResumeCamera();
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot switch pause mode on/off.", e);
                }
            }
        });
        ImageButton imageButton4 = (ImageButton) findViewById(R.id.btnTakePicture);
        this.btnTakePicture = imageButton4;
        imageButton4.setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass6 */

            public void onClick(View view) {
                float f;
                float f2;
                try {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, "android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, MainActivity.REQUEST_CODE_ASK_STORAGE_PERMISSIONS);
                        return;
                    }
                    Toast.makeText(view.getContext(), MainActivity.this.getString(R.string.start_saving_image_toast), 1).show();
                    AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 0.75f);
                    alphaAnimation.setDuration(100);
                    alphaAnimation.setRepeatMode(2);
                    MainActivity.this.camSurfaceViewFrame.startAnimation(alphaAnimation);
                    if (MainActivity.this.currentRotation != 90.0f) {
                        if (MainActivity.this.currentRotation != 270.0f) {
                            f = MainActivity.this.currentRotation;
                            f2 = MainActivity.this.defaultRotationBasedOnScreenOrientation;
                            MainActivity.this.image_orientation_degree = f + f2;
                            new Handler().postDelayed(new Runnable() {
                                /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass6.AnonymousClass1 */

                                public void run() {
                                    Bitmap bitmapFromSurfaceView = MainActivity.this.camSurfaceView.camGLRenderer.getBitmapFromSurfaceView();
                                    if (bitmapFromSurfaceView != null) {
                                        MainActivity.this.shareImageFromBitmap(MainActivity.rotateBitmap(bitmapFromSurfaceView, MainActivity.this.image_orientation_degree));
                                        return;
                                    }
                                    Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.failed_saving_image_toast), 1).show();
                                }
                            }, (long) (MainActivity.PAUSE_CAMERA_DELAY + 25));
                        }
                    }
                    f = MainActivity.this.currentRotation + MainActivity.this.defaultRotationBasedOnScreenOrientation;
                    f2 = MainActivity.this.glRotationOffset;
                    MainActivity.this.image_orientation_degree = f + f2;
                    new Handler().postDelayed(new Runnable() {
                        /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass6.AnonymousClass1 */

                        public void run() {
                            Bitmap bitmapFromSurfaceView = MainActivity.this.camSurfaceView.camGLRenderer.getBitmapFromSurfaceView();
                            if (bitmapFromSurfaceView != null) {
                                MainActivity.this.shareImageFromBitmap(MainActivity.rotateBitmap(bitmapFromSurfaceView, MainActivity.this.image_orientation_degree));
                                return;
                            }
                            Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.failed_saving_image_toast), 1).show();
                        }
                    }, (long) (MainActivity.PAUSE_CAMERA_DELAY + 25));
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot take photo", e);
                }
            }
        });
        ImageButton imageButton5 = (ImageButton) findViewById(R.id.btnToggleFullscreen);
        this.btnToggleFullscreen = imageButton5;
        imageButton5.setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass7 */

            public void onClick(View view) {
                try {
                    if (defaultSharedPreferences.getBoolean("ALLOW_FULLSCREEN_LOCK", false) && defaultSharedPreferences.getBoolean("FULLSCREEN_TOGGLE_LOCKED", false)) {
                        return;
                    }
                    if (MainActivity.this.btnShowAdvancedOptions.getVisibility() == 0) {
                        MainActivity.this.btnToggleFullscreen.setImageResource(R.drawable.ic_twotone_toggle_on_24px);
                        defaultSharedPreferences.edit().putBoolean("FULLSCREEN_TOGGLE_ON", true).apply();
                        MainActivity.this.setViewElementsVisibilityForToggleFullscreen(8);
                        return;
                    }
                    MainActivity.this.btnToggleFullscreen.setImageResource(R.drawable.ic_twotone_toggle_off_24px);
                    defaultSharedPreferences.edit().putBoolean("FULLSCREEN_TOGGLE_ON", false).apply();
                    MainActivity.this.setViewElementsVisibilityForToggleFullscreen(0);
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot toggle fullscreen - so show all buttons.", e);
                }
            }
        });
        this.btnToggleFullscreen.setOnLongClickListener(new View.OnLongClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass8 */

            public boolean onLongClick(View view) {
                try {
                    if (!defaultSharedPreferences.getBoolean("ALLOW_FULLSCREEN_LOCK", false)) {
                        MainActivity.this.btnToggleFullscreen.performClick();
                    } else if (MainActivity.this.btnShowAdvancedOptions.getVisibility() == 0) {
                        MainActivity.this.btnToggleFullscreen.setImageResource(R.drawable.ic_twotone_toggle_on_locked_24px);
                        defaultSharedPreferences.edit().putBoolean("FULLSCREEN_TOGGLE_ON", true).apply();
                        defaultSharedPreferences.edit().putBoolean("FULLSCREEN_TOGGLE_LOCKED", true).apply();
                        MainActivity.this.setViewElementsVisibilityForToggleFullscreen(8);
                    } else {
                        MainActivity.this.btnToggleFullscreen.setImageResource(R.drawable.ic_twotone_toggle_off_24px);
                        defaultSharedPreferences.edit().putBoolean("FULLSCREEN_TOGGLE_ON", false).apply();
                        defaultSharedPreferences.edit().putBoolean("FULLSCREEN_TOGGLE_LOCKED", false).apply();
                        MainActivity.this.setViewElementsVisibilityForToggleFullscreen(0);
                    }
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot lock / unlock fullscreen - so show all buttons.", e);
                }
                return true;
            }
        });
    }

    private void initAdvancedOptionsListeners() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.layoutExposureCompensation = (LinearLayout) findViewById(R.id.layoutExposureCompensation);
        ImageButton imageButton = (ImageButton) findViewById(R.id.btnShowAdvancedOptions);
        this.btnShowAdvancedOptions = imageButton;
        imageButton.setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass9 */

            public void onClick(View view) {
                try {
                    if (MainActivity.this.layoutAdvancedOptions.getVisibility() == 0) {
                        MainActivity.this.layoutAdvancedOptions.setVisibility(8);
                        return;
                    }
                    MainActivity.this.updateAdvancedOptionItemsEnabled();
                    MainActivity.this.layoutAdvancedOptions.setVisibility(0);
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot show/hide advanced options.", e);
                }
            }
        });
        ImageButton imageButton2 = (ImageButton) findViewById(R.id.btnCloseAdvancedOptions);
        this.btnCloseAdvancedOptions = imageButton2;
        imageButton2.setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass10 */

            public void onClick(View view) {
                MainActivity.this.btnShowAdvancedOptions.callOnClick();
            }
        });
        this.imgExposureCompensation = (ImageView) findViewById(R.id.imgExposureCompensation);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBarExposureCompensation);
        this.seekBarExposureCompensation = seekBar;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass11 */

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                try {
                    if (MainActivity.this.hasExposureCompensation) {
                        Camera.Parameters parameters = MainActivity.this.camSurfaceView.camGLRenderer.camera.getParameters();
                        parameters.setExposureCompensation(i - MainActivity.this.maxExposureValue);
                        MainActivity.this.camSurfaceView.camGLRenderer.camera.setParameters(parameters);
                    }
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot change exposure compensation.", e);
                }
            }
        });
        this.imgThreshold = (ImageView) findViewById(R.id.imgThreshold);
        SeekBar seekBar2 = (SeekBar) findViewById(R.id.seekBarThreshold);
        this.seekBarThreshold = seekBar2;
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass12 */

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                try {
                    MainActivity.this.camSurfaceView.camGLRenderer.threshold = ((float) i) / 100.0f;
                    if (MainActivity.this.camSurfaceView.camGLRenderer.pauseRendering) {
                        MainActivity.this.camSurfaceView.requestRender();
                    }
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot change threshold.", e);
                }
            }
        });
        this.layoutContinuousFocus = (LinearLayout) findViewById(R.id.layoutContinuousFocus);
        this.imgContinuousFocus = (ImageView) findViewById(R.id.imgContinuousFocus);
        CheckBox checkBox = (CheckBox) findViewById(R.id.cbContinuousFocus);
        this.cbContinuousFocus = checkBox;
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass13 */

            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                try {
                    Camera.Parameters parameters = MainActivity.this.camSurfaceView.camGLRenderer.camera.getParameters();
                    if (z) {
                        MainActivity.this.camSurfaceView.camGLRenderer.setContinuousFocusMode(parameters);
                        defaultSharedPreferences.edit().putBoolean("ENABLE_CONTINUOUS_FOCUS", true).apply();
                    } else {
                        MainActivity.this.camSurfaceView.camGLRenderer.setManualFocusMode(parameters);
                        defaultSharedPreferences.edit().putBoolean("ENABLE_CONTINUOUS_FOCUS", false).apply();
                    }
                    MainActivity.this.camSurfaceView.camGLRenderer.camera.setParameters(parameters);
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot switch focus mode (continuous/touch).", e);
                }
            }
        });
        ImageButton imageButton3 = (ImageButton) findViewById(R.id.btnResetAdvancedOptions);
        this.btnResetAdvancedOptions = imageButton3;
        imageButton3.setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass14 */

            public void onClick(View view) {
                try {
                    if (MainActivity.this.hasExposureCompensation) {
                        MainActivity.this.seekBarExposureCompensation.setProgress(MainActivity.this.maxExposureValue);
                    }
                    MainActivity.this.seekBarThreshold.setProgress(50);
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot reset advanced options.", e);
                }
            }
        });
        ImageButton imageButton4 = (ImageButton) findViewById(R.id.btnShowAppInfo);
        this.btnShowAppInfo = imageButton4;
        imageButton4.setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass15 */

            public void onClick(View view) {
                try {
                    Intent intent = new Intent(view.getContext(), AppInfoActivity.class);
                    intent.putExtra("key", 16842788);
                    view.getContext().startActivity(intent);
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot start app info activity.", e);
                }
            }
        });
        ImageButton imageButton5 = (ImageButton) findViewById(R.id.btnShowSettings);
        this.btnShowSettings = imageButton5;
        imageButton5.setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass16 */

            public void onClick(View view) {
                try {
                    Intent intent = new Intent(view.getContext(), SettingsActivity.class);
                    List<Camera.Size> supportedPreviewSizes = MainActivity.this.camSurfaceView.camGLRenderer.camera.getParameters().getSupportedPreviewSizes();
                    Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
                        /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass16.AnonymousClass1 */

                        public int compare(Camera.Size size, Camera.Size size2) {
                            return (size2.width * size2.height) - (size.width * size.height);
                        }
                    });
                    String[] strArr = new String[supportedPreviewSizes.size()];
                    int i = 0;
                    for (Camera.Size size : supportedPreviewSizes) {
                        strArr[i] = size.width + "x" + size.height;
                        i++;
                    }
                    intent.putExtra("supportedPreviewSizesOrdered", strArr);
                    view.getContext().startActivity(intent);
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Cannot start app settings activity.", e);
                }
            }
        });
    }

    private void initFurtherListeners() {
        this.tvZoomFactor = (TextView) findViewById(R.id.tvZoomFactor);
        updateTvZoomFactorVisibility();
    }

    public void setTvZoomFactor(float f) {
        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        decimalFormat.setRoundingMode(RoundingMode.CEILING);
        TextView textView = this.tvZoomFactor;
        textView.setText(decimalFormat.format((double) f) + "x");
    }

    public void updateTvZoomFactorVisibility() {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!defaultSharedPreferences.getBoolean("SHOW_ZOOM_STATUS", true) || defaultSharedPreferences.getBoolean("FULLSCREEN_TOGGLE_ON", false)) {
            this.tvZoomFactor.setVisibility(8);
        } else {
            this.tvZoomFactor.setVisibility(0);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void puaseFlashlightOnPauseCamera() {
        try {
            if (!this.hasFlashlight) {
                return;
            }
            if (this.camSurfaceView.camGLRenderer.camera.getParameters().getFlashMode().equals("off")) {
                this.flashlightPausedByPausedCamera = false;
                return;
            }
            Log.d(TAG, "Pause Flashlight");
            this.btnFlashlight.performClick();
            this.flashlightPausedByPausedCamera = true;
        } catch (Exception e) {
            Log.e(TAG, "Cannot pause flashlight on pause camera.", e);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void resumeFlashlightOnResumeCamera() {
        try {
            if (this.hasFlashlight && this.flashlightPausedByPausedCamera) {
                this.btnFlashlight.performClick();
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot resume flashlight on resume camera.", e);
        }
    }

    private void checkAndSetupExposureCompensation() {
        Camera.Parameters parameters = this.camSurfaceView.camGLRenderer.camera.getParameters();
        this.minExposureValue = parameters.getMinExposureCompensation();
        int maxExposureCompensation = parameters.getMaxExposureCompensation();
        this.maxExposureValue = maxExposureCompensation;
        int i = this.minExposureValue;
        if (i == 0 || maxExposureCompensation == 0) {
            this.hasExposureCompensation = false;
            this.layoutExposureCompensation.setVisibility(8);
            return;
        }
        this.hasExposureCompensation = true;
        this.seekBarExposureCompensation.setMax(Math.abs(i) + this.maxExposureValue);
        int exposureCompensation = parameters.getExposureCompensation() + Math.abs(this.minExposureValue);
        this.currentExposureValue = exposureCompensation;
        this.seekBarExposureCompensation.setProgress(exposureCompensation);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateAdvancedOptionItemsEnabled() {
        if (this.camSurfaceView.camGLRenderer.pauseRendering) {
            this.seekBarExposureCompensation.setEnabled(false);
            this.seekBarExposureCompensation.setAlpha(0.5f);
            this.cbContinuousFocus.setEnabled(false);
            this.cbContinuousFocus.setAlpha(0.5f);
            return;
        }
        this.seekBarExposureCompensation.setEnabled(true);
        this.seekBarExposureCompensation.setAlpha(1.0f);
        this.cbContinuousFocus.setEnabled(true);
        this.cbContinuousFocus.setAlpha(1.0f);
    }

    private void rotateViewElements() {
        float f = this.currentRotation + this.defaultRotationBasedOnScreenOrientation;
        this.btnShowAdvancedOptions.setRotation(f);
        this.btnSwitchAccessibilityMode.setRotation(f);
        this.btnTakePicture.setRotation(f);
        this.btnFlashlight.setRotation(f);
        this.btnPauseRendering.setRotation(f);
        this.tvZoomFactor.setRotation(f);
        this.btnCloseAdvancedOptions.setRotation(f);
        this.btnResetAdvancedOptions.setRotation(f);
        this.btnShowAppInfo.setRotation(f);
        this.imgExposureCompensation.setRotation(f);
        this.imgThreshold.setRotation(f);
        this.imgContinuousFocus.setRotation(f);
        this.cbContinuousFocus.setRotation(f);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setViewElementsVisibilityForToggleFullscreen(int i) {
        this.btnShowAdvancedOptions.setVisibility(i);
        this.btnSwitchAccessibilityMode.setVisibility(i);
        if (this.camSurfaceView.camGLRenderer.pauseRendering) {
            this.btnTakePicture.setVisibility(i);
        }
        if (!this.camSurfaceView.camGLRenderer.pauseRendering) {
            this.btnFlashlight.setVisibility(i);
        }
        this.btnPauseRendering.setVisibility(i);
        updateTvZoomFactorVisibility();
        this.layoutAdvancedOptions.setVisibility(8);
    }

    public void shareImageFromBitmap(Bitmap bitmap) {
        Log.d(TAG, "Start taking image by saving bitmap  ...");
        try {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            File file = new File(externalStorageDirectory.getAbsolutePath() + "/" + getString(R.string.app_name));
            file.mkdirs();
            File file2 = new File(file, String.format(getString(R.string.app_name) + "-%d.jpg", Long.valueOf(System.currentTimeMillis())));
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
            intent.setData(Uri.fromFile(file2));
            sendBroadcast(intent);
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("OPEN_SHARE_IMAGE_DIALOG", true)) {
                Uri uriForFile = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), file2);
                Intent intent2 = new Intent();
                intent2.setAction("android.intent.action.SEND");
                intent2.addFlags(1);
                intent2.putExtra("android.intent.extra.STREAM", uriForFile);
                intent2.setType("image/*");
                startActivity(Intent.createChooser(intent2, getString(R.string.share_image_dialog_title)));
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Cannot open file to save image", e);
        } catch (IOException e2) {
            Log.e(TAG, "Failed IO operation when trying to save image", e2);
        } catch (Exception e3) {
            Log.e(TAG, "Failed to take photo of glReadPixels", e3);
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, float f) {
        Matrix matrix = new Matrix();
        matrix.postRotate(f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.SupportActivity
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int intValue = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("VOLUME_KEY_TRIGGER", "1")).intValue();
        int action = keyEvent.getAction();
        int keyCode = keyEvent.getKeyCode();
        if (intValue == VolumeKeyMode.NONE.value) {
            return super.dispatchKeyEvent(keyEvent);
        }
        if (keyCode == 24) {
            if (action == 0) {
                Log.d(TAG, "VolumeButtonFeature: Vol Up Clicked");
                if (intValue == VolumeKeyMode.ZOOM.value) {
                    this.camSurfaceView.performZoom(0.25f);
                } else if (intValue == VolumeKeyMode.COLOR_FILTER.value || intValue == VolumeKeyMode.COMBO_COLOR_FILTER_AND_FLASHLIGHT.value || intValue == VolumeKeyMode.COMBO_COLOR_FILTER_AND_PAUSE.value) {
                    this.btnSwitchAccessibilityMode.performClick();
                } else if (intValue == VolumeKeyMode.EXPOSURE.value) {
                    if (this.hasExposureCompensation && !this.camSurfaceView.camGLRenderer.pauseRendering) {
                        SeekBar seekBar = this.seekBarExposureCompensation;
                        seekBar.setProgress(seekBar.getProgress() + 5);
                    }
                } else if (intValue == VolumeKeyMode.THRESHOLD.value) {
                    SeekBar seekBar2 = this.seekBarThreshold;
                    seekBar2.setProgress(seekBar2.getProgress() + 10);
                } else if (intValue == VolumeKeyMode.FLASHLIGHT.value || intValue == VolumeKeyMode.COMBO_FLASHLIGHT_AND_PAUSE.value || intValue == VolumeKeyMode.COMBO_FLASHLIGHT_AND_TAKE_PICTURE.value) {
                    if (this.hasFlashlight) {
                        this.btnFlashlight.performClick();
                    }
                } else if (intValue == VolumeKeyMode.PAUSE.value) {
                    this.btnPauseRendering.performClick();
                } else if (intValue == VolumeKeyMode.TAKE_PICTURE.value) {
                    if (!this.camSurfaceView.camGLRenderer.pauseRendering) {
                        this.btnPauseRendering.performClick();
                    }
                    this.btnTakePicture.performClick();
                }
            }
            return true;
        } else if (keyCode != 25) {
            return super.dispatchKeyEvent(keyEvent);
        } else {
            if (action == 0) {
                Log.d(TAG, "VolumeButtonFeature: Vol Down Clicked");
                if (intValue == VolumeKeyMode.ZOOM.value) {
                    this.camSurfaceView.performZoom(-0.25f);
                } else if (intValue == VolumeKeyMode.COLOR_FILTER.value) {
                    this.btnSwitchAccessibilityMode.performClick();
                } else if (intValue == VolumeKeyMode.EXPOSURE.value) {
                    if (this.hasExposureCompensation && !this.camSurfaceView.camGLRenderer.pauseRendering) {
                        SeekBar seekBar3 = this.seekBarExposureCompensation;
                        seekBar3.setProgress(seekBar3.getProgress() - 5);
                    }
                } else if (intValue == VolumeKeyMode.THRESHOLD.value) {
                    SeekBar seekBar4 = this.seekBarThreshold;
                    seekBar4.setProgress(seekBar4.getProgress() - 10);
                } else if (intValue == VolumeKeyMode.FLASHLIGHT.value || intValue == VolumeKeyMode.COMBO_COLOR_FILTER_AND_FLASHLIGHT.value) {
                    if (this.hasFlashlight) {
                        this.btnFlashlight.performClick();
                    }
                } else if (intValue == VolumeKeyMode.PAUSE.value || intValue == VolumeKeyMode.COMBO_COLOR_FILTER_AND_PAUSE.value || intValue == VolumeKeyMode.COMBO_FLASHLIGHT_AND_PAUSE.value) {
                    this.btnPauseRendering.performClick();
                } else if (intValue == VolumeKeyMode.TAKE_PICTURE.value || intValue == VolumeKeyMode.COMBO_FLASHLIGHT_AND_TAKE_PICTURE.value) {
                    if (!this.camSurfaceView.camGLRenderer.pauseRendering) {
                        this.btnPauseRendering.performClick();
                    }
                    this.btnTakePicture.performClick();
                }
            }
            return true;
        }
    }

    @Override // android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback, android.support.v4.app.FragmentActivity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 5678) {
            int i2 = 0;
            for (int i3 : iArr) {
                if (i3 == 0) {
                    i2++;
                }
            }
            if (iArr.length <= 0 || this.nrOfPermissionsNeeded != i2) {
                Log.i(TAG, "After asking the user: One or more permissions not granted");
                Log.i(TAG, "Present more information to the user why this permissions are needed.");
                new AlertDialog.Builder(this).setTitle(R.string.permissions_missing_dialog_title).setMessage(R.string.storage_permissions_missing_dialog_description).setPositiveButton(R.string.permissions_missing_dialog_retry, new DialogInterface.OnClickListener() {
                    /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass18 */

                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity.this.btnTakePicture.performClick();
                    }
                }).setNegativeButton(R.string.permissions_missing_dialog_exit, new DialogInterface.OnClickListener() {
                    /* class de.stoehr.loviapps.wezoom.MainActivity.AnonymousClass17 */

                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).show();
                return;
            }
            Log.i(TAG, "After asking the user: All permissions granted");
            this.btnTakePicture.performClick();
        }
    }

    public void updateTheme() {
        int intValue = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("THEME", "1")).intValue();
        if (intValue == Theme.DEFAULT.value) {
            changeMainButtonBackground(getResources().getDrawable(R.drawable.button_bg_round));
            changeMainButtonPadding(Math.round(getResources().getDimension(R.dimen.big_button_padding)));
            this.layoutAdvancedOptions.setBackgroundColor(getResources().getColor(R.color.black_transparent));
            ViewGroup.LayoutParams layoutParams = this.layoutAdvancedOptions.getLayoutParams();
            layoutParams.width = Math.round(getResources().getDimension(R.dimen.advanced_options_box_width));
            this.layoutAdvancedOptions.setLayoutParams(layoutParams);
            changeAdvancedButtonBackground(getResources().getDrawable(R.drawable.button_bg_round));
            this.tvZoomFactor.setBackground(getResources().getDrawable(R.drawable.label_bg_rectangle));
        } else if (intValue == Theme.THEME1_COLORFUL_BUTTONS.value) {
            this.btnFlashlight.setBackground(getResources().getDrawable(R.drawable.button_bg_round_theme1_blue));
            this.btnTakePicture.setBackground(getResources().getDrawable(R.drawable.button_bg_round_theme1));
            this.btnPauseRendering.setBackground(getResources().getDrawable(R.drawable.button_bg_round_theme1_orange));
            this.btnSwitchAccessibilityMode.setBackground(getResources().getDrawable(R.drawable.button_bg_round_theme1));
            this.btnShowAdvancedOptions.setBackground(getResources().getDrawable(R.drawable.button_bg_round_theme1_red));
            changeMainButtonPadding(Math.round(getResources().getDimension(R.dimen.big_button_padding_theme1)));
            this.layoutAdvancedOptions.setBackgroundColor(getResources().getColor(R.color.black));
            ViewGroup.LayoutParams layoutParams2 = this.layoutAdvancedOptions.getLayoutParams();
            layoutParams2.width = Math.round(getResources().getDimension(R.dimen.advanced_options_box_width_theme1));
            this.layoutAdvancedOptions.setLayoutParams(layoutParams2);
            changeAdvancedButtonBackground(getResources().getDrawable(R.drawable.button_bg_round_theme1_primary));
            this.tvZoomFactor.setBackground(getResources().getDrawable(R.drawable.label_bg_rectangle_theme1));
        }
    }

    public void changeMainButtonBackground(Drawable drawable) {
        this.btnFlashlight.setBackground(drawable);
        this.btnTakePicture.setBackground(drawable);
        this.btnPauseRendering.setBackground(drawable);
        this.btnSwitchAccessibilityMode.setBackground(drawable);
        this.btnShowAdvancedOptions.setBackground(drawable);
    }

    public void changeMainButtonPadding(int i) {
        this.btnToggleFullscreen.setPadding(i, i, i, i);
        this.btnFlashlight.setPadding(i, i, i, i);
        this.btnTakePicture.setPadding(i, i, i, i);
        this.btnPauseRendering.setPadding(i, i, i, i);
        this.btnSwitchAccessibilityMode.setPadding(i, i, i, i);
        this.btnShowAdvancedOptions.setPadding(i, i, i, i);
    }

    public void changeAdvancedButtonBackground(Drawable drawable) {
        this.btnCloseAdvancedOptions.setBackground(drawable);
        this.btnResetAdvancedOptions.setBackground(drawable);
        this.btnShowAppInfo.setBackground(drawable);
        this.btnShowSettings.setBackground(drawable);
    }
}
