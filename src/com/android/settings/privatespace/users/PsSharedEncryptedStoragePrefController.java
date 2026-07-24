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

package com.android.settings.privatespace.users;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

/**
 * Private Space toggle for Shared encrypted storage opt-in.
 */
public class PsSharedEncryptedStoragePrefController extends TogglePreferenceController {

    private static final String TAG = "PsSharedEncryptedStoragePrefCtrl";

    private final PrivateSpaceMaintainer mPrivateSpaceMaintainer;
    private final UserManager mUserManager;

    public PsSharedEncryptedStoragePrefController(Context context, String key) {
        super(context, key);
        mPrivateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(context);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        final UserHandle privateSpaceUserHandle =
                mPrivateSpaceMaintainer.getPrivateProfileHandle();
        if (privateSpaceUserHandle == null) {
            Log.w(TAG, "No private space user fetched, treating as unavailable");
            return CONDITIONALLY_UNAVAILABLE;
        }
        if (mPrivateSpaceMaintainer.isPrivateSpaceLocked()) {
            return DISABLED_FOR_USER;
        }
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        final UserHandle handle = mPrivateSpaceMaintainer.getPrivateProfileHandle();
        if (handle == null) {
            return false;
        }
        return mUserManager.isSharedEncryptedStorageEnabled(handle.getIdentifier());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final UserHandle handle = mPrivateSpaceMaintainer.getPrivateProfileHandle();
        if (handle == null) {
            return false;
        }
        mUserManager.setSharedEncryptedStorageEnabled(handle.getIdentifier(), isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
