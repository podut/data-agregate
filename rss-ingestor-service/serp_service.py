"""
SERP / fallback article fetching logic.
Tries SerpAPI first, falls back to Google News RSS + category-specific feeds.
"""
import asyncio
import feedparser
import httpx

from config import SERP_API_KEY, USE_MYSQL, USE_POSTGRES
from database import db_conn, PLACEHOLDER
from utils import clean_html, google_news_rss_url
from ai_service import enrich_pending_articles
import cache_service
from logging_config import get_logger

logger = get_logger(__name__)

# ─── Category-aware fallback sources ─────────────────────────────────────────
# Only sources relevant to a category are fetched, reducing noise and DB pollution.

_CATEGORY_RSS_SOURCES: dict[str, list[str]] = {
    "_default": [
        "google_news",
        "https://feeds.bbci.co.uk/news/rss.xml",
        "https://feeds.reuters.com/reuters/topNews",
        "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
    ],
    "tech": [
        "google_news",
        "https://techcrunch.com/feed/",
        "https://www.theverge.com/rss/index.xml",
        "https://feeds.arstechnica.com/arstechnica/index",
        "https://hnrss.org/frontpage",
        "https://www.wired.com/feed/rss",
        "https://www.zdnet.com/news/rss.xml",
        "https://feeds.bbci.co.uk/news/technology/rss.xml",
        "https://www.cnet.com/rss/news/",
        "https://www.engadget.com/rss.xml",
        "https://www.venturebeat.com/feed/",
        "https://www.thenextweb.com/feed/",
        "https://www.startupcafe.ro/rss.xml",
        "https://www.go4it.ro/feed/",
    ],
    "business": [
        "google_news",
        "https://feeds.bbci.co.uk/news/business/rss.xml",
        "https://feeds.reuters.com/reuters/businessNews",
        "https://www.economica.net/rss",
        "https://www.profit.ro/rss",
        "https://www.zf.ro/rss",
        "https://www.biziday.ro/feed/",
    ],
    "science": [
        "google_news",
        "https://www.hotnews.ro/rss/science",
        "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
        "https://feeds.reuters.com/reuters/scienceNews",
        "https://www.newscientist.com/feed/home/",
    ],
    "health": [
        "google_news",
        "https://feeds.bbci.co.uk/news/health/rss.xml",
        "https://feeds.reuters.com/reuters/healthNews",
    ],
    "sport": [
        "google_news",
        "https://feeds.bbci.co.uk/sport/rss.xml",
        "https://feeds.reuters.com/reuters/sportsNews",
        "https://www.digi24.ro/rss/stiri/sport",
    ],
    "ro": [
        "google_news",
        "https://www.digi24.ro/rss/stiri",
        "https://www.hotnews.ro/rss",
        "https://www.biziday.ro/feed/",
        "https://www.startupcafe.ro/rss.xml",
    ],
}


def _get_sources_for_category(category: str) -> list[str]:
    cat_lower = category.lower()
    for key in _CATEGORY_RSS_SOURCES:
        if key != "_default" and key in cat_lower:
            return _CATEGORY_RSS_SOURCES[key]
    ro_keywords = ["stiri", "știri", "politic", "sport", "romania", "românia"]
    if any(k in cat_lower for k in ro_keywords):
        return _CATEGORY_RSS_SOURCES["ro"]
    return _CATEGORY_RSS_SOURCES["_default"]


# ─── Public entry point ───────────────────────────────────────────────────────

async def fetch_for_category(category: str, query: str, region: str):
    """Tries SerpAPI, falls back to Google News RSS + category-relevant feeds."""
    if SERP_API_KEY:
        await _fetch_via_serpapi(category, query, region)
    else:
        await _fetch_via_google_news_rss(category, query, region)


# ─── SerpAPI ──────────────────────────────────────────────────────────────────

async def _fetch_via_serpapi(category: str, query: str, region: str):
    try:
        async with httpx.AsyncClient(timeout=15) as client:
            resp = await client.get(
                "https://serpapi.com/search",
                params={
                    "engine":  "google_news",
                    "q":       query,
                    "gl":      region.upper(),
                    "hl":      region.lower(),
                    "api_key": SERP_API_KEY,
                }
            )
            data = resp.json()
    except Exception as e:
        logger.warning("[SERP] SerpAPI error: %s — falling back to Google News RSS", e)
        await _fetch_via_google_news_rss(category, query, region)
        return

    news_results = data.get("news_results", [])
    if not news_results:
        logger.info("[SERP] No results from SerpAPI — falling back")
        await _fetch_via_google_news_rss(category, query, region)
        return

    articles = []
    for item in news_results:
        url = item.get("link", "")
        if not url:
            continue
        source = item.get("source", {})
        source_name = source.get("name", "SerpAPI") if isinstance(source, dict) else str(source)
        articles.append((
            item.get("title", ""), url, category,
            item.get("snippet", ""), source_name, item.get("thumbnail")
        ))

    await _insert_articles(articles)
    logger.info("[SERP] Inserted %d articles via SerpAPI for '%s'", len(articles), category)


# ─── Google News RSS + Category Feeds ────────────────────────────────────────

async def _fetch_via_google_news_rss(category: str, query: str, region: str):
    clean_query = query.replace("&", " ").replace("+", " ").replace("  ", " ").strip()
    sources = _get_sources_for_category(category)
    all_articles = []

    for source in sources:
        try:
            is_google = (source == "google_news")
            if is_google:
                feed_url    = google_news_rss_url(clean_query, region)
                source_name = "Google News"
            else:
                feed_url    = source
                source_name = source.split("/")[2].replace("feeds.", "").replace("www.", "")

            feed = await asyncio.to_thread(feedparser.parse, feed_url)
            count = 0
            for entry in feed.entries[:15]:
                url = entry.get("link", "")
                if not url:
                    continue

                title   = clean_html(entry.get("title", ""))
                summary = clean_html(entry.get("summary", "") + " " + entry.get("description", ""))
                if len(summary) > 500:
                    summary = summary[:500] + "..."

                src = clean_html(
                    entry.get("source", {}).get("title", source_name)
                    if isinstance(entry.get("source"), dict) else source_name
                )

                if is_google or (clean_query.lower() in title.lower() or clean_query.lower() in summary.lower()):
                    all_articles.append((title, url, category, summary, src, None))
                    count += 1

            logger.debug("[SERP fallback] %s: %d articles for '%s'", source_name, count, category)
        except Exception as e:
            logger.warning("[SERP fallback] Error at %s: %s", source, e)

    if all_articles:
        await _insert_articles(all_articles)
        logger.info("[SERP fallback] Total inserted: %d articles for '%s'", len(all_articles), category)


# ─── Insert + Enrich ─────────────────────────────────────────────────────────

async def _insert_articles(articles: list):
    if not articles:
        return

    unique = []
    for a in articles:
        url, title = a[1], a[0]
        if cache_service.is_url_seen(url):
            continue
        if cache_service.is_title_duplicate(title):
            logger.debug("[SERP] Skipping duplicate title: %s", title[:60])
            continue
        unique.append(a)
        cache_service.mark_url_processed(url)
        cache_service.mark_title_processed(title)

    if not unique:
        return

    with db_conn() as conn:
        cursor = conn.cursor()
        if USE_POSTGRES:
            cursor.executemany(
                'INSERT INTO pending_articles (title, url, category, raw_content, source_name, image_url) '
                'VALUES (%s, %s, %s, %s, %s, %s) ON CONFLICT (url) DO NOTHING',
                unique
            )
        else:
            prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
            cursor.executemany(
                f'{prefix} INTO pending_articles (title, url, category, raw_content, source_name, image_url) '
                f'VALUES ({PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER})',
                unique
            )

    categories = {a[2] for a in unique if a[2]}
    for cat in categories:
        await enrich_pending_articles(cat)
