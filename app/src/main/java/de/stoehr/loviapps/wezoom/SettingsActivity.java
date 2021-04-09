package de.stoehr.loviapps.wezoom;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private static SharedPreferences sharedPref;
    private static String[] supportedPreviewSizesOrdered;

    /* access modifiers changed from: protected */
    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.SupportActivity, android.support.v4.app.FragmentActivity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        supportedPreviewSizesOrdered = getIntent().getExtras().getStringArray("supportedPreviewSizesOrdered");
        getFragmentManager().beginTransaction().replace(16908290, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        private final String PREVIEW_SIZES_LIST_PREF_KEY = "PREVIEW_SIZES_LIST";
        private CheckBoxPreference enableHighestPreviewSizeCheckBox;
        private CheckBoxPreference enableLeftHandedModeCheckBox;
        private CheckBoxPreference fullscreenCheckBox;
        private ListPreference previewSizesListPref;
        private CheckBoxPreference showZoomStatusCheckBox;

        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            addPreferencesFromResource(R.xml.preferences);
            CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference("ENABLE_HIGHEST_PREVIEW_SIZE");
            this.enableHighestPreviewSizeCheckBox = checkBoxPreference;
            checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                /* class de.stoehr.loviapps.wezoom.SettingsActivity.SettingsFragment.AnonymousClass1 */

                public boolean onPreferenceChange(Preference preference, Object obj) {
                    if (((Boolean) obj).booleanValue()) {
                        SettingsFragment.this.previewSizesListPref.setEnabled(false);
                    } else {
                        SettingsFragment.this.previewSizesListPref.setEnabled(true);
                    }
                    ((MainApplication) SettingsFragment.this.getActivity().getApplication()).setPreviewSizeChangedInSettings(true);
                    return true;
                }
            });
            PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("PREF_CATEGORY_CAMERA");
            this.previewSizesListPref = setListPreferenceData((ListPreference) findPreference("PREVIEW_SIZES_LIST"), getActivity());
            String string = SettingsActivity.sharedPref.getString("CAM_CURRENT_PREVIEW_SIZE", "LVM");
            String string2 = SettingsActivity.sharedPref.getString("CAM_CURRENT_PREVIEW_SIZE_FROM_SETTINGS", "LVM");
            if (string2 == null || string2.equalsIgnoreCase("LVM")) {
                this.previewSizesListPref.setSummary(string);
            } else {
                this.previewSizesListPref.setSummary(string2);
            }
            this.previewSizesListPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                /* class de.stoehr.loviapps.wezoom.SettingsActivity.SettingsFragment.AnonymousClass2 */

                public boolean onPreferenceClick(Preference preference) {
                    SettingsFragment settingsFragment = SettingsFragment.this;
                    settingsFragment.setListPreferenceData(settingsFragment.previewSizesListPref, SettingsFragment.this.getActivity());
                    return false;
                }
            });
            this.previewSizesListPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                /* class de.stoehr.loviapps.wezoom.SettingsActivity.SettingsFragment.AnonymousClass3 */

                public boolean onPreferenceChange(Preference preference, Object obj) {
                    int findIndexOfValue = SettingsFragment.this.previewSizesListPref.findIndexOfValue(obj.toString());
                    if (findIndexOfValue != -1) {
                        SettingsFragment.this.previewSizesListPref.setSummary(SettingsFragment.this.previewSizesListPref.getEntries()[findIndexOfValue]);
                        SettingsActivity.sharedPref.edit().putString("CAM_CURRENT_PREVIEW_SIZE_FROM_SETTINGS", SettingsFragment.this.previewSizesListPref.getEntries()[findIndexOfValue].toString()).apply();
                        ((MainApplication) SettingsFragment.this.getActivity().getApplication()).setPreviewSizeChangedInSettings(true);
                    }
                    return true;
                }
            });
            this.previewSizesListPref.setEnabled(!SettingsActivity.sharedPref.getBoolean("ENABLE_HIGHEST_PREVIEW_SIZE", true));
            preferenceCategory.addPreference(this.previewSizesListPref);
            CheckBoxPreference checkBoxPreference2 = (CheckBoxPreference) findPreference("ENABLE_LEFTHANDED_MODE");
            this.enableLeftHandedModeCheckBox = checkBoxPreference2;
            checkBoxPreference2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                /* class de.stoehr.loviapps.wezoom.SettingsActivity.SettingsFragment.AnonymousClass4 */

                public boolean onPreferenceChange(Preference preference, Object obj) {
                    ((MainApplication) SettingsFragment.this.getActivity().getApplication()).setLeftHandedModeChangedInSettings(true);
                    return true;
                }
            });
            CheckBoxPreference checkBoxPreference3 = (CheckBoxPreference) findPreference("SHOW_ZOOM_STATUS");
            this.showZoomStatusCheckBox = checkBoxPreference3;
            checkBoxPreference3.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                /* class de.stoehr.loviapps.wezoom.SettingsActivity.SettingsFragment.AnonymousClass5 */

                public boolean onPreferenceChange(Preference preference, Object obj) {
                    ((MainApplication) SettingsFragment.this.getActivity().getApplication()).setShowZoomStatusChangedInSettings(true);
                    return true;
                }
            });
        }

        /* access modifiers changed from: protected */
        public ListPreference setListPreferenceData(ListPreference listPreference, Activity activity) {
            String string = SettingsActivity.sharedPref.getString("CAM_CURRENT_PREVIEW_SIZE", "LVM");
            if (listPreference == null) {
                listPreference = new ListPreference(activity);
            }
            listPreference.setEntries(SettingsActivity.supportedPreviewSizesOrdered);
            listPreference.setDefaultValue(string);
            listPreference.setEntryValues(SettingsActivity.supportedPreviewSizesOrdered);
            listPreference.setTitle(R.string.title_settings_manual_prev_isze);
            listPreference.setDialogTitle(R.string.title_settings_manual_prev_isze);
            listPreference.setKey("PREVIEW_SIZES_LIST");
            return listPreference;
        }
    }
}
