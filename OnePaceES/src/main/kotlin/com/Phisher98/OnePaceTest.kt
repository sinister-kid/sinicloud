package com.Phisher98

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class OnePaceTest : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://onepace.net"
    override var name = "One Pace Test"
    override val hasMainPage = true
    override var lang = "es"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime)
    override val mainPage = mainPageOf("$mainUrl/es/watch" to "One Pace")


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document
        val scriptElements = document.select("body > script")
        val nodeElements = document.dataNodes()
        if (!scriptElements.isNullOrEmpty()) {
            val data = scriptElements.find({ it.data().contains("romance-dawn") })?.data()
            val scriptText = if (data != null) data else " elements"
            val jString = scriptText.replace("\\\"", "\"")
                .replaceBefore("\"data\":", "")
                .replaceAfterLast("}]\\n\"]", "").toJson()
            val jArcs = parseJson<JsonData>(jString).arcs
            val mainAnimeView = jArcs.map { it.toSearchResult() }
            return newHomePageResponse(request.name, mainAnimeView)
        } else {
            val scriptText = nodeElements.find({ node -> node.wholeData.contains("romance-dawn") })?.wholeData ?: " nodes"
            val jString = scriptText.replace("\\\"", "\"").replaceBefore("\"data\":", "")
                .replaceAfterLast("}]\\n\"]", "")
            val jArcs = parseJson<JsonData>(jString).arcs
            val mainAnimeView = jArcs.map { it.toSearchResult() }
            return newHomePageResponse(request.name, mainAnimeView)
        }

    }

    private fun JsonArc.toSearchResult(): AnimeSearchResponse {
        return newAnimeSearchResponse(title, this.toJson() , TvType.Anime) {
            posterUrl = "https://raw.githubusercontent.com/sinister-kid/sinicloud-data/refs/heads/main/onepace/covers/$slug.png"
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val jArc = parseJson<JsonArc>(url)
        //val seasons = mutableListOf<SeasonData>()
        val episodes = mutableMapOf<DubStatus, List<Episode>>()
        val subEps = mutableListOf<Episode>()
        val dubEps = mutableListOf<Episode>()
        val pdUrl = "https://pixeldrain.com"
        val jAlbum = jArc.album.firstOrNull { it.sub == "es"}
        if (!jArc.album.isEmpty()) {
            //val jAlbum = jArc.album.firstOrNull { it.sub == "es"}
        }
        val albumid = jAlbum?.qualities?.last()
        val media = parseJson<MediaAlbum>(app.get("$pdUrl/api/list/${albumid?.id}").text).files
        media.map { jEp ->
            subEps.add(newEpisode("$pdUrl/u/${jEp.id}", {
                name = jEp.name
                posterUrl = "$pdUrl/api${jEp.thumbnail}"
            })) }
        episodes.set(DubStatus.Subbed, subEps)
//        jArc.album.forEach { arcAlbum ->
//            val albumpair = arcAlbum.qualities.map { album ->
//                Pair(IntToQuality(album.resolution), parseJson<MediaAlbum>(app.get("$pdUrl/api/list/${album.id}").text).files)
//            }
//            var multiId = mutableListOf<MultiId>()
//            if (arcAlbum.dub == "es") {
//                albumpair.forEach{ pair ->
//
//                    //pair.second.forEachIndexed { index, mediaFile ->  multiId[index].idList.add(mediaFile)}
//
//                }
//            }
//            else if (arcAlbum.sub == "es") {
//                albumpair.forEach{ pair ->
//                    //pair.second.forEach { subEps.add(newEpisode(MultiId(pair.first, it).toJson())) }
//                }
//            }
//
//        }
//        episodes[DubStatus.Dubbed] = dubEps
//        episodes[DubStatus.Subbed] = subEps

        return newAnimeLoadResponse(jArc.title , jArc.toJson(), TvType.Anime) {
            this.episodes = episodes
            posterUrl = "https://raw.githubusercontent.com/sinister-kid/sinicloud-data/refs/heads/main/onepace/covers/posteronepace-poster-1.png"
            plot = jArc.description
            backgroundPosterUrl = "https://raw.githubusercontent.com/sinister-kid/sinicloud-data/refs/heads/main/onepace/covers/posteronepace-poster-bg.png"


        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        //callback(ExtractorLink("", "", "", "", Qualities.P720.value))
        //val info = parseJson<JsonArcQuality>(data)

        val mId = Regex("/u/(.*)").find(data)?.groupValues?.get(1)
        //if (apiId.isEmpty()) {
        //    return false
        //}
        //else {
            loadExtractor("https://pd.cybar.xyz/${mId}", subtitleCallback, callback)
            loadExtractor(data, subtitleCallback, callback)
            return true
        //}
    }

    private suspend fun IntToQuality(resolution: Int): Int {
        return when (resolution) {
            480 -> Qualities.P480.value
            720 -> Qualities.P720.value
            1080 -> Qualities.P1080.value
            else -> Qualities.Unknown.value
        }
    }

    data class MultiId(
        val resInt: Int,
        val idList: MutableList<MediaFile>
    )

    data class JsonData(
        @JsonProperty("data") val arcs: Array<JsonArc>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as JsonData

            return arcs.contentEquals(other.arcs)
        }

        override fun hashCode(): Int {
            return arcs.contentHashCode()
        }
    }

    data class JsonArc(
        @JsonProperty("slug") val slug: String,
        @JsonProperty("special") val special: Boolean,
        @JsonProperty("title") val title: String,
        @JsonProperty("description") val description: String,
        @JsonProperty("pixeldrain") val album: Array<JsonArcAlbum>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as JsonArc

            if (slug != other.slug) return false
            if (special != other.special) return false
            if (title != other.title) return false
            if (description != other.description) return false
            if (!album.contentEquals(other.album)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = slug.hashCode()
            result = 31 * result + special.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + album.contentHashCode()
            return result
        }
    }

    data class JsonArcAlbum(
        @JsonProperty("sub") val sub: String,
        @JsonProperty("dub") val dub: String,
        @JsonProperty("variant") val variant: String,
        @JsonProperty("items") val qualities: Array<JsonArcQuality>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as JsonArcAlbum

            if (sub != other.sub) return false
            if (dub != other.dub) return false
            if (variant != other.variant) return false
            if (!qualities.contentEquals(other.qualities)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = sub.hashCode()
            result = 31 * result + dub.hashCode()
            result = 31 * result + variant.hashCode()
            result = 31 * result + qualities.contentHashCode()
            return result
        }
    }

    data class JsonArcQuality(
        @JsonProperty("id") val id: String,
        @JsonProperty("resolution") val resolution: Int
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
