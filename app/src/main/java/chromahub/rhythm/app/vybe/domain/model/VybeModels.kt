/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.domain.model

/**
 * Provider-independent Track model consumed by VYBE's UI, repositories, and Media3 playback engine.
 * Remote API adapters map their provider-specific payloads into this model.
 */
data class VybeTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String,
    val albumId: String = "",
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
    val streamUrl: String? = null,
    val lyrics: String? = null,
    val syncedLyrics: String? = null,
    val providerId: String,
    val genre: String? = null,
    val year: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val albumArtist: String? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val codec: String? = null,
    val isExplicit: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val extraMetadata: Map<String, String> = emptyMap()
) {
    val hasPlayableStream: Boolean
        get() = !streamUrl.isNullOrBlank() &&
            (streamUrl.startsWith("http://", ignoreCase = true) ||
                streamUrl.startsWith("https://", ignoreCase = true) ||
                streamUrl.startsWith("vybe://", ignoreCase = true))
}

data class VybeAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val artworkUrl: String? = null,
    val year: Int = 0,
    val tracks: List<VybeTrack> = emptyList(),
    val trackCount: Int = tracks.size,
    val providerId: String,
    val dateModified: Long = System.currentTimeMillis()
)

data class VybeArtist(
    val id: String,
    val name: String,
    val artworkUrl: String? = null,
    val bio: String? = null,
    val monthlyListeners: Long? = null,
    val albums: List<VybeAlbum> = emptyList(),
    val topTracks: List<VybeTrack> = emptyList(),
    val providerId: String
)

data class VybePlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val artworkUrl: String? = null,
    val tracks: List<VybeTrack> = emptyList(),
    val creator: String? = null,
    val isUserCreated: Boolean = false,
    val providerId: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis()
)

data class VybeLyrics(
    val trackId: String,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val source: String = "VYBE"
)

enum class HomeSectionType {
    FEATURED_ALBUMS,
    QUICK_PICKS,
    TRENDING_TRACKS,
    NEW_RELEASES,
    TOP_ARTISTS,
    CURATED_PLAYLISTS
}

data class VybeHomeSection(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val type: HomeSectionType,
    val tracks: List<VybeTrack> = emptyList(),
    val albums: List<VybeAlbum> = emptyList(),
    val artists: List<VybeArtist> = emptyList(),
    val playlists: List<VybePlaylist> = emptyList()
)

data class VybeHomeData(
    val providerId: String,
    val featuredAlbums: List<VybeAlbum> = emptyList(),
    val quickPicks: List<VybeTrack> = emptyList(),
    val trendingTracks: List<VybeTrack> = emptyList(),
    val topArtists: List<VybeArtist> = emptyList(),
    val curatedPlaylists: List<VybePlaylist> = emptyList(),
    val sections: List<VybeHomeSection> = emptyList()
)

data class VybeSearchResult(
    val query: String,
    val tracks: List<VybeTrack> = emptyList(),
    val albums: List<VybeAlbum> = emptyList(),
    val artists: List<VybeArtist> = emptyList(),
    val playlists: List<VybePlaylist> = emptyList()
) {
    val isEmpty: Boolean
        get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

sealed class VybeResult<out T> {
    data class Success<T>(val data: T) : VybeResult<T>()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val isNetworkError: Boolean = false
    ) : VybeResult<Nothing>()
}
