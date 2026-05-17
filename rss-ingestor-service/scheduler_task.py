from apscheduler.schedulers.asyncio import AsyncIOScheduler
from database import get_db_connection
from rss_service import process_feeds_task
from datetime import datetime
from config import USE_POSTGRES

scheduler = AsyncIOScheduler()

async def scheduled_sync():
    print(f"[SCHEDULER] Triggering sync-all at {datetime.now()}")
    conn = get_db_connection()
    cursor = conn.cursor()
    active_val = True if USE_POSTGRES else 1
    cursor.execute("SELECT url, category FROM rss_sources WHERE is_active = %s" if USE_POSTGRES else "SELECT url, category FROM rss_sources WHERE is_active = 1", (active_val,) if USE_POSTGRES else ())
    sources = cursor.fetchall()
    conn.close()
    
    by_category = {}
    for url, cat in sources:
        if cat not in by_category: by_category[cat] = []
        by_category[cat].append(url)
        
    for cat, urls in by_category.items():
        await process_feeds_task(urls, cat)
    print("[SCHEDULER] sync-all finished")
