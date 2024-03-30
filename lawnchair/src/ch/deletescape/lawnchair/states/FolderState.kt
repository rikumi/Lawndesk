package ch.deletescape.lawnchair.states

import com.android.launcher3.Launcher
import com.android.launcher3.LauncherAnimUtils.SPRING_LOADED_TRANSITION_MS
import com.android.launcher3.LauncherState
import com.android.launcher3.anim.Interpolators
import com.android.launcher3.states.RotationHelper
import com.android.launcher3.userevent.nano.LauncherLogProto


class FolderState(id: Int) : LauncherState(id, LauncherLogProto.ContainerType.FOLDER,
                                           SPRING_LOADED_TRANSITION_MS, STATE_FLAGS) {

    override fun getWorkspaceScrimAlpha(launcher: Launcher?): Float {
        return 0f
    }

    override fun getWorkspaceBlurAlpha(launcher: Launcher?): Float {
        return 1f
    }

    override fun getVisibleElements(launcher: Launcher): Int {
        return 0
    }

    private val PAGE_ALPHA_PROVIDER: PageAlphaProvider =
            object : PageAlphaProvider(Interpolators.DEACCEL_2) {
                override fun getPageAlpha(pageIndex: Int): Float {
                    return 0f
                }
            }

    override fun getWorkspacePageAlphaProvider(
            launcher: Launcher?): PageAlphaProvider? {
        return PAGE_ALPHA_PROVIDER
    }

    override fun onStateEnabled(launcher: Launcher) {
        super.onStateEnabled(launcher)
        launcher.rotationHelper.setCurrentStateRequest(RotationHelper.REQUEST_LOCK)
    }

    companion object {
        private const val STATE_FLAGS = FLAG_HAS_SYS_UI_SCRIM
    }
}
