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

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.users.HideUsersUtils;

/**
 * Entry under Multiple users that opens the Hide Users feature screen.
 * Available only while Hide Users is not armed.
 */
public class HideUsersPreferenceController extends BasePreferenceController {
    private final UserManager mUserManager;

    public HideUsersPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!UserManager.supportsMultipleUsers()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (UserHandle.myUserId() != UserHandle.USER_SYSTEM
                && !mUserManager.isAdminUser()) {
            return DISABLED_FOR_USER;
        }
        if (HideUsersUtils.isFeatureEnabled(mContext)) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }
}
