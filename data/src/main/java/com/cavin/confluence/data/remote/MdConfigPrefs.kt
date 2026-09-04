package com.cavin.confluence.data.remote

import android.content.Context

/** Persist MD base URL override (debug / local docker). */
class MdConfigPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun baseUrl(): String =
        prefs.getString(KEY_BASE, null)?.takeIf { it.isNotBlank() } ?: MdEndpoints.DEFAULT_BASE_URL

    fun setBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE, url.trim().trimEnd('/')).apply()
    }

    companion object {
        private const val PREFS = "confluence_md"
        private const val KEY_BASE = "base_url"
    }
}
