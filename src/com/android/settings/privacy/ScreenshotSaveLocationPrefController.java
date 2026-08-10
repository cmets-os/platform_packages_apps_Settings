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

package com.android.settings.privacy;

import android.content.Context;
import android.ext.settings.ExtSettings;
import android.ext.settings.StringSetting;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.AbstractListPreferenceController;
import com.android.settings.ext.RadioButtonPickerFragment2;

/**
 * Owner preference: Default ({@code Pictures/Screenshots}) vs Shared ({@code Shared/Screenshots}).
 */
public class ScreenshotSaveLocationPrefController extends AbstractListPreferenceController {

    static final String SETTING_VALUE_DEFAULT = "default";
    static final String SETTING_VALUE_SHARED = "shared";

    static final int VALUE_DEFAULT = 0;
    static final int VALUE_SHARED = 1;

    private final StringSetting mSetting = ExtSettings.SCREENSHOT_SAVE_LOCATION;
    private final Context mUserContext;
    private final UserManager mUserManager;
    private final int mTargetUserId;

    public ScreenshotSaveLocationPrefController(Context context, String key) {
        this(context, key, context.getUser());
    }

    protected ScreenshotSaveLocationPrefController(Context context, String key, UserHandle user) {
        super(context, key);
        final UserHandle target = user != null ? user : context.getUser();
        mTargetUserId = target.getIdentifier();
        mUserContext =
                context.getUser().equals(target) ? context : context.createContextAsUser(target, 0);
        mUserManager = context.getSystemService(UserManager.class);
    }

    protected boolean isSharedOptedIn() {
        return mUserManager != null
                && mUserManager.isSharedEncryptedStorageEnabled(mTargetUserId);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    protected void getEntries(Entries entries) {
        entries.add(R.string.screenshot_save_location_default, VALUE_DEFAULT);
        entries.add(
                mContext.getText(R.string.screenshot_save_location_shared),
                isSharedOptedIn()
                        ? null
                        : mContext.getText(
                                R.string.screenshot_save_location_shared_disabled_summary),
                VALUE_SHARED,
                isSharedOptedIn());
    }

    @Override
    protected int getCurrentValue() {
        if (SETTING_VALUE_SHARED.equals(mSetting.get(mUserContext, mTargetUserId))
                && isSharedOptedIn()) {
            return VALUE_SHARED;
        }
        return VALUE_DEFAULT;
    }

    @Override
    protected boolean setValue(int val) {
        if (val == VALUE_SHARED) {
            if (!isSharedOptedIn()) {
                return false;
            }
            return mSetting.put(mUserContext, SETTING_VALUE_SHARED);
        }
        return mSetting.put(mUserContext, SETTING_VALUE_DEFAULT);
    }

    @Override
    public void addPrefsAfterList(RadioButtonPickerFragment2 fragment, PreferenceScreen screen) {
        addFooterPreference(screen, R.string.screenshot_save_location_footer);
    }
}
