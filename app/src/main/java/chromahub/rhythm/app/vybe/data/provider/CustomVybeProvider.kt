/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.vybe.data.provider

import chromahub.rhythm.app.BuildConfig
import chromahub.rhythm.app.vybe.config.VybeApiConfig
import chromahub.rhythm.app.vybe.data.mapper.VybeModelMapper.toVybeAlbum
import chromahub.rhythm.app.vybe.data.mapper.VybeModelMapper.toVybeArtist
import chromahub.rhythm.app.vybe.data.mapper.VybeModelMapper.toVybeHomeData
import chromahub.rhythm.app.vybe.data.mapper.VybeModelMapper.toVybeLyrics
import chromahub.rhythm.app.vybe.data.mapper.VybeModelMapper.toVybePlaylist
import chromahub.rhythm.app.vybe.data.mapper.VybeModelMapper.toVybeSearchResult
import chromahub.rhythm.app.vybe.data.mapper.VybeModelMapper.toVybeTrack
import chromahub.rhythm.app.vybe.data.remote.VybeRemoteApi
import chromahub.rhythm.app.vybe.domain.model.VybeAlbum
import chromahub.rhythm.app.vybe.domain.model.VybeArtist
import chromahub.rhythm.app.vybe.domain.model.VybeHomeData
import chromahub.rhythm.app.vybe.domain.model.VybeLyrics
import chromahub.rhythm.app.vybe.domain.model.VybePlaylist
import chromahub.rhythm.app.vybe.domain.model.VybeSearchResult
import chromahub.rhythm.app.vybe.domain.model.VybeTrack
import chromahub.rhythm.app.vybe.domain.provider.LyricsCapableProvider
import chromahub.rhythm.app.vybe.domain.provider.MusicProvider
import chromahub.rhythm.app.vybe.domain.provider.ProviderCapability
import chromahub.rhythm.app.vybe.domain.provider.RecommendationsCapableProvider
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Ready-to-use Retrofit-backed [MusicProvider] for connecting VYBE to your own custom
 * music backend API (or proxy server wrapping upstream music APIs).
 *
 * Configure the endpoint via:
 * - `local.properties`: `MUSIC_PROVIDER=custom_vybe` and `VYBE_API_BASE_URL=https://api.yourdomain.com/`
 * - Or at runtime in **Settings → Music Provider & API**.
 *
 * To integrate a different upstream API with a custom JSON schema (e.g. JioSaavn, YouTube Music),
 * create a sibling provider class implementing [MusicProvider] in this package and register it
 * in [ProviderRegistry].
 */
class CustomVybeProvider(
    private val baseUrlProvider: () -> String,
    private val apiOverride: VybeRemoteApi? = null
) : MusicProvider, LyricsCapableProvider, RecommendationsCapableProvider {

    override val providerId: String = VybeApiConfig.PROVIDER_CUSTOM_VYBE
    override val displayName: String = "Custom VYBE API"
    override val description: String =
        "Connect to your own REST music streaming backend configured via VYBE_API_BASE_URL"
    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.STREAMING,
        ProviderCapability.SEARCH,
        ProviderCapability.LYRICS,
        ProviderCapability.RECOMMENDATIONS,
        ProviderCapability.PLAYLISTS
    )

    private var cachedBaseUrl: String? = null
    private var cachedApi: VybeRemoteApi? = null

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "VYBE/${BuildConfig.VERSION_NAME} (Android)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }
        builder.build()
    }

    private val gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    override fun isConfigured(): Boolean {
        if (apiOverride != null) return true
        val url = normalizeBaseUrl(baseUrlProvider())
        return url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
    }

    private fun requireApi(): VybeRemoteApi {
        apiOverride?.let { return it }
        val normalized = normalizeBaseUrl(baseUrlProvider())
        if (!normalized.startsWith("http://", ignoreCase = true) &&
            !normalized.startsWith("https://", ignoreCase = true)
        ) {
            throw IllegalStateException(
                "Custom VYBE API base URL is not configured. Set VYBE_API_BASE_URL in Settings or local.properties."
            )
        }

        synchronized(this) {
            if (cachedApi == null || cachedBaseUrl != normalized) {
                cachedApi = Retrofit.Builder()
                    .baseUrl(normalized)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
                    .create(VybeRemoteApi::class.java)
                cachedBaseUrl = normalized
            }
            return cachedApi!!
        }
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private fun <T> Response<T>.requireBody(operation: String): T {
        if (!isSuccessful) {
            throw IOException("VYBE API $operation failed with HTTP ${code()}: ${message()}")
        }
        return body() ?: throw IOException("VYBE API $operation returned an empty body")
    }

    override suspend fun getHome(): VybeHomeData {
        val dto = requireApi().getHome().requireBody("getHome")
        return dto.toVybeHomeData(providerId)
    }

    override suspend fun search(query: String): VybeSearchResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return VybeSearchResult(query = query)
        val dto = requireApi().search(trimmed).requireBody("search")
        return dto.toVybeSearchResult(trimmed, providerId)
    }

    override suspend fun getTrack(id: String): VybeTrack? {
        val response = requireApi().getTrack(id)
        if (response.code() == 404) return null
        return response.requireBody("getTrack").toVybeTrack(providerId)
    }

    override suspend fun getStreamUrl(trackId: String): String? {
        val response = requireApi().getStreamUrl(trackId)
        if (response.isSuccessful) {
            val stream = response.body()?.streamUrl?.trim()
            if (!stream.isNullOrBlank()) return stream
        }
        return getTrack(trackId)?.streamUrl
    }

    override suspend fun getAlbum(id: String): VybeAlbum? {
        val response = requireApi().getAlbum(id)
        if (response.code() == 404) return null
        return response.requireBody("getAlbum").toVybeAlbum(providerId)
    }

    override suspend fun getArtist(id: String): VybeArtist? {
        val response = requireApi().getArtist(id)
        if (response.code() == 404) return null
        return response.requireBody("getArtist").toVybeArtist(providerId)
    }

    override suspend fun getPlaylist(id: String): VybePlaylist? {
        val response = requireApi().getPlaylist(id)
        if (response.code() == 404) return null
        return response.requireBody("getPlaylist").toVybePlaylist(providerId)
    }

    override suspend fun getLyrics(trackId: String): VybeLyrics? {
        val response = requireApi().getLyrics(trackId)
        if (response.code() == 404 || !response.isSuccessful) return null
        return response.body()?.toVybeLyrics(trackId)
    }

    override suspend fun getRecommendations(seedTrackId: String, limit: Int): List<VybeTrack> {
        val response = requireApi().getRecommendations(seedTrackId, limit)
        if (!response.isSuccessful) return emptyList()
        return response.body().orEmpty().mapNotNull { it.toVybeTrack(providerId) }
    }
}
