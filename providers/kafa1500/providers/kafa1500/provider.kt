package com.atlaskafa1500

import app.api.MainAPI
import app.api.TvType
import app.api.models.*
import kotlin.random.Random

class kafa1500 : MainAPI() {

    override var name = "kafa1500"

    // Piped + Invidious fallback
    private val pipedInstance = "https://piped.video/api/v1"
    private val invidiousInstances = listOf(
        "https://invidious.snopyta.org",
        "https://invidious.kavin.rocks",
        "https://yewtu.be"
    )

    override var mainUrl = pipedInstance

    override val supportedTypes = setOf(TvType.Other)

    // Kanal arama (kısa kod: kafa)
    override suspend fun search(query: String): List<SearchResponse> {
        try {
            val json = app.get("$mainUrl/search?q=$query&type=channel").parsedSafe<List<Channel>>()
            return json.map {
                newTvSeriesSearchResponse(it.author, "$mainUrl/channel/${it.authorId}")
            }
        } catch (e: Exception) {
            // Fallback Invidious
            mainUrl = invidiousInstances[Random.nextInt(invidiousInstances.size)]
            val json = app.get("$mainUrl/api/v1/search?q=$query&type=channel")
                .parsedSafe<List<Channel>>()
            return json.map {
                newTvSeriesSearchResponse(it.author, "$mainUrl/channel/${it.authorId}")
            }
        }
    }

    // Kanal videolarını yükleme (yeni → eski)
    override suspend fun load(url: String): LoadResponse {
        val channelId = url.substringAfterLast("/")
        try {
            val videos = app.get("$mainUrl/channels/$channelId/videos").parsedSafe<List<Video>>()
            val sorted = videos.sortedByDescending { it.published }
            val episodes = sorted.map {
                newEpisode(
                    it.title,
                    "$mainUrl/watch?v=${it.videoId}&vq=hd2160&cc_lang_pref=tr&cc_load_policy=1"
                )
            }
            return newTvSeriesLoadResponse("Kanal", url, TvType.Other, episodes)
        } catch (e: Exception) {
            // Fallback Invidious
            mainUrl = invidiousInstances[Random.nextInt(invidiousInstances.size)]
            val videos = app.get("$mainUrl/api/v1/channels/$channelId/videos").parsedSafe<List<Video>>()
            val sorted = videos.sortedByDescending { it.published }
            val episodes = sorted.map {
                newEpisode(
                    it.title,
                    "$mainUrl/watch?v=${it.videoId}&vq=hd2160&cc_lang_pref=tr&cc_load_policy=1"
                )
            }
            return newTvSeriesLoadResponse("Kanal", url, TvType.Other, episodes)
        }
    }
}
