import feedparser
import time
from datetime import datetime, timedelta, timezone
from typing import List
from database import get_db_connection, PLACEHOLDER
from utils import clean_html
from ai_service import enrich_pending_articles
from config import USE_MYSQL, USE_POSTGRES
import cache_service

async def process_feeds_task(urls: List[str], category: str):
    lock_key = f"process:{category}"
    if not cache_service.set_processing_lock(lock_key, timeout=600):
        print(f"[RSS] Category '{category}' is already being processed. Skipping.")
        return

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        all_pending = []
        now = datetime.now(timezone.utc)
        # Cautam articole din ultimele 7 zile pentru a popula rapid baza de date
        today_limit = now - timedelta(days=7)

        def do_insert(batch):
            if USE_POSTGRES:
                cursor.executemany('INSERT INTO pending_articles (title, url, category, raw_content, source_name, image_url, created_at) VALUES (%s, %s, %s, %s, %s, %s, %s) ON CONFLICT (url) DO NOTHING', batch)
            else:
                prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
                cursor.executemany(f'{prefix} INTO pending_articles (title, url, category, raw_content, source_name, image_url, created_at) VALUES ({PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER})', batch)
            conn.commit()

        for feed_url in urls:
            try:
                feed = feedparser.parse(feed_url)
                source_title = clean_html(feed.feed.get("title", feed_url))
                for entry in feed.entries:
                    published_at = None
                    published_parsed = entry.get("published_parsed")
                    if published_parsed:
                        dt = datetime.fromtimestamp(time.mktime(published_parsed), timezone.utc)
                        if dt < today_limit: continue
                        published_at = dt.strftime('%Y-%m-%d %H:%M:%S')

                    title = clean_html(entry.get("title", "No Title"))
                    url = entry.get("link", "")
                    if not url or cache_service.is_url_seen(url):
                        continue
                    
                    cache_service.mark_url_processed(url)

                    raw_body = entry.get("summary", "") + " " + entry.get("description", "")
                    summary = clean_html(raw_body)
                    if len(summary) > 400: summary = summary[:400] + "..."

                    image_url = None
                    if 'media_content' in entry and len(entry.media_content) > 0:
                        image_url = entry.media_content[0]['url']
                    elif 'links' in entry:
                        for link in entry.links:
                            if 'image' in link.get('type', ''):
                                image_url = link.get('href'); break

                    if url:
                        all_pending.append((title, url, category, summary, source_title, image_url, published_at or datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S')))
                        if len(all_pending) >= 50:
                            do_insert(all_pending)
                            all_pending = []
            except Exception as e: print(f"Error syncing {feed_url}: {e}")

        if all_pending:
            do_insert(all_pending)
        conn.close()
        
        # Proceseaza cu AI fiecare articol nou, pe rand
        await enrich_pending_articles(category)
    finally:
        cache_service.release_processing_lock(lock_key)
