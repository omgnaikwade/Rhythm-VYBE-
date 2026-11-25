/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.data.provider

import chromahub.rhythm.app.vybe.config.VybeApiConfig
import chromahub.rhythm.app.vybe.domain.provider.MusicProvider
import chromahub.rhythm.app.vybe.domain.provider.ProviderCapability

/**
 * Lightweight descriptor used by Settings UI to list available providers cleanly.
 */
data class ProviderInfo(
    val providerId: String,
    val displayName: String,
    val description: String,
    val capabilities: Set<ProviderCapability>,
    val isConfigured: Boolean
)

/**
 * Registry of all [MusicProvider] adapters available in VYBE.
 *
 * Adding a new music API in the future (e.g. JioSaavnProvider, YouTubeMusicProvider)
 * only requires implementing [MusicProvider] and calling [registerProvider] or adding it
 * to the default provider list here.
 */
class ProviderRegistry(
    initialProviders: List<MusicProvider>,
    private val defaultProviderId: String = VybeApiConfig.PROVIDER_DUMMY
) {
    private val providers = LinkedHashMap<String, MusicProvider>()

    init {
        initialProviders.forEach { registerProvider(it) }
    }

    fun registerProvider(provider: MusicProvider) {
        providers[provider.providerId.lowercase()] = provider
    }

    fun getProviderOrNull(providerId: String): MusicProvider? {
        return providers[providerId.trim().lowercase()]
    }

    /**
     * Resolves the requested provider by [providerId]. If unknown, safely falls back to the
     * default provider ([DummyMusicProvider]) so the app never crashes due to an invalid config.
     */
    fun resolveProvider(providerId: String): MusicProvider {
        val key = providerId.trim().lowercase()
        return providers[key]
            ?: providers[defaultProviderId.lowercase()]
            ?: providers.values.first()
    }

    fun listProviders(): List<ProviderInfo> {
        return providers.values.map { provider ->
            ProviderInfo(
                providerId = provider.providerId,
                displayName = provider.displayName,
                description = provider.description,
                capabilities = provider.capabilities,
                isConfigured = provider.isConfigured()
            )
        }
    }
}
