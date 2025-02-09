package com.Phisher98

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

open class PDcybarExtractor : ExtractorApi() {
    override val name            = "PDcybar"
    override val mainUrl         = "https://pd.cybar.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val mId = Regex("xyz/(.*)").find(url)?.groupValues?.get(1)
        if (mId.isNullOrEmpty()) {
            callback.invoke (
                ExtractorLink (
                    this.name,
                    this.name,
                    url,
                    url,
                    Qualities.Unknown.value,
                    )
                )
        } else {
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    "$mainUrl/${mId}?download",
                    url,
                    Qualities.Unknown.value,
                )
            )
        }
    }
}