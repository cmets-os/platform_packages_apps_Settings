/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class UserIdsSeriesTest {

    @Mock private UserManager mUserManager;

    private Context mContext;
    private int mCurrentUserId;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mCurrentUserId = mContext.getUserId();
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(true).when(mUserManager).isSameProfileGroup(anyInt(), anyInt());
    }

    @Test
    public void getVisibleUserIds_hiddenFullUserInSameGroup_notListed() {
        final int hiddenUserId = mCurrentUserId + 1;
        final UserInfo currentUser =
                new UserInfo(mCurrentUserId, "current", UserInfo.FLAG_FULL);
        final UserInfo hiddenUser =
                new UserInfo(
                        hiddenUserId, "hidden", UserInfo.FLAG_FULL | UserInfo.FLAG_UI_HIDDEN);
        doReturn(List.of(currentUser, hiddenUser)).when(mUserManager).getAliveUsers();

        final UserIdsSeries series = new UserIdsSeries(mContext, /* isNonUIRequest= */ false);

        assertThat(series.getVisibleUserIds()).contains(mCurrentUserId);
        assertThat(series.getVisibleUserIds()).doesNotContain(hiddenUserId);
        assertThat(series.isFromOtherUsers(hiddenUserId)).isTrue();
    }

    @Test
    public void getVisibleUserIds_visibleFullUserInSameGroup_listed() {
        final int visibleUserId = mCurrentUserId + 2;
        final UserInfo currentUser =
                new UserInfo(mCurrentUserId, "current", UserInfo.FLAG_FULL);
        final UserInfo visibleUser =
                new UserInfo(visibleUserId, "visible", UserInfo.FLAG_FULL);
        doReturn(List.of(currentUser, visibleUser)).when(mUserManager).getAliveUsers();

        final UserIdsSeries series = new UserIdsSeries(mContext, /* isNonUIRequest= */ false);

        assertThat(series.getVisibleUserIds()).contains(mCurrentUserId);
        assertThat(series.getVisibleUserIds()).contains(visibleUserId);
        assertThat(series.isFromOtherUsers(visibleUserId)).isFalse();
    }
}
