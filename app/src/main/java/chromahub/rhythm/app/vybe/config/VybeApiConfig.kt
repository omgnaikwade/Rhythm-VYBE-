/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import chromahub.rhythm.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized API & MusicProvider configuration for VYBE.
 *
 * Configuration priority:
 * 1. Runtime user override stored in local SharedPreferences (for switching providers in Settings).
 * 2. Build-time configuration injected via `local.properties` / Gradle properties / BuildConfig:
 *    - `MUSIC_PROVIDER=dummy` (or `custom_vybe`, etc.)
 *    - `VYBE_API_BASE_URL=https://api.example.com/`
 *
 * Never store private API secrets or credentials in client source code. Route authenticated
 * upstream music API calls through your own backend service configured via [customApiBaseUrl].
 */
class VybeApiConfig(context: Context) {

    companion object {
        const val PROVIDER_DUMMY = "dummy"
        const val PROVIDER_CUSTOM_VYBE = "custom_vybe"

        private const val PREFS_NAME = "vybe_api_config"
        private const val KEY_ACTIVE_PROVIDER = "active_provider_id"
        private const val KEY_CUSTOM_BASE_URL = "custom_api_base_url"

        @Volatile
        private var instance: VybeApiConfig? = null

        fun getInstance(context: Context): VybeApiConfig {
            return instance ?: synchronized(this) {
                instance ?: VybeApiConfig(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val defaultProviderId: String
        get() = BuildConfig.MUSIC_PROVIDER.ifBlank { PROVIDER_DUMMY }

    val defaultCustomBaseUrl: String
        get() = BuildConfig.VYBE_API_BASE_URL.trim()

    private val _activeProviderId = MutableStateFlow(
        prefs.getString(KEY_ACTIVE_PROVIDER, null)?.takeIf { it.isNotBlank() } ?: defaultProviderId
    )
    val activeProviderId: StateFlow<String> = _activeProviderId.asStateFlow()

    private val _customApiBaseUrl = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_BASE_URL, null)?.takeIf { it.isNotBlank() } ?: defaultCustomBaseUrl
    )
    val customApiBaseUrl: StateFlow<String> = _customApiBaseUrl.asStateFlow()

    fun setActiveProviderId(providerId: String) {
        val sanitized = providerId.trim().lowercase().ifBlank { PROVIDER_DUMMY }
        prefs.edit { putString(KEY_ACTIVE_PROVIDER, sanitized) }
        _activeProviderId.value = sanitized
    }

    fun setCustomApiBaseUrl(baseUrl: String) {
        val normalized = normalizeBaseUrl(baseUrl)
        prefs.edit { putString(KEY_CUSTOM_BASE_URL, normalized) }
        _customApiBaseUrl.value = normalized
    }

    fun resetToBuildDefaults() {
        prefs.edit {
            remove(KEY_ACTIVE_PROVIDER)
            remove(KEY_CUSTOM_BASE_URL)
        }
        _activeProviderId.value = defaultProviderId
        _customApiBaseUrl.value = defaultCustomBaseUrl
    }

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
