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

import android.annotation.NonNull;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.ext.integrity.IntegritySpoofStore;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

/**
 * System page for importing Play Integrity spoof keybox/props/telephony and applying hot reload.
 */
public class IntegritySpoofSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String TAG = "IntegritySpoofSettings";

    private static final String KEY_STATUS = "integrity_spoof_status";
    private static final String KEY_IMPORT_KEYBOX = "integrity_spoof_import_keybox";
    private static final String KEY_IMPORT_PROPS = "integrity_spoof_import_props";
    private static final String KEY_PROPS_URL = "integrity_spoof_props_url";
    private static final String KEY_UPDATE_PROPS_URL = "integrity_spoof_update_props_url";
    private static final String KEY_MCC = "integrity_spoof_telephony_mcc";
    private static final String KEY_MNC = "integrity_spoof_telephony_mnc";
    private static final String KEY_ISO = "integrity_spoof_telephony_iso";
    private static final String KEY_NAME = "integrity_spoof_telephony_name";
    private static final String KEY_SAVE_TELEPHONY = "integrity_spoof_save_telephony";
    private static final String KEY_APPLY = "integrity_spoof_apply";

    private Preference mStatusPref;
    private EditTextPreference mPropsUrlPref;
    private EditTextPreference mMccPref;
    private EditTextPreference mMncPref;
    private EditTextPreference mIsoPref;
    private EditTextPreference mNamePref;

    private ActivityResultLauncher<String[]> mKeyboxPicker;
    private ActivityResultLauncher<String[]> mPropsPicker;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SECURITY;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.integrity_spoof_settings);

        mStatusPref = findPreference(KEY_STATUS);
        mPropsUrlPref = findPreference(KEY_PROPS_URL);
        mMccPref = findPreference(KEY_MCC);
        mMncPref = findPreference(KEY_MNC);
        mIsoPref = findPreference(KEY_ISO);
        mNamePref = findPreference(KEY_NAME);

        findPreference(KEY_IMPORT_KEYBOX).setOnPreferenceClickListener(this);
        findPreference(KEY_IMPORT_PROPS).setOnPreferenceClickListener(this);
        findPreference(KEY_UPDATE_PROPS_URL).setOnPreferenceClickListener(this);
        findPreference(KEY_SAVE_TELEPHONY).setOnPreferenceClickListener(this);
        findPreference(KEY_APPLY).setOnPreferenceClickListener(this);
        mPropsUrlPref.setOnPreferenceChangeListener(this);

        mKeyboxPicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::onKeyboxPicked);
        mPropsPicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::onPropsPicked);

        IntegritySpoofStore.ensureDir();
        IntegritySpoofStore.syncPolicy(getPrefContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        final Context ctx = getPrefContext();
        IntegritySpoofStore.syncPolicy(ctx);
        final String keyboxSource = IntegritySpoofStore.getKeyboxSource();
        final boolean anyPi = Settings.Global.getInt(ctx.getContentResolver(),
                Settings.Global.INTEGRITY_SPOOF_ANY_PI, 0) == 1;
        final boolean anyTel = Settings.Global.getInt(ctx.getContentResolver(),
                Settings.Global.INTEGRITY_SPOOF_ANY_TEL, 0) == 1;
        final long lastReload = Settings.Global.getLong(ctx.getContentResolver(),
                Settings.Global.INTEGRITY_SPOOF_LAST_RELOAD_MS, 0L);
        final String lastReloadText = lastReload > 0
                ? DateFormat.getDateTimeInstance().format(new Date(lastReload))
                : ctx.getString(R.string.integrity_spoof_never_applied);
        final String keyboxStatus;
        if (keyboxSource == null) {
            keyboxStatus = ctx.getString(R.string.integrity_spoof_keybox_missing);
        } else if (IntegritySpoofStore.KEYBOX_SOURCE_DEFAULT_AOSP_SOFT.equals(keyboxSource)) {
            if (IntegritySpoofStore.isEcdsaSigningCertExpired()) {
                keyboxStatus = ctx.getString(
                        R.string.integrity_spoof_keybox_default_soft_ecdsa_expired);
            } else {
                keyboxStatus = ctx.getString(R.string.integrity_spoof_keybox_default_soft);
            }
        } else if (IntegritySpoofStore.KEYBOX_SOURCE_IMPORTED.equals(keyboxSource)) {
            keyboxStatus = ctx.getString(R.string.integrity_spoof_keybox_status_imported);
        } else {
            keyboxStatus = ctx.getString(R.string.integrity_spoof_keybox_present);
        }
        mStatusPref.setSummary(ctx.getString(R.string.integrity_spoof_status_summary,
                keyboxStatus,
                anyPi ? ctx.getString(R.string.integrity_spoof_yes)
                        : ctx.getString(R.string.integrity_spoof_no),
                anyTel ? ctx.getString(R.string.integrity_spoof_yes)
                        : ctx.getString(R.string.integrity_spoof_no),
                lastReloadText));

        final String url = Settings.Global.getString(ctx.getContentResolver(),
                Settings.Global.INTEGRITY_SPOOF_PROPS_URL);
        mPropsUrlPref.setText(url);
        mPropsUrlPref.setSummary(url != null && !url.isEmpty()
                ? url
                : ctx.getString(R.string.integrity_spoof_props_url_summary));

        final IntegritySpoofStore.TelephonySpoof tel = IntegritySpoofStore.getTelephony(ctx);
        if (tel != null) {
            mMccPref.setText(tel.mcc);
            mMncPref.setText(tel.mnc);
            mIsoPref.setText(tel.simCountryIso);
            mNamePref.setText(tel.operatorName);
            mMccPref.setSummary(tel.mcc);
            mMncPref.setSummary(tel.mnc);
            mIsoPref.setSummary(tel.simCountryIso);
            mNamePref.setSummary(tel.operatorName);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPropsUrlPref) {
            Settings.Global.putString(getPrefContext().getContentResolver(),
                    Settings.Global.INTEGRITY_SPOOF_PROPS_URL, (String) newValue);
            refresh();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String key = preference.getKey();
        if (KEY_IMPORT_KEYBOX.equals(key)) {
            mKeyboxPicker.launch(new String[] {"*/*", "text/xml", "application/xml"});
            return true;
        }
        if (KEY_IMPORT_PROPS.equals(key)) {
            mPropsPicker.launch(new String[] {"*/*", "application/json", "text/plain"});
            return true;
        }
        if (KEY_UPDATE_PROPS_URL.equals(key)) {
            updatePropsFromUrl();
            return true;
        }
        if (KEY_SAVE_TELEPHONY.equals(key)) {
            saveTelephony();
            return true;
        }
        if (KEY_APPLY.equals(key)) {
            IntegritySpoofStore.applyNow(getPrefContext());
            Toast.makeText(getPrefContext(), R.string.integrity_spoof_apply_done, Toast.LENGTH_SHORT)
                    .show();
            refresh();
            return true;
        }
        return false;
    }

    private void onKeyboxPicked(Uri uri) {
        if (uri == null) {
            return;
        }
        try (InputStream in = getPrefContext().getContentResolver().openInputStream(uri)) {
            if (in == null) {
                toastError();
                return;
            }
            final byte[] bytes = in.readAllBytes();
            if (!IntegritySpoofStore.importKeybox(bytes)) {
                Toast.makeText(getPrefContext(), R.string.integrity_spoof_keybox_invalid,
                        Toast.LENGTH_LONG).show();
                return;
            }
            IntegritySpoofStore.syncPolicy(getPrefContext());
            Toast.makeText(getPrefContext(), R.string.integrity_spoof_keybox_imported,
                    Toast.LENGTH_SHORT).show();
            refresh();
        } catch (Exception e) {
            Log.e(TAG, "keybox import failed", e);
            toastError();
        }
    }

    private void onPropsPicked(Uri uri) {
        if (uri == null) {
            return;
        }
        try (InputStream in = getPrefContext().getContentResolver().openInputStream(uri)) {
            if (in == null) {
                toastError();
                return;
            }
            final String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!IntegritySpoofStore.importPropsJson(getPrefContext(), json)) {
                Toast.makeText(getPrefContext(), R.string.integrity_spoof_props_invalid,
                        Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(getPrefContext(), R.string.integrity_spoof_props_imported,
                    Toast.LENGTH_SHORT).show();
            refresh();
        } catch (Exception e) {
            Log.e(TAG, "props import failed", e);
            toastError();
        }
    }

    private void updatePropsFromUrl() {
        final String url = Settings.Global.getString(getPrefContext().getContentResolver(),
                Settings.Global.INTEGRITY_SPOOF_PROPS_URL);
        if (url == null || url.isEmpty() || !(url.startsWith("https://"))) {
            Toast.makeText(getPrefContext(), R.string.integrity_spoof_props_url_invalid,
                    Toast.LENGTH_LONG).show();
            return;
        }
        final Context appCtx = getPrefContext().getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setInstanceFollowRedirects(false);
                try (InputStream in = conn.getInputStream()) {
                    final byte[] bytes = in.readAllBytes();
                    final String json = new String(bytes, StandardCharsets.UTF_8);
                    final String sha256 = sha256Hex(bytes);
                    final boolean ok = IntegritySpoofStore.importPropsJson(appCtx, json);
                    final var activity = getActivity();
                    if (activity == null) {
                        return;
                    }
                    activity.runOnUiThread(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        if (ok) {
                            Toast.makeText(appCtx,
                                    appCtx.getString(R.string.integrity_spoof_props_imported_fp,
                                            sha256),
                                    Toast.LENGTH_LONG).show();
                            refresh();
                        } else {
                            Toast.makeText(appCtx, R.string.integrity_spoof_props_invalid,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {
                Log.e(TAG, "URL props update failed", e);
                final var activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (isAdded()) {
                            toastError();
                        }
                    });
                }
            }
        });
    }

    private void saveTelephony() {
        final String mcc = textOrEmpty(mMccPref);
        final String mnc = textOrEmpty(mMncPref);
        final String iso = textOrEmpty(mIsoPref);
        final String name = textOrEmpty(mNamePref);
        if (mcc.isEmpty() || mnc.isEmpty() || iso.isEmpty()) {
            Toast.makeText(getPrefContext(), R.string.integrity_spoof_telephony_invalid,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!IntegritySpoofStore.importTelephonyFields(
                getPrefContext(), mcc, mnc, iso, iso, name)) {
            Toast.makeText(getPrefContext(), R.string.integrity_spoof_telephony_invalid,
                    Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(getPrefContext(), R.string.integrity_spoof_telephony_saved,
                Toast.LENGTH_SHORT).show();
        refresh();
    }

    private static String textOrEmpty(EditTextPreference pref) {
        final String t = pref.getText();
        return t != null ? t.trim() : "";
    }

    private void toastError() {
        Toast.makeText(getPrefContext(), R.string.integrity_spoof_import_failed, Toast.LENGTH_LONG)
                .show();
    }

    @NonNull
    private static String sha256Hex(@NonNull byte[] bytes) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] dig = md.digest(bytes);
            final StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
