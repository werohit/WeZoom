package de.stoehr.loviapps.wezoom;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

public class AppRater {
    private static final int LAUNCHES_NEEDED_FOR_RATE_POPUP = 15;

    public static void appHasLaunched(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int i = defaultSharedPreferences.getInt("NR_OF_APP_LAUNCHES_FOR_APP_RATER", 0);
        if (defaultSharedPreferences.getBoolean("APP_RATING_DISABLED_BY_USER", false)) {
            return;
        }
        if (i == 15) {
            showRateDialog(context, defaultSharedPreferences);
        } else if (i < 15) {
            defaultSharedPreferences.edit().putInt("NR_OF_APP_LAUNCHES_FOR_APP_RATER", i + 1).apply();
        }
    }

    public static void showRateDialog(final Context context, final SharedPreferences sharedPreferences) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.MyAlertDialogTheme);
        builder.setTitle(R.string.app_rater_title);
        builder.setMessage(R.string.app_rater_desc);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            /* class de.stoehr.loviapps.wezoom.AppRater.AnonymousClass1 */

            public void onCancel(DialogInterface dialogInterface) {
                sharedPreferences.edit().putBoolean("APP_RATING_DISABLED_BY_USER", true).apply();
            }
        });
        builder.setNegativeButton(R.string.app_rater_btn_no, new DialogInterface.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.AppRater.AnonymousClass2 */

            public void onClick(DialogInterface dialogInterface, int i) {
                sharedPreferences.edit().putBoolean("APP_RATING_DISABLED_BY_USER", true).apply();
            }
        });
        builder.setNeutralButton(R.string.app_rater_btn_remind, new DialogInterface.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.AppRater.AnonymousClass3 */

            public void onClick(DialogInterface dialogInterface, int i) {
                sharedPreferences.edit().putInt("NR_OF_APP_LAUNCHES_FOR_APP_RATER", 0).apply();
            }
        });
        builder.setPositiveButton(R.string.app_rater_btn_yes, new DialogInterface.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.AppRater.AnonymousClass4 */

            public void onClick(DialogInterface dialogInterface, int i) {
                sharedPreferences.edit().putBoolean("APP_RATING_DISABLED_BY_USER", true).apply();
                AppRater.openGoolePlayStore(context);
            }
        });
        AlertDialog create = builder.create();
        create.setCanceledOnTouchOutside(false);
        create.show();
    }

    public static void openGoolePlayStore(Context context) {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + context.getPackageName()));
        if (Build.VERSION.SDK_INT < 21) {
            intent.addFlags(1208483840);
        } else {
            intent.addFlags(1208483840);
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
        }
    }
}
