package de.stoehr.loviapps.wezoom;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class PreparationActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1234;
    private static final String TAG = "PreparationActivity";
    private boolean hasCamera = false;
    private int nrOfPermissionsNeeded;
    String[] permissions = {"android.permission.CAMERA"};

    /* access modifiers changed from: protected */
    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.SupportActivity, android.support.v4.app.FragmentActivity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setFlags(128, 128);
        this.hasCamera = getPackageManager().hasSystemFeature("android.hardware.camera");
        Log.i(TAG, "Camera available: " + this.hasCamera);
        if (!this.hasCamera) {
            new AlertDialog.Builder(this).setTitle(R.string.startup_dialog_title).setMessage(R.string.startup_dialog_description).setNeutralButton(R.string.startup_dialog_button_ok, new DialogInterface.OnClickListener() {
                /* class de.stoehr.loviapps.wezoom.PreparationActivity.AnonymousClass1 */

                public void onClick(DialogInterface dialogInterface, int i) {
                    PreparationActivity.this.finishAffinity();
                }
            }).show();
        } else if (checkPermissions()) {
            Log.i(TAG, "All permissions already granted");
            startMainActivity();
        } else {
            Log.i(TAG, "Still some permissions to be granted");
        }
        setContentView(R.layout.activity_preparation);
        Log.i(TAG, "PreparationActivity successfully created");
    }

    private void startMainActivity() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("key", 16842788);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot start main activity.", e);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean checkPermissions() {
        ArrayList arrayList = new ArrayList();
        String[] strArr = this.permissions;
        for (String str : strArr) {
            if (ContextCompat.checkSelfPermission(this, str) != 0) {
                arrayList.add(str);
            }
        }
        if (arrayList.isEmpty()) {
            return true;
        }
        this.nrOfPermissionsNeeded = arrayList.size();
        ActivityCompat.requestPermissions(this, (String[]) arrayList.toArray(new String[arrayList.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        return false;
    }

    @Override // android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback, android.support.v4.app.FragmentActivity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 1234) {
            int i2 = 0;
            for (int i3 : iArr) {
                if (i3 == 0) {
                    i2++;
                }
            }
            if (iArr.length <= 0 || this.nrOfPermissionsNeeded != i2) {
                Log.i(TAG, "After asking the user: One or more permissions not granted");
                Log.i(TAG, "Present more information to the user why this permissions are needed.");
                new AlertDialog.Builder(this).setTitle(R.string.permissions_missing_dialog_title).setMessage(R.string.camera_permissions_missing_dialog_description).setPositiveButton(R.string.permissions_missing_dialog_retry, new DialogInterface.OnClickListener() {
                    /* class de.stoehr.loviapps.wezoom.PreparationActivity.AnonymousClass3 */

                    public void onClick(DialogInterface dialogInterface, int i) {
                        PreparationActivity.this.checkPermissions();
                    }
                }).setNegativeButton(R.string.permissions_missing_dialog_exit, new DialogInterface.OnClickListener() {
                    /* class de.stoehr.loviapps.wezoom.PreparationActivity.AnonymousClass2 */

                    public void onClick(DialogInterface dialogInterface, int i) {
                        PreparationActivity.this.finishAffinity();
                    }
                }).show();
                return;
            }
            Log.i(TAG, "After asking the user: All permissions granted");
            startMainActivity();
        }
    }
}
