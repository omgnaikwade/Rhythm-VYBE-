/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.data.provider

import chromahub.rhythm.app.vybe.config.VybeApiConfig
import chromahub.rhythm.app.vybe.domain.model.HomeSectionType
import chromahub.rhythm.app.vybe.domain.model.VybeAlbum
import chromahub.rhythm.app.vybe.domain.model.VybeArtist
import chromahub.rhythm.app.vybe.domain.model.VybeHomeData
import chromahub.rhythm.app.vybe.domain.model.VybeHomeSection
import chromahub.rhythm.app.vybe.domain.model.VybeLyrics
import chromahub.rhythm.app.vybe.domain.model.VybePlaylist
import chromahub.rhythm.app.vybe.domain.model.VybeSearchResult
import chromahub.rhythm.app.vybe.domain.model.VybeTrack
import chromahub.rhythm.app.vybe.domain.provider.LyricsCapableProvider
import chromahub.rhythm.app.vybe.domain.provider.MusicProvider
import chromahub.rhythm.app.vybe.domain.provider.PlaylistCapableProvider
import chromahub.rhythm.app.vybe.domain.provider.ProviderCapability
import chromahub.rhythm.app.vybe.domain.provider.RecommendationsCapableProvider

/**
 * Built-in DummyMusicProvider for offline-safe UI development and testing until
 * real music APIs are connected.
 *
 * Implements the exact same [MusicProvider] and capability interfaces as real providers.
 * Active by default when `MUSIC_PROVIDER=dummy`.
 */
class DummyMusicProvider :
    MusicProvider,
    LyricsCapableProvider,
    RecommendationsCapableProvider,
    PlaylistCapableProvider {

    override val providerId: String = VybeApiConfig.PROVIDER_DUMMY
    override val displayName: String = "VYBE Sample Catalog"
    override val description: String =
        "Built-in sample music catalog with streamable demo tracks for UI development"
    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.STREAMING,
        ProviderCapability.SEARCH,
        ProviderCapability.LYRICS,
        ProviderCapability.RECOMMENDATIONS,
        ProviderCapability.PLAYLISTS
    )

    private val nowMs = System.currentTimeMillis()
    private val dayMs = 86_400_000L

    // Royalty-free public sample MP3 streams from SoundHelix for genuine ExoPlayer streaming tests.
    private fun sampleStream(index: Int): String =
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-${((index - 1) % 16) + 1}.mp3"

    private val tracks: List<VybeTrack> by lazy {
        listOf(
            // Album 1: Midnight Neon — Nova Pulse
            VybeTrack(
                id = "vybe_t_01",
                title = "Neon Horizon",
                artist = "Nova Pulse",
                artistId = "vybe_ar_01",
                album = "Midnight Neon",
                albumId = "vybe_al_01",
                artworkUrl = "https://images.unsplash.com/photo-1614149162883-504ce4d13909?w=600&q=80",
                durationMs = 372_000L,
                streamUrl = sampleStream(1),
                syncedLyrics = """
                    [00:05.00]City lights blur across the midnight rain
                    [00:12.50]Chasing frequencies along the empty lane
                    [00:20.00]Neon horizon calling out your name
                    [00:28.00]Feel the synthwave pulse inside the frame
                    [00:36.50]Turn the volume up and let the night unfold
                    [00:45.00]Electric skies turning violet into gold
                """.trimIndent(),
                lyrics = "City lights blur across the midnight rain\nChasing frequencies along the empty lane\nNeon horizon calling out your name\nFeel the synthwave pulse inside the frame\nTurn the volume up and let the night unfold\nElectric skies turning violet into gold",
                providerId = providerId,
                genre = "Synthwave",
                year = 2026,
                trackNumber = 1,
                bitrate = 320_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "AAC",
                dateAdded = nowMs - dayMs * 1
            ),
            VybeTrack(
                id = "vybe_t_02",
                title = "Velvet Skyline",
                artist = "Nova Pulse",
                artistId = "vybe_ar_01",
                album = "Midnight Neon",
                albumId = "vybe_al_01",
                artworkUrl = "https://images.unsplash.com/photo-1614149162883-504ce4d13909?w=600&q=80",
                durationMs = 425_000L,
                streamUrl = sampleStream(2),
                syncedLyrics = """
                    [00:04.00]Drifting above the velvet skyline glow
                    [00:13.00]Every heartbeat moving soft and slow
                    [00:22.00]Static fading as the stars align
                    [00:31.00]Lost inside the rhythm out of time
                """.trimIndent(),
                lyrics = "Drifting above the velvet skyline glow\nEvery heartbeat moving soft and slow\nStatic fading as the stars align\nLost inside the rhythm out of time",
                providerId = providerId,
                genre = "Synthwave",
                year = 2026,
                trackNumber = 2,
                bitrate = 320_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "AAC",
                dateAdded = nowMs - dayMs * 1
            ),
            VybeTrack(
                id = "vybe_t_03",
                title = "Afterhours Drive",
                artist = "Nova Pulse",
                artistId = "vybe_ar_01",
                album = "Midnight Neon",
                albumId = "vybe_al_01",
                artworkUrl = "https://images.unsplash.com/photo-1614149162883-504ce4d13909?w=600&q=80",
                durationMs = 344_000L,
                streamUrl = sampleStream(3),
                providerId = providerId,
                genre = "Synthwave",
                year = 2026,
                trackNumber = 3,
                bitrate = 320_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "AAC",
                dateAdded = nowMs - dayMs * 2
            ),

            // Album 2: Rain on Cassette — Kairo Lo-Fi
            VybeTrack(
                id = "vybe_t_04",
                title = "Coffee & Cloudburst",
                artist = "Kairo Lo-Fi",
                artistId = "vybe_ar_02",
                album = "Rain on Cassette",
                albumId = "vybe_al_02",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                durationMs = 302_000L,
                streamUrl = sampleStream(4),
                providerId = providerId,
                genre = "Lo-Fi",
                year = 2026,
                trackNumber = 1,
                bitrate = 256_000,
                sampleRate = 44_100,
                channels = 2,
                codec = "OPUS",
                dateAdded = nowMs - dayMs * 2
            ),
            VybeTrack(
                id = "vybe_t_05",
                title = "Windowpane Dreams",
                artist = "Kairo Lo-Fi",
                artistId = "vybe_ar_02",
                album = "Rain on Cassette",
                albumId = "vybe_al_02",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                durationMs = 353_000L,
                streamUrl = sampleStream(5),
                providerId = providerId,
                genre = "Lo-Fi",
                year = 2026,
                trackNumber = 2,
                bitrate = 256_000,
                sampleRate = 44_100,
                channels = 2,
                codec = "OPUS",
                dateAdded = nowMs - dayMs * 3
            ),
            VybeTrack(
                id = "vybe_t_06",
                title = "Analog Dusk",
                artist = "Kairo Lo-Fi",
                artistId = "vybe_ar_02",
                album = "Rain on Cassette",
                albumId = "vybe_al_02",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                durationMs = 280_000L,
                streamUrl = sampleStream(6),
                providerId = providerId,
                genre = "Lo-Fi",
                year = 2026,
                trackNumber = 3,
                bitrate = 256_000,
                sampleRate = 44_100,
                channels = 2,
                codec = "OPUS",
                dateAdded = nowMs - dayMs * 4
            ),

            // Album 3: Solar Echoes — Aria Vale
            VybeTrack(
                id = "vybe_t_07",
                title = "Golden Hour Static",
                artist = "Aria Vale",
                artistId = "vybe_ar_03",
                album = "Solar Echoes",
                albumId = "vybe_al_03",
                artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&q=80",
                durationMs = 420_000L,
                streamUrl = sampleStream(7),
                syncedLyrics = """
                    [00:06.00]Sunlight spilling across the wooden floor
                    [00:14.00]Shadows dancing beside the open door
                    [00:23.00]We hold onto the echo of the afternoon
                    [00:32.00]Singing quiet verses to a golden tune
                """.trimIndent(),
                lyrics = "Sunlight spilling across the wooden floor\nShadows dancing beside the open door\nWe hold onto the echo of the afternoon\nSinging quiet verses to a golden tune",
                providerId = providerId,
                genre = "Indie Pop",
                year = 2025,
                trackNumber = 1,
                bitrate = 960_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "FLAC",
                dateAdded = nowMs - dayMs * 5
            ),
            VybeTrack(
                id = "vybe_t_08",
                title = "Paper Satellites",
                artist = "Aria Vale",
                artistId = "vybe_ar_03",
                album = "Solar Echoes",
                albumId = "vybe_al_03",
                artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&q=80",
                durationMs = 325_000L,
                streamUrl = sampleStream(8),
                providerId = providerId,
                genre = "Indie Pop",
                year = 2025,
                trackNumber = 2,
                bitrate = 960_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "FLAC",
                dateAdded = nowMs - dayMs * 5
            ),
            VybeTrack(
                id = "vybe_t_09",
                title = "Mirage Coast",
                artist = "Aria Vale",
                artistId = "vybe_ar_03",
                album = "Solar Echoes",
                albumId = "vybe_al_03",
                artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&q=80",
                durationMs = 389_000L,
                streamUrl = sampleStream(9),
                providerId = providerId,
                genre = "Indie Pop",
                year = 2025,
                trackNumber = 3,
                bitrate = 960_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "FLAC",
                dateAdded = nowMs - dayMs * 6
            ),

            // Album 4: Deep Blue Frequency — Solara Drift
            VybeTrack(
                id = "vybe_t_10",
                title = "Submerged Reflections",
                artist = "Solara Drift",
                artistId = "vybe_ar_04",
                album = "Deep Blue Frequency",
                albumId = "vybe_al_04",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
                durationMs = 527_000L,
                streamUrl = sampleStream(10),
                providerId = providerId,
                genre = "Electronic",
                year = 2026,
                trackNumber = 1,
                bitrate = 1_411_000,
                sampleRate = 96_000,
                channels = 2,
                codec = "FLAC",
                dateAdded = nowMs - dayMs * 7
            ),
            VybeTrack(
                id = "vybe_t_11",
                title = "Bioluminescence",
                artist = "Solara Drift",
                artistId = "vybe_ar_04",
                album = "Deep Blue Frequency",
                albumId = "vybe_al_04",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
                durationMs = 468_000L,
                streamUrl = sampleStream(11),
                providerId = providerId,
                genre = "Electronic",
                year = 2026,
                trackNumber = 2,
                bitrate = 1_411_000,
                sampleRate = 96_000,
                channels = 2,
                codec = "FLAC",
                dateAdded = nowMs - dayMs * 7
            ),
            VybeTrack(
                id = "vybe_t_12",
                title = "Tidal Horizon",
                artist = "Solara Drift",
                artistId = "vybe_ar_04",
                album = "Deep Blue Frequency",
                albumId = "vybe_al_04",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
                durationMs = 412_000L,
                streamUrl = sampleStream(12),
                providerId = providerId,
                genre = "Electronic",
                year = 2026,
                trackNumber = 3,
                bitrate = 1_411_000,
                sampleRate = 96_000,
                channels = 2,
                codec = "FLAC",
                dateAdded = nowMs - dayMs * 8
            ),

            // Album 5: Velvet Nocturne — Zayn Mercer
            VybeTrack(
                id = "vybe_t_13",
                title = "Midnight Silk",
                artist = "Zayn Mercer",
                artistId = "vybe_ar_05",
                album = "Velvet Nocturne",
                albumId = "vybe_al_05",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80",
                durationMs = 310_000L,
                streamUrl = sampleStream(13),
                providerId = providerId,
                genre = "R&B",
                year = 2025,
                trackNumber = 1,
                bitrate = 320_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "AAC",
                dateAdded = nowMs - dayMs * 9
            ),
            VybeTrack(
                id = "vybe_t_14",
                title = "Candlelight Echo",
                artist = "Zayn Mercer",
                artistId = "vybe_ar_05",
                album = "Velvet Nocturne",
                albumId = "vybe_al_05",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80",
                durationMs = 342_000L,
                streamUrl = sampleStream(14),
                providerId = providerId,
                genre = "R&B",
                year = 2025,
                trackNumber = 2,
                bitrate = 320_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "AAC",
                dateAdded = nowMs - dayMs * 10
            ),
            VybeTrack(
                id = "vybe_t_15",
                title = "Low Light Groove",
                artist = "Zayn Mercer",
                artistId = "vybe_ar_05",
                album = "Velvet Nocturne",
                albumId = "vybe_al_05",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80",
                durationMs = 298_000L,
                streamUrl = sampleStream(15),
                providerId = providerId,
                genre = "R&B",
                year = 2025,
                trackNumber = 3,
                bitrate = 320_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "AAC",
                dateAdded = nowMs - dayMs * 11
            ),

            // Album 6: Astral Stillness — Lyra Moss
            VybeTrack(
                id = "vybe_t_16",
                title = "Weightless Orbit",
                artist = "Lyra Moss",
                artistId = "vybe_ar_06",
                album = "Astral Stillness",
                albumId = "vybe_al_06",
                artworkUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&q=80",
                durationMs = 456_000L,
                streamUrl = sampleStream(16),
                providerId = providerId,
                genre = "Ambient",
                year = 2026,
                trackNumber = 1,
                bitrate = 320_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "MP3",
                dateAdded = nowMs - dayMs * 12
            ),
            VybeTrack(
                id = "vybe_t_17",
                title = "Quiet Constellations",
                artist = "Lyra Moss",
                artistId = "vybe_ar_06",
                album = "Astral Stillness",
                albumId = "vybe_al_06",
                artworkUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&q=80",
                durationMs = 365_000L,
                streamUrl = sampleStream(1),
                providerId = providerId,
                genre = "Ambient",
                year = 2026,
                trackNumber = 2,
                bitrate = 320_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "MP3",
                dateAdded = nowMs - dayMs * 13
            ),
            VybeTrack(
                id = "vybe_t_18",
                title = "Dawn Over Saturn",
                artist = "Lyra Moss",
                artistId = "vybe_ar_06",
                album = "Astral Stillness",
                albumId = "vybe_al_06",
                artworkUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&q=80",
                durationMs = 390_000L,
                streamUrl = sampleStream(4),
                providerId = providerId,
                genre = "Ambient",
                year = 2026,
                trackNumber = 3,
                bitrate = 320_000,
                sampleRate = 48_000,
                channels = 2,
                codec = "MP3",
                dateAdded = nowMs - dayMs * 14
            )
        )
    }

    private val albums: List<VybeAlbum> by lazy {
        tracks.groupBy { it.albumId }.map { (albumId, albumTracks) ->
            val first = albumTracks.first()
            VybeAlbum(
                id = albumId,
                title = first.album,
                artist = first.artist,
                artistId = first.artistId,
                artworkUrl = first.artworkUrl,
                year = first.year,
                tracks = albumTracks.sortedBy { it.trackNumber },
                trackCount = albumTracks.size,
                providerId = providerId,
                dateModified = albumTracks.maxOfOrNull { it.dateAdded } ?: nowMs
            )
        }
    }

    private val artistArtworks = mapOf(
        "vybe_ar_01" to "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=500&q=80",
        "vybe_ar_02" to "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
        "vybe_ar_03" to "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=500&q=80",
        "vybe_ar_04" to "https://images.unsplash.com/photo-1571330735066-03aaa9429d89?w=500&q=80",
        "vybe_ar_05" to "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80",
        "vybe_ar_06" to "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80"
    )

    private val artists: List<VybeArtist> by lazy {
        tracks.groupBy { it.artistId }.map { (artistId, artistTracks) ->
            val first = artistTracks.first()
            val artistAlbums = albums.filter { it.artistId == artistId }
            VybeArtist(
                id = artistId,
                name = first.artist,
                artworkUrl = artistArtworks[artistId] ?: first.artworkUrl,
                bio = "${first.artist} crafts atmospheric ${first.genre ?: "electronic"} soundscapes curated for VYBE.",
                monthlyListeners = 250_000L + (artistId.hashCode().and(0xFFFF) * 17L),
                albums = artistAlbums,
                topTracks = artistTracks,
                providerId = providerId
            )
        }
    }

    private val playlists: List<VybePlaylist> by lazy {
        listOf(
            VybePlaylist(
                id = "vybe_pl_01",
                name = "VYBE Night Drive",
                description = "Glowing synthwave and midnight grooves for late-night sessions",
                artworkUrl = "https://images.unsplash.com/photo-1614149162883-504ce4d13909?w=600&q=80",
                tracks = listOf(tracks[0], tracks[1], tracks[9], tracks[12], tracks[2]),
                creator = "VYBE Editorial",
                providerId = providerId
            ),
            VybePlaylist(
                id = "vybe_pl_02",
                name = "Deep Focus Flow",
                description = "Warm lo-fi beats and ambient textures for uninterrupted concentration",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                tracks = listOf(tracks[3], tracks[4], tracks[15], tracks[16], tracks[5]),
                creator = "VYBE Editorial",
                providerId = providerId
            ),
            VybePlaylist(
                id = "vybe_pl_03",
                name = "Indie Sunsets",
                description = "Sun-soaked indie pop and soulful vocals",
                artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&q=80",
                tracks = listOf(tracks[6], tracks[7], tracks[8], tracks[13], tracks[14]),
                creator = "VYBE Editorial",
                providerId = providerId
            ),
            VybePlaylist(
                id = "vybe_pl_04",
                name = "Hi-Res Electronic",
                description = "Lossless electronic frequencies tuned for headphones",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
                tracks = listOf(tracks[9], tracks[10], tracks[11], tracks[0], tracks[17]),
                creator = "VYBE Editorial",
                providerId = providerId
            )
        )
    }

    override suspend fun getHome(): VybeHomeData {
        val quickPicks = listOf(
            tracks[0], tracks[3], tracks[6], tracks[9],
            tracks[12], tracks[15], tracks[1], tracks[7]
        )
        val trending = listOf(
            tracks[0], tracks[6], tracks[9], tracks[12], tracks[3], tracks[10], tracks[13], tracks[15]
        )
        return VybeHomeData(
            providerId = providerId,
            featuredAlbums = albums,
            quickPicks = quickPicks,
            trendingTracks = trending,
            topArtists = artists,
            curatedPlaylists = playlists,
            sections = listOf(
                VybeHomeSection("featured_albums", "Featured Albums", type = HomeSectionType.FEATURED_ALBUMS, albums = albums),
                VybeHomeSection("quick_picks", "Quick Picks", type = HomeSectionType.QUICK_PICKS, tracks = quickPicks),
                VybeHomeSection("trending", "Trending on VYBE", type = HomeSectionType.TRENDING_TRACKS, tracks = trending),
                VybeHomeSection("top_artists", "Featured Artists", type = HomeSectionType.TOP_ARTISTS, artists = artists),
                VybeHomeSection("curated_playlists", "Made for Your VYBE", type = HomeSectionType.CURATED_PLAYLISTS, playlists = playlists)
            )
        )
    }

    override suspend fun search(query: String): VybeSearchResult {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return VybeSearchResult(query = query)

        return VybeSearchResult(
            query = query,
            tracks = tracks.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q) ||
                    (it.genre?.lowercase()?.contains(q) == true)
            },
            albums = albums.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.tracks.any { t -> t.title.lowercase().contains(q) }
            },
            artists = artists.filter {
                it.name.lowercase().contains(q) ||
                    (it.bio?.lowercase()?.contains(q) == true)
            },
            playlists = playlists.filter {
                it.name.lowercase().contains(q) ||
                    (it.description?.lowercase()?.contains(q) == true)
            }
        )
    }

    override suspend fun getTrack(id: String): VybeTrack? = tracks.firstOrNull { it.id == id }

    override suspend fun getStreamUrl(trackId: String): String? = getTrack(trackId)?.streamUrl

    override suspend fun getAlbum(id: String): VybeAlbum? = albums.firstOrNull { it.id == id }

    override suspend fun getArtist(id: String): VybeArtist? =
        artists.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) }

    override suspend fun getPlaylist(id: String): VybePlaylist? =
        playlists.firstOrNull { it.id == id }

    override suspend fun getCatalogTracks(): List<VybeTrack> = tracks

    override suspend fun getCatalogAlbums(): List<VybeAlbum> = albums

    override suspend fun getCatalogArtists(): List<VybeArtist> = artists

    override suspend fun getFeaturedPlaylists(): List<VybePlaylist> = playlists

    override suspend fun getLyrics(trackId: String): VybeLyrics? {
        val track = getTrack(trackId) ?: return null
        if (track.syncedLyrics.isNullOrBlank() && track.lyrics.isNullOrBlank()) return null
        return VybeLyrics(
            trackId = trackId,
            plainLyrics = track.lyrics,
            syncedLyrics = track.syncedLyrics,
            source = "VYBE Sample Catalog"
        )
    }

    override suspend fun getRecommendations(seedTrackId: String, limit: Int): List<VybeTrack> {
        val seed = getTrack(seedTrackId)
        val prioritized = if (seed != null) {
            tracks.filter { it.id != seedTrackId }
                .sortedByDescending {
                    (if (it.genre == seed.genre) 2 else 0) + (if (it.artistId == seed.artistId) 1 else 0)
                }
        } else {
            tracks
        }
        return prioritized.take(limit)
    }

    override suspend fun getCuratedPlaylists(): List<VybePlaylist> = playlists
}
