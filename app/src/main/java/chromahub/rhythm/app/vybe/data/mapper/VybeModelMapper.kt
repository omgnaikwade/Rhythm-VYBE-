/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.data.mapper

import android.net.Uri
import androidx.core.net.toUri
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.vybe.data.remote.CustomVybeAlbumDto
import chromahub.rhythm.app.vybe.data.remote.CustomVybeArtistDto
import chromahub.rhythm.app.vybe.data.remote.CustomVybeHomeResponseDto
import chromahub.rhythm.app.vybe.data.remote.CustomVybeLyricsDto
import chromahub.rhythm.app.vybe.data.remote.CustomVybePlaylistDto
import chromahub.rhythm.app.vybe.data.remote.CustomVybeSearchResponseDto
import chromahub.rhythm.app.vybe.data.remote.CustomVybeTrackDto
import chromahub.rhythm.app.vybe.domain.model.HomeSectionType
import chromahub.rhythm.app.vybe.domain.model.VybeAlbum
import chromahub.rhythm.app.vybe.domain.model.VybeArtist
import chromahub.rhythm.app.vybe.domain.model.VybeHomeData
import chromahub.rhythm.app.vybe.domain.model.VybeHomeSection
import chromahub.rhythm.app.vybe.domain.model.VybeLyrics
import chromahub.rhythm.app.vybe.domain.model.VybePlaylist
import chromahub.rhythm.app.vybe.domain.model.VybeSearchResult
import chromahub.rhythm.app.vybe.domain.model.VybeTrack

/**
 * Maps provider-specific DTOs into common VYBE domain models, and maps VYBE domain models
 * to the existing UI presentation models ([Song], [Album], [Artist], [Playlist]) so all
 * retained Jetpack Compose screens remain completely independent of API response formats.
 */
object VybeModelMapper {

    // ---- Custom VYBE API DTO -> Common VYBE Domain Models ----

    fun CustomVybeTrackDto.toVybeTrack(providerId: String): VybeTrack? {
        val safeId = id?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val safeTitle = title?.trim().takeUnless { it.isNullOrBlank() } ?: "Untitled Track"
        return VybeTrack(
            id = safeId,
            title = safeTitle,
            artist = artist?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown Artist",
            artistId = artistId?.trim().orEmpty(),
            album = album?.trim().takeUnless { it.isNullOrBlank() } ?: "Single",
            albumId = albumId?.trim().orEmpty(),
            artworkUrl = artworkUrl?.trim()?.takeIf { it.isNotBlank() },
            durationMs = (durationMs ?: 0L).coerceAtLeast(0L),
            streamUrl = streamUrl?.trim()?.takeIf { it.isNotBlank() },
            lyrics = lyrics?.takeIf { it.isNotBlank() },
            syncedLyrics = syncedLyrics?.takeIf { it.isNotBlank() },
            providerId = providerId,
            genre = genre?.trim()?.takeIf { it.isNotBlank() },
            year = (year ?: 0).coerceAtLeast(0),
            trackNumber = (trackNumber ?: 0).coerceAtLeast(0),
            discNumber = (discNumber ?: 1).coerceAtLeast(1),
            bitrate = bitrate,
            sampleRate = sampleRate,
            channels = channels,
            codec = codec?.trim()?.takeIf { it.isNotBlank() },
            isExplicit = isExplicit ?: false
        )
    }

    fun CustomVybeAlbumDto.toVybeAlbum(providerId: String): VybeAlbum? {
        val safeId = id?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val mappedTracks = tracks.orEmpty().mapNotNull { it.toVybeTrack(providerId) }
        return VybeAlbum(
            id = safeId,
            title = title?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown Album",
            artist = artist?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown Artist",
            artistId = artistId?.trim().orEmpty(),
            artworkUrl = artworkUrl?.trim()?.takeIf { it.isNotBlank() }
                ?: mappedTracks.firstOrNull()?.artworkUrl,
            year = (year ?: 0).coerceAtLeast(0),
            tracks = mappedTracks,
            trackCount = mappedTracks.size,
            providerId = providerId
        )
    }

    fun CustomVybeArtistDto.toVybeArtist(providerId: String): VybeArtist? {
        val safeId = id?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val mappedAlbums = albums.orEmpty().mapNotNull { it.toVybeAlbum(providerId) }
        val mappedTracks = topTracks.orEmpty().mapNotNull { it.toVybeTrack(providerId) }
        return VybeArtist(
            id = safeId,
            name = name?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown Artist",
            artworkUrl = artworkUrl?.trim()?.takeIf { it.isNotBlank() }
                ?: mappedAlbums.firstOrNull()?.artworkUrl,
            bio = bio?.trim()?.takeIf { it.isNotBlank() },
            monthlyListeners = monthlyListeners,
            albums = mappedAlbums,
            topTracks = mappedTracks,
            providerId = providerId
        )
    }

    fun CustomVybePlaylistDto.toVybePlaylist(providerId: String): VybePlaylist? {
        val safeId = id?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val mappedTracks = tracks.orEmpty().mapNotNull { it.toVybeTrack(providerId) }
        return VybePlaylist(
            id = safeId,
            name = name?.trim().takeUnless { it.isNullOrBlank() } ?: "Playlist",
            description = description?.trim()?.takeIf { it.isNotBlank() },
            artworkUrl = artworkUrl?.trim()?.takeIf { it.isNotBlank() }
                ?: mappedTracks.firstOrNull()?.artworkUrl,
            tracks = mappedTracks,
            creator = creator?.trim()?.takeIf { it.isNotBlank() },
            isUserCreated = false,
            providerId = providerId
        )
    }

    fun CustomVybeHomeResponseDto.toVybeHomeData(providerId: String): VybeHomeData {
        val albums = featuredAlbums.orEmpty().mapNotNull { it.toVybeAlbum(providerId) }
        val picks = quickPicks.orEmpty().mapNotNull { it.toVybeTrack(providerId) }
        val trending = trendingTracks.orEmpty().mapNotNull { it.toVybeTrack(providerId) }
        val artists = topArtists.orEmpty().mapNotNull { it.toVybeArtist(providerId) }
        val playlists = curatedPlaylists.orEmpty().mapNotNull { it.toVybePlaylist(providerId) }

        val sections = buildList {
            if (albums.isNotEmpty()) {
                add(VybeHomeSection("featured_albums", "Featured Albums", type = HomeSectionType.FEATURED_ALBUMS, albums = albums))
            }
            if (picks.isNotEmpty()) {
                add(VybeHomeSection("quick_picks", "Quick Picks", type = HomeSectionType.QUICK_PICKS, tracks = picks))
            }
            if (trending.isNotEmpty()) {
                add(VybeHomeSection("trending", "Trending Now", type = HomeSectionType.TRENDING_TRACKS, tracks = trending))
            }
            if (artists.isNotEmpty()) {
                add(VybeHomeSection("top_artists", "Top Artists", type = HomeSectionType.TOP_ARTISTS, artists = artists))
            }
            if (playlists.isNotEmpty()) {
                add(VybeHomeSection("curated_playlists", "Curated Playlists", type = HomeSectionType.CURATED_PLAYLISTS, playlists = playlists))
            }
        }

        return VybeHomeData(
            providerId = providerId,
            featuredAlbums = albums,
            quickPicks = picks,
            trendingTracks = trending,
            topArtists = artists,
            curatedPlaylists = playlists,
            sections = sections
        )
    }

    fun CustomVybeSearchResponseDto.toVybeSearchResult(
        query: String,
        providerId: String
    ): VybeSearchResult {
        return VybeSearchResult(
            query = query,
            tracks = tracks.orEmpty().mapNotNull { it.toVybeTrack(providerId) },
            albums = albums.orEmpty().mapNotNull { it.toVybeAlbum(providerId) },
            artists = artists.orEmpty().mapNotNull { it.toVybeArtist(providerId) },
            playlists = playlists.orEmpty().mapNotNull { it.toVybePlaylist(providerId) }
        )
    }

    fun CustomVybeLyricsDto.toVybeLyrics(trackId: String): VybeLyrics? {
        val plain = plainLyrics?.takeIf { it.isNotBlank() }
        val synced = syncedLyrics?.takeIf { it.isNotBlank() }
        if (plain == null && synced == null) return null
        return VybeLyrics(
            trackId = trackId,
            plainLyrics = plain,
            syncedLyrics = synced,
            source = source?.takeIf { it.isNotBlank() } ?: "VYBE API"
        )
    }

    // ---- Common VYBE Domain Models -> Retained Compose UI & Player Models ----

    fun VybeTrack.toSong(): Song {
        // Use direct stream URL when available; otherwise use vybe:// scheme so ResolvingDataSource
        // can lazily resolve the URL via MusicProvider.getStreamUrl(id) when playback starts.
        val playbackUri: Uri = when {
            !streamUrl.isNullOrBlank() -> streamUrl.toUri()
            else -> "vybe://track/$id".toUri()
        }
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            albumId = albumId,
            duration = durationMs,
            uri = playbackUri,
            artworkUri = artworkUrl?.takeIf { it.isNotBlank() }?.toUri(),
            trackNumber = trackNumber,
            year = year,
            genre = genre,
            dateAdded = dateAdded,
            dateModified = dateAdded,
            albumArtist = albumArtist ?: artist,
            bitrate = bitrate,
            sampleRate = sampleRate,
            channels = channels,
            codec = codec,
            discNumber = discNumber,
            path = null
        )
    }

    fun Song.toVybeTrack(providerId: String = "vybe"): VybeTrack {
        val rawUri = uri.toString()
        val resolvedStreamUrl = rawUri.takeIf {
            it.isNotBlank() &&
                (it.startsWith("http://", ignoreCase = true) ||
                    it.startsWith("https://", ignoreCase = true) ||
                    it.startsWith("vybe://", ignoreCase = true))
        }
        return VybeTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            albumId = albumId,
            artworkUrl = artworkUri?.toString(),
            durationMs = duration,
            streamUrl = resolvedStreamUrl,
            providerId = providerId,
            genre = genre,
            year = year,
            trackNumber = trackNumber,
            discNumber = discNumber,
            albumArtist = albumArtist,
            bitrate = bitrate,
            sampleRate = sampleRate,
            channels = channels,
            codec = codec,
            dateAdded = dateAdded
        )
    }

    fun VybeAlbum.toAlbum(): Album {
        val mappedSongs = tracks.map { it.toSong() }
        return Album(
            id = id,
            title = title,
            artist = artist,
            artworkUri = artworkUrl?.takeIf { it.isNotBlank() }?.toUri(),
            year = year,
            songs = mappedSongs,
            numberOfSongs = trackCount.coerceAtLeast(mappedSongs.size),
            dateModified = dateModified
        )
    }

    fun VybeArtist.toArtist(): Artist {
        val mappedAlbums = albums.map { it.toAlbum() }
        val mappedSongs = topTracks.map { it.toSong() }
        return Artist(
            id = id,
            name = name,
            artworkUri = artworkUrl?.takeIf { it.isNotBlank() }?.toUri(),
            albums = mappedAlbums,
            songs = mappedSongs,
            numberOfAlbums = mappedAlbums.size,
            numberOfTracks = mappedSongs.size
        )
    }

    fun VybePlaylist.toPlaylist(): Playlist {
        return Playlist(
            id = id,
            name = name,
            songs = tracks.map { it.toSong() },
            dateCreated = dateCreated,
            dateModified = dateModified,
            artworkUri = artworkUrl?.takeIf { it.isNotBlank() }?.toUri()
        )
    }
}
