package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class ArabicToonsProvider : MainAPI() {
    override var mainUrl = "https://arabic-toons.com"
    override var name = "Arabic Toons"
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
        val href = mainUrl + "/" + this.selectFirst("a")?.attr("href")
        val posterUrl = this.selectFirst("img")?.attr("src")
        return AnimeSearchResponse(title, href, this@ArabicToonsProvider.name, TvType.Anime, posterUrl, null)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search.php?q=$query"
        val document = app.get(url).document
        return document.select("div.item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: ""
        val poster = document.selectFirst("img.cat_img")?.attr("src")
        
        val episodes = document.select("div.episode_item").map {
            val epTitle = it.selectFirst("h4")?.text() ?: ""
            val epHref = mainUrl + "/" + it.selectFirst("a")?.attr("href")
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
        val document = app.get(data).document
        val videoUrl = document.selectFirst("video")?.attr("src") ?: return false
        
        callback.invoke(
            newExtractorLink(
                name = this.name,
                source = this.name,
                url = videoUrl
            ) {
                this.quality = Qualities.Unknown.value
                this.isM3u8 = videoUrl.contains("m3u8")
            }
        )
        return true
    }
}