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

package com.android.settings.users;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl;
import com.android.settingslib.users.HideUsersUtils;

/**
 * Hide Users feature screen: enable toggle and editable Dialer secret codes.
 * Self-hides after enable (only reachable again after Dialer disable code).
 */
public class HideUsersSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener,
        ValidatedEditTextPreference.Validator {

    private static final String KEY_ENABLE = "hide_users_enable";
    private static final String KEY_CODE_DISABLE = "hide_users_code_disable";
    private static final String KEY_CODE_SWITCHER = "hide_users_code_switcher";

    private UserManager mUserManager;
    private SwitchPreferenceCompat mEnablePref;
    private ValidatedEditTextPreference mDisableCodePref;
    private ValidatedEditTextPreference mSwitcherCodePref;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USER;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getPrefContext();
        mUserManager = context.getSystemService(UserManager.class);

        if (HideUsersUtils.isFeatureEnabled(context)) {
            finish();
            return;
        }

        addPreferencesFromResource(R.xml.hide_users_settings);

        mEnablePref = findPreference(KEY_ENABLE);
        mDisableCodePref = findPreference(KEY_CODE_DISABLE);
        mSwitcherCodePref = findPreference(KEY_CODE_SWITCHER);

        mEnablePref.setChecked(false);
        mEnablePref.setOnPreferenceChangeListener(this);

        final String disableCode = HideUsersUtils.getDisableCode(context);
        final String switcherCode = HideUsersUtils.getSwitcherCode(context);
        mDisableCodePref.setText(disableCode);
        mDisableCodePref.setSummary(disableCode);
        mDisableCodePref.setValidator(this);
        mDisableCodePref.setOnPreferenceChangeListener(this);

        mSwitcherCodePref.setText(switcherCode);
        mSwitcherCodePref.setSummary(switcherCode);
        mSwitcherCodePref.setValidator(this);
        mSwitcherCodePref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (HideUsersUtils.isFeatureEnabled(getPrefContext())) {
            finish();
        }
    }

    @Override
    public boolean isTextValid(String value) {
        return HideUsersUtils.isValidSecretCode(getPrefContext(), value);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (KEY_ENABLE.equals(key)) {
            final boolean enable = (Boolean) newValue;
            if (enable) {
                showEnableConfirmDialog();
            }
            // Never flip the switch here; enable path finishes the screen after confirm.
            return false;
        }
        if (KEY_CODE_DISABLE.equals(key) || KEY_CODE_SWITCHER.equals(key)) {
            final String code = (String) newValue;
            if (!HideUsersUtils.isValidSecretCode(getPrefContext(), code)) {
                Toast.makeText(getPrefContext(), R.string.hide_users_code_invalid,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            final String other = KEY_CODE_DISABLE.equals(key)
                    ? HideUsersUtils.getSwitcherCode(getPrefContext())
                    : HideUsersUtils.getDisableCode(getPrefContext());
            if (TextUtils.equals(code, other)) {
                Toast.makeText(getPrefContext(), R.string.hide_users_codes_must_differ,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            final String settingKey = KEY_CODE_DISABLE.equals(key)
                    ? Settings.Global.HIDE_USERS_CODE_DISABLE
                    : Settings.Global.HIDE_USERS_CODE_SWITCHER;
            Settings.Global.putString(getPrefContext().getContentResolver(), settingKey, code);
            preference.setSummary(code);
            ((ValidatedEditTextPreference) preference).setText(code);
            return true;
        }
        return false;
    }

    private void showEnableConfirmDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.hide_users_enable_confirm_title)
                .setMessage(getString(R.string.hide_users_enable_confirm_message,
                        HideUsersUtils.getDisableCode(getPrefContext()),
                        HideUsersUtils.getSwitcherCode(getPrefContext())))
                .setPositiveButton(R.string.hide_users_enable_confirm_button,
                        (dialog, which) -> enableHideUsers())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void enableHideUsers() {
        final Context context = getPrefContext();
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.HIDE_USERS, 1);
        mUserManager.markUsersHiddenAtEnable(UserHandle.myUserId());
        clearAppListCaches(context);
        final Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    static void clearAppListCaches(Context context) {
        AppListRepositoryImpl.clearCaches();
        final ApplicationsState state = ApplicationsState.getInstance(
                (android.app.Application) context.getApplicationContext());
        if (state != null) {
            state.rebuildForHideUsersChange();
        }
    }
}
