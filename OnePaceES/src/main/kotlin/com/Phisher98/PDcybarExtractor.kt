package com.Phisher98

import com.lagradost.cloudstream3.app
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
            val resp = app.get(
                    url = url, allowRedirects = true
            )
            val newUrl = resp.url
            callback.invoke (
                ExtractorLink (
                    this.name,
                    this.name,
                    "$newUrl?download",
                    newUrl,
                    Qualities.Unknown.value,
                    )
                )
        } else {
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    "$url?download",
                    url,
                    Qualities.Unknown.value,
                )
            )
        }
    }
}
/*

//val mId = Regex("xyz/(.*)").find(data)?.groupValues?.get(1)
        val document = app.get(
            url = data, allowRedirects = true
        )
        // interceptor = WebViewResolver(Regex(".*\\.workers.dev/api/file/$mId"))
        val destUrl = document.url
        //val hjson = app.head(url = data).headers


 */