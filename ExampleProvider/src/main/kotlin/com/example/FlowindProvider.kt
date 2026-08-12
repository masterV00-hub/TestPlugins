package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class FlowindProvider : MainAPI() {
    override var mainUrl = "https://flowind.net"
    override var name = "Flowind"
    override val hasMainPage = true
    override var lang = "ar"
    override val supportedTypes = setOf(TvType.Anime, TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: List<HomePageRequest>): HomePageResponse {
        val document = app.get(mainUrl).document
        val items = document.select("div.item").mapNotNull {
            it.toSearchResult()
        }
        return HomePageResponse(listOf(HomePageList("Latest Anime", items)), hasNext = false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("src")
        return AnimeSearchResponse(title, href, this@FlowindProvider.name, TvType.Anime, posterUrl, null)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        return document.select("div.item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: ""
        val poster = document.selectFirst("img.show_img")?.attr("src")
        
        val episodes = document.select("div.episode_item").map {
            val epTitle = it.selectFirst("h4")?.text() ?: ""
            val epHref = it.selectFirst("a")?.attr("href") ?: ""
            Episode(epHref, epTitle)
        }
        
        return AnimeLoadResponse(
            title,
            url,
            this.name,
            TvType.Anime,
            url,
            poster,
            null,
            null,
            null,
            episodes
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Flowind often links directly to external players like Videa
        if (data.contains("videa.hu")) {
            val id = data.substringAfter("videa.hu/videok/").substringBefore("-")
            callback.invoke(
                ExtractorLink(
                    "Videa",
                    "Videa",
                    "https://videa.hu/videok/film-animacio/$id",
                    "",
                    Qualities.Unknown.value,
                    isM3u8 = false
                )
            )
            return true
        }
        return false
    }
}
