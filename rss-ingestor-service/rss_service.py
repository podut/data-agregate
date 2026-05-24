import asyncio
import feedparser
import time
from datetime import datetime, timedelta, timezone
from typing import List

from database import db_conn, PLACEHOLDER
from utils import clean_html
from ai_service import enrich_pending_articles
from config import USE_MYSQL, USE_POSTGRES
import cache_service
from logging_config import get_logger

logger = get_logger(__name__)


def _parse_image_from_entry(entry) -> str | None:
    if 'media_content' in entry and entry.media_content:
        return entry.media_content[0].get('url')
    for link in entry.get('links', []):
        if 'image' in link.get('type', ''):
            return link.get('href')
    return None


async def process_feeds_task(urls: List[str], category: str, device_id: str = None):
    lock_key = f"process:{category}"
    if not cache_service.set_processing_lock(lock_key, timeout=600):
        logger.info("[RSS] Category '%s' already being processed. Skipping.", category)
        return

    try:
        all_pending = []
        now = datetime.now(timezone.utc)
        cutoff = now - timedelta(days=7)

        for feed_url in urls:
            try:
                feed = await asyncio.to_thread(feedparser.parse, feed_url)
                source_title = clean_html(feed.feed.get("title", feed_url))

                for entry in feed.entries:
                    published_at = None
                    published_parsed = entry.get("published_parsed")
                    if published_parsed:
                        dt = datetime.fromtimestamp(time.mktime(published_parsed), timezone.utc)
                        if dt < cutoff:
                            continue
                        published_at = dt.strftime('%Y-%m-%d %H:%M:%S')

                    url = entry.get("link", "")
                    if not url:
                        continue
                    if cache_service.is_url_seen(url):
                        continue

                    title = clean_html(entry.get("title", "No Title"))

                    # Title-based deduplication: skip near-identical titles from different sources
                    if cache_service.is_title_duplicate(title):
                        logger.debug("[RSS] Skipping duplicate title: %s", title[:60])
                        continue

                    cache_service.mark_url_processed(url)
                    cache_service.mark_title_processed(title)

                    raw_body = entry.get("summary", "") + " " + entry.get("description", "")
                    summary = clean_html(raw_body)
                    if len(summary) > 400:
                        summary = summary[:400] + "..."

                    image_url = _parse_image_from_entry(entry)
                    ts = published_at or now.strftime('%Y-%m-%d %H:%M:%S')
                    all_pending.append((title, url, category, summary, source_title, image_url, ts))

                    if len(all_pending) >= 50:
                        _insert_batch(all_pending)
                        all_pending = []

            except Exception as e:
                logger.error("[RSS] Error syncing %s: %s", feed_url, e)

        if all_pending:
            _insert_batch(all_pending)

        await enrich_pending_articles(category)

    finally:
        cache_service.release_processing_lock(lock_key)


def _insert_batch(batch: list):
    with db_conn() as conn:
        cursor = conn.cursor()
        if USE_POSTGRES:
            cursor.executemany(
                'INSERT INTO pending_articles (title, url, category, raw_content, source_name, image_url, created_at) '
                'VALUES (%s, %s, %s, %s, %s, %s, %s) ON CONFLICT (url) DO NOTHING',
                batch
            )
        else:
            prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
            cursor.executemany(
                f'{prefix} INTO pending_articles (title, url, category, raw_content, source_name, image_url, created_at) '
                f'VALUES ({PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER})',
                batch
            )
    logger.debug("[RSS] Inserted batch of %d articles", len(batch))
