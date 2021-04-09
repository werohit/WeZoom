package de.stoehr.loviapps.wezoom;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

public class AppInfoActivity extends AppCompatActivity {
    private CustomFragmentPagerAdapter customFragmentPagerAdapter;
    private ViewPager viewPager;

    /* access modifiers changed from: protected */
    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.SupportActivity, android.support.v4.app.FragmentActivity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_app_info);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.customFragmentPagerAdapter = new CustomFragmentPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager2 = (ViewPager) findViewById(R.id.container);
        this.viewPager = viewPager2;
        viewPager2.setAdapter(this.customFragmentPagerAdapter);
        ((TabLayout) findViewById(R.id.tabs)).setupWithViewPager(this.viewPager);
        ((ImageButton) findViewById(R.id.sendMail)).setOnClickListener(new View.OnClickListener() {
            /* class de.stoehr.loviapps.wezoom.AppInfoActivity.AnonymousClass1 */

            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("message/rfc822");
                intent.putExtra("android.intent.extra.EMAIL", new String[]{AppInfoActivity.this.getString(R.string.author_email)});
                intent.putExtra("android.intent.extra.SUBJECT", "[" + AppInfoActivity.this.getString(R.string.app_name) + "] " + AppInfoActivity.this.getString(R.string.sendMail_subject));
                try {
                    AppInfoActivity.this.startActivity(Intent.createChooser(intent, AppInfoActivity.this.getString(R.string.sendMail_dialog_title)));
                } catch (ActivityNotFoundException unused) {
                    AppInfoActivity appInfoActivity = AppInfoActivity.this;
                    Toast.makeText(appInfoActivity, appInfoActivity.getString(R.string.sendMail_dialog_exception), 0).show();
                }
            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        menuItem.getItemId();
        return super.onOptionsItemSelected(menuItem);
    }

    public static class AppInfoGuideFragment extends Fragment {
        @Override // android.support.v4.app.Fragment
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            return layoutInflater.inflate(R.layout.fragment_app_info_guide, viewGroup, false);
        }
    }

    public static class AppInfoAboutFragment extends Fragment {
        @Override // android.support.v4.app.Fragment
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            View inflate = layoutInflater.inflate(R.layout.fragment_app_info_about, viewGroup, false);
            ((ImageButton) inflate.findViewById(R.id.rateThisApp)).setOnClickListener(new View.OnClickListener() {
                /* class de.stoehr.loviapps.wezoom.AppInfoActivity.AppInfoAboutFragment.AnonymousClass1 */

                public void onClick(View view) {
                    AppRater.openGoolePlayStore(view.getContext());
                }
            });
            return inflate;
        }
    }

    public static class AppInfoDebugFragment extends Fragment {
        @Override // android.support.v4.app.Fragment
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String string = defaultSharedPreferences.getString("CAM_CURRENT_PREVIEW_SIZE", "0x0");
            String string2 = defaultSharedPreferences.getString("REAL_DISPLAY_SIZE", "0x0");
            String string3 = defaultSharedPreferences.getString("CURRRENT_SURFACE_VIEW_SIZE", "0x0");
            boolean z = defaultSharedPreferences.getBoolean("IS_CAMERA_SENSOR_MOUNTED_UPSIDEDOWN", false);
            int i = defaultSharedPreferences.getInt("CAMERA_SENSOR_MOUNT_ORIENTATION", 0);
            String string4 = defaultSharedPreferences.getString("CAM_PREVIEW_SIZE_LIST_ORDERED", "-");
            String string5 = defaultSharedPreferences.getString("CAM_DEFAULT_FOCUS_MODE", "-");
            String string6 = defaultSharedPreferences.getString("CAM_CURRENT_FOCUS_MODE", "-");
            String string7 = defaultSharedPreferences.getString("CAM_MAX_FOCUS_AREAS", "-");
            String string8 = defaultSharedPreferences.getString("CAM_MAX_METERING_AREAS", "-");
            String string9 = defaultSharedPreferences.getString("CAM_SUPPORTED_FOCUS_MODES", "-");
            View inflate = layoutInflater.inflate(R.layout.fragment_app_info_debug, viewGroup, false);
            ((TextView) inflate.findViewById(R.id.debugInformation)).setText("LVM" + getString(R.string.debug_category_rendering) + "\n\n" + getString(R.string.debug_metric_previewsize) + ": " + string + "\n" + getString(R.string.debug_metric_realDisplaySize) + ": " + string2 + "\n" + getString(R.string.debug_metric_currentSurfaceViewSize) + ": " + string3 + "\n" + getString(R.string.debug_metric_isCameraSensorMountedUpsidedown) + ": " + z + "\n" + getString(R.string.debug_metric_cameraSensorMountOrientation) + ": " + i + "\n" + "\n" + getString(R.string.debug_category_available_preview_sizes_ordered) + "\n\n" + string4 + "\n" + "\n" + getString(R.string.debug_category_focus) + "\n\n" + getString(R.string.debug_metric_focus_default) + ": " + string5 + "\n" + getString(R.string.debug_metric_focus_current) + ": " + string6 + "\n" + getString(R.string.debug_metric_focus_maxFocusAreas) + ": " + string7 + "\n" + getString(R.string.debug_metric_focus_maxMeteringAreas) + ": " + string8 + "\n" + "\n" + getString(R.string.debug_category_supportedfocus) + "\n\n" + string9 + "\n" + "\n");
            return inflate;
        }
    }

    public class CustomFragmentPagerAdapter extends FragmentPagerAdapter {
        @Override // android.support.v4.view.PagerAdapter
        public int getCount() {
            return 3;
        }

        public CustomFragmentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override // android.support.v4.app.FragmentPagerAdapter
        public Fragment getItem(int i) {
            if (i == 0) {
                return new AppInfoGuideFragment();
            }
            if (i == 1) {
                return new AppInfoAboutFragment();
            }
            if (i != 2) {
                return null;
            }
            return new AppInfoDebugFragment();
        }

        @Override // android.support.v4.view.PagerAdapter
        public CharSequence getPageTitle(int i) {
            if (i == 0) {
                return AppInfoActivity.this.getString(R.string.title_tab_user_manual);
            }
            if (i == 1) {
                return AppInfoActivity.this.getString(R.string.title_tab_about);
            }
            if (i != 2) {
                return null;
            }
            return AppInfoActivity.this.getString(R.string.title_tab_debug);
        }
    }
}
