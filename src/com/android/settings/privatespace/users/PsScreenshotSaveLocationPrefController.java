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
import android.util.Log;

import com.android.settings.privacy.ScreenshotSaveLocationPrefController;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

/**
 * Private Space preference for screenshot save location (Secure setting for the PS userId).
 */
public class PsScreenshotSaveLocationPrefController extends ScreenshotSaveLocationPrefController {

    private static final String TAG = "PsScreenshotSaveLocationPrefCtrl";

    private final PrivateSpaceMaintainer mPrivateSpaceMaintainer;

    public PsScreenshotSaveLocationPrefController(Context context, String key) {
        super(context, key, PrivateSpaceMaintainer.getInstance(context).getPrivateProfileHandle());
        mPrivateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(context);
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
}
