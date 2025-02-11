package com.Phisher98

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

class OnePaceES : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://rentry.org"
    override var name = "One Pace ES"
    override val hasMainPage = true
    override var lang = "es"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime)
    override val mainPage = mainPageOf("${mainUrl}/onepaces/" to "OnePace")

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

    override suspend fun load(url: String): LoadResponse {
        val title = url.substringAfterLast("/")
        val document = app.get("https://rentry.org/onepaces").document
        val poster = document.selectFirst("img")?.attr("src") ?: "https://images3.alphacoders.com/134/1342304.jpeg"
        val episodes = mutableListOf<Episode>()
        val elements= document.selectFirst("article div h3:contains($title)")
        val description = elements?.nextElementSibling()?.nextElementSibling()?.selectFirst("p")?.text()
        var PTag = elements
        repeat(6) {
            PTag = PTag?.nextElementSibling() // Move to the next sibling 5 times
        }
        PTag?.select("div.ntable-wrapper td")?.map { Ep ->
            val href= Ep.selectFirst("a")?.attr("href") ?: ""
            if (href.isNotEmpty()) {
                val name = Ep.selectFirst("a")?.text()
                val ep = newEpisode(url = href, {
                    this.name = name
                    posterUrl = poster
                })
                episodes.add(ep)
            }
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
        return true
    }
}
