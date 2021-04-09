package de.stoehr.loviapps.wezoom;

import android.app.Application;
import android.preference.PreferenceManager;

public class MainApplication extends Application {
    private static final String TAG = "MainApplication";
    private boolean leftHandedModeChangedInSettings = false;
    private boolean previewSizeChangedInSettings = false;
    private boolean showZoomStatusChangedInSettings = false;

    public boolean getPreviewSizeChangedInSettings() {
        return this.previewSizeChangedInSettings;
    }

    public void setPreviewSizeChangedInSettings(boolean z) {
        this.previewSizeChangedInSettings = z;
    }

    public boolean getLeftHandedModeChangedInSettings() {
        return this.leftHandedModeChangedInSettings;
    }

    public void setLeftHandedModeChangedInSettings(boolean z) {
        this.leftHandedModeChangedInSettings = z;
    }

    public boolean getShowZoomStatusChangedInSettings() {
        return this.showZoomStatusChangedInSettings;
    }

    public void setShowZoomStatusChangedInSettings(boolean z) {
        this.showZoomStatusChangedInSettings = z;
    }

    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }
}
