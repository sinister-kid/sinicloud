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
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor

class OnePaceTest : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://onepace.net"
    override var name = "One Pace Test"
    override val hasMainPage = true
    override var lang = "es"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime)
    override val mainPage = mainPageOf("$mainUrl/$lang/watch" to "One Pace")


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document
        val scriptElements = document.select("body > script")
        val scriptText = scriptElements.find { it.data().contains("romance-dawn") }
                        ?.dataNodes()?.first()?.wholeData ?: emptyJsonData().toString()
        val jString = scriptText.replace("\\\"", "\"")
                                .replaceBefore("{\"data\":", "")
                                .replaceAfterLast("}]}", "").toJson()
        val jArcs = parseJson<JsonData>(jString).arcs
        val mainAnimeView = jArcs.map { it.toSearchResult() }
        return newHomePageResponse(request.name, mainAnimeView)
    }

    private fun emptyJsonData(): JsonData {
        val emptyDataArc = DataArc("empty", false, "empty", "empty", emptyArray<DataArcVariant>())
        return JsonData(arrayOf(emptyDataArc))
    }

    private fun DataArc.toSearchResult(): AnimeSearchResponse {
        return newAnimeSearchResponse(title, this.toJson() , TvType.Anime) {
            posterUrl = "https://raw.githubusercontent.com/sinister-kid/sinicloud-data/refs/heads/main/onepace/covers/$slug.png"
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val jArc = parseJson<DataArc>(url)
        //val seasons = mutableListOf<SeasonData>()
        val episodes = mutableMapOf<DubStatus, List<Episode>>()
        val subEps = mutableListOf<Episode>()
        val dubEps = mutableListOf<Episode>()
        val pdUrl = "https://pixeldrain.com"
        if (jArc.variant?.isNotEmpty()!!) {
            val jArcExtended = jArc.variant.firstOrNull { (it.sub == lang) && (it.dub == "ja") && (it.variant == "extended")}
            val jArcVar = jArcExtended ?: jArc.variant.firstOrNull { (it.sub == lang) && (it.dub == "ja") }
            val mIdEpisodes = mutableListOf<Array<MediaFile>>()
            val mIdQualities = mutableListOf<Int>()
            jArcVar?.qualities?.forEach { pdAlbum ->
                val mediaFiles = parseJson<MediaAlbum>(app.get("$pdUrl/api/list/${pdAlbum.id}").text).files
                mIdEpisodes.add(mediaFiles)
                mIdQualities.add(pdAlbum.resolution)
            }
            val episodesData = mutableListOf<EpisodeData>()
            mIdEpisodes.forEachIndexed{ qIndex, arr ->
                arr.forEachIndexed { eIndex, eMedia ->
                    if (qIndex == 1) {
                        val episodesPair = mutableListOf<Pair<Int, String>>()
                        episodesData.add(EpisodeData(pdUrl, eIndex.toString(), episodesPair))
                    }
                    episodesData[eIndex-1].resid.add(Pair(mIdQualities[qIndex-1], eMedia.id))
                }
            }
            episodesData.forEach { dEp -> subEps.add(newEpisode(dEp.toJson())) }
            episodes[DubStatus.Subbed] = subEps
        }

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
            posterUrl = "https://raw.githubusercontent.com/sinister-kid/sinicloud-data/refs/heads/main/onepace/covers/onepace-poster-1.png"
            plot = jArc.description
            backgroundPosterUrl = "https://raw.githubusercontent.com/sinister-kid/sinicloud-data/refs/heads/main/onepace/covers/onepace-poster-bg.png"


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
        val eData =  parseJson<EpisodeData>(data)
        eData.resid.forEach{e ->
            callback.invoke(ExtractorLink(
                "PDcybar",
                "PDcybar",
                "https://pd.cybar.xyz/${e.second}",
                "https://pd.cybar.xyz",
                IntToQuality(e.first)
            ))
            callback.invoke(ExtractorLink(
                "Pixeldrain",
                "Pixeldrain",
                "${eData.mainUrl}/u/${e.second}",
                eData.mainUrl,
                IntToQuality(e.first)
            ))
        }
        //val mId = Regex("/u/(.*)").find(data)?.groupValues?.get(1)
        //if (apiId.isEmpty()) {
        //    return false
        //}
        //else {
            //loadExtractor("https://pd.cybar.xyz/${mId}", subtitleCallback, callback)
            //loadExtractor(data, subtitleCallback, callback)
            return true
        //}
    }

    private fun IntToQuality(resolution: Int): Int {
        return when (resolution) {
            480 -> Qualities.P480.value
            720 -> Qualities.P720.value
            1080 -> Qualities.P1080.value
            else -> Qualities.Unknown.value
        }
    }

    data class EpisodeData(
        val mainUrl: String,
        val name: String?,
        val resid: MutableList<Pair<Int, String>>
    )


    data class JsonData(
        @JsonProperty("data") val arcs: Array<DataArc>
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

    data class DataArc(
        @JsonProperty("slug") val slug: String,
        @JsonProperty("special") val special: Boolean,
        @JsonProperty("title") val title: String,
        @JsonProperty("description") val description: String,
        @JsonProperty("pixeldrain") val variant: Array<DataArcVariant>?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DataArc

            if (slug != other.slug) return false
            if (special != other.special) return false
            if (title != other.title) return false
            if (description != other.description) return false
            if (!variant.contentEquals(other.variant)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = slug.hashCode()
            result = 31 * result + special.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + variant.contentHashCode()
            return result
        }
    }

    data class DataArcVariant(
        @JsonProperty("sub") val sub: String,
        @JsonProperty("dub") val dub: String,
        @JsonProperty("variant") val variant: String,
        @JsonProperty("items") val qualities: Array<DaraArcQuality>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DataArcVariant

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

    data class DaraArcQuality(
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
