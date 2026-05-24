from apscheduler.schedulers.asyncio import AsyncIOScheduler
from datetime import datetime

from database import db_conn, PLACEHOLDER
from rss_service import process_feeds_task
from config import USE_POSTGRES, USE_MYSQL
from logging_config import get_logger

logger = get_logger(__name__)

scheduler = AsyncIOScheduler()


async def scheduled_sync():
    logger.info("[SCHEDULER] Triggering sync-all at %s", datetime.now())

    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()
        active_val = True if USE_POSTGRES else 1
        # rss_feeds has UNIQUE(url) — each URL is fetched exactly once
        cursor.execute(
            f"SELECT url, category FROM rss_feeds WHERE is_active = {PLACEHOLDER}",
            (active_val,)
        )
        sources = cursor.fetchall()

    by_category: dict[str, list[str]] = {}
    for url, cat in sources:
        by_category.setdefault(cat, []).append(url)

    for cat, urls in by_category.items():
        await process_feeds_task(urls, cat)

    logger.info("[SCHEDULER] sync-all finished")


async def cleanup_old_articles(days: int = 30):
    """Deletes articles older than `days` days to keep the DB lean."""
    with db_conn() as conn:
        cursor = conn.cursor()
        if USE_POSTGRES:
            cursor.execute(
                f"DELETE FROM pending_articles WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '{days} days'"
            )
        elif USE_MYSQL:
            cursor.execute(
                f"DELETE FROM pending_articles WHERE created_at < DATE_SUB(NOW(), INTERVAL {days} DAY)"
            )
        else:
            cursor.execute(
                f"DELETE FROM pending_articles WHERE created_at < datetime('now', '-{days} days')"
            )
        deleted = cursor.rowcount

    logger.info("[CLEANUP] Deleted %d articles older than %d days", deleted, days)
    return deleted
