import asyncio
from database import get_db_connection
from utils import scrape_image

async def backfill():
    print("[BACKFILL] Starting image backfill for processed articles without images...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Gasim articole care sunt procesate (sau nu) dar nu au imagine
    cursor.execute("SELECT id, url FROM pending_articles WHERE image_url IS NULL OR image_url = ''")
    rows = cursor.fetchall()
    
    print(f"[BACKFILL] Found {len(rows)} articles to check.")
    
    for article_id, url in rows:
        print(f"[BACKFILL] Scraping image for #{article_id}: {url}")
        img_url = await scrape_image(url)
        if img_url:
            cursor.execute("UPDATE pending_articles SET image_url = ? WHERE id = ?", (img_url, article_id))
            conn.commit()
            print(f"[BACKFILL] Found and updated image for #{article_id}")
        else:
            print(f"[BACKFILL] No image found for #{article_id}")
            
    conn.close()
    print("[BACKFILL] Finished.")

if __name__ == "__main__":
    asyncio.run(backfill())
