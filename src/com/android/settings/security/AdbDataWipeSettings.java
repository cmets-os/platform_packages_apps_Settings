/*
 * Copyright (C) 2026 cmets-os
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.security;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.ext.settings.AdbDataWipeUtils;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.users.HideUsersUtils;

/**
 * ADB data wipe management screen: enable toggle and editable Dialer disable code.
 * Self-hides after arming (reachable again only after Dialer disable code).
 */
public class AdbDataWipeSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener,
        ValidatedEditTextPreference.Validator {

    private static final String KEY_ENABLE = "adb_data_wipe_enable";
    private static final String KEY_CODE_DISABLE = "adb_data_wipe_code_disable";

    private SwitchPreferenceCompat mEnablePref;
    private ValidatedEditTextPreference mDisableCodePref;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SECURITY;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getPrefContext();

        if (AdbDataWipeUtils.isArmed(context)) {
            finish();
            return;
        }

        addPreferencesFromResource(R.xml.adb_data_wipe_settings);

        mEnablePref = findPreference(KEY_ENABLE);
        mDisableCodePref = findPreference(KEY_CODE_DISABLE);

        mEnablePref.setChecked(false);
        mEnablePref.setOnPreferenceChangeListener(this);

        final String disableCode = AdbDataWipeUtils.getDisableCode(context);
        mDisableCodePref.setText(disableCode);
        mDisableCodePref.setSummary(disableCode);
        mDisableCodePref.setValidator(this);
        mDisableCodePref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (AdbDataWipeUtils.isArmed(getPrefContext())) {
            finish();
        }
    }

    @Override
    public boolean isTextValid(String value) {
        return isAcceptableDisableCode(value);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (KEY_ENABLE.equals(key)) {
            final boolean enable = (Boolean) newValue;
            if (enable) {
                showEnableConfirmDialog();
            }
            // Never flip the switch here; arm path finishes the screen after confirm.
            return false;
        }
        if (KEY_CODE_DISABLE.equals(key)) {
            final String code = (String) newValue;
            if (!isAcceptableDisableCode(code)) {
                Toast.makeText(getPrefContext(), R.string.adb_data_wipe_code_invalid,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            Settings.Global.putString(getPrefContext().getContentResolver(),
                    Settings.Global.ADB_DATA_WIPE_CODE_DISABLE, code);
            preference.setSummary(code);
            ((ValidatedEditTextPreference) preference).setText(code);
            return true;
        }
        return false;
    }

    private boolean isAcceptableDisableCode(String code) {
        if (!AdbDataWipeUtils.isValidSecretCode(code)) {
            return false;
        }
        final Context context = getPrefContext();
        // Reject currently configured Hide Users codes (not only their defaults).
        if (code.equals(HideUsersUtils.getDisableCode(context))
                || code.equals(HideUsersUtils.getSwitcherCode(context))) {
            return false;
        }
        return true;
    }

    private void showEnableConfirmDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.adb_data_wipe_enable_confirm_title)
                .setMessage(getString(R.string.adb_data_wipe_enable_confirm_message,
                        AdbDataWipeUtils.getDisableCode(getPrefContext())))
                .setPositiveButton(R.string.adb_data_wipe_enable_confirm_button,
                        (dialog, which) -> armAdbDataWipe())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void armAdbDataWipe() {
        final Context context = getPrefContext();
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.ADB_DATA_WIPE, 1);
        final Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }
}
