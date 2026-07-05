/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;
import ch.deletescape.lawnchair.LawnchairPreferences;
import com.android.launcher3.CellLayout.ContainerType;
import com.android.launcher3.badge.BadgeRenderer;
import com.android.launcher3.graphics.IconNormalizer;

public class DeviceProfile implements LawnchairPreferences.OnPreferenceChangeListener {

    private Context mContext;

    public final InvariantDeviceProfile inv;

    // Device properties
    public final boolean isTablet;
    public final boolean isLargeTablet;
    public final boolean isPhone;
    public final boolean transposeLayoutWithOrientation;
    public boolean isTallDevice;

    // Device properties in current orientation
    public final boolean isLandscape;
    public final boolean isMultiWindowMode;

    public int widthPx;
    public int heightPx;
    public int availableWidthPx;
    public int availableHeightPx;
    /**
     * The maximum amount of left/right workspace padding as a percentage of the screen width.
     * To be clear, this means that up to 7% of the screen width can be used as left padding, and
     * 7% of the screen width can be used as right padding.
     */
    private static final float MAX_HORIZONTAL_PADDING_PERCENT = 0.14f;

    private static final float TALL_DEVICE_ASPECT_RATIO_THRESHOLD = 2.0f;

    // To evenly space the icons, increase the left/right margins for tablets in portrait mode.
    private static final int PORTRAIT_TABLET_LEFT_RIGHT_PADDING_MULTIPLIER = 4;

    // Workspace
    public final int desiredWorkspaceLeftRightMarginPx;
    public int cellLayoutPaddingLeftRightPx;
    public final int cellLayoutBottomPaddingPx;
    public final int edgeMarginPx;
    public final Rect defaultWidgetPadding;
    public final int defaultPageSpacingPx;
    private final int topWorkspacePadding;
    public float workspaceSpringLoadShrinkFactor;
    public final int workspaceSpringLoadedBottomSpace;
    public float workspaceOptionsShrinkFactor;

    // Page indicator
    public int pageIndicatorSizePx;

    // Workspace icons
    public int iconSizePx;
    public int iconSizeOriginalPx;
    public int iconTextSizePx;
    public int iconTextSizeOriginalPx;
    public int iconDrawablePaddingPx;
    public int iconDrawablePaddingOriginalPx;

    public int cellWidthPx;
    public int cellHeightPx;
    public int workspaceCellPaddingXPx;

    public int hotseatIconSizePx;
    public int hotseatIconSizeOriginalPx;

    // Folder
    public int folderIconSizePx;
    public int folderIconOffsetYPx;

    // Folder cell
    public int folderCellWidthPx;
    public int folderCellHeightPx;
    public int folderCellPaddingX;
    public int folderCellPaddingY;

    // Hotseat
    public int hotseatCellHeightPx;
    // In portrait: size = height, in landscape: size = width
    public int hotseatBarSizePx;
    public int hotseatBarTopPaddingPx;
    public int hotseatBarBottomPaddingPx;
    // Start is the side next to the nav bar, end is the side next to the workspace
    public int hotseatBarSidePaddingStartPx;
    public int hotseatBarSidePaddingEndPx;
    public int hotseatIconTextSizePx;
    public int hotseatIconTextSizeOriginalPx;

    // Widgets
    public final PointF appWidgetScale = new PointF(1.0f, 1.0f);

    // Drop Target
    public int dropTargetBarSizePx;

    // Insets
    private final Rect mInsets = new Rect();
    public final Rect workspacePadding = new Rect();
    private final Rect mHotseatPadding = new Rect();
    private boolean mIsSeascape;

    // Icon badges
    public BadgeRenderer mBadgeRenderer;

    private final Point minSize;
    private final Point maxSize;

    private final LawnchairPreferences prefs;

    public DeviceProfile(Context context, InvariantDeviceProfile inv,
            Point minSize, Point maxSize,
            int width, int height, boolean isLandscape, boolean isMultiWindowMode) {

        widthPx = width;
        heightPx = height;
        this.inv = inv;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.isLandscape = isLandscape;
        this.isMultiWindowMode = isMultiWindowMode;

        // Determine sizes.
        widthPx = width;
        heightPx = height;
        if (isLandscape) {
            availableWidthPx = maxSize.x;
            availableHeightPx = minSize.y;
        } else {
            availableWidthPx = minSize.x;
            availableHeightPx = maxSize.y;
        }

        Resources res = context.getResources();

        // Constants from resources
        isTablet = res.getBoolean(R.bool.is_tablet);
        isLargeTablet = res.getBoolean(R.bool.is_large_tablet);
        isPhone = !isTablet && !isLargeTablet;

        // Some more constants
        transposeLayoutWithOrientation =
                res.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);

        context = getContext(context, isVerticalBarLayout()
                ? Configuration.ORIENTATION_LANDSCAPE
                : Configuration.ORIENTATION_PORTRAIT);
        mContext = context;
        res = context.getResources();


        ComponentName cn = new ComponentName(context.getPackageName(),
                this.getClass().getName());
        defaultWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(context, cn, null);
        edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        desiredWorkspaceLeftRightMarginPx = isVerticalBarLayout() ? 0 : edgeMarginPx;
        int cellLayoutPaddingLeftRightMultiplier = !isVerticalBarLayout() && isTablet
                ? PORTRAIT_TABLET_LEFT_RIGHT_PADDING_MULTIPLIER : 1;
        cellLayoutPaddingLeftRightPx = cellLayoutPaddingLeftRightMultiplier *
                res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_padding);
        cellLayoutBottomPaddingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_bottom_padding);
        pageIndicatorSizePx = res.getDimensionPixelSize(
                R.dimen.dynamic_grid_min_page_indicator_size);
        defaultPageSpacingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_page_spacing);
        topWorkspacePadding =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_top_padding);
        iconDrawablePaddingOriginalPx = 0;
        dropTargetBarSizePx = res.getDimensionPixelSize(R.dimen.dynamic_grid_drop_target_size);
        workspaceSpringLoadedBottomSpace =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_min_spring_loaded_space);

        workspaceCellPaddingXPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_padding_x);

        prefs = Utilities.getLawnchairPrefs(context);
        prefs.addOnPreferenceChangeListener(this, "pref_fullWidthWidgets",
                "pref_twoRowDock", "pref_dockScale", "pref_iconTextScaleSB", "pref_folderIconScale",
                "pref_displayNotificationCount");
    }

    public DeviceProfile copy(Context context) {
        Point size = new Point(availableWidthPx, availableHeightPx);
        return new DeviceProfile(context, inv, size, size, widthPx, heightPx, isLandscape,
                isMultiWindowMode);
    }

    public DeviceProfile getMultiWindowProfile(Context context, Point mwSize) {
        // We take the minimum sizes of this profile and it's multi-window variant to ensure that
        // the system decor is always excluded.
        mwSize.set(Math.min(availableWidthPx, mwSize.x), Math.min(availableHeightPx, mwSize.y));

        // In multi-window mode, we can have widthPx = availableWidthPx
        // and heightPx = availableHeightPx because Launcher uses the InvariantDeviceProfiles'
        // widthPx and heightPx values where it's needed.
        DeviceProfile profile = new DeviceProfile(context, inv, mwSize, mwSize, mwSize.x, mwSize.y,
                isLandscape, true);

        // If there isn't enough vertical cell padding with the labels displayed, hide the labels.
        float workspaceCellPaddingY = profile.getCellSize().y - profile.iconSizePx
                - iconDrawablePaddingPx - profile.iconTextSizePx;
        if (workspaceCellPaddingY < profile.iconDrawablePaddingPx * 2) {
            profile.adjustToHideWorkspaceLabels();
        }

        // We use these scales to measure and layout the widgets using their full invariant profile
        // sizes and then draw them scaled and centered to fit in their multi-window mode cellspans.
        float appWidgetScaleX = (float) profile.getCellSize().x / getCellSize().x;
        float appWidgetScaleY = (float) profile.getCellSize().y / getCellSize().y;
        profile.appWidgetScale.set(appWidgetScaleX, appWidgetScaleY);
        profile.updateWorkspacePadding();

        return profile;
    }

    /**
     * Inverse of {@link #getMultiWindowProfile(Context, Point)}
     *
     * @return device profile corresponding to the current orientation in non multi-window mode.
     */
    public DeviceProfile getFullScreenProfile() {
        return isLandscape ? inv.landscapeProfile : inv.portraitProfile;
    }

    /**
     * All values which (even indirectly) depend on preferences have to be calculated here. This
     * removes the need to completely restart the launcher to apply changes. A simple recreate
     * should be enough to apply this.
     */
    @Override
    public void onValueChanged(String key, LawnchairPreferences prefs, boolean force) {
        Resources res = mContext.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        boolean fullWidthWidgets = prefs.getAllowFullWidthWidgets();
        boolean dockHidden = prefs.getDockHide();
        int dockRows = prefs.getDockRowsCount();
        float dockScale = prefs.getDockScale();

        cellLayoutPaddingLeftRightPx = (!isVerticalBarLayout() && fullWidthWidgets) ? 0
                : res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_padding);

        float aspectRatio = ((float) Math.max(widthPx, heightPx)) / Math.min(widthPx, heightPx);
        isTallDevice = Float.compare(aspectRatio, TALL_DEVICE_ASPECT_RATIO_THRESHOLD) >= 0;

        hotseatBarTopPaddingPx =
                res.getDimensionPixelSize(R.dimen.v1_dynamic_grid_hotseat_top_padding);
        hotseatBarBottomPaddingPx = res.getDimensionPixelSize(R.dimen.v1_dynamic_grid_hotseat_bottom_padding);
        hotseatBarSidePaddingEndPx = 0;
        // Add a bit of space between nav bar and hotseat.
        hotseatBarSidePaddingStartPx = res.getDimensionPixelSize(R.dimen.hotseat_navbar_padding);
        hotseatBarSizePx = isVerticalBarLayout()
                ? hotseatIconSizePx + hotseatBarSidePaddingStartPx
                + hotseatBarSidePaddingEndPx
                : res.getDimensionPixelSize(R.dimen.v1_dynamic_grid_hotseat_size) * dockRows
                        + hotseatBarTopPaddingPx + hotseatBarBottomPaddingPx;

        // Calculate all of the remaining variables.
        updateAvailableDimensions(dm, res);

        iconTextSizePx = (int) (iconTextSizeOriginalPx * prefs.getDesktopTextScale());
        float dockTextScale = prefs.getDockTextScale();
        if (dockTextScale < 0) {
            hotseatIconTextSizePx = iconTextSizePx;
        } else {
            hotseatIconTextSizePx = (int) (hotseatIconTextSizeOriginalPx * dockTextScale);
        }

        // Calculate again to apply text size
        updateAvailableDimensions(dm, res);

        if (dockHidden) {
            hotseatBarSizePx = 0;
            pageIndicatorSizePx = 0;

            updateAvailableDimensions(dm, res);
        } else if (!isVerticalBarLayout()) {
            int bottomPaddingNew = hotseatBarBottomPaddingPx;
            int difference = hotseatBarBottomPaddingPx - bottomPaddingNew;
            hotseatBarTopPaddingPx -= difference;
            hotseatBarBottomPaddingPx = bottomPaddingNew;

            int minHeight = hotseatCellHeightPx * dockRows + hotseatBarBottomPaddingPx + hotseatBarTopPaddingPx;
            if (dockScale < 0) {
                hotseatBarSizePx = Math.max(minHeight, hotseatBarSizePx);
            } else {
                hotseatBarSizePx = Math.max(minHeight, (int) (hotseatBarSizePx * dockScale));
            }
        }

        updateWorkspacePadding();

        // This is done last, after iconSizePx is calculated above.
        mBadgeRenderer = new BadgeRenderer(iconSizePx, prefs.getDisplayNotificationCount());
    }

    /**
     * Adjusts the profile so that the labels on the Workspace are hidden.
     * It is important to call this method after the All Apps variables have been set.
     */
    private void adjustToHideWorkspaceLabels() {
        iconTextSizePx = 0;
        iconDrawablePaddingPx = 0;
        cellHeightPx = iconSizePx;
    }

    private void updateAvailableDimensions(DisplayMetrics dm, Resources res) {
        updateIconSize(1f, res, dm);

        // Check to see if the icons fit within the available height.  If not, then scale down.
        float usedHeight = (cellHeightPx * inv.numRows);
        int maxHeight = (availableHeightPx - getTotalWorkspacePadding().y);
        if (usedHeight > maxHeight) {
            float scale = maxHeight / usedHeight;
            updateIconSize(scale, res, dm);
        }
        updateAvailableFolderCellDimensions(dm, res);
    }

    private void updateIconSize(float scale, Resources res, DisplayMetrics dm) {
        boolean dockVisible = !prefs.getDockHide();
        int labelRowCount = prefs.getHomeLabelRows();
        int dockLabelRowCount = prefs.getDockLabelRows();
        // Workspace
        final boolean isVerticalLayout = isVerticalBarLayout();
        float invIconSizePx = isVerticalLayout ? inv.landscapeIconSize : inv.iconSize;
        iconSizeOriginalPx = Utilities.pxFromDp(invIconSizePx, dm);
        iconSizePx = (int) (iconSizeOriginalPx * scale);
        iconTextSizeOriginalPx = (int) (Utilities.pxFromSp(inv.iconTextSize, dm) * scale);
        iconTextSizePx = (int) (iconTextSizePx * scale);
        iconDrawablePaddingPx = 0;

        int textHeight = Utilities.calculateTextHeight(iconTextSizePx) * labelRowCount;
        cellHeightPx = iconSizePx + iconDrawablePaddingPx + textHeight;
        cellWidthPx = iconSizePx + iconDrawablePaddingPx;

        // TODO: We might eventually also need some calculations to ensure that the label is close
        // TODO: enough to the label, but I couldn't yet figure out how to make a variant of
        // TODO: #getCellSize which properly works for all apps cells

        if (isVerticalLayout) {
            // Always hide the Workspace text with vertical bar layout.
            adjustToHideWorkspaceLabels();
        }

        // Hotseat
        float invHotseatIconSizePx = isVerticalLayout ? inv.landscapeHotseatIconSize : inv.hotseatIconSize;
        hotseatIconTextSizeOriginalPx = (int) (Utilities.pxFromSp(inv.iconTextSize, dm) * scale);
        hotseatIconTextSizePx = (int) (hotseatIconTextSizeOriginalPx * scale);
        textHeight = Utilities.calculateTextHeight(hotseatIconTextSizePx) * dockLabelRowCount;
        hotseatIconSizeOriginalPx = Utilities.pxFromDp(invHotseatIconSizePx, dm);
        hotseatIconSizePx = (int) (hotseatIconSizeOriginalPx * scale);
        if (isVerticalLayout) {
            hotseatBarSizePx =
                    hotseatIconSizePx * prefs.getDockRowsCount()
                    + hotseatBarSidePaddingStartPx + hotseatBarSidePaddingEndPx;
        }
        int additionalHeight =
                prefs.getHideDockLabels() ? 0 : (int) (textHeight + (iconDrawablePaddingOriginalPx
                        * scale));
        hotseatCellHeightPx = hotseatIconSizePx + additionalHeight;

        if (!isVerticalLayout) {
            int expectedWorkspaceHeight = availableHeightPx - (dockVisible ? hotseatBarSizePx : 0)
                    - pageIndicatorSizePx - topWorkspacePadding;
            float minRequiredHeight = dropTargetBarSizePx + workspaceSpringLoadedBottomSpace;
            workspaceSpringLoadShrinkFactor = Math.min(
                    res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f,
                    1 - (minRequiredHeight / expectedWorkspaceHeight));
        } else {
            workspaceSpringLoadShrinkFactor =
                    res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        }
        workspaceOptionsShrinkFactor =
                res.getInteger(R.integer.config_workspaceOptionsShrinkPercentage) / 100.0f;

        // Folder icon
        float folderIconScale = prefs.getFolderIconScale();
        float normalizedCircleSize = IconNormalizer.getNormalizedCircleSize(iconSizePx) * 0.95f;
        // The iOS squircle path fills nearly the entire bounding box, making it visually larger
        // than other shapes (circle, rounded square) of the same radius. Apply an extra shrink
        // so the folder background matches the visual size of regular icons.
        ch.deletescape.lawnchair.adaptive.IconShape currentIconShape =
                ch.deletescape.lawnchair.adaptive.IconShapeManager.Companion
                        .getInstance(mContext).getIconShape();
        if (currentIconShape instanceof ch.deletescape.lawnchair.adaptive.IconShape.IOS) {
            normalizedCircleSize *= 0.95f;
        }
        if (folderIconScale < 0) {
            folderIconSizePx = (int) normalizedCircleSize;
        } else {
            folderIconSizePx = (int) (normalizedCircleSize * folderIconScale);
        }
        folderIconOffsetYPx = (iconSizePx - folderIconSizePx) / 2;
    }

    private void updateAvailableFolderCellDimensions(DisplayMetrics dm, Resources res) {
        int folderBottomPanelSize = res.getDimensionPixelSize(R.dimen.folder_label_padding_top)
                + res.getDimensionPixelSize(R.dimen.folder_label_padding_bottom)
                + Utilities.calculateTextHeight(res.getDimension(R.dimen.folder_label_text_size));

        updateFolderCellSize(1f, dm, res);

        // Don't let the folder get too close to the edges of the screen.
        int folderMargin = edgeMarginPx;
        Point totalWorkspacePadding = getTotalWorkspacePadding();

        // Check if the icons fit within the available height.
        float usedHeight = folderCellHeightPx * inv.numFolderRows + folderBottomPanelSize;
        int maxHeight = availableHeightPx - totalWorkspacePadding.y - folderMargin;
        float scaleY = maxHeight / usedHeight;

        // Check if the icons fit within the available width.
        float usedWidth = folderCellWidthPx * inv.numFolderColumns;
        int maxWidth = availableWidthPx - totalWorkspacePadding.x - folderMargin;
        float scaleX = maxWidth / usedWidth;

        float scale = Math.min(scaleX, scaleY);
        if (scale < 1f) {
            updateFolderCellSize(scale, dm, res);
        }
    }

    private void updateFolderCellSize(float scale, DisplayMetrics dm, Resources res) {
        int folderLabelRowCount = prefs.getHomeLabelRows();

        int textHeight = Utilities.calculateTextHeight(iconTextSizePx) * folderLabelRowCount;
        folderCellPaddingX = (int) (res.getDimensionPixelSize(R.dimen.folder_cell_x_padding) * scale);
        folderCellPaddingY = (int) (res.getDimensionPixelSize(R.dimen.folder_cell_y_padding) * scale);

        folderCellWidthPx = iconSizePx + 2 * folderCellPaddingX;
        folderCellHeightPx = iconSizePx + 2 * folderCellPaddingY;
        if (!prefs.getHideAppLabels()) {
            folderCellHeightPx += textHeight;
        }
    }

    public void updateInsets(Rect insets) {
        mInsets.set(insets);
        updateWorkspacePadding();
    }

    public Rect getInsets() {
        return mInsets;
    }

    public Point getCellSize() {
        Point result = new Point();
        // Since we are only concerned with the overall padding, layout direction does
        // not matter.
        Point padding = getTotalWorkspacePadding();
        result.x = calculateCellWidth(availableWidthPx - padding.x
                - cellLayoutPaddingLeftRightPx * 2, inv.numColumns);
        result.y = calculateCellHeight(availableHeightPx - padding.y
                - cellLayoutBottomPaddingPx, inv.numRows);
        return result;
    }

    public Point getCellSizeOriginal() {
        Point result = new Point();
        // Since we are only concerned with the overall padding, layout direction does
        // not matter.
        Point padding = getTotalWorkspacePadding();
        result.x = calculateCellWidth(availableWidthPx - padding.x
                - cellLayoutPaddingLeftRightPx * 2, inv.numColumnsOriginal);
        result.y = calculateCellHeight(availableHeightPx - padding.y
                - cellLayoutBottomPaddingPx, inv.numRowsOriginal);
        return result;
    }

    public Point getTotalWorkspacePadding() {
        updateWorkspacePadding();
        return new Point(workspacePadding.left + workspacePadding.right,
                workspacePadding.top + workspacePadding.bottom);
    }

    /**
     * Updates {@link #workspacePadding} as a result of any internal value change to reflect the
     * new workspace padding
     */
    private void updateWorkspacePadding() {
        boolean dockVisible = !prefs.getDockHide();
        Rect padding = workspacePadding;
        if (isVerticalBarLayout()) {
            padding.top = 0;
            padding.bottom = edgeMarginPx;
            if (isSeascape()) {
                padding.left = dockVisible ? hotseatBarSizePx : 0;
                padding.right = pageIndicatorSizePx;
            } else {
                padding.left = pageIndicatorSizePx;
                padding.right = dockVisible ? hotseatBarSizePx : 0;
            }
        } else {
            int paddingBottom = (dockVisible ? hotseatBarSizePx : 0) + pageIndicatorSizePx;
            if (isTablet) {
                // Pad the left and right of the workspace to ensure consistent spacing
                // between all icons
                // The amount of screen space available for left/right padding.
                int availablePaddingX = Math.max(0, widthPx - ((inv.numColumns * cellWidthPx) +
                        ((inv.numColumns - 1) * cellWidthPx)));
                availablePaddingX = (int) Math.min(availablePaddingX,
                        widthPx * MAX_HORIZONTAL_PADDING_PERCENT);
                int availablePaddingY = Math.max(0, heightPx - topWorkspacePadding - paddingBottom
                        - (2 * inv.numRows * cellHeightPx) - hotseatBarTopPaddingPx
                        - hotseatBarBottomPaddingPx);
                padding.set(availablePaddingX / 2, topWorkspacePadding + availablePaddingY / 2,
                        availablePaddingX / 2, paddingBottom + availablePaddingY / 2);
            } else {
                int horizontalPadding = Utilities.getLawnchairPrefs(mContext)
                        .getAllowFullWidthWidgets() ? 0 : desiredWorkspaceLeftRightMarginPx;

                // Pad the top and bottom of the workspace with search/hotseat bar sizes
                padding.set(horizontalPadding,
                        topWorkspacePadding,
                        horizontalPadding,
                        paddingBottom);
            }
        }
    }

    public Rect getHotseatLayoutPadding() {
        if (isVerticalBarLayout()) {
            if (isSeascape()) {
                mHotseatPadding.set(mInsets.left + hotseatBarSidePaddingStartPx,
                        mInsets.top, hotseatBarSidePaddingEndPx, mInsets.bottom);
            } else {
                mHotseatPadding.set(hotseatBarSidePaddingEndPx, mInsets.top,
                        mInsets.right + hotseatBarSidePaddingStartPx, mInsets.bottom);
            }
        } else {

            // We want the edges of the hotseat to line up with the edges of the workspace, but the
            // icons in the hotseat are a different size, and so don't line up perfectly. To account
            // for this, we pad the left and right of the hotseat with half of the difference of a
            // workspace cell vs a hotseat cell.
            float workspaceCellWidth = (float) widthPx / inv.numColumns;
            float hotseatCellWidth = (float) widthPx / inv.numHotseatIcons;
            int hotseatAdjustment = Math.round((workspaceCellWidth - hotseatCellWidth) / 2);
            mHotseatPadding.set(
                    hotseatAdjustment + workspacePadding.left + cellLayoutPaddingLeftRightPx,
                    hotseatBarTopPaddingPx,
                    hotseatAdjustment + workspacePadding.right + cellLayoutPaddingLeftRightPx,
                    hotseatBarBottomPaddingPx + mInsets.bottom + cellLayoutBottomPaddingPx);
        }
        return mHotseatPadding;
    }

    /**
     * @return the bounds for which the open folders should be contained within
     */
    public Rect getAbsoluteOpenFolderBounds() {
        if (isVerticalBarLayout()) {
            // Folders should only appear right of the drop target bar and left of the hotseat
            return new Rect(mInsets.left + dropTargetBarSizePx,
                    mInsets.top,
                    mInsets.left + availableWidthPx,
                    mInsets.top + availableHeightPx);
        } else {
            // Folders should only appear below the drop target bar and above the hotseat
            return new Rect(mInsets.left,
                    mInsets.top + dropTargetBarSizePx,
                    mInsets.left + availableWidthPx,
                    mInsets.top + availableHeightPx);
        }
    }

    public static int calculateCellWidth(int width, int countX) {
        return width / countX;
    }
    public static int calculateCellHeight(int height, int countY) {
        return height / countY;
    }

    /**
     * When {@code true}, the device is in landscape mode and the hotseat is on the right column.
     * When {@code false}, either device is in portrait mode or the device is in landscape mode and
     * the hotseat is on the bottom row.
     */
    public boolean isVerticalBarLayout() {
        return isLandscape && transposeLayoutWithOrientation;
    }

    /**
     * Updates orientation information and returns true if it has changed from the previous value.
     */
    public boolean updateIsSeascape(WindowManager wm) {
        if (isVerticalBarLayout()) {
            boolean isSeascape = wm.getDefaultDisplay().getRotation() == Surface.ROTATION_270;
            if (mIsSeascape != isSeascape) {
                mIsSeascape = isSeascape;
                return true;
            }
        }
        return false;
    }

    public boolean isSeascape() {
        return isVerticalBarLayout() && mIsSeascape;
    }

    public boolean shouldFadeAdjacentWorkspaceScreens() {
        return isVerticalBarLayout() || isLargeTablet;
    }

    public int getCellHeight(@ContainerType int containerType) {
        switch (containerType) {
            case CellLayout.WORKSPACE:
                return cellHeightPx;
            case CellLayout.FOLDER:
                return folderCellHeightPx;
            case CellLayout.HOTSEAT:
                return hotseatCellHeightPx;
            default:
                // ??
                return 0;
        }
    }

    private static Context getContext(Context c, int orientation) {
        Configuration context = new Configuration(c.getResources().getConfiguration());
        context.orientation = orientation;
        return c.createConfigurationContext(context);
    }

    /**
     * Callback when a component changes the DeviceProfile associated with it, as a result of
     * configuration change
     */
    public interface OnDeviceProfileChangeListener {

        /**
         * Called when the device profile is reassigned. Note that for layout and measurements, it
         * is sufficient to listen for inset changes. Use this callback when you need to perform
         * a one time operation.
         */
        void onDeviceProfileChanged(DeviceProfile dp);
    }
}
