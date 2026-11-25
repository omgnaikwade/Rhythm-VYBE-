/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.di

import android.content.Context
import chromahub.rhythm.app.vybe.config.VybeApiConfig
import chromahub.rhythm.app.vybe.data.provider.CustomVybeProvider
import chromahub.rhythm.app.vybe.data.provider.DummyMusicProvider
import chromahub.rhythm.app.vybe.data.provider.ProviderRegistry
import chromahub.rhythm.app.vybe.data.repository.LocalUserLibraryRepository
import chromahub.rhythm.app.vybe.data.repository.UserLibraryRepository
import chromahub.rhythm.app.vybe.data.repository.VybeMusicRepository
import chromahub.rhythm.app.vybe.data.repository.VybeMusicRepositoryImpl
import chromahub.rhythm.app.vybe.domain.provider.MusicProvider

/**
 * Lightweight dependency wiring for VYBE's API layer, provider registry, catalog repository,
 * and user library repository.
 *
 * Register future real music API adapters (e.g. JioSaavnProvider, YouTubeMusicProvider)
 * in [createDefaultRegistry] or dynamically via [registerCustomProvider].
 */
object VybeServiceLocator {

    @Volatile
    private var apiConfig: VybeApiConfig? = null

    @Volatile
    private var providerRegistry: ProviderRegistry? = null

    @Volatile
    private var musicRepository: VybeMusicRepository? = null

    @Volatile
    private var userLibraryRepository: UserLibraryRepository? = null

    fun provideApiConfig(context: Context): VybeApiConfig {
        return apiConfig ?: synchronized(this) {
            apiConfig ?: VybeApiConfig.getInstance(context.applicationContext).also {
                apiConfig = it
            }
        }
    }

    fun provideProviderRegistry(context: Context): ProviderRegistry {
        return providerRegistry ?: synchronized(this) {
            providerRegistry ?: createDefaultRegistry(provideApiConfig(context)).also {
                providerRegistry = it
            }
        }
    }

    fun provideMusicRepository(context: Context): VybeMusicRepository {
        return musicRepository ?: synchronized(this) {
            musicRepository ?: run {
                val config = provideApiConfig(context)
                val registry = provideProviderRegistry(context)
                VybeMusicRepositoryImpl(
                    registry = registry,
                    initialProviderIdProvider = { config.activeProviderId.value },
                    onProviderSwitched = { newId -> config.setActiveProviderId(newId) }
                ).also { musicRepository = it }
            }
        }
    }

    fun provideUserLibraryRepository(context: Context): UserLibraryRepository {
        return userLibraryRepository ?: synchronized(this) {
            userLibraryRepository ?: LocalUserLibraryRepository(context.applicationContext).also {
                userLibraryRepository = it
            }
        }
    }

    /**
     * Hook for registering additional [MusicProvider] adapters at runtime or startup.
     */
    fun registerCustomProvider(context: Context, provider: MusicProvider) {
        provideProviderRegistry(context).registerProvider(provider)
    }

    private fun createDefaultRegistry(config: VybeApiConfig): ProviderRegistry {
        val dummyProvider = DummyMusicProvider()
        val customVybeProvider = CustomVybeProvider(
            baseUrlProvider = { config.customApiBaseUrl.value }
        )
        return ProviderRegistry(
            initialProviders = listOf(
                dummyProvider,
                customVybeProvider
            ),
            defaultProviderId = VybeApiConfig.PROVIDER_DUMMY
        )
    }
}
