/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.data.repository

import chromahub.rhythm.app.vybe.data.provider.ProviderInfo
import chromahub.rhythm.app.vybe.data.provider.ProviderRegistry
import chromahub.rhythm.app.vybe.domain.model.VybeAlbum
import chromahub.rhythm.app.vybe.domain.model.VybeArtist
import chromahub.rhythm.app.vybe.domain.model.VybeHomeData
import chromahub.rhythm.app.vybe.domain.model.VybeLyrics
import chromahub.rhythm.app.vybe.domain.model.VybePlaylist
import chromahub.rhythm.app.vybe.domain.model.VybeResult
import chromahub.rhythm.app.vybe.domain.model.VybeSearchResult
import chromahub.rhythm.app.vybe.domain.model.VybeTrack
import chromahub.rhythm.app.vybe.domain.provider.LyricsCapableProvider
import chromahub.rhythm.app.vybe.domain.provider.MusicProvider
import chromahub.rhythm.app.vybe.domain.provider.RecommendationsCapableProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Snapshot of all catalog data hydrated from the active [MusicProvider].
 */
data class VybeCatalogSnapshot(
    val providerId: String,
    val home: VybeHomeData,
    val tracks: List<VybeTrack>,
    val albums: List<VybeAlbum>,
    val artists: List<VybeArtist>,
    val playlists: List<VybePlaylist>
)

/**
 * Backend-agnostic catalog repository consumed by ViewModels and Media3 playback.
 * Handles caching, request deduplication, error wrapping, and runtime provider switching.
 */
interface VybeMusicRepository {
    val activeProviderInfo: StateFlow<ProviderInfo>

    fun availableProviders(): List<ProviderInfo>
    fun switchProvider(providerId: String): ProviderInfo

    suspend fun getCatalogSnapshot(forceRefresh: Boolean = false): VybeResult<VybeCatalogSnapshot>
    suspend fun getHome(forceRefresh: Boolean = false): VybeResult<VybeHomeData>
    suspend fun search(query: String): VybeResult<VybeSearchResult>
    suspend fun getTrack(id: String): VybeResult<VybeTrack?>
    suspend fun getStreamUrl(trackId: String): VybeResult<String?>
    suspend fun getAlbum(id: String): VybeResult<VybeAlbum?>
    suspend fun getArtist(id: String): VybeResult<VybeArtist?>
    suspend fun getPlaylist(id: String): VybeResult<VybePlaylist?>
    suspend fun getLyrics(trackId: String): VybeResult<VybeLyrics?>
    suspend fun getRecommendations(seedTrackId: String, limit: Int = 15): VybeResult<List<VybeTrack>>
    fun clearCache()
}

class VybeMusicRepositoryImpl(
    private val registry: ProviderRegistry,
    private val initialProviderIdProvider: () -> String,
    private val onProviderSwitched: (String) -> Unit = {}
) : VybeMusicRepository {

    private var activeProvider: MusicProvider =
        registry.resolveProvider(initialProviderIdProvider())

    private val _activeProviderInfo = MutableStateFlow(activeProvider.toInfo())
    override val activeProviderInfo: StateFlow<ProviderInfo> = _activeProviderInfo.asStateFlow()

    private val homeMutex = Mutex()
    private val snapshotMutex = Mutex()

    @Volatile
    private var cachedHome: VybeHomeData? = null

    @Volatile
    private var cachedSnapshot: VybeCatalogSnapshot? = null

    private val trackCache = ConcurrentHashMap<String, VybeTrack>()
    private val albumCache = ConcurrentHashMap<String, VybeAlbum>()
    private val artistCache = ConcurrentHashMap<String, VybeArtist>()
    private val playlistCache = ConcurrentHashMap<String, VybePlaylist>()
    private val lyricsCache = ConcurrentHashMap<String, VybeLyrics>()
    private val searchCache = ConcurrentHashMap<String, VybeSearchResult>()

    private fun MusicProvider.toInfo(): ProviderInfo = ProviderInfo(
        providerId = providerId,
        displayName = displayName,
        description = description,
        capabilities = capabilities,
        isConfigured = isConfigured()
    )

    override fun availableProviders(): List<ProviderInfo> = registry.listProviders()

    override fun switchProvider(providerId: String): ProviderInfo {
        val resolved = registry.resolveProvider(providerId)
        activeProvider = resolved
        onProviderSwitched(resolved.providerId)
        clearCache()
        val info = resolved.toInfo()
        _activeProviderInfo.value = info
        return info
    }

    override fun clearCache() {
        cachedHome = null
        cachedSnapshot = null
        trackCache.clear()
        albumCache.clear()
        artistCache.clear()
        playlistCache.clear()
        lyricsCache.clear()
        searchCache.clear()
    }

    override suspend fun getCatalogSnapshot(forceRefresh: Boolean): VybeResult<VybeCatalogSnapshot> {
        if (!forceRefresh) {
            cachedSnapshot?.let { return VybeResult.Success(it) }
        }
        return snapshotMutex.withLock {
            if (!forceRefresh) {
                cachedSnapshot?.let { return@withLock VybeResult.Success(it) }
            }
            safeCall("load catalog") {
                val provider = activeProvider
                val home = provider.getHome()
                val tracks = provider.getCatalogTracks()
                val albums = provider.getCatalogAlbums()
                val artists = provider.getCatalogArtists()
                val playlists = provider.getFeaturedPlaylists()

                cachedHome = home
                tracks.forEach { trackCache[it.id] = it }
                albums.forEach { album ->
                    albumCache[album.id] = album
                    album.tracks.forEach { trackCache[it.id] = it }
                }
                artists.forEach { artist ->
                    artistCache[artist.id] = artist
                    artist.topTracks.forEach { trackCache[it.id] = it }
                }
                playlists.forEach { playlist ->
                    playlistCache[playlist.id] = playlist
                    playlist.tracks.forEach { trackCache[it.id] = it }
                }

                VybeCatalogSnapshot(
                    providerId = provider.providerId,
                    home = home,
                    tracks = tracks,
                    albums = albums,
                    artists = artists,
                    playlists = playlists
                ).also { cachedSnapshot = it }
            }
        }
    }

    override suspend fun getHome(forceRefresh: Boolean): VybeResult<VybeHomeData> {
        if (!forceRefresh) {
            cachedHome?.let { return VybeResult.Success(it) }
        }
        return homeMutex.withLock {
            if (!forceRefresh) {
                cachedHome?.let { return@withLock VybeResult.Success(it) }
            }
            safeCall("load home feed") {
                activeProvider.getHome().also { home ->
                    cachedHome = home
                    home.quickPicks.forEach { trackCache[it.id] = it }
                    home.trendingTracks.forEach { trackCache[it.id] = it }
                    home.featuredAlbums.forEach { albumCache[it.id] = it }
                    home.topArtists.forEach { artistCache[it.id] = it }
                    home.curatedPlaylists.forEach { playlistCache[it.id] = it }
                }
            }
        }
    }

    override suspend fun search(query: String): VybeResult<VybeSearchResult> {
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            return VybeResult.Success(VybeSearchResult(query = query))
        }
        val cacheKey = "${activeProvider.providerId}:${normalized.lowercase()}"
        searchCache[cacheKey]?.let { return VybeResult.Success(it) }

        return safeCall("search catalog") {
            activeProvider.search(normalized).also { result ->
                searchCache[cacheKey] = result
                result.tracks.forEach { trackCache[it.id] = it }
                result.albums.forEach { albumCache[it.id] = it }
                result.artists.forEach { artistCache[it.id] = it }
                result.playlists.forEach { playlistCache[it.id] = it }
            }
        }
    }

    override suspend fun getTrack(id: String): VybeResult<VybeTrack?> {
        trackCache[id]?.let { return VybeResult.Success(it) }
        return safeCall("fetch track") {
            activeProvider.getTrack(id)?.also { trackCache[it.id] = it }
        }
    }

    override suspend fun getStreamUrl(trackId: String): VybeResult<String?> {
        return safeCall("resolve stream URL") {
            val fresh = activeProvider.getStreamUrl(trackId)?.takeIf { it.isNotBlank() }
            fresh ?: trackCache[trackId]?.streamUrl?.takeIf { it.isNotBlank() }
        }
    }

    override suspend fun getAlbum(id: String): VybeResult<VybeAlbum?> {
        albumCache[id]?.let { return VybeResult.Success(it) }
        return safeCall("fetch album") {
            activeProvider.getAlbum(id)?.also { album ->
                albumCache[album.id] = album
                album.tracks.forEach { trackCache[it.id] = it }
            }
        }
    }

    override suspend fun getArtist(id: String): VybeResult<VybeArtist?> {
        artistCache[id]?.let { return VybeResult.Success(it) }
        return safeCall("fetch artist") {
            activeProvider.getArtist(id)?.also { artist ->
                artistCache[artist.id] = artist
                artist.topTracks.forEach { trackCache[it.id] = it }
            }
        }
    }

    override suspend fun getPlaylist(id: String): VybeResult<VybePlaylist?> {
        playlistCache[id]?.let { return VybeResult.Success(it) }
        return safeCall("fetch playlist") {
            activeProvider.getPlaylist(id)?.also { playlist ->
                playlistCache[playlist.id] = playlist
                playlist.tracks.forEach { trackCache[it.id] = it }
            }
        }
    }

    override suspend fun getLyrics(trackId: String): VybeResult<VybeLyrics?> {
        lyricsCache[trackId]?.let { return VybeResult.Success(it) }
        val provider = activeProvider
        if (provider !is LyricsCapableProvider) {
            return VybeResult.Success(null)
        }
        return safeCall("fetch lyrics") {
            provider.getLyrics(trackId)?.also { lyricsCache[trackId] = it }
        }
    }

    override suspend fun getRecommendations(
        seedTrackId: String,
        limit: Int
    ): VybeResult<List<VybeTrack>> {
        val provider = activeProvider
        if (provider !is RecommendationsCapableProvider) {
            return VybeResult.Success(emptyList())
        }
        return safeCall("fetch recommendations") {
            provider.getRecommendations(seedTrackId, limit).also { recs ->
                recs.forEach { trackCache[it.id] = it }
            }
        }
    }

    private suspend fun <T> safeCall(operation: String, block: suspend () -> T): VybeResult<T> {
        return try {
            val result = withContext(Dispatchers.IO) { block() }
            VybeResult.Success(result)
        } catch (ce: CancellationException) {
            throw ce
        } catch (io: IOException) {
            VybeResult.Error(
                message = io.message?.takeIf { it.isNotBlank() }
                    ?: "Network error while trying to $operation. Check your connection.",
                cause = io,
                isNetworkError = true
            )
        } catch (t: Throwable) {
            VybeResult.Error(
                message = t.message?.takeIf { it.isNotBlank() }
                    ?: "Unable to $operation with ${activeProvider.displayName}.",
                cause = t,
                isNetworkError = false
            )
        }
    }
}
