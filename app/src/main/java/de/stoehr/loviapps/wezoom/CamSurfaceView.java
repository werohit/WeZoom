package de.stoehr.loviapps.wezoom;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.ItemTouchHelper;

import java.util.ArrayList;
import java.util.List;

public class CamSurfaceView extends GLSurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CamSurfaceView";
    CamGLRenderer camGLRenderer;
    private int currentSurfaceHeight;
    private int currentSurfaceWidth;
    private int currentZoomFactorIndexCam = 0;
    Camera.AutoFocusCallback customAutoFocusCallback = new Camera.AutoFocusCallback() {
        /* class de.stoehr.loviapps.wezoom.CamSurfaceView.AnonymousClass4 */

        public void onAutoFocus(boolean z, Camera camera) {
            try {
                Log.d(CamSurfaceView.TAG, "Auto focus success: " + z);
                CamSurfaceView.this.focusRectView.invalidate();
            } catch (Exception e) {
                Log.e(CamSurfaceView.TAG, "Could not perform auto focus callback", e);
            }
        }
    };
    Display display;
    private int displayHeight;
    private int displayWidth;
    FocusRectView focusRectView;
    private GestureDetectorCompat gestureDetector;
    private boolean isLongeClick = false;
    final Handler longPressedHandler = new Handler();
    Runnable longPressedRunnable = new Runnable() {
        /* class de.stoehr.loviapps.wezoom.CamSurfaceView.AnonymousClass2 */

        public void run() {
            CamSurfaceView.this.isLongeClick = true;
            boolean z = PreferenceManager.getDefaultSharedPreferences(CamSurfaceView.this.getContext()).getBoolean("ALLOW_ADVANCED_GESTURES", false);
            if (CamSurfaceView.this.pointerCount == 1 && CamSurfaceView.this.scrollDistance < 2.0f && z) {
                Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Long Toch-Up 1 finger");
                ((MainActivity) CamSurfaceView.this.getContext()).btnToggleFullscreen.performClick();
            } else if (CamSurfaceView.this.pointerCount == 2 && CamSurfaceView.this.scrollDistance < 8.0f && CamSurfaceView.this.scaleDistance < 0.035f && z) {
                Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Long Toch-Up 2 finger");
                if (((MainActivity) CamSurfaceView.this.getContext()).hasFlashlight) {
                    ((MainActivity) CamSurfaceView.this.getContext()).btnFlashlight.performClick();
                }
            } else if (CamSurfaceView.this.pointerCount == 3 && CamSurfaceView.this.scrollDistance < 8.0f && z) {
                Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Long Toch-Up 3 finger");
                if (!CamSurfaceView.this.camGLRenderer.pauseRendering) {
                    ((MainActivity) CamSurfaceView.this.getContext()).btnPauseRendering.performClick();
                }
                ((MainActivity) CamSurfaceView.this.getContext()).btnTakePicture.performClick();
            } else if (CamSurfaceView.this.pointerCount == 4 && CamSurfaceView.this.scrollDistance < 8.0f && z) {
                Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Long Toch-Up 4 finger");
                ((MainActivity) CamSurfaceView.this.getContext()).btnResetAdvancedOptions.performClick();
            }
        }
    };
    private float maxZoomFactorCam = 1.0f;
    private float maxZoomFactorExtension = 0.0f;
    private float maxZoomFactorGL = 1.0f;
    private int maxZoomFactorIndexCam = 0;
    private float maxZoomFactorOverall = 8.0f;
    private float minZoomFactor = 1.0f;
    private int pointerCount = 0;
    private int realDisplayHeight;
    private int realDisplayWidth;
    private float scaleDistance = 0.0f;
    private float scaleFactor = 1.0f;
    private float scaleFactorBefore = 1.0f;
    private ScaleGestureDetector scaleGestureDetector;
    private float scrollDistance = 0.0f;
    private float zoomFactorCam = 1.0f;
    private float zoomFactorCamFollower = 1.0f;
    private float zoomFactorGL = 1.0f;

    public CamSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        if (!isInEditMode()) {
            this.display = ((MainActivity) getContext()).getWindowManager().getDefaultDisplay();
        }
        PreferenceManager.getDefaultSharedPreferences(context);
        this.camGLRenderer = new CamGLRenderer(this);
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        setRenderer(this.camGLRenderer);
        setRenderMode(0);
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.scaleGestureDetector = new ScaleGestureDetector(getContext(), new CustomScaleGestureListener());
        this.gestureDetector = new GestureDetectorCompat(getContext(), new CustomGestureListener());
        super.surfaceCreated(surfaceHolder);
    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        super.surfaceDestroyed(surfaceHolder);
    }

    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        super.surfaceChanged(surfaceHolder, i, i2, i3);
    }

    public void updateSurfaceViewSize() {
        int i;
        int i2;
        Log.d(TAG, "Entering CamGSurfaceView updateSurfaceViewSize");
        Point point = new Point();
        this.display.getRealSize(point);
        this.realDisplayWidth = point.x;
        int i3 = point.y;
        this.realDisplayHeight = i3;
        double d = (double) this.realDisplayWidth;
        double d2 = (double) i3;
        Double.isNaN(d);
        Double.isNaN(d2);
        double d3 = d / d2;
        Log.d(TAG, "Detected real display size: " + this.realDisplayWidth + " - " + this.realDisplayHeight);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor edit = defaultSharedPreferences.edit();
        edit.putString("REAL_DISPLAY_SIZE", this.realDisplayWidth + "x" + this.realDisplayHeight).apply();
        Camera.Size previewSize = this.camGLRenderer.camera.getParameters().getPreviewSize();
        int i4 = previewSize.width;
        int i5 = previewSize.height;
        double d4 = (double) i4;
        double d5 = (double) i5;
        Double.isNaN(d4);
        Double.isNaN(d5);
        double d6 = d4 / d5;
        Log.d(TAG, "Choosen preview size: " + i4 + " - " + i5);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) getLayoutParams();
        if (d3 != d6) {
            Log.d(TAG, "Display ratio does not match choosen preview ratio: " + d3 + " != " + d6);
            if (d3 < d6) {
                i2 = this.realDisplayHeight;
                i = (i4 * i2) / i5;
            } else {
                int i6 = this.realDisplayWidth;
                int i7 = (i5 * i6) / i4;
                i = i6;
                i2 = i7;
            }
        } else {
            i = this.realDisplayWidth;
            i2 = this.realDisplayHeight;
        }
        layoutParams.width = i;
        layoutParams.height = i2;
        Log.d(TAG, "Changed layout size and ratio to handle ratio mismatch: " + i + " - " + i2);
        ((MainActivity) getContext()).runOnUiThread(new Runnable() {
            /* class de.stoehr.loviapps.wezoom.CamSurfaceView.AnonymousClass1 */

            public void run() {
                CamSurfaceView.this.requestLayout();
            }
        });
        SharedPreferences.Editor edit2 = defaultSharedPreferences.edit();
        edit2.putString("CURRRENT_SURFACE_VIEW_SIZE", getWidth() + "x" + getHeight()).apply();
        this.currentSurfaceWidth = i;
        this.currentSurfaceHeight = i2;
        Log.d(TAG, "CurrentSurfaceSize: " + this.currentSurfaceWidth + " - " + this.currentSurfaceHeight);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        try {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            boolean z = defaultSharedPreferences.getBoolean("ALLOW_ADVANCED_GESTURES", false);
            this.scaleGestureDetector.onTouchEvent(motionEvent);
            this.gestureDetector.onTouchEvent(motionEvent);
            int actionMasked = MotionEventCompat.getActionMasked(motionEvent);
            if (this.pointerCount < motionEvent.getPointerCount()) {
                this.pointerCount = motionEvent.getPointerCount();
            }
            if (actionMasked == 0) {
                this.pointerCount = 0;
                this.scrollDistance = 0.0f;
                this.scaleDistance = 0.0f;
                this.isLongeClick = false;
                this.longPressedHandler.postDelayed(this.longPressedRunnable, (long) ViewConfiguration.getLongPressTimeout());
                return true;
            } else if (actionMasked != 1) {
                if (actionMasked != 2) {
                    if (actionMasked == 3) {
                        this.longPressedHandler.removeCallbacks(this.longPressedRunnable);
                    } else if (actionMasked != 4) {
                        return super.onTouchEvent(motionEvent);
                    } else {
                        this.longPressedHandler.removeCallbacks(this.longPressedRunnable);
                        return true;
                    }
                }
                return true;
            } else {
                if (this.pointerCount == 1 && this.scrollDistance < 2.0f && !this.camGLRenderer.isContinuousFocus && !this.camGLRenderer.pauseRendering && (!this.isLongeClick || !z)) {
                    Log.d(TAG, "GESTURE-DEBUGGER: Toch-Up 1 finger");
                    Point point = new Point();
                    this.display.getSize(point);
                    this.displayWidth = point.x;
                    this.displayHeight = point.y;
                    int i = this.currentSurfaceWidth > this.realDisplayWidth ? (this.currentSurfaceWidth - this.realDisplayWidth) / 2 : 0;
                    int i2 = this.currentSurfaceHeight > this.realDisplayHeight ? (this.currentSurfaceHeight - this.realDisplayHeight) / 2 : 0;
                    if (hasNavBar(getContext().getResources())) {
                        if (this.realDisplayWidth > this.displayWidth) {
                            i += (this.realDisplayWidth - this.displayWidth) / 2;
                        }
                        if (this.realDisplayHeight > this.displayHeight) {
                            i2 += (this.realDisplayHeight - this.displayHeight) / 2;
                        }
                    }
                    Log.d(TAG, "TouchToFocus offsetX: " + i);
                    Log.d(TAG, "TouchToFocus offsetY: " + i2);
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    int i3 = x - i;
                    int i4 = y - i2;
                    if (defaultSharedPreferences.getBoolean("ENABLE_LEFTHANDED_MODE", false)) {
                        x = this.currentSurfaceWidth - x;
                        y = this.currentSurfaceHeight - y;
                    }
                    if (this.zoomFactorGL > this.minZoomFactor) {
                        x = (int) ((((((float) getWidth()) / this.zoomFactorGL) * ((float) x)) / ((float) getWidth())) + (((float) (getWidth() / 2)) - ((((float) getWidth()) / this.zoomFactorGL) / 2.0f)));
                        y = (int) ((((((float) getHeight()) / this.zoomFactorGL) * ((float) y)) / ((float) getHeight())) + (((float) (getHeight() / 2)) - ((((float) getHeight()) / this.zoomFactorGL) / 2.0f)));
                    }
                    int i5 = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION;
                    if (getWidth() < getHeight()) {
                        if (200 > getWidth() / 5) {
                            i5 = getWidth() / 5;
                        }
                    } else if (200 > getHeight() / 5) {
                        i5 = getHeight() / 5;
                    }
                    int i6 = i5 / 2;
                    if (getWidth() - x <= i6) {
                        x = getWidth() - i6;
                    } else if (getWidth() - x >= getWidth() - i6) {
                        x = i6;
                    }
                    if (getHeight() - y <= i6) {
                        y = getHeight() - i6;
                    } else if (getHeight() - y >= getHeight() - i6) {
                        y = i6;
                    }
                    Rect rect = new Rect(i3 - i6, i4 - i6, i3 + i6, i4 + i6);
                    float f = (float) i6;
                    Rect rect2 = new Rect(x - ((int) (f / this.zoomFactorGL)), y - ((int) (f / this.zoomFactorGL)), x + ((int) (f / this.zoomFactorGL)), y + ((int) (f / this.zoomFactorGL)));
                    triggerFocusOnRect(new Rect(((rect2.left * 2000) / getWidth()) + NotificationManagerCompat.IMPORTANCE_UNSPECIFIED, ((rect2.top * 2000) / getHeight()) + NotificationManagerCompat.IMPORTANCE_UNSPECIFIED, ((rect2.right * 2000) / getWidth()) + NotificationManagerCompat.IMPORTANCE_UNSPECIFIED, ((rect2.bottom * 2000) / getHeight()) + NotificationManagerCompat.IMPORTANCE_UNSPECIFIED));
                    FocusRectView focusRectView2 = ((MainActivity) getContext()).focusRectView;
                    this.focusRectView = focusRectView2;
                    focusRectView2.setIsTouched(true, rect);
                    this.focusRectView.invalidate();
                    new Handler().postDelayed(new Runnable() {
                        /* class de.stoehr.loviapps.wezoom.CamSurfaceView.AnonymousClass3 */

                        public void run() {
                            CamSurfaceView.this.focusRectView.setIsTouched(false, new Rect(0, 0, 0, 0));
                            CamSurfaceView.this.focusRectView.invalidate();
                        }
                    }, 1000);
                } else if (this.pointerCount == 2 && this.scrollDistance < 8.0f && this.scaleDistance < 0.03f && z && !this.isLongeClick) {
                    Log.d(TAG, "GESTURE-DEBUGGER: Toch-Up 2 finger");
                    ((MainActivity) getContext()).btnSwitchAccessibilityMode.performClick();
                } else if (this.pointerCount == 3 && this.scrollDistance < 8.0f && this.scaleDistance < 0.05f && z && !this.isLongeClick) {
                    Log.d(TAG, "GESTURE-DEBUGGER: Toch-Up 3 finger");
                    ((MainActivity) getContext()).btnPauseRendering.performClick();
                }
                this.longPressedHandler.removeCallbacks(this.longPressedRunnable);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not handle touch event", e);
            return false;
        }
    }

    public void triggerFocusOnRect(Rect rect) {
        try {
            this.camGLRenderer.camera.cancelAutoFocus();
        } catch (Exception e) {
            Log.d(TAG, "Unable to cancel auto focus", e);
        }
        try {
            ArrayList arrayList = new ArrayList();
            arrayList.add(new Camera.Area(rect, 1000));
            Camera.Parameters parameters = this.camGLRenderer.camera.getParameters();
            if (parameters.getMaxNumFocusAreas() > 0) {
                parameters.setFocusAreas(arrayList);
            }
            if (parameters.getMaxNumMeteringAreas() > 0) {
                parameters.setMeteringAreas(arrayList);
            }
            this.camGLRenderer.camera.setParameters(parameters);
            this.camGLRenderer.camera.autoFocus(this.customAutoFocusCallback);
        } catch (Exception e2) {
            Log.i(TAG, "Unable to auto focus", e2);
        }
    }

    public void performZoom(float f) {
        try {
            this.scaleFactorBefore = this.scaleFactor;
            this.scaleFactor += f;
            Camera.Parameters parameters = this.camGLRenderer.camera.getParameters();
            if (!this.camGLRenderer.isZoomSupportedByCamera) {
                float f2 = this.zoomFactorGL + f;
                this.zoomFactorGL = f2;
                this.zoomFactorGL = Math.max(this.minZoomFactor, Math.min(f2, this.maxZoomFactorGL));
            } else if (parameters.getZoom() == parameters.getMaxZoom() && !this.camGLRenderer.pauseRendering) {
                float f3 = this.zoomFactorGL + f;
                this.zoomFactorGL = f3;
                this.zoomFactorGL = Math.max(this.minZoomFactor, Math.min(f3, this.maxZoomFactorGL));
            } else if (this.camGLRenderer.pauseRendering) {
                float f4 = this.zoomFactorGL + f;
                this.zoomFactorGL = f4;
                this.zoomFactorGL = Math.max(this.minZoomFactor, Math.min(f4, ((this.maxZoomFactorOverall + this.maxZoomFactorExtension) - this.zoomFactorCam) + 1.0f));
            } else {
                float f5 = this.zoomFactorCamFollower + f;
                this.zoomFactorCamFollower = f5;
                this.zoomFactorCamFollower = Math.max(this.minZoomFactor, Math.min(f5, this.maxZoomFactorCam));
            }
            updateZoomLevel();
        } catch (Exception e) {
            Log.e(TAG, "Could not handle zoom programmatically", e);
        }
    }

    private class CustomScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private CustomScaleGestureListener() {
        }

        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            try {
                boolean z = PreferenceManager.getDefaultSharedPreferences(CamSurfaceView.this.getContext()).getBoolean("ALLOW_ADVANCED_GESTURES", false);
                if ((CamSurfaceView.this.pointerCount == 2 && z) || !z) {
                    CamSurfaceView.this.scaleFactorBefore = CamSurfaceView.this.scaleFactor;
                    float f = 0.0f;
                    if (scaleGestureDetector.getScaleFactor() > 1.0f) {
                        f = scaleGestureDetector.getScaleFactor() - 1.0f;
                        CamSurfaceView.this.scaleFactor += f;
                    } else if (scaleGestureDetector.getScaleFactor() < 1.0f) {
                        f = 1.0f - scaleGestureDetector.getScaleFactor();
                        CamSurfaceView.this.scaleFactor -= f;
                    }
                    CamSurfaceView.this.scaleDistance += Math.abs(f);
                    Camera.Parameters parameters = CamSurfaceView.this.camGLRenderer.camera.getParameters();
                    if (!CamSurfaceView.this.camGLRenderer.isZoomSupportedByCamera) {
                        CamSurfaceView.this.zoomFactorGL *= scaleGestureDetector.getScaleFactor();
                        CamSurfaceView.this.zoomFactorGL = Math.max(CamSurfaceView.this.minZoomFactor, Math.min(CamSurfaceView.this.zoomFactorGL, CamSurfaceView.this.maxZoomFactorGL));
                    } else if (parameters.getZoom() == parameters.getMaxZoom() && !CamSurfaceView.this.camGLRenderer.pauseRendering) {
                        CamSurfaceView.this.zoomFactorGL *= scaleGestureDetector.getScaleFactor();
                        CamSurfaceView.this.zoomFactorGL = Math.max(CamSurfaceView.this.minZoomFactor, Math.min(CamSurfaceView.this.zoomFactorGL, CamSurfaceView.this.maxZoomFactorGL));
                    } else if (CamSurfaceView.this.camGLRenderer.pauseRendering) {
                        CamSurfaceView.this.zoomFactorGL *= scaleGestureDetector.getScaleFactor();
                        CamSurfaceView.this.zoomFactorGL = Math.max(CamSurfaceView.this.minZoomFactor, Math.min(CamSurfaceView.this.zoomFactorGL, ((CamSurfaceView.this.maxZoomFactorOverall + CamSurfaceView.this.maxZoomFactorExtension) - CamSurfaceView.this.zoomFactorCam) + 1.0f));
                    } else {
                        CamSurfaceView.this.zoomFactorCamFollower *= scaleGestureDetector.getScaleFactor();
                        CamSurfaceView.this.zoomFactorCamFollower = Math.max(CamSurfaceView.this.minZoomFactor, Math.min(CamSurfaceView.this.zoomFactorCamFollower, CamSurfaceView.this.maxZoomFactorCam));
                    }
                    CamSurfaceView.this.updateZoomLevel();
                    if (CamSurfaceView.this.camGLRenderer.pauseRendering) {
                        if (CamSurfaceView.this.camGLRenderer.zoomOffsetX < (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor) - 4.0f) {
                            CamSurfaceView.this.camGLRenderer.zoomOffsetX = (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor) - 4.0f;
                        } else if (CamSurfaceView.this.camGLRenderer.zoomOffsetX > 1.0f - (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor)) {
                            CamSurfaceView.this.camGLRenderer.zoomOffsetX = 1.0f - (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor);
                        }
                        if (CamSurfaceView.this.camGLRenderer.zoomOffsetY < (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor) - 4.0f) {
                            CamSurfaceView.this.camGLRenderer.zoomOffsetY = (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor) - 4.0f;
                        } else if (CamSurfaceView.this.camGLRenderer.zoomOffsetY > 1.0f - (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor)) {
                            CamSurfaceView.this.camGLRenderer.zoomOffsetY = 1.0f - (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor);
                        }
                        CamSurfaceView.this.requestRender();
                    }
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scale");
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scale distance " + CamSurfaceView.this.scaleDistance);
                }
                return true;
            } catch (Exception e) {
                Log.e(CamSurfaceView.TAG, "Could not handle zoom gesture", e);
                return false;
            }
        }
    }

    class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final float DIV_FACTOR = 125.0f;

        CustomGestureListener() {
        }

        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            float f3;
            float f4;
            try {
                SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(CamSurfaceView.this.getContext());
                boolean z = defaultSharedPreferences.getBoolean("ALLOW_ADVANCED_GESTURES", false);
                if (CamSurfaceView.this.pointerCount == 1 && !CamSurfaceView.this.camGLRenderer.pauseRendering) {
                    CamSurfaceView.this.scaleFactorBefore = CamSurfaceView.this.scaleFactor;
                    if (Math.abs(f) > Math.abs(f2)) {
                        CamSurfaceView.this.scrollDistance += Math.abs(f);
                        float f5 = f / DIV_FACTOR;
                        if (defaultSharedPreferences.getBoolean("ENABLE_LEFTHANDED_MODE", false)) {
                            CamSurfaceView.this.scaleFactor -= f5;
                        } else {
                            CamSurfaceView.this.scaleFactor += f5;
                        }
                    } else {
                        CamSurfaceView.this.scrollDistance += Math.abs(f2);
                        CamSurfaceView.this.scaleFactor += f2 / DIV_FACTOR;
                    }
                    Camera.Parameters parameters = CamSurfaceView.this.camGLRenderer.camera.getParameters();
                    if (!CamSurfaceView.this.camGLRenderer.isZoomSupportedByCamera) {
                        if (Math.abs(f) <= Math.abs(f2)) {
                            CamSurfaceView.this.zoomFactorGL += f2 / DIV_FACTOR;
                        } else if (defaultSharedPreferences.getBoolean("ENABLE_LEFTHANDED_MODE", false)) {
                            CamSurfaceView.this.zoomFactorGL -= f / DIV_FACTOR;
                        } else {
                            CamSurfaceView.this.zoomFactorGL += f / DIV_FACTOR;
                        }
                        CamSurfaceView.this.zoomFactorGL = Math.max(CamSurfaceView.this.minZoomFactor, Math.min(CamSurfaceView.this.zoomFactorGL, CamSurfaceView.this.maxZoomFactorGL));
                    } else if (parameters.getZoom() == parameters.getMaxZoom()) {
                        if (Math.abs(f) <= Math.abs(f2)) {
                            CamSurfaceView.this.zoomFactorGL += f2 / DIV_FACTOR;
                        } else if (defaultSharedPreferences.getBoolean("ENABLE_LEFTHANDED_MODE", false)) {
                            CamSurfaceView.this.zoomFactorGL -= f / DIV_FACTOR;
                        } else {
                            CamSurfaceView.this.zoomFactorGL += f / DIV_FACTOR;
                        }
                        CamSurfaceView.this.zoomFactorGL = Math.max(CamSurfaceView.this.minZoomFactor, Math.min(CamSurfaceView.this.zoomFactorGL, CamSurfaceView.this.maxZoomFactorGL));
                    } else if (!CamSurfaceView.this.camGLRenderer.pauseRendering) {
                        if (Math.abs(f) <= Math.abs(f2)) {
                            CamSurfaceView.this.zoomFactorCamFollower += f2 / DIV_FACTOR;
                        } else if (defaultSharedPreferences.getBoolean("ENABLE_LEFTHANDED_MODE", false)) {
                            CamSurfaceView.this.zoomFactorCamFollower -= f / DIV_FACTOR;
                        } else {
                            CamSurfaceView.this.zoomFactorCamFollower += f / DIV_FACTOR;
                        }
                        CamSurfaceView.this.zoomFactorCamFollower = Math.max(CamSurfaceView.this.minZoomFactor, Math.min(CamSurfaceView.this.zoomFactorCamFollower, CamSurfaceView.this.maxZoomFactorCam));
                    }
                    CamSurfaceView.this.updateZoomLevel();
                    if (CamSurfaceView.this.camGLRenderer.pauseRendering) {
                        CamSurfaceView.this.requestRender();
                    }
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scroll 1 finger");
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scroll distance " + CamSurfaceView.this.scrollDistance);
                } else if (CamSurfaceView.this.pointerCount == 2 && !CamSurfaceView.this.camGLRenderer.pauseRendering && z) {
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scroll 2 finger");
                    if (Math.abs(f) > Math.abs(f2)) {
                        CamSurfaceView.this.scrollDistance += Math.abs(f);
                    } else {
                        CamSurfaceView.this.scrollDistance += Math.abs(f2);
                    }
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scroll distance " + CamSurfaceView.this.scrollDistance);
                    return false;
                } else if (CamSurfaceView.this.pointerCount == 3 && z) {
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scroll 3 finger");
                    if (Math.abs(f) > Math.abs(f2)) {
                        CamSurfaceView.this.scrollDistance += Math.abs(f);
                        f4 = f / 10.0f;
                    } else {
                        CamSurfaceView.this.scrollDistance += Math.abs(f2);
                        f4 = f2 / 10.0f;
                    }
                    ((MainActivity) CamSurfaceView.this.getContext()).seekBarThreshold.setProgress(((MainActivity) CamSurfaceView.this.getContext()).seekBarThreshold.getProgress() - Math.round(f4));
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scroll distance " + CamSurfaceView.this.scrollDistance);
                    return false;
                } else if (CamSurfaceView.this.pointerCount == 4 && z) {
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scroll 4 finger");
                    if (Math.abs(f) > Math.abs(f2)) {
                        CamSurfaceView.this.scrollDistance += Math.abs(f);
                        f3 = f / 15.0f;
                    } else {
                        CamSurfaceView.this.scrollDistance += Math.abs(f2);
                        f3 = f2 / 15.0f;
                    }
                    if (((MainActivity) CamSurfaceView.this.getContext()).hasExposureCompensation && !CamSurfaceView.this.camGLRenderer.pauseRendering) {
                        ((MainActivity) CamSurfaceView.this.getContext()).seekBarExposureCompensation.setProgress(((MainActivity) CamSurfaceView.this.getContext()).seekBarExposureCompensation.getProgress() - Math.round(f3));
                    }
                    Log.d(CamSurfaceView.TAG, "GESTURE-DEBUGGER: Scroll distance " + CamSurfaceView.this.scrollDistance);
                    return false;
                } else if (!CamSurfaceView.this.camGLRenderer.pauseRendering) {
                    return false;
                } else {
                    float f6 = CamSurfaceView.this.camGLRenderer.zoomOffsetX + (f / (((float) CamSurfaceView.this.camGLRenderer.zoomOffsetXLimit) * CamSurfaceView.this.camGLRenderer.zoomFactor));
                    if (f6 >= (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor) - 4.0f && f6 <= 1.0f - (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor)) {
                        CamSurfaceView.this.camGLRenderer.zoomOffsetX = f6;
                    }
                    float f7 = CamSurfaceView.this.camGLRenderer.zoomOffsetY + (f2 / (((float) CamSurfaceView.this.camGLRenderer.zoomOffsetYLimit) * CamSurfaceView.this.camGLRenderer.zoomFactor));
                    if (f7 >= (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor) - 4.0f && f7 <= 1.0f - (1.0f / CamSurfaceView.this.camGLRenderer.zoomFactor)) {
                        CamSurfaceView.this.camGLRenderer.zoomOffsetY = f7;
                    }
                    CamSurfaceView.this.requestRender();
                }
                return true;
            } catch (Exception e) {
                Log.e(CamSurfaceView.TAG, "Could not use scroll gesture for zooming/chaingin position on screen", e);
                return false;
            }
        }
    }

    public void resetScaleFactor() {
        this.scaleFactor = 1.0f;
        this.scaleFactorBefore = 1.0f;
    }

    public void resetZoomFactorGL() {
        this.zoomFactorGL = 1.0f;
    }

    public void resetZoomFactorCam() {
        this.zoomFactorCam = 1.0f;
        this.zoomFactorCamFollower = 1.0f;
        this.currentZoomFactorIndexCam = 0;
    }

    public float getMaxZoomFactorGL() {
        return this.maxZoomFactorGL;
    }

    public void setMaxZoomFactorGL(float f) {
        this.maxZoomFactorGL = f;
    }

    public float getMaxZoomFactorCam() {
        return this.maxZoomFactorCam;
    }

    public void setMaxZoomFactorCam(float f) {
        this.maxZoomFactorCam = f;
    }

    public float getMaxZoomFactorExtension() {
        return this.maxZoomFactorExtension;
    }

    public void setMaxZoomFactorExtension(float f) {
        this.maxZoomFactorExtension = f;
    }

    public float getMaxZoomFactorOverall() {
        return this.maxZoomFactorOverall;
    }

    public void setMaxZoomFactorOverall(float f) {
        this.maxZoomFactorOverall = f;
    }

    public void updateZoomLevel() {

        if (this.camGLRenderer.isZoomSupportedByCamera) {
            Camera.Parameters var7 = this.camGLRenderer.camera.getParameters();
            this.maxZoomFactorIndexCam = var7.getMaxZoom();
            this.currentZoomFactorIndexCam = var7.getZoom();
            this.zoomFactorCam = (float)(Integer)var7.getZoomRatios().get(var7.getZoom()) / 100.0F;
            if (this.currentZoomFactorIndexCam == this.maxZoomFactorIndexCam || this.camGLRenderer.pauseRendering) {
                this.camGLRenderer.zoomFactor = this.zoomFactorGL;
                ((MainActivity)this.getContext()).setTvZoomFactor(this.zoomFactorGL + (float)(Integer)var7.getZoomRatios().get(var7.getZoom()) / 100.0F - 1.0F);
            }

            if (this.currentZoomFactorIndexCam != this.maxZoomFactorIndexCam && !this.camGLRenderer.pauseRendering && this.zoomFactorGL > 1.0F) {
                this.zoomFactorGL = 1.0F;
                this.camGLRenderer.zoomFactor = 1.0F;
                ((MainActivity)this.getContext()).setTvZoomFactor(this.zoomFactorGL + (float)(Integer)var7.getZoomRatios().get(var7.getZoom()) / 100.0F - 1.0F);
            }

            if (!this.camGLRenderer.pauseRendering && this.zoomFactorGL == 1.0F) {
                boolean var3;
                label69: {
                    float var1 = this.scaleFactor;
                    float var2 = this.scaleFactorBefore;
                    boolean var5 = false;
                    int var4;
                    List<Integer> var8;
                    if (var1 > var2) {
                        if (this.zoomFactorCamFollower == this.maxZoomFactorCam) {
                            var4 = this.currentZoomFactorIndexCam;
                            int var6 = this.maxZoomFactorIndexCam;
                            var3 = var5;
                            if (var4 == var6) {
                                break label69;
                            }

                            this.currentZoomFactorIndexCam = var6;
                        } else {
                            var8 = var7.getZoomRatios();
                            var4 = 0;

                            while(true) {
                                var3 = var5;
                                if (var4 >= var8.size()) {
                                    break label69;
                                }

                                if ((float)(Integer)var8.get(var4) / 100.0F >= this.zoomFactorCamFollower) {
                                    this.currentZoomFactorIndexCam = var4;
                                    if (var4 == this.maxZoomFactorIndexCam) {
                                        this.zoomFactorCamFollower = this.maxZoomFactorCam;
                                    }
                                    break;
                                }

                                ++var4;
                            }
                        }
                    } else {
                        var3 = var5;
                        if (var1 >= var2) {
                            break label69;
                        }

                        if (this.zoomFactorCamFollower == this.minZoomFactor) {
                            var3 = var5;
                            if (this.currentZoomFactorIndexCam == 0) {
                                break label69;
                            }

                            this.currentZoomFactorIndexCam = 0;
                        } else {
                            var8 = var7.getZoomRatios();
                            var4 = var8.size() - 1;

                            while(true) {
                                var3 = var5;
                                if (var4 < 0) {
                                    break label69;
                                }

                                if ((float)(Integer)var8.get(var4) / 100.0F < this.zoomFactorCamFollower) {
                                    this.currentZoomFactorIndexCam = var4;
                                    break;
                                }

                                --var4;
                            }
                        }
                    }

                    var3 = true;
                }

                if (var3) {
                    var7.setZoom(this.currentZoomFactorIndexCam);
                    this.camGLRenderer.camera.setParameters(var7);
                    this.zoomFactorCam = (float)(Integer)var7.getZoomRatios().get(var7.getZoom()) / 100.0F;
                    Log.e(TAG,"Digital Zoom "+this.currentZoomFactorIndexCam+",Manual Zoom "+this.zoomFactorCam);
                    ((MainActivity)this.getContext()).setTvZoomFactor(this.zoomFactorCam);
                    return;
                }
            }
        } else {
            this.camGLRenderer.zoomFactor = this.zoomFactorGL;
            ((MainActivity)this.getContext()).setTvZoomFactor(this.zoomFactorGL);
        }

}

    public int getCamZoomIndexStepJump(int i) {
        int round = Math.round(((float) i) / 50.0f);
        if (round <= 0) {
            return 1;
        }
        return round;
    }

    public boolean hasNavBar(Resources resources) {
        int identifier = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        return identifier > 0 && resources.getBoolean(identifier);
    }
}
