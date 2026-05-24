import re
from datetime import datetime, timezone


def clean_html(raw_html: str) -> str:
    if not raw_html:
        return ""
    clean = re.sub(r'<[^>]+>', ' ', raw_html)
    entities = {
        '&nbsp;': ' ', '&amp;': '&', '&lt;': '<', '&gt;': '>',
        '&quot;': '"', '&#8217;': "'", '&#8230;': '...'
    }
    for ent, val in entities.items():
        clean = clean.replace(ent, val)
    return re.sub(r'\s+', ' ', clean).strip()


def google_news_rss_url(query: str, region: str) -> str:
    from urllib.parse import quote_plus
    region_upper = region.upper()
    lang = region.lower()
    return (
        f"https://news.google.com/rss/search"
        f"?q={quote_plus(query)}"
        f"&hl={lang}"
        f"&gl={region_upper}"
        f"&ceid={region_upper}:{lang}"
    )


class FeedRanker:
    """
    Scores and ranks articles using interest boost, recency decay, and diversity penalties.

    Final score = base_score + interest_boost + recency_bonus - cat_penalty - source_penalty
    """

    INTEREST_BOOST_CAT   = 25
    INTEREST_BOOST_TITLE = 15
    INTEREST_BOOST_TAG   = 10
    CAT_PENALTY_PER_ITEM = 5    # applied for each article already picked from same category
    SRC_PENALTY_PER_ITEM = 8    # applied for each article already picked from same source
    RECENCY_MAX_BONUS    = 20   # bonus for articles < 1 hour old
    RECENCY_HALF_LIFE_H  = 12   # bonus halves every 12 hours, goes negative after that

    def rank(self, articles: list[dict], favorites: list[str]) -> list[dict]:
        keywords = [k.lower().strip() for k in favorites if k.strip()]
        scored = [self._compute_score(a, keywords) for a in articles]
        scored.sort(key=lambda x: x["_final"], reverse=True)
        return self._apply_diversity(scored)

    def _compute_score(self, article: dict, keywords: list[str]) -> dict:
        base   = article.get("score", 70)
        boost  = self._interest_boost(article, keywords)
        recency = self._recency_bonus(article)
        article["_final"] = base + boost + recency
        return article

    def _interest_boost(self, a: dict, keywords: list[str]) -> int:
        if not keywords:
            return 0
        title_low = (a.get("title") or "").lower()
        cat_low   = (a.get("category") or "").lower()
        tags      = [t.lower() for t in a.get("tags", [])]
        boost = 0
        for kw in keywords:
            if kw in cat_low:
                boost += self.INTEREST_BOOST_CAT
            if kw in title_low:
                boost += self.INTEREST_BOOST_TITLE
            if any(kw in t for t in tags):
                boost += self.INTEREST_BOOST_TAG
        return boost

    def _recency_bonus(self, a: dict) -> int:
        try:
            pub = (a.get("publish_date") or "").replace(" ", "T")
            dt = datetime.fromisoformat(pub)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=timezone.utc)
            age_hours = (datetime.now(timezone.utc) - dt).total_seconds() / 3600
            bonus = self.RECENCY_MAX_BONUS * (1 - age_hours / self.RECENCY_HALF_LIFE_H)
            return int(bonus)
        except:
            return 0

    def _apply_diversity(self, articles: list[dict]) -> list[dict]:
        cat_counts    = {}
        source_counts = {}
        result = []
        for a in articles:
            cat    = (a.get("category") or "general").lower()
            source = (a.get("source") or "").lower()

            cat_pen    = cat_counts.get(cat, 0) * self.CAT_PENALTY_PER_ITEM
            source_pen = source_counts.get(source, 0) * self.SRC_PENALTY_PER_ITEM

            a["score"] = max(1, min(100, a["_final"] - cat_pen - source_pen))
            cat_counts[cat]       = cat_counts.get(cat, 0) + 1
            source_counts[source] = source_counts.get(source, 0) + 1
            # clean up temp key
            a.pop("_final", None)
            result.append(a)

        return sorted(result, key=lambda x: x["score"], reverse=True)


# Module-level convenience wrapper (keeps existing callers working)
def rank_articles(articles: list, favorite_keywords: list) -> list:
    return FeedRanker().rank(articles, favorite_keywords)


async def scrape_image(url: str) -> str | None:
    """Tries to find a representative image via og:image, twitter:image, or first img tag."""
    import httpx
    from bs4 import BeautifulSoup
    from urllib.parse import urljoin

    try:
        async with httpx.AsyncClient(timeout=5.0, follow_redirects=True) as client:
            r = await client.get(
                url,
                headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
            )
            if r.status_code != 200:
                return None

            soup = BeautifulSoup(r.text, 'html.parser')

            og_img = soup.find("meta", property="og:image") or soup.find("meta", attrs={"name": "og:image"})
            if og_img and og_img.get("content"):
                return urljoin(url, og_img["content"])

            tw_img = soup.find("meta", attrs={"name": "twitter:image"})
            if tw_img and tw_img.get("content"):
                return urljoin(url, tw_img["content"])

            for img in soup.find_all("img"):
                src = img.get("src")
                if not src:
                    continue
                if any(x in src.lower() for x in ["icon", "logo", "avatar", "ads", "track", "pixel"]):
                    continue
                return urljoin(url, src)

    except Exception as e:
        print(f"[Scrape] Error extracting image for {url}: {e}")
    return None
