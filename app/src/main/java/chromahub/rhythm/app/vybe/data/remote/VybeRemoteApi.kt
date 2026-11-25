/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Provider-specific Retrofit API contract for a custom VYBE music backend.
 *
 * Replace or add new Retrofit interfaces in this package for other APIs
 * (e.g. JioSaavnApi, YouTubeMusicApi) and wire them through their own
 * [chromahub.rhythm.app.vybe.domain.provider.MusicProvider] adapter.
 */
interface VybeRemoteApi {

    @GET("v1/home")
    suspend fun getHome(): Response<CustomVybeHomeResponseDto>

    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25
    ): Response<CustomVybeSearchResponseDto>

    @GET("v1/tracks/{id}")
    suspend fun getTrack(@Path("id") id: String): Response<CustomVybeTrackDto>

    @GET("v1/tracks/{id}/stream")
    suspend fun getStreamUrl(@Path("id") id: String): Response<CustomVybeStreamResponseDto>

    @GET("v1/tracks/{id}/lyrics")
    suspend fun getLyrics(@Path("id") id: String): Response<CustomVybeLyricsDto>

    @GET("v1/tracks/{id}/recommendations")
    suspend fun getRecommendations(
        @Path("id") id: String,
        @Query("limit") limit: Int = 15
    ): Response<List<CustomVybeTrackDto>>

    @GET("v1/albums/{id}")
    suspend fun getAlbum(@Path("id") id: String): Response<CustomVybeAlbumDto>

    @GET("v1/artists/{id}")
    suspend fun getArtist(@Path("id") id: String): Response<CustomVybeArtistDto>

    @GET("v1/playlists/{id}")
    suspend fun getPlaylist(@Path("id") id: String): Response<CustomVybePlaylistDto>
}

data class CustomVybeTrackDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("artist_id") val artistId: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("album_id") val albumId: String? = null,
    @SerializedName("artwork_url") val artworkUrl: String? = null,
    @SerializedName("duration_ms") val durationMs: Long? = null,
    @SerializedName("stream_url") val streamUrl: String? = null,
    @SerializedName("lyrics") val lyrics: String? = null,
    @SerializedName("synced_lyrics") val syncedLyrics: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("track_number") val trackNumber: Int? = null,
    @SerializedName("disc_number") val discNumber: Int? = null,
    @SerializedName("bitrate") val bitrate: Int? = null,
    @SerializedName("sample_rate") val sampleRate: Int? = null,
    @SerializedName("channels") val channels: Int? = null,
    @SerializedName("codec") val codec: String? = null,
    @SerializedName("explicit") val isExplicit: Boolean? = null
)

data class CustomVybeAlbumDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("artist_id") val artistId: String? = null,
    @SerializedName("artwork_url") val artworkUrl: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("tracks") val tracks: List<CustomVybeTrackDto>? = null
)

data class CustomVybeArtistDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("artwork_url") val artworkUrl: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("monthly_listeners") val monthlyListeners: Long? = null,
    @SerializedName("albums") val albums: List<CustomVybeAlbumDto>? = null,
    @SerializedName("top_tracks") val topTracks: List<CustomVybeTrackDto>? = null
)

data class CustomVybePlaylistDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("artwork_url") val artworkUrl: String? = null,
    @SerializedName("creator") val creator: String? = null,
    @SerializedName("tracks") val tracks: List<CustomVybeTrackDto>? = null
)

data class CustomVybeHomeResponseDto(
    @SerializedName("featured_albums") val featuredAlbums: List<CustomVybeAlbumDto>? = null,
    @SerializedName("quick_picks") val quickPicks: List<CustomVybeTrackDto>? = null,
    @SerializedName("trending_tracks") val trendingTracks: List<CustomVybeTrackDto>? = null,
    @SerializedName("top_artists") val topArtists: List<CustomVybeArtistDto>? = null,
    @SerializedName("curated_playlists") val curatedPlaylists: List<CustomVybePlaylistDto>? = null
)

data class CustomVybeSearchResponseDto(
    @SerializedName("tracks") val tracks: List<CustomVybeTrackDto>? = null,
    @SerializedName("albums") val albums: List<CustomVybeAlbumDto>? = null,
    @SerializedName("artists") val artists: List<CustomVybeArtistDto>? = null,
    @SerializedName("playlists") val playlists: List<CustomVybePlaylistDto>? = null
)

data class CustomVybeStreamResponseDto(
    @SerializedName("stream_url") val streamUrl: String? = null
)

data class CustomVybeLyricsDto(
    @SerializedName("plain_lyrics") val plainLyrics: String? = null,
    @SerializedName("synced_lyrics") val syncedLyrics: String? = null,
    @SerializedName("source") val source: String? = null
)
