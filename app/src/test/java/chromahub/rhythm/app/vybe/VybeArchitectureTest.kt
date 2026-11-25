/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe

import chromahub.rhythm.app.vybe.config.VybeApiConfig
import chromahub.rhythm.app.vybe.data.mapper.VybeModelMapper.toVybeAlbum
import chromahub.rhythm.app.vybe.data.mapper.VybeModelMapper.toVybeTrack
import chromahub.rhythm.app.vybe.data.provider.CustomVybeProvider
import chromahub.rhythm.app.vybe.data.provider.DummyMusicProvider
import chromahub.rhythm.app.vybe.data.provider.ProviderRegistry
import chromahub.rhythm.app.vybe.data.remote.CustomVybeAlbumDto
import chromahub.rhythm.app.vybe.data.remote.CustomVybeTrackDto
import chromahub.rhythm.app.vybe.data.repository.VybeMusicRepositoryImpl
import chromahub.rhythm.app.vybe.domain.model.VybeAlbum
import chromahub.rhythm.app.vybe.domain.model.VybeArtist
import chromahub.rhythm.app.vybe.domain.model.VybeHomeData
import chromahub.rhythm.app.vybe.domain.model.VybePlaylist
import chromahub.rhythm.app.vybe.domain.model.VybeResult
import chromahub.rhythm.app.vybe.domain.model.VybeSearchResult
import chromahub.rhythm.app.vybe.domain.model.VybeTrack
import chromahub.rhythm.app.vybe.domain.provider.MusicProvider
import chromahub.rhythm.app.vybe.domain.provider.ProviderCapability
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class VybeArchitectureTest {

    @Test
    fun dummyProvider_suppliesCompleteSampleCatalogAndStreamingUrls() = runBlocking {
        val provider = DummyMusicProvider()

        assertEquals(VybeApiConfig.PROVIDER_DUMMY, provider.providerId)
        assertTrue(provider.isConfigured())

        val home = provider.getHome()
        assertTrue(home.featuredAlbums.isNotEmpty())
        assertTrue(home.quickPicks.isNotEmpty())
        assertTrue(home.trendingTracks.isNotEmpty())
        assertTrue(home.topArtists.isNotEmpty())
        assertTrue(home.curatedPlaylists.isNotEmpty())

        val tracks = provider.getCatalogTracks()
        assertTrue(tracks.size >= 10)
        assertTrue(tracks.all { !it.streamUrl.isNullOrBlank() && it.streamUrl!!.startsWith("https://") })

        val firstTrack = tracks.first()
        val fetchedTrack = provider.getTrack(firstTrack.id)
        assertEquals(firstTrack.id, fetchedTrack?.id)
        assertEquals(firstTrack.streamUrl, provider.getStreamUrl(firstTrack.id))

        val lyrics = provider.getLyrics(firstTrack.id)
        assertNotNull(lyrics)
        assertTrue(!lyrics?.syncedLyrics.isNullOrBlank() || !lyrics?.plainLyrics.isNullOrBlank())

        val searchResult = provider.search(firstTrack.title.substringBefore(" "))
        assertTrue(searchResult.tracks.any { it.id == firstTrack.id })

        val recommendations = provider.getRecommendations(firstTrack.id, limit = 5)
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.none { it.id == firstTrack.id })
    }

    @Test
    fun providerRegistry_resolvesKnownProvidersAndFallsBackSafely() {
        val dummy = DummyMusicProvider()
        val custom = CustomVybeProvider(baseUrlProvider = { "" })
        val registry = ProviderRegistry(
            initialProviders = listOf(dummy, custom),
            defaultProviderId = VybeApiConfig.PROVIDER_DUMMY
        )

        assertEquals(2, registry.listProviders().size)
        assertEquals(VybeApiConfig.PROVIDER_DUMMY, registry.resolveProvider("dummy").providerId)
        assertEquals(VybeApiConfig.PROVIDER_CUSTOM_VYBE, registry.resolveProvider("custom_vybe").providerId)
        assertEquals(VybeApiConfig.PROVIDER_DUMMY, registry.resolveProvider("nonexistent_provider").providerId)
        assertFalse(custom.isConfigured())
    }

    @Test
    fun repository_cachesCatalogHandlesErrorsAndSwitchesProvidersCleanly() = runBlocking {
        val failingProvider = object : MusicProvider {
            override val providerId = "failing_api"
            override val displayName = "Failing API"
            override val description = "Always fails for testing"
            override val capabilities = setOf(ProviderCapability.STREAMING)
            override suspend fun getHome(): VybeHomeData = throw IOException("Simulated network outage")
            override suspend fun search(query: String): VybeSearchResult = throw IOException("Search offline")
            override suspend fun getTrack(id: String): VybeTrack? = null
            override suspend fun getAlbum(id: String): VybeAlbum? = null
            override suspend fun getArtist(id: String): VybeArtist? = null
            override suspend fun getPlaylist(id: String): VybePlaylist? = null
        }

        val dummy = DummyMusicProvider()
        val registry = ProviderRegistry(listOf(dummy, failingProvider))
        var switchedTo: String? = null
        val repo = VybeMusicRepositoryImpl(
            registry = registry,
            initialProviderIdProvider = { VybeApiConfig.PROVIDER_DUMMY },
            onProviderSwitched = { switchedTo = it }
        )

        val initialSnapshot = repo.getCatalogSnapshot()
        assertTrue(initialSnapshot is VybeResult.Success)
        val data = (initialSnapshot as VybeResult.Success).data
        assertEquals(VybeApiConfig.PROVIDER_DUMMY, data.providerId)
        assertTrue(data.tracks.isNotEmpty())

        // Switch to failing provider: must invalidate cache and return VybeResult.Error cleanly
        repo.switchProvider("failing_api")
        assertEquals("failing_api", switchedTo)
        assertEquals("failing_api", repo.activeProviderInfo.value.providerId)

        val failedSnapshot = repo.getCatalogSnapshot()
        assertTrue(failedSnapshot is VybeResult.Error)
        val err = failedSnapshot as VybeResult.Error
        assertTrue(err.isNetworkError)
        assertTrue(err.message.contains("Simulated network outage"))
    }

    @Test
    fun customVybeProvider_parsesRetrofitEndpointsAndHandles404Gracefully() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val baseUrl = server.url("/").toString()
            val provider = CustomVybeProvider(baseUrlProvider = { baseUrl })
            assertTrue(provider.isConfigured())

            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body(
                        """
                        {
                          "featured_albums": [
                            {
                              "id": "alb_1",
                              "title": "Neon Horizon",
                              "artist": "Synth Collective",
                              "year": 2026,
                              "tracks": [
                                {
                                  "id": "trk_1",
                                  "title": "Midnight Drive",
                                  "artist": "Synth Collective",
                                  "album": "Neon Horizon",
                                  "album_id": "alb_1",
                                  "duration_ms": 215000,
                                  "stream_url": "https://cdn.example.com/trk_1.mp3"
                                }
                              ]
                            }
                          ],
                          "quick_picks": [
                            {
                              "id": "trk_1",
                              "title": "Midnight Drive",
                              "artist": "Synth Collective",
                              "album": "Neon Horizon",
                              "duration_ms": 215000,
                              "stream_url": "https://cdn.example.com/trk_1.mp3"
                            }
                          ]
                        }
                        """.trimIndent()
                    )
                    .build()
            )

            val home = provider.getHome()
            assertEquals(1, home.featuredAlbums.size)
            assertEquals("Neon Horizon", home.featuredAlbums.first().title)
            assertEquals("trk_1", home.quickPicks.first().id)

            // Stream URL endpoint
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body("""{"stream_url": "https://cdn.example.com/signed/trk_1.mp3"}""")
                    .build()
            )
            assertEquals("https://cdn.example.com/signed/trk_1.mp3", provider.getStreamUrl("trk_1"))

            // 404 track lookup returns null cleanly instead of throwing
            server.enqueue(MockResponse.Builder().code(404).build())
            assertNull(provider.getTrack("missing_track"))

            // Lyrics endpoint
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .body("""{"plain_lyrics": "Driving under neon skies", "synced_lyrics": "[00:01.00] Driving under neon skies", "source": "Custom API"}""")
                    .build()
            )
            val lyrics = provider.getLyrics("trk_1")
            assertNotNull(lyrics)
            assertEquals("Custom API", lyrics?.source)
        } finally {
            server.close()
        }
    }

    @Test
    fun mapper_sanitizesMalformedDtosSafely() {
        val blankIdDto = CustomVybeTrackDto(id = "   ", title = "No ID")
        assertNull(blankIdDto.toVybeTrack("custom_vybe"))

        val partialTrack = CustomVybeTrackDto(
            id = "t_42",
            title = null,
            artist = "",
            durationMs = -100L
        ).toVybeTrack("custom_vybe")
        assertNotNull(partialTrack)
        assertEquals("Untitled Track", partialTrack?.title)
        assertEquals("Unknown Artist", partialTrack?.artist)
        assertEquals(0L, partialTrack?.durationMs)

        val album = CustomVybeAlbumDto(
            id = "a_1",
            title = "Test Album",
            tracks = listOf(CustomVybeTrackDto(id = "t_42", title = "Song", artworkUrl = "https://img.example.com/cover.jpg"))
        ).toVybeAlbum("custom_vybe")
        assertEquals("https://img.example.com/cover.jpg", album?.artworkUrl)
        assertEquals(1, album?.trackCount)
    }
}
