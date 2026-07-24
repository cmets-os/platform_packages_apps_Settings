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

package com.android.settings.deviceinfo.storage;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Owner management page for Shared encrypted storage: enable for user 0, status, wipe.
 */
public class SharedEncryptedStorageSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String KEY_ENABLE = "shared_encrypted_storage_enable";
    private static final String KEY_STATUS = "shared_encrypted_storage_status";
    private static final String KEY_WIPE = "shared_encrypted_storage_wipe";

    private UserManager mUserManager;
    private StorageManager mStorageManager;
    private SwitchPreferenceCompat mEnablePref;
    private Preference mStatusPref;
    private Preference mWipePref;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_STORAGE_CATEGORY;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getPrefContext();
        mUserManager = context.getSystemService(UserManager.class);
        mStorageManager = context.getSystemService(StorageManager.class);

        if (UserHandle.myUserId() != UserHandle.USER_SYSTEM || !mUserManager.isAdminUser()) {
            finish();
            return;
        }

        addPreferencesFromResource(R.xml.shared_encrypted_storage_settings);

        mEnablePref = findPreference(KEY_ENABLE);
        mStatusPref = findPreference(KEY_STATUS);
        mWipePref = findPreference(KEY_WIPE);

        mEnablePref.setOnPreferenceChangeListener(this);
        mWipePref.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        final boolean enabled = mUserManager.isSharedEncryptedStorageEnabled(UserHandle.USER_SYSTEM);
        mEnablePref.setChecked(enabled);
        final boolean unlocked = mStorageManager != null
                && mStorageManager.isSharedEncryptedStorageUnlocked();
        mStatusPref.setSummary(unlocked
                ? R.string.shared_encrypted_storage_status_unlocked
                : R.string.shared_encrypted_storage_status_locked);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnablePref) {
            mUserManager.setSharedEncryptedStorageEnabled(
                    UserHandle.USER_SYSTEM, Boolean.TRUE.equals(newValue));
            refresh();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mWipePref) {
            new AlertDialog.Builder(getPrefContext())
                    .setTitle(R.string.shared_encrypted_storage_wipe_confirm_title)
                    .setMessage(R.string.shared_encrypted_storage_wipe_confirm_message)
                    .setPositiveButton(R.string.shared_encrypted_storage_wipe_confirm_button,
                            (dialog, which) -> {
                                mStorageManager.destroySharedEncryptedStorage();
                                refresh();
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }
        return false;
    }
}
