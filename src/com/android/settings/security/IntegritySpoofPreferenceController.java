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

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.core.BasePreferenceController;

/**
 * Entry under More security & privacy for the Play Integrity spoof system page.
 * Restricted to the device owner admin, matching Shared / ADB wipe entry points.
 */
public class IntegritySpoofPreferenceController extends BasePreferenceController {
    private final UserManager mUserManager;

    public IntegritySpoofPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (UserHandle.myUserId() != UserHandle.USER_SYSTEM) {
            return DISABLED_FOR_USER;
        }
        if (mUserManager == null || !mUserManager.isAdminUser()) {
            return DISABLED_FOR_USER;
        }
        return AVAILABLE;
    }
}
