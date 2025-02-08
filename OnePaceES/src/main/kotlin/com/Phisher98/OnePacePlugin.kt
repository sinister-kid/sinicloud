package com.Phisher98

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class OnePaceESPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(OnePaceES())
    }
}
