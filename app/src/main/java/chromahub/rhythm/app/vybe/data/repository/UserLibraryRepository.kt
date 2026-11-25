/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import chromahub.rhythm.app.vybe.domain.model.VybePlaylist
import chromahub.rhythm.app.vybe.domain.model.VybeTrack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Abstraction for user-owned library data (favorites, user playlists, recently played, cached
 * track metadata), intentionally decoupled from remote [MusicProvider] catalog data.
 *
 * Currently backed by local storage ([LocalUserLibraryRepository]), and designed so it can
 * later be replaced with a remote user database (e.g. Supabase or custom backend) without
 * touching UI screens or ViewModels.
 */
interface UserLibraryRepository {
    val favoriteTrackIds: StateFlow<Set<String>>
    val userPlaylists: StateFlow<List<VybePlaylist>>
    val recentlyPlayedTracks: StateFlow<List<VybeTrack>>

    fun isFavorite(trackId: String): Boolean
    fun toggleFavorite(track: VybeTrack): Boolean
    fun setFavorite(track: VybeTrack, favorite: Boolean)

    fun recordRecentlyPlayed(track: VybeTrack)
    fun clearRecentlyPlayed()

    fun createPlaylist(name: String, description: String? = null): VybePlaylist
    fun renamePlaylist(playlistId: String, newName: String)
    fun deletePlaylist(playlistId: String)
    fun addTrackToPlaylist(playlistId: String, track: VybeTrack)
    fun removeTrackFromPlaylist(playlistId: String, trackId: String)
    fun reorderPlaylistTracks(playlistId: String, orderedTracks: List<VybeTrack>)

    fun getCachedTrack(trackId: String): VybeTrack?
}

class LocalUserLibraryRepository(context: Context) : UserLibraryRepository {

    companion object {
        private const val PREFS_NAME = "vybe_user_library"
        private const val KEY_FAVORITE_IDS = "favorite_track_ids"
        private const val KEY_USER_PLAYLISTS = "user_playlists_json"
        private const val KEY_RECENTLY_PLAYED = "recently_played_json"
        private const val KEY_CACHED_TRACKS = "cached_tracks_json"
        private const val MAX_RECENTLY_PLAYED = 50
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val lock = Any()

    private val cachedTracksMap: MutableMap<String, VybeTrack> = loadCachedTracks()

    private val _favoriteTrackIds = MutableStateFlow(loadFavoriteIds())
    override val favoriteTrackIds: StateFlow<Set<String>> = _favoriteTrackIds.asStateFlow()

    private val _userPlaylists = MutableStateFlow(loadUserPlaylists())
    override val userPlaylists: StateFlow<List<VybePlaylist>> = _userPlaylists.asStateFlow()

    private val _recentlyPlayedTracks = MutableStateFlow(loadRecentlyPlayed())
    override val recentlyPlayedTracks: StateFlow<List<VybeTrack>> =
        _recentlyPlayedTracks.asStateFlow()

    override fun isFavorite(trackId: String): Boolean = _favoriteTrackIds.value.contains(trackId)

    override fun toggleFavorite(track: VybeTrack): Boolean {
        val nowFavorite = !isFavorite(track.id)
        setFavorite(track, nowFavorite)
        return nowFavorite
    }

    override fun setFavorite(track: VybeTrack, favorite: Boolean) {
        synchronized(lock) {
            cacheTrackInternal(track)
            val updated = _favoriteTrackIds.value.toMutableSet()
            if (favorite) updated.add(track.id) else updated.remove(track.id)
            prefs.edit { putStringSet(KEY_FAVORITE_IDS, updated) }
            _favoriteTrackIds.value = updated.toSet()
        }
    }

    override fun recordRecentlyPlayed(track: VybeTrack) {
        synchronized(lock) {
            cacheTrackInternal(track)
            val updated = buildList {
                add(track)
                _recentlyPlayedTracks.value
                    .filterNot { it.id == track.id }
                    .take(MAX_RECENTLY_PLAYED - 1)
                    .forEach { add(it) }
            }
            saveRecentlyPlayed(updated)
            _recentlyPlayedTracks.value = updated
        }
    }

    override fun clearRecentlyPlayed() {
        synchronized(lock) {
            saveRecentlyPlayed(emptyList())
            _recentlyPlayedTracks.value = emptyList()
        }
    }

    override fun createPlaylist(name: String, description: String?): VybePlaylist {
        val now = System.currentTimeMillis()
        val playlist = VybePlaylist(
            id = "vybe_user_pl_${UUID.randomUUID().toString().take(8)}",
            name = name.trim().ifBlank { "Untitled Playlist" },
            description = description?.trim()?.takeIf { it.isNotBlank() },
            tracks = emptyList(),
            creator = "You",
            isUserCreated = true,
            providerId = "user_library",
            dateCreated = now,
            dateModified = now
        )
        synchronized(lock) {
            val updated = _userPlaylists.value + playlist
            saveUserPlaylists(updated)
            _userPlaylists.value = updated
        }
        return playlist
    }

    override fun renamePlaylist(playlistId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        synchronized(lock) {
            val updated = _userPlaylists.value.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(name = trimmed, dateModified = System.currentTimeMillis())
                } else {
                    playlist
                }
            }
            saveUserPlaylists(updated)
            _userPlaylists.value = updated
        }
    }

    override fun deletePlaylist(playlistId: String) {
        synchronized(lock) {
            val updated = _userPlaylists.value.filterNot { it.id == playlistId }
            saveUserPlaylists(updated)
            _userPlaylists.value = updated
        }
    }

    override fun addTrackToPlaylist(playlistId: String, track: VybeTrack) {
        synchronized(lock) {
            cacheTrackInternal(track)
            val updated = _userPlaylists.value.map { playlist ->
                if (playlist.id == playlistId) {
                    if (playlist.tracks.any { it.id == track.id }) {
                        playlist
                    } else {
                        val newTracks = playlist.tracks + track
                        playlist.copy(
                            tracks = newTracks,
                            artworkUrl = playlist.artworkUrl ?: track.artworkUrl,
                            dateModified = System.currentTimeMillis()
                        )
                    }
                } else {
                    playlist
                }
            }
            saveUserPlaylists(updated)
            _userPlaylists.value = updated
        }
    }

    override fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        synchronized(lock) {
            val updated = _userPlaylists.value.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(
                        tracks = playlist.tracks.filterNot { it.id == trackId },
                        dateModified = System.currentTimeMillis()
                    )
                } else {
                    playlist
                }
            }
            saveUserPlaylists(updated)
            _userPlaylists.value = updated
        }
    }

    override fun reorderPlaylistTracks(playlistId: String, orderedTracks: List<VybeTrack>) {
        synchronized(lock) {
            val updated = _userPlaylists.value.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(
                        tracks = orderedTracks,
                        dateModified = System.currentTimeMillis()
                    )
                } else {
                    playlist
                }
            }
            saveUserPlaylists(updated)
            _userPlaylists.value = updated
        }
    }

    override fun getCachedTrack(trackId: String): VybeTrack? {
        synchronized(lock) {
            return cachedTracksMap[trackId]
        }
    }

    private fun cacheTrackInternal(track: VybeTrack) {
        cachedTracksMap[track.id] = track
        runCatching {
            prefs.edit { putString(KEY_CACHED_TRACKS, gson.toJson(cachedTracksMap)) }
        }
    }

    private fun loadFavoriteIds(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITE_IDS, emptySet()).orEmpty().toSet()
    }

    private fun loadUserPlaylists(): List<VybePlaylist> {
        val raw = prefs.getString(KEY_USER_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<VybePlaylist>>() {}.type
            gson.fromJson<List<VybePlaylist>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun saveUserPlaylists(playlists: List<VybePlaylist>) {
        runCatching {
            prefs.edit { putString(KEY_USER_PLAYLISTS, gson.toJson(playlists)) }
        }
    }

    private fun loadRecentlyPlayed(): List<VybeTrack> {
        val raw = prefs.getString(KEY_RECENTLY_PLAYED, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<VybeTrack>>() {}.type
            gson.fromJson<List<VybeTrack>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun saveRecentlyPlayed(tracks: List<VybeTrack>) {
        runCatching {
            prefs.edit { putString(KEY_RECENTLY_PLAYED, gson.toJson(tracks)) }
        }
    }

    private fun loadCachedTracks(): MutableMap<String, VybeTrack> {
        val raw = prefs.getString(KEY_CACHED_TRACKS, null) ?: return LinkedHashMap()
        return runCatching {
            val type = object : TypeToken<MutableMap<String, VybeTrack>>() {}.type
            gson.fromJson<MutableMap<String, VybeTrack>>(raw, type) ?: LinkedHashMap()
        }.getOrElse { LinkedHashMap() }
    }
}
