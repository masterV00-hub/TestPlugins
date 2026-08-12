package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class AQfilmProvider : MainAPI() {
    override var mainUrl = "https://a.qfilm.tv"
    override var name = "QFilm"
    override val hasMainPage = true
    override var lang = "ar"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: List<HomePageRequest>): HomePageResponse {
        val document = app.get(mainUrl).document
        val items = document.select("ul.pm-ul-browse-videos li").mapNotNull {
            it.toSearchResult()
        }
        return HomePageResponse(listOf(HomePageList("Latest Movies", items)), hasNext = false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3 a")?.text() ?: return null
        val href = mainUrl + this.selectFirst("h3 a")?.attr("href")
        val posterUrl = this.selectFirst("img")?.attr("src")
        return MovieSearchResponse(title, href, this@AQfilmProvider.name, TvType.Movie, posterUrl, null)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search.php?keywords=$query"
        val document = app.get(url).document
        return document.select("ul.pm-ul-browse-videos li").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: ""
        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")
        val plot = document.selectFirst("div.pm-video-description")?.text()
        
        return MovieLoadResponse(
            title,
            url,
            this.name,
            TvType.Movie,
            url,
            poster,
            null,
            plot
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data.replace("watch.php", "play.php")).document
        val script = document.select("script").find { it.data().contains("Playerjs") }?.data() ?: return false
        val file = Regex("""file:"([^"]+)"""").find(script)?.groupValues?.get(1) ?: return false
        
        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                file,
                "",
                Qualities.Unknown.value,
                isM3u8 = file.contains("m3u8")
            )
        )
        return true
    }
}
