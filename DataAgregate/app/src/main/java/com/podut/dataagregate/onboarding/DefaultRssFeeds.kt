package com.podut.dataagregate.onboarding

internal object DefaultRssFeeds {

    private val feeds: Map<String, Map<String, List<String>>> = mapOf(
        "AI" to mapOf(
            "en" to listOf(
                "https://artificialintelligence-news.com/feed/",
                "https://feeds.feedburner.com/oreilly/radar",
                "https://openai.com/blog/rss/"
            ),
            "ro" to listOf(
                "https://www.go4it.ro/category/it/feed/",
                "https://www.startupcafe.ro/rss.xml",
                "https://arenait.ro/category/tehnologie/feed/"
            )
        ),
        "Dev" to mapOf(
            "en" to listOf(
                "https://dev.to/feed",
                "https://stackoverflow.blog/feed/",
                "https://www.infoq.com/feed/"
            ),
            "ro" to listOf(
                "https://www.softbinator.com/blog/feed/",
                "https://arenait.ro/category/it/feed/",
                "https://goit.global/ro/blog/feed/",
                "https://www.roweb.ro/blog/feed/"
            )
        ),
        "Tech" to mapOf(
            "en" to listOf(
                "https://techcrunch.com/feed/",
                "https://www.theverge.com/rss/index.xml",
                "https://www.wired.com/feed/rss"
            ),
            "ro" to listOf(
                "https://www.go4it.ro/feed/",
                "https://arenait.ro/feed/",
                "https://gadget.ro/feed/",
                "https://www.nwradu.ro/feed/"
            )
        ),
        "Business" to mapOf(
            "en" to listOf(
                "https://feeds.hbr.org/harvardbusiness",
                "https://www.fastcompany.com/latest/rss"
            ),
            "ro" to listOf(
                "https://www.zf.ro/rss/",
                "https://www.wall-street.ro/rss.xml",
                "https://www.economica.net/rss",
                "https://www.profit.ro/rss"
            )
        ),
        "Security" to mapOf(
            "en" to listOf(
                "https://feeds.feedburner.com/TheHackersNews",
                "https://www.bleepingcomputer.com/feed/",
                "https://krebsonsecurity.com/feed/"
            ),
            "ro" to listOf(
                "https://dnsc.ro/rss",
                "https://arenait.ro/category/securitate/feed/",
                "https://www.go4it.ro/category/internet-securitate/feed/"
            )
        ),
        "Science" to mapOf(
            "en" to listOf(
                "https://www.sciencedaily.com/rss/all.xml",
                "https://feeds.wired.com/wired/category/science"
            ),
            "ro" to listOf(
                "https://www.hotnews.ro/rss/science",
                "https://www.descopera.ro/feed/",
                "https://stiintasitehnica.com/feed/"
            )
        ),
        "Health" to mapOf(
            "en" to listOf(
                "https://feeds.webmd.com/rss/rss.aspx?RSSSource=RSS_PUBLIC",
                "https://www.medicalnewstoday.com/rss"
            ),
            "ro" to listOf(
                "https://www.csid.ro/feed/",
                "https://www.medichub.ro/rss/articole"
            )
        ),
        "Crypto" to mapOf(
            "en" to listOf(
                "https://cointelegraph.com/rss",
                "https://coindesk.com/arc/outboundfeeds/rss/"
            ),
            "ro" to listOf(
                "https://goanadupacripto.ro/feed/",
                "https://cryptoro.com/feed/"
            )
        ),
        "Startups" to mapOf(
            "en" to listOf(
                "https://techcrunch.com/category/startups/feed/",
                "https://feeds.feedburner.com/entrepreneur/latest"
            ),
            "ro" to listOf(
                "https://www.startupcafe.ro/rss.xml",
                "https://startups.ro/feed/",
                "https://start-up.ro/feed/"
            )
        ),
        "Gaming" to mapOf(
            "en" to listOf(
                "https://kotaku.com/rss",
                "https://www.rockpapershotgun.com/feed"
            ),
            "ro" to listOf(
                "https://wasd.ro/feed/",
                "https://www.go4it.ro/category/jocuri/feed/",
                "https://overheat.ro/feed/"
            )
        ),
        "Romania" to mapOf(
            "en" to listOf(
                "https://feeds.digi24.ro/rss/stiri",
                "https://www.hotnews.ro/rss",
                "https://stirileprotv.ro/rss.xml"
            ),
            "ro" to listOf(
                "https://feeds.digi24.ro/rss/stiri",
                "https://www.g4media.ro/feed",
                "https://www.hotnews.ro/rss",
                "https://stirileprotv.ro/rss.xml",
                "https://feeds.digi24.ro/rss/stiri-externe",
                "https://www.mediafax.ro/rss",
                "https://www.adevarul.ro/rss"
            )
        ),
        "Politics" to mapOf(
            "en" to listOf(
                "https://feeds.npr.org/1014/rss.xml",
                "https://rss.politico.com/politics-news.xml"
            ),
            "ro" to listOf(
                "https://feeds.digi24.ro/rss/stiri-politica",
                "https://www.g4media.ro/feed",
                "https://www.hotnews.ro/rss",
                "https://www.mediafax.ro/rss"
            )
        ),
        "Climate" to mapOf(
            "en" to listOf(
                "https://insideclimatenews.org/feed/",
                "https://www.carbonbrief.org/feed"
            ),
            "ro" to listOf(
                "https://green-report.ro/feed/",
                "https://infoclima.ro/feed/"
            )
        ),
        "Space" to mapOf(
            "en" to listOf(
                "https://www.nasa.gov/rss/dyn/breaking_news.rss",
                "https://www.space.com/feeds/all"
            ),
            "ro" to listOf(
                "https://www.descopera.ro/stiinta/feed",
                "https://www.hotnews.ro/rss/science"
            )
        ),
        "Finance" to mapOf(
            "en" to listOf(
                "https://www.marketwatch.com/rss/topstories",
                "https://feeds.bloomberg.com/markets/news.rss"
            ),
            "ro" to listOf(
                "https://www.zf.ro/rss/",
                "https://www.profit.ro/rss",
                "https://www.economica.net/rss",
                "https://www.wall-street.ro/rss.xml"
            )
        ),
        "Design" to mapOf(
            "en" to listOf(
                "https://feeds.feedburner.com/smashingmagazine",
                "https://www.creativebloq.com/rss"
            ),
            "ro" to listOf(
                "https://designist.ro/feed/",
                "https://www.igloo.ro/feed/"
            )
        )
    )


    fun forInterestsAndLanguage(interests: Set<String>, language: String): List<Pair<String, String>> =
        interests.flatMap { cat ->
            val catFeeds = feeds[cat] ?: return@flatMap emptyList<Pair<String, String>>()
            (catFeeds[language] ?: catFeeds["en"] ?: emptyList()).map { cat to it }
        }
}
