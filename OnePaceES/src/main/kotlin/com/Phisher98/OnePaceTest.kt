package com.Phisher98

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SeasonData
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.addSeasonNames
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class OnePaceTest : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://raw.githubusercontent.com/sinister-kid/sinicloud-data/refs/heads/main/onepace/onepace.json"
    override var name = "One Pace Test"
    override val hasMainPage = true
    override var lang = "es"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime)
    override val mainPage = mainPageOf(mainUrl to "One Pace")


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val json = parseJson<OnePaceData>(app.get(request.data).text)
        val mainAnimeview = listOf(json.toSearchResult())
        return newHomePageResponse(request.name, mainAnimeview)
    }

    private fun OnePaceArc.toSearchResult(): AnimeSearchResponse {
        return newAnimeSearchResponse("Arco $number - $name", this.toJson(), TvType.Anime) {
            this.posterUrl = cover
        }
    }

    private fun OnePaceData.toSearchResult(): AnimeSearchResponse {
        return newAnimeSearchResponse(name, this.toJson(), TvType.Anime) {
            this.posterUrl = cover
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val jPace = parseJson<OnePaceData>(url)
        val arcs = jPace.arcs
        val seasons = mutableListOf<SeasonData>()
        val episodes = mutableListOf<Episode>()

        arcs.map { jArc ->
            if (jArc.apiId.isNotEmpty()) {
                seasons.add(SeasonData(jArc.number, jArc.name, jArc.number))
                val pdUrl = "https://pixeldrain.com"
                val pdAlbum = parseJson<MediaAlbum>(app.get("$pdUrl/api/list/${jArc.apiId}").text)
                pdAlbum.files.map { mFile ->
                    episodes.add(newEpisode("$pdUrl/u/${mFile.id}") {
                        this.season = jArc.number
                        this.name = mFile.name
                        this.posterUrl = mFile.thumbnail
                    })
                }
            }

        }

        return newAnimeLoadResponse(jPace.name, url, TvType.Anime) {
            addEpisodes(DubStatus.Subbed, episodes)
            addSeasonNames(seasons)
            posterUrl = jPace.cover
            plot = jPace.description
            backgroundPosterUrl = jPace.coverbg
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mId = Regex("/u/(.*)").find(data)?.groupValues?.get(1)
        //if (apiId.isEmpty()) {
        //    return false
        //}
        //else {
            loadExtractor("https://pd.cybar.xyz/$mId", subtitleCallback, callback)
            loadExtractor(data, subtitleCallback, callback)
            return true
        //}
    }

    data class OnePaceData(
        @JsonProperty("name") val name: String,
        @JsonProperty("description") val description: String,
        @JsonProperty("cover") val cover: String,
        @JsonProperty("coverbg") val coverbg: String,
        @JsonProperty("arcs") val arcs: Array<OnePaceArc>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OnePaceData

            if (name != other.name) return false
            if (description != other.description) return false
            if (cover != other.cover) return false
            if (!arcs.contentEquals(other.arcs)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + cover.hashCode()
            result = 31 * result + arcs.contentHashCode()
            return result
        }
    }

    data class OnePaceArc(
        @JsonProperty("number") val number: Int,
        @JsonProperty("name") val name: String,
        @JsonProperty("description") val description: String,
        @JsonProperty("cover") val cover: String,
        @JsonProperty("apiId") val apiId: String,
    )

    data class MediaAlbum(
        @JsonProperty("id") val id: String,
        @JsonProperty("file_count") val fileCount: Int,
        @JsonProperty("files") val files: Array<MediaFile>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MediaAlbum

            if (id != other.id) return false
            if (fileCount != other.fileCount) return false
            if (!files.contentEquals(other.files)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + fileCount
            result = 31 * result + files.contentHashCode()
            return result
        }
    }

    data class MediaFile(
        @JsonProperty("id") val id: String,
        @JsonProperty("name") val name: String,
        @JsonProperty("thumbnail_href") val thumbnail: String
    )
}
