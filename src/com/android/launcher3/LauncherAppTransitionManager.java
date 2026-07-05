/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.launcher3;


import android.app.ActivityOptions;
import android.content.Context;
import android.view.View;

/**
 * Manages the opening and closing app transitions from Launcher.
 */
public class LauncherAppTransitionManager {

    public static LauncherAppTransitionManager newInstance(Context context) {
        return Utilities.getOverrideObject(LauncherAppTransitionManager.class,
                context, R.string.app_transition_manager_class);
    }

    public ActivityOptions getActivityLaunchOptions(Launcher launcher, View v) {
        if (Utilities.ATLEAST_MARSHMALLOW) {
            // Use a custom scale-up window animation. makeScaleUpAnimation /
            // makeClipRevealAnimation are silently overridden by the system's starting window
            // (splash) on Android 10+, leaving only a faint default scale. A custom animation is
            // reliably applied by the framework, giving a visible "app zooms to fullscreen" open
            // transition. The launcher-side icon zoom (Launcher#playAppLaunchIconZoom) complements
            // this so the icon appears to grow into the opening app.
            return ActivityOptions.makeCustomAnimation(launcher,
                    R.anim.app_open_scale_up, R.anim.no_anim);
        } else if (Utilities.ATLEAST_LOLLIPOP_MR1) {
            // On L devices, we use the device default slide-up transition.
            // On L MR1 devices, we use a custom version of the slide-up transition which
            // doesn't have the delay present in the device default.
            return ActivityOptions.makeCustomAnimation(launcher, R.anim.task_open_enter,
                    R.anim.no_anim);
        }
        return null;
    }

    public void destroy() {

    }
}
