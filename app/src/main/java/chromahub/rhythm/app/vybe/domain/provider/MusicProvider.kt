/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.domain.provider

import chromahub.rhythm.app.vybe.domain.model.VybeAlbum
import chromahub.rhythm.app.vybe.domain.model.VybeArtist
import chromahub.rhythm.app.vybe.domain.model.VybeHomeData
import chromahub.rhythm.app.vybe.domain.model.VybeLyrics
import chromahub.rhythm.app.vybe.domain.model.VybePlaylist
import chromahub.rhythm.app.vybe.domain.model.VybeSearchResult
import chromahub.rhythm.app.vybe.domain.model.VybeTrack

/**
 * Declares optional capabilities supported by a [MusicProvider].
 * UI and repositories query capabilities rather than assuming every backend supports every feature.
 */
enum class ProviderCapability {
    STREAMING,
    SEARCH,
    LYRICS,
    RECOMMENDATIONS,
    PLAYLISTS,
    DOWNLOADS
}

/**
 * Provider-independent music backend contract.
 *
 * Implement this interface to plug in any music API (e.g. Custom VYBE backend, JioSaavn,
 * YouTube Music, Subsonic, etc.) without modifying ViewModels or UI screens.
 */
interface MusicProvider {
    /** Unique stable identifier, e.g. "dummy", "custom_vybe", "jiosaavn", "ytmusic". */
    val providerId: String

    /** Human-readable provider name displayed in Settings / Provider Switcher. */
    val displayName: String

    /** Short description shown in Settings. */
    val description: String

    /** Set of optional features supported by this provider. */
    val capabilities: Set<ProviderCapability>

    /** Whether this provider has enough configuration (e.g. base URL) to make requests. */
    fun isConfigured(): Boolean = true

    suspend fun getHome(): VybeHomeData

    suspend fun search(query: String): VybeSearchResult

    suspend fun getTrack(id: String): VybeTrack?

    /**
     * Resolves a fresh playable HTTP/HTTPS stream URL for the given [trackId].
     * Providers with expiring signed URLs should fetch a fresh stream URL here.
     */
    suspend fun getStreamUrl(trackId: String): String? = getTrack(trackId)?.streamUrl

    suspend fun getAlbum(id: String): VybeAlbum?

    suspend fun getArtist(id: String): VybeArtist?

    suspend fun getPlaylist(id: String): VybePlaylist?

    /**
     * Optional catalog snapshots used to populate Library tabs immediately.
     * Providers that are search-first can return tracks/albums/artists derived from [getHome].
     */
    suspend fun getCatalogTracks(): List<VybeTrack> {
        val home = getHome()
        return (home.quickPicks + home.trendingTracks + home.featuredAlbums.flatMap { it.tracks })
            .distinctBy { it.id }
    }

    suspend fun getCatalogAlbums(): List<VybeAlbum> = getHome().featuredAlbums

    suspend fun getCatalogArtists(): List<VybeArtist> = getHome().topArtists

    suspend fun getFeaturedPlaylists(): List<VybePlaylist> = getHome().curatedPlaylists
}

/** Optional capability for providers that supply plain or time-synced lyrics. */
interface LyricsCapableProvider {
    suspend fun getLyrics(trackId: String): VybeLyrics?
}

/** Optional capability for providers that return track radio / related recommendations. */
interface RecommendationsCapableProvider {
    suspend fun getRecommendations(seedTrackId: String, limit: Int = 15): List<VybeTrack>
}

/** Optional capability for providers that expose curated/editorial playlists. */
interface PlaylistCapableProvider {
    suspend fun getCuratedPlaylists(): List<VybePlaylist>
}

/** Optional capability for providers that expose downloadable audio stream URLs. */
interface DownloadCapableProvider {
    suspend fun getDownloadUrl(trackId: String): String?
}
