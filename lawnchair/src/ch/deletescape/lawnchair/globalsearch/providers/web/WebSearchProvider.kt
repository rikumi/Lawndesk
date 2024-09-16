/*
 *     Copyright (C) 2019 Lawnchair Team.
 *
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.deletescape.lawnchair.globalsearch.providers.web

import android.content.Context
import android.content.Intent
import ch.deletescape.lawnchair.globalsearch.SearchProvider
import ch.deletescape.lawnchair.toArrayList
import ch.deletescape.lawnchair.util.extensions.e
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherState
import com.android.launcher3.Utilities
import org.json.JSONArray
import java.lang.Exception

abstract class WebSearchProvider(context: Context) : SearchProvider(context) {
    override val supportsVoiceSearch = false
    override val supportsAssistant = false
    override val supportsFeed = false
    /**
     * Web URL to the search results page. %s will be replaced with the search query.
     */
    protected abstract val searchUrl: String
    /**
     * Suggestions API URL. %s will be replaced with the search query.
     */
    protected abstract val suggestionsUrl: String?

    override fun startSearch(callback: (intent: Intent) -> Unit){
    }

    open fun getSuggestions(query: String): List<String> {
        return emptyList()
    }

    open fun openResults(query: String) {
        Utilities.openURLinBrowser(context, getResultUrl(query))
    }

    protected open fun getResultUrl(query: String) = searchUrl.format(query)

    companion object {
        const val MAX_SUGGESTIONS = 5
    }
}
