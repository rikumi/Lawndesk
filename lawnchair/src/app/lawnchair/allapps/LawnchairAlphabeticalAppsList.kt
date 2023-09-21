package app.lawnchair.allapps

import android.content.Context
import androidx.lifecycle.lifecycleScope
import app.lawnchair.launcher
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.allapps.AllAppsStore
import com.android.launcher3.allapps.AlphabeticalAppsList
import com.android.launcher3.allapps.WorkProfileManager
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.views.ActivityContext
import com.patrykmichalik.opto.core.onEach
import java.util.function.Predicate

class LawnchairAlphabeticalAppsList<T>(context: T,
                                       appsStore: AllAppsStore?,
                                       workProfileManager: WorkProfileManager?
): AlphabeticalAppsList<T>(context, appsStore, workProfileManager)
        where T : Context, T : ActivityContext {

    private var hiddenApps: Set<String> = setOf()

    private var itemFilter: Predicate<ItemInfo>? = null;

    init {
        super.updateItemFilter { info ->
            require(info is AppInfo) { "`info` must be an instance of `AppInfo`." }
            when {
                itemFilter?.test(info) == false -> false
                hiddenApps.contains(info.toComponentKey().toString()) -> false
                else -> true
            }
        }
        val prefs = PreferenceManager2.getInstance(context)
        prefs.hiddenApps.onEach(launchIn = context.launcher.lifecycleScope) {
            hiddenApps = it
            onAppsUpdated()
        }
    }

    override fun updateItemFilter(itemFilter: Predicate<ItemInfo>?) {
        this.mItemFilter = itemFilter
        onAppsUpdated()
    }
}
