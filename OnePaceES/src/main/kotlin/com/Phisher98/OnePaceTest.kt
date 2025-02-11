package com.Phisher98

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.ProviderType
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class OnePaceTest : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://rentry.org"
    override var name = "One Pace Test"
    override val hasMainPage = true
    override var lang = "es"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime)
    override val mainPage = mainPageOf("${mainUrl}/onepacestest/" to "OnePace")

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document
        val home = document.select("article div h3").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): AnimeSearchResponse {
        val title = this.text()
        val posterUrl = this.nextElementSibling()?.select("p span img")?.attr("src") ?:""
        return newAnimeSearchResponse(title, url=title, TvType.Anime) { this.posterUrl = posterUrl }
    }

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

    override suspend fun load(url: String): LoadResponse {
        val pdApi = "https://pixeldrain.com/api"
        val title = url.substringAfterLast("/")
        val document = app.get("https://rentry.org/onepacestest").document
        val poster = document.selectFirst("img")?.attr("src") ?: "https://images3.alphacoders.com/134/1342304.jpeg"
        val episodes = mutableListOf<Episode>()
        val elements= document.selectFirst("article div h3:contains($title)")
        val description = elements?.nextElementSibling()?.nextElementSibling()?.selectFirst("p")?.text()
        val albumId = elements?.nextElementSibling()?.nextElementSibling()?.nextElementSibling()?.nextElementSibling()?.nextElementSibling()?.nextElementSibling()?.selectFirst("p")?.text()
        val json = parseJson<MediaAlbum>(app.get("$pdApi/list/$albumId").text)
        json.files.mapNotNull { jEp ->
            episodes.add(newEpisode("https://pd.cybar.xyz/${jEp.id}", {
                name = jEp.name
                posterUrl = "$pdApi${jEp.thumbnail}"
            }))
        }
        return newAnimeLoadResponse(title, url, TvType.Anime) {
            addEpisodes(DubStatus.Subbed, episodes)
            posterUrl = poster
            plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        loadExtractor(data, subtitleCallback, callback)
        val mId = Regex("xyz/(.*)").find(data)?.groupValues?.get(1)
        if (mId.isNullOrEmpty()) {
            return true
        }
        else {
            val otherUrl = "https://pixeldrain.com/u/$mId"
            loadExtractor(otherUrl, subtitleCallback, callback)
        }
        return true
    }
}
