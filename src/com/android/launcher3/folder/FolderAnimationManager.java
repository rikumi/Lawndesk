/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.folder;

import static com.android.launcher3.BubbleTextView.TEXT_ALPHA_PROPERTY;
import static com.android.launcher3.LauncherAnimUtils.SCALE_PROPERTY;
import static com.android.launcher3.folder.NineFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.graphics.ColorUtils;
import android.util.Pair;
import android.util.Property;
import android.view.View;
import android.view.animation.AnimationUtils;
import ch.deletescape.lawnchair.LawnchairPreferences;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.graphics.IconShape;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.PropertyResetListener;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.util.Themes;
import java.util.List;

/**
 * Manages the opening and closing animations for a {@link Folder}.
 *
 * All of the animations are done in the Folder.
 * ie. When the user taps on the FolderIcon, we immediately hide the FolderIcon and show the Folder
 * in its place before starting the animation.
 */
public class FolderAnimationManager {

    private Folder mFolder;
    private FolderPagedView mContent;
    private GradientDrawable mFolderBackground;

    private FolderIcon mFolderIcon;
    private PreviewBackground mPreviewBackground;

    private Context mContext;
    private Launcher mLauncher;

    private final boolean mIsOpening;

    private final int mDuration;
    private final int mDelay;

    private final TimeInterpolator mFolderInterpolator;
    private final TimeInterpolator mLargeFolderPreviewItemOpenInterpolator;
    private final TimeInterpolator mLargeFolderPreviewItemCloseInterpolator;

    private final PreviewItemDrawingParams mTmpParams = new PreviewItemDrawingParams(0, 0, 0, 0);

    private final LawnchairPreferences prefs;

    public FolderAnimationManager(Folder folder, boolean isOpening) {
        mFolder = folder;
        mContent = folder.mContent;
        mFolderBackground = (GradientDrawable) mFolder.getBackground();

        mFolderIcon = folder.mFolderIcon;
        mPreviewBackground = mFolderIcon.mBackground;

        mContext = folder.getContext();
        mLauncher = folder.mLauncher;

        mIsOpening = isOpening;

        Resources res = mContent.getResources();
        mDuration = res.getInteger(R.integer.config_materialFolderExpandDuration);
        mDelay = res.getInteger(R.integer.config_folderDelay);

        mFolderInterpolator = AnimationUtils.loadInterpolator(mContext,
                R.interpolator.folder_interpolator);
        mLargeFolderPreviewItemOpenInterpolator = AnimationUtils.loadInterpolator(mContext,
                R.interpolator.large_folder_preview_item_open_interpolator);
        mLargeFolderPreviewItemCloseInterpolator = AnimationUtils.loadInterpolator(mContext,
                R.interpolator.large_folder_preview_item_close_interpolator);

        prefs = Utilities.getLawnchairPrefs(folder.getContext());
    }


    private static final boolean DEBUG = false;

    /**
     * Prepares the Folder for animating between open / closed states.
     */
    public AnimatorSet getAnimator() {
        final DragLayer.LayoutParams lp = (DragLayer.LayoutParams) mFolder.getLayoutParams();
        NineFolderIconLayoutRule rule = mFolderIcon.getLayoutRule();
        final List<BubbleTextView> itemsInPreview = mFolderIcon.getPreviewItems();

        DeviceProfile grid = mLauncher.getDeviceProfile();

        // Match position of the FolderIcon
        final Rect folderIconPos = new Rect();
        float scaleRelativeToDragLayer = mLauncher.getDragLayer()
                .getDescendantRectRelativeToSelf(mFolderIcon, folderIconPos);
        int scaledRadius = mPreviewBackground.getScaledRadius();
        float initialSize = mPreviewBackground.previewSize;

        // Match size/scale of icons in the preview
        float previewScale = rule.scaleForItem(itemsInPreview.size());
        float previewSize = rule.getIconSize() * previewScale;
        float initialScale = initialSize / (rule.getIconSize() * 3
                + mFolder.getPaddingLeft() + mContent.getPaddingLeft()
                + mFolder.getPaddingRight() + mContent.getPaddingRight());
        final float finalScale = 1f;
        float scale = mIsOpening ? initialScale : finalScale;
        mFolder.setScaleX(scale);
        mFolder.setScaleY(scale);
        mFolder.setPivotX(0);
        mFolder.setPivotY(0);

        // We want to create a small X offset for the preview items, so that they follow their
        // expected path to their final locations. ie. an icon should not move right, if it's final
        // location is to its left. This value is arbitrarily defined.
        float iconOffsetX = grid.folderCellPaddingX * initialScale;
        float iconOffsetY = grid.folderCellPaddingY * initialScale;
        // The increased folder cell padding shifts the folder's animation end position to the
        // bottom-right of the actual folder icon. Nudge it back left/up by ~2dp so it lines up
        // with the real folder icon when the open/close animation finishes.
        float alignNudge = 2 * mContext.getResources().getDisplayMetrics().density;
        iconOffsetX -= alignNudge;
        iconOffsetY -= alignNudge;
        float previewItemOffsetX = - ((mFolder.getPaddingLeft() + mContent.getPaddingLeft()
                + rule.getPadding() - mPreviewBackground.getOffsetX() * initialScale / 2)
                * initialScale);
        float previewItemOffsetY = - ((mFolder.getPaddingTop() + mContent.getPaddingTop())
                * initialScale);

        float initialX = folderIconPos.left + mPreviewBackground.getLeftPadding() + previewItemOffsetX + iconOffsetX;
        float initialY = folderIconPos.top + mPreviewBackground.getTopPadding() + previewItemOffsetY + iconOffsetY - mFolder.getHeaderHeight() * initialScale;
        final float xDistance = initialX - lp.x;
        final float yDistance = initialY - lp.y;

        // Set up the Folder background.
        final int finalColor = 0x00000000;
        final int initialColor = ColorUtils.setAlphaComponent(mPreviewBackground.getBgColor(), 0);
        mFolderBackground.mutate();
        mFolderBackground.setColor(mIsOpening ? initialColor : finalColor);

        // Create the animators.
        AnimatorSet a = LauncherAnimUtils.createAnimatorSet();

        // Initialize the Folder items' text.
//        PropertyResetListener colorResetListener =
//                new PropertyResetListener<>(TEXT_ALPHA_PROPERTY, 1f);
        for (BubbleTextView icon : mFolder.getItemsOnPage(mFolder.mContent.getCurrentPage())) {
            if (mIsOpening) {
                icon.setTextVisibility(false);
            }
            ObjectAnimator anim = icon.createTextAlphaAnimator(mIsOpening);
//            anim.addListener(colorResetListener);
            play(a, anim);
        }

        if (mFolder.mInfo.useIconMode(mContext)) {
            play(a, getAnimator(mFolder, View.ALPHA, 0f, 1f));
        }
        play(a, getAnimator(mFolder, View.TRANSLATION_X, xDistance, DEBUG ? xDistance : 0f));
        play(a, getAnimator(mFolder, View.TRANSLATION_Y, yDistance, DEBUG ? yDistance : 0f));
        play(a, getAnimator(mFolder, SCALE_PROPERTY, initialScale, DEBUG ? initialScale : finalScale));
        play(a, getAnimator(mFolderBackground, "color", initialColor, finalColor));

        // Animate the folder icon preview background alpha (fade out when opening, fade in when closing)
        play(a, getAnimator(mPreviewBackground, PreviewBackground.COLOR_MULTIPLIER, 1f, 0f));

        // Animate the elevation midway so that the shadow is not noticeable in the background.
        int midDuration = mDuration / 2;
        Animator z = getAnimator(mFolder, View.TRANSLATION_Z, -mFolder.getElevation(), 0);
        play(a, z, mIsOpening ? midDuration : 0, midDuration);

        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mFolder.setAlpha(1f);
                if (!DEBUG) {
                    mFolder.setTranslationX(0.0f);
                    mFolder.setTranslationY(0.0f);
                    mFolder.setTranslationZ(0.0f);
                    mFolder.setScaleX(1f);
                    mFolder.setScaleY(1f);
                }
            }
        });

        // We set the interpolator on all current child animators here, because the preview item
        // animators may use a different interpolator.
        for (Animator animator : a.getChildAnimations()) {
            animator.setInterpolator(mFolderInterpolator);
        }

        int radiusDiff = scaledRadius - mPreviewBackground.getRadius();
        addPreviewItemAnimators(a, initialScale / scaleRelativeToDragLayer,
                // Background can have a scaled radius in drag and drop mode, so we need to add the
                // difference to keep the preview items centered.
                previewItemOffsetX + radiusDiff, previewItemOffsetY + radiusDiff);
        return a;
    }

    /**
     * Animate the items on the current page.
     */
    private void addPreviewItemAnimators(AnimatorSet animatorSet, final float folderScale,
            float previewItemOffsetX, float previewItemOffsetY) {
        NineFolderIconLayoutRule rule = mFolderIcon.getLayoutRule();
        boolean isOnFirstPage = mFolder.mContent.getCurrentPage() == 0;
        final Pair<List<BubbleTextView>,List<BubbleTextView>> itemsOnPage =
                mFolderIcon.getItemsOnPage(mFolder.mContent.getCurrentPage());
        List<BubbleTextView> itemsInPreview = itemsOnPage.first;
        List<BubbleTextView> itemsNotInPreview = itemsOnPage.second;
        final int numItemsInPreview = itemsInPreview.size();
        final int numItemsInFirstPagePreview = isOnFirstPage
                ? numItemsInPreview : MAX_NUM_ITEMS_IN_PREVIEW;

        final boolean hideAppLabels = prefs.getHideAppLabels();

        if (!mIsOpening) {
            for (int i = 0; i < itemsNotInPreview.size(); ++i) {
                final BubbleTextView btv = itemsNotInPreview.get(i);

                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        btv.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        btv.setVisibility(View.VISIBLE);
                        if (!hideAppLabels) {
                            btv.setTextVisibility(true);
                        }
                    }
                });
            }
        }

        TimeInterpolator previewItemInterpolator = getPreviewItemInterpolator();

        ShortcutAndWidgetContainer cwc = mContent.getPageAt(0).getShortcutsAndWidgets();
        for (int i = 0; i < numItemsInPreview; ++i) {
            final BubbleTextView btv = itemsInPreview.get(i);
            CellLayout.LayoutParams btvLp = (CellLayout.LayoutParams) btv.getLayoutParams();

            // Calculate the final values in the LayoutParams.
            btvLp.isLockedToGrid = true;
            cwc.setupLp(btv);

            // Match scale of icons in the preview of the items on the first page.
            float previewScale = rule.scaleForItem(numItemsInFirstPagePreview);
            float previewSize = rule.getIconSize() * previewScale;
            float iconScale = previewSize / itemsInPreview.get(i).getIconSize();

            final float initialScale = iconScale / folderScale;
            final float finalScale = 1f;
            float scale = mIsOpening ? initialScale : finalScale;
            btv.setScaleX(scale);
            btv.setScaleY(scale);

            // Match positions of the icons in the folder with their positions in the preview
            rule.computePreviewItemDrawingParams(i, numItemsInFirstPagePreview, mTmpParams);
            // The PreviewLayoutRule assumes that the icon size takes up the entire width so we
            // offset by the actual size.
            float iconOffsetX = ((btvLp.width - btv.getIconSize()) * iconScale);

            final float previewPosX = ((mTmpParams.transX + previewItemOffsetX) / folderScale);
            final float previewPosY = ((mTmpParams.transY + previewItemOffsetY) / folderScale);

            final float xDistance = previewPosX - btvLp.x;
            final float yDistance = previewPosY - btvLp.y;

            Animator translationX = getAnimator(btv, View.TRANSLATION_X, xDistance, DEBUG ? xDistance : 0f);
            translationX.setInterpolator(previewItemInterpolator);
            play(animatorSet, translationX);

            Animator translationY = getAnimator(btv, View.TRANSLATION_Y, yDistance, DEBUG ? yDistance : 0f);
            translationY.setInterpolator(previewItemInterpolator);
            play(animatorSet, translationY);

            Animator scaleAnimator = getAnimator(btv, SCALE_PROPERTY, initialScale, DEBUG ? initialScale : finalScale);
            scaleAnimator.setInterpolator(previewItemInterpolator);
            play(animatorSet, scaleAnimator);

            if (mFolder.getItemCount() > MAX_NUM_ITEMS_IN_PREVIEW) {
                // These delays allows the preview items to move as part of the Folder's motion,
                // and its only necessary for large folders because of differing interpolators.
                int delay = mIsOpening ? mDelay : mDelay * 2;
                if (mIsOpening) {
                    translationX.setStartDelay(delay);
                    translationY.setStartDelay(delay);
                    scaleAnimator.setStartDelay(delay);
                }
                translationX.setDuration(translationX.getDuration() - delay);
                translationY.setDuration(translationY.getDuration() - delay);
                scaleAnimator.setDuration(scaleAnimator.getDuration() - delay);
            }

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    // Necessary to initialize values here because of the start delay.
                    if (mIsOpening) {
                        btv.setTranslationX(xDistance);
                        btv.setTranslationY(yDistance);
                        btv.setScaleX(initialScale);
                        btv.setScaleY(initialScale);
                    }
                    if (hideAppLabels) {
                        btv.setTextVisibility(false);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (!DEBUG) {
                        btv.setTranslationX(0.0f);
                        btv.setTranslationY(0.0f);
                        btv.setScaleX(1f);
                        btv.setScaleY(1f);
                    }
                    if (!hideAppLabels) {
                        btv.setTextVisibility(true);
                    }
                }
            });
        }
    }

    private void play(AnimatorSet as, Animator a) {
        play(as, a, a.getStartDelay(), mDuration);
    }

    private void play(AnimatorSet as, Animator a, long startDelay, int duration) {
        a.setStartDelay(startDelay);
        a.setDuration(duration);
        as.play(a);
    }

    private TimeInterpolator getPreviewItemInterpolator() {
        if (mFolder.getItemCount() > MAX_NUM_ITEMS_IN_PREVIEW) {
            // With larger folders, we want the preview items to reach their final positions faster
            // (when opening) and later (when closing) so that they appear aligned with the rest of
            // the folder items when they are both visible.
            return mIsOpening
                    ? mLargeFolderPreviewItemOpenInterpolator
                    : mLargeFolderPreviewItemCloseInterpolator;
        }
        return mFolderInterpolator;
    }

    private Animator getAnimator(View view, Property property, float v1, float v2) {
        return mIsOpening
                ? ObjectAnimator.ofFloat(view, property, v1, v2)
                : ObjectAnimator.ofFloat(view, property, v2, v1);
    }

    private Animator getAnimator(Object object, Property property, float v1, float v2) {
        return mIsOpening
                ? ObjectAnimator.ofFloat(object, property, v1, v2)
                : ObjectAnimator.ofFloat(object, property, v2, v1);
    }

    private Animator getAnimator(GradientDrawable drawable, String property, int v1, int v2) {
        return mIsOpening
                ? ObjectAnimator.ofArgb(drawable, property, v1, v2)
                : ObjectAnimator.ofArgb(drawable, property, v2, v1);
    }
}
