import json
from typing import Dict, List, Optional

from fastapi import APIRouter, HTTPException, BackgroundTasks, Query
from database import db_conn, PLACEHOLDER
from models import (
    CategoryCreate, RssFeedRequest, SerpConfigUpdate,
    CategoryResponse, FeedInfoResponse, SerpInfo, StatusResponse,
)
from serp_service import fetch_for_category
from config import USE_MYSQL, USE_POSTGRES, SERP_API_KEY
from logging_config import get_logger

logger = get_logger(__name__)
router = APIRouter(prefix="/categories", tags=["Categories"])

DEFAULT_FEEDS: Dict[str, List[str]] = {
    "AI":       [
        "https://www.go4it.ro/category/it/feed/",
        "https://www.startupcafe.ro/rss.xml",
        "https://arenait.ro/category/tehnologie/feed/",
    ],
    "Dev":      [
        "https://www.softbinator.com/blog/feed/",
        "https://arenait.ro/category/it/feed/",
        "https://goit.global/ro/blog/feed/",
        "https://www.roweb.ro/blog/feed/",
    ],
    "Tech":     [
        "https://www.go4it.ro/feed/",
        "https://arenait.ro/feed/",
        "https://gadget.ro/feed/",
        "https://www.nwradu.ro/feed/",
    ],
    "Security": [
        "https://dnsc.ro/rss",
        "https://arenait.ro/category/securitate/feed/",
        "https://www.go4it.ro/category/internet-securitate/feed/",
    ],
    "Business": [
        "https://www.zf.ro/rss/",
        "https://www.wall-street.ro/rss.xml",
        "https://www.economica.net/rss",
        "https://www.profit.ro/rss",
    ],
    "Science":  [
        "https://www.hotnews.ro/rss/science",
        "https://www.descopera.ro/feed/",
        "https://stiintasitehnica.com/feed/",
    ],
    "Startups": [
        "https://www.startupcafe.ro/rss.xml",
        "https://startups.ro/feed/",
        "https://start-up.ro/feed/",
    ],
    "Finance":  [
        "https://www.zf.ro/rss/",
        "https://www.profit.ro/rss",
        "https://www.economica.net/rss",
        "https://www.wall-street.ro/rss.xml",
    ],
    "Crypto":   [
        "https://goanadupacripto.ro/feed/",
        "https://cryptoro.com/feed/",
    ],
    "Gaming":   [
        "https://wasd.ro/feed/",
        "https://www.go4it.ro/category/jocuri/feed/",
        "https://overheat.ro/feed/",
    ],
    "Politics": [
        "https://feeds.digi24.ro/rss/stiri-politica",
        "https://www.g4media.ro/feed",
        "https://www.hotnews.ro/rss",
        "https://www.mediafax.ro/rss",
    ],
    "Health":   [
        "https://www.csid.ro/feed/",
        "https://www.medichub.ro/rss/articole",
    ],
    "Climate":  [
        "https://green-report.ro/feed/",
        "https://infoclima.ro/feed/",
    ],
    "Space":    [
        "https://www.descopera.ro/stiinta/feed",
        "https://www.hotnews.ro/rss/science",
    ],
    "Romania":  [
        "https://feeds.digi24.ro/rss/stiri",
        "https://www.g4media.ro/feed",
        "https://www.hotnews.ro/rss",
        "https://stirileprotv.ro/rss.xml",
        "https://feeds.digi24.ro/rss/stiri-externe",
        "https://www.mediafax.ro/rss",
        "https://www.adevarul.ro/rss",
    ],
    "Design":   [
        "https://designist.ro/feed/",
        "https://www.igloo.ro/feed/",
    ],
}



def _get_feed_id(cursor, url: str) -> int | None:
    """Returns the id of a feed by URL, or None if not found."""
    cursor.execute(f"SELECT id FROM rss_feeds WHERE url = {PLACEHOLDER}", (url,))
    row = cursor.fetchone()
    return row[0] if row else None


def _upsert_feed(cursor, url: str, category: str) -> int:
    """
    Inserts the URL into rss_feeds if it doesn't exist yet (global dedup).
    Returns the feed id.
    """
    if USE_POSTGRES:
        cursor.execute(
            "INSERT INTO rss_feeds (url, category) VALUES (%s, %s) ON CONFLICT (url) DO NOTHING",
            (url, category)
        )
        cursor.execute("SELECT id FROM rss_feeds WHERE url = %s", (url,))
    else:
        prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
        cursor.execute(
            f"{prefix} INTO rss_feeds (url, category) VALUES ({PLACEHOLDER}, {PLACEHOLDER})",
            (url, category)
        )
        cursor.execute(f"SELECT id FROM rss_feeds WHERE url = {PLACEHOLDER}", (url,))
    return cursor.fetchone()[0]


def _upsert_subscription(cursor, device_id: str, feed_id: int, is_active: bool = True):
    """Subscribes device_id to feed_id (or re-activates if already exists)."""
    active = is_active if USE_POSTGRES else (1 if is_active else 0)
    if USE_POSTGRES:
        cursor.execute(
            "INSERT INTO user_feed_subscriptions (device_id, feed_id, is_active) "
            "VALUES (%s, %s, %s) ON CONFLICT (device_id, feed_id) DO UPDATE SET is_active = %s",
            (device_id, feed_id, is_active, is_active)
        )
    elif USE_MYSQL:
        cursor.execute(
            "INSERT INTO user_feed_subscriptions (device_id, feed_id, is_active) VALUES (%s, %s, %s) "
            "ON DUPLICATE KEY UPDATE is_active = %s",
            (device_id, feed_id, active, active)
        )
    else:
        cursor.execute(
            "INSERT INTO user_feed_subscriptions (device_id, feed_id, is_active) VALUES (?, ?, ?) "
            "ON CONFLICT(device_id, feed_id) DO UPDATE SET is_active = ?",
            (device_id, feed_id, active, active)
        )


@router.get("/defaults", response_model=Dict[str, List[str]])
def get_default_feeds(categories: Optional[str] = Query(None)) -> Dict[str, List[str]]:
    if not categories:
        return {}
    cat_list = [c.strip() for c in categories.split(",") if c.strip()]
    return {
        cat: DEFAULT_FEEDS[matched]
        for cat in cat_list
        if (matched := next((k for k in DEFAULT_FEEDS if k.lower() == cat.lower()), None))
    }


@router.get("", response_model=list[CategoryResponse])
def list_categories(deviceId: Optional[str] = Query(None)):
    dev_id     = deviceId or "global"
    active_val = True if USE_POSTGRES else 1

    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()

        # Categories where this device has subscriptions
        cursor.execute(
            f"SELECT DISTINCT f.category FROM rss_feeds f "
            f"JOIN user_feed_subscriptions s ON f.id = s.feed_id "
            f"WHERE s.device_id = {PLACEHOLDER} AND s.is_active = {PLACEHOLDER} AND f.is_active = {PLACEHOLDER}",
            (dev_id, active_val, active_val)
        )
        cats = {row[0] for row in cursor.fetchall()}

        # Also include categories from SERP config
        cursor.execute(
            f"SELECT category FROM category_config "
            f"WHERE (device_id = {PLACEHOLDER} OR device_id = 'global' OR device_id IS NULL)",
            (dev_id,)
        )
        cats |= {row[0] for row in cursor.fetchall()}

        result = []
        for cat in cats:
            # Feeds this user is subscribed to in this category
            cursor.execute(
                f"SELECT f.url, s.is_active FROM rss_feeds f "
                f"JOIN user_feed_subscriptions s ON f.id = s.feed_id "
                f"WHERE f.category = {PLACEHOLDER} AND s.device_id = {PLACEHOLDER}",
                (cat, dev_id)
            )
            feeds = [FeedInfoResponse(url=r[0], is_active=bool(r[1])) for r in cursor.fetchall()]

            cursor.execute(
                f"SELECT COUNT(*) FROM pending_articles WHERE category = {PLACEHOLDER}", (cat,)
            )
            count = cursor.fetchone()[0]

            cursor.execute(
                f"SELECT serp_query, serp_region, serp_enabled FROM category_config "
                f"WHERE category = {PLACEHOLDER} AND (device_id = {PLACEHOLDER} OR device_id = 'global' OR device_id IS NULL)",
                (cat, dev_id)
            )
            sr = cursor.fetchone()
            result.append(CategoryResponse(
                name=cat, feeds=feeds, article_count=count,
                serp=SerpInfo(
                    query=sr[0] if sr else None,
                    region=sr[1] if sr else "us",
                    enabled=bool(sr[2]) if sr else False,
                )
            ))

    return result


@router.post("", response_model=StatusResponse)
def create_category(body: CategoryCreate):
    dev_id = body.deviceId or "global"
    with db_conn() as conn:
        cursor = conn.cursor()
        if USE_POSTGRES:
            cursor.execute(
                "INSERT INTO category_config (category, device_id) VALUES (%s, %s) ON CONFLICT (category, device_id) DO NOTHING",
                (body.name, dev_id)
            )
        else:
            prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
            cursor.execute(
                f"{prefix} INTO category_config (category, device_id) VALUES ({PLACEHOLDER}, {PLACEHOLDER})",
                (body.name, dev_id)
            )

        # Seed explicit feeds from request
        for url in body.rss_urls:
            if url.strip():
                feed_id = _upsert_feed(cursor, url.strip(), body.name)
                _upsert_subscription(cursor, dev_id, feed_id)

        # Auto-seed default feeds if user has no subscriptions yet for this category
        cursor.execute(
            f"SELECT COUNT(*) FROM rss_feeds f "
            f"JOIN user_feed_subscriptions s ON f.id = s.feed_id "
            f"WHERE f.category = {PLACEHOLDER} AND s.device_id = {PLACEHOLDER}",
            (body.name, dev_id)
        )
        has_feeds = cursor.fetchone()[0] > 0

        if not has_feeds:
            matched_key = next((k for k in DEFAULT_FEEDS if k.lower() == body.name.lower()), None)
            if matched_key:
                for url in DEFAULT_FEEDS[matched_key]:
                    feed_id = _upsert_feed(cursor, url, matched_key)
                    _upsert_subscription(cursor, dev_id, feed_id)
                logger.info("[Category] Auto-seeded %d default feeds for '%s' (device=%s)",
                            len(DEFAULT_FEEDS[matched_key]), body.name, dev_id)

    return {"status": "created"}


@router.delete("/{category}", response_model=StatusResponse)
def delete_category(category: str, deviceId: Optional[str] = Query(None)):
    dev_id = deviceId or "global"
    with db_conn() as conn:
        cursor = conn.cursor()
        # Remove user's subscriptions in this category
        cursor.execute(
            f"DELETE FROM user_feed_subscriptions "
            f"WHERE device_id = {PLACEHOLDER} "
            f"AND feed_id IN (SELECT id FROM rss_feeds WHERE category = {PLACEHOLDER})",
            (dev_id, category)
        )
        if USE_POSTGRES:
            cursor.execute(
                "DELETE FROM category_config WHERE category = %s AND device_id = %s",
                (category, dev_id)
            )
        else:
            cursor.execute(
                f"DELETE FROM category_config WHERE category = {PLACEHOLDER} AND device_id = {PLACEHOLDER}",
                (category, dev_id)
            )
    return {"status": "deleted"}


# ─── Feeds per category ───────────────────────────────────────────────────────

@router.post("/{category}/feeds", response_model=StatusResponse)
def add_feed(category: str, body: RssFeedRequest):
    dev_id = body.deviceId or "global"
    with db_conn() as conn:
        cursor = conn.cursor()
        # Ensure category_config row exists for SERP management
        if USE_POSTGRES:
            cursor.execute(
                "INSERT INTO category_config (category, device_id) VALUES (%s, %s) ON CONFLICT (category, device_id) DO NOTHING",
                (category, dev_id)
            )
        else:
            prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
            cursor.execute(
                f"{prefix} INTO category_config (category, device_id) VALUES ({PLACEHOLDER}, {PLACEHOLDER})",
                (category, dev_id)
            )
        # Upsert global feed + create user subscription
        feed_id = _upsert_feed(cursor, body.url, category)
        _upsert_subscription(cursor, dev_id, feed_id, is_active=True)
    logger.info("[API] Feed added: %s → %s (device=%s)", body.url, category, dev_id)
    return {"status": "added"}


@router.put("/{category}/feeds/toggle")
def toggle_feed(category: str, body: RssFeedRequest):
    dev_id = body.deviceId or "global"
    with db_conn() as conn:
        cursor = conn.cursor()
        feed_id = _get_feed_id(cursor, body.url)
        if feed_id is None:
            raise HTTPException(404, "Feed not found")

        cursor.execute(
            f"SELECT is_active FROM user_feed_subscriptions "
            f"WHERE device_id = {PLACEHOLDER} AND feed_id = {PLACEHOLDER}",
            (dev_id, feed_id)
        )
        row = cursor.fetchone()
        if not row:
            raise HTTPException(404, "Subscription not found")

        if body.is_active is not None:
            new_state = body.is_active
        else:
            new_state = not row[0] if USE_POSTGRES else (row[0] == 0)

        sql_state = new_state if USE_POSTGRES else (1 if new_state else 0)
        cursor.execute(
            f"UPDATE user_feed_subscriptions SET is_active = {PLACEHOLDER} "
            f"WHERE device_id = {PLACEHOLDER} AND feed_id = {PLACEHOLDER}",
            (sql_state, dev_id, feed_id)
        )
    logger.info("[API] Feed %s toggled to %s (device=%s)", body.url, new_state, dev_id)
    return {"status": "toggled", "is_active": bool(new_state)}


@router.delete("/{category}/feeds", response_model=StatusResponse)
def remove_feed(category: str, body: RssFeedRequest):
    dev_id = body.deviceId or "global"
    with db_conn() as conn:
        cursor = conn.cursor()
        feed_id = _get_feed_id(cursor, body.url)
        if feed_id:
            cursor.execute(
                f"DELETE FROM user_feed_subscriptions "
                f"WHERE device_id = {PLACEHOLDER} AND feed_id = {PLACEHOLDER}",
                (dev_id, feed_id)
            )
            # If no one else is subscribed, deactivate the global feed
            cursor.execute(
                f"SELECT COUNT(*) FROM user_feed_subscriptions WHERE feed_id = {PLACEHOLDER}",
                (feed_id,)
            )
            if cursor.fetchone()[0] == 0:
                active_false = False if USE_POSTGRES else 0
                cursor.execute(
                    f"UPDATE rss_feeds SET is_active = {PLACEHOLDER} WHERE id = {PLACEHOLDER}",
                    (active_false, feed_id)
                )
    return {"status": "removed"}


# ─── SERP ─────────────────────────────────────────────────────────────────────

@router.put("/{category}/serp", response_model=StatusResponse)
def update_serp_config(category: str, body: SerpConfigUpdate):
    dev_id = body.deviceId or "global"
    with db_conn() as conn:
        cursor = conn.cursor()
        if USE_POSTGRES:
            cursor.execute(
                "INSERT INTO category_config (category, device_id) VALUES (%s, %s) ON CONFLICT (category, device_id) DO NOTHING",
                (category, dev_id)
            )
        else:
            prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
            cursor.execute(
                f"{prefix} INTO category_config (category, device_id) VALUES ({PLACEHOLDER}, {PLACEHOLDER})",
                (category, dev_id)
            )
        cursor.execute(
            f"UPDATE category_config SET serp_query = {PLACEHOLDER}, serp_region = {PLACEHOLDER}, serp_enabled = {PLACEHOLDER} "
            f"WHERE category = {PLACEHOLDER} AND device_id = {PLACEHOLDER}",
            (body.query, body.region, 1 if body.enabled else 0, category, dev_id)
        )
    return {"status": "updated"}


@router.post("/{category}/serp/fetch")
async def fetch_serp(category: str, background_tasks: BackgroundTasks):
    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()
        cursor.execute(
            f"SELECT serp_query, serp_region, serp_enabled FROM category_config WHERE category = {PLACEHOLDER}",
            (category,)
        )
        row = cursor.fetchone()

    if not row or not row[2]:
        raise HTTPException(400, "SERP not enabled for this category")
    if not row[0]:
        raise HTTPException(400, "No SERP query configured")

    query, region = row[0], row[1] or "us"
    background_tasks.add_task(fetch_for_category, category, query, region)
    return {"status": "fetch started", "mode": "serpapi" if SERP_API_KEY else "google_news_rss"}
