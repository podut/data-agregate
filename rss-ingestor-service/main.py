from fastapi import FastAPI, HTTPException, BackgroundTasks, Query
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from typing import List, Optional
import sqlite3
import json
import httpx

from database import init_db, get_db_connection, PLACEHOLDER
from models import RSSIngestRequest, CategoryCreate, SerpConfigUpdate, RssFeedRequest, UserProfileSync, SchedulerConfig, MarkSeenRequest
from rss_service import process_feeds_task
from ai_service import generate_digest, get_fallback_digest, fix_missing_data
from scheduler_task import scheduler, scheduled_sync
from utils import google_news_rss_url, rank_articles
from config import SERP_API_KEY, USE_MYSQL, USE_POSTGRES
import cache_service

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Run migrations/fixes on startup
    try:
        fix_missing_data()
    except Exception as e:
        print(f"[Lifespan] Error running fix_missing_data: {e}")

    from database import get_setting
    interval = int(get_setting("scheduler_interval", "8"))

    scheduler.add_job(
        scheduled_sync,
        'interval',
        hours=interval,
        id="hourly_sync",
        replace_existing=True,
    )
    scheduler.start()
    print(f"[Lifespan] Scheduler started with interval: {interval} hours")
    yield
    scheduler.shutdown()

app = FastAPI(title="RSS Aggregator PRO", lifespan=lifespan)

# Configurare CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def read_root():
    return {"status": "RSS Aggregator API is running", "version": "1.0.0"}

# ─── Ingest / Sync ───────────────────────────────────────────────────────────

@app.post("/ingest")
async def ingest_rss(request: RSSIngestRequest, background_tasks: BackgroundTasks):
    # Rate limit per device and category
    cache_key = f"ingest:{request.deviceId or 'global'}:{request.category}"
    if not cache_service.check_rate_limit(cache_key, limit=5, period=60):
        raise HTTPException(status_code=429, detail="Too many ingestion requests. Try again later.")

    print(f"[API] POST /ingest category='{request.category}' device='{request.deviceId}'")
    background_tasks.add_task(process_feeds_task, request.urls, request.category)
    
    # ALGORITM: Auto-fetch Google News pentru noua categorie/interes
    background_tasks.add_task(_fetch_via_google_news_rss, request.category, request.category, "us")
    
    return {"status": "Accepted"}

@app.post("/sync-all")
async def sync_all():
    await scheduled_sync()
    return {"status": "Started"}

@app.post("/scheduler/run-now")
async def run_now():
    await scheduled_sync()
    return {"status": "done"}

@app.put("/scheduler/config")
def update_scheduler(body: SchedulerConfig):
    from database import set_setting
    set_setting("scheduler_interval", str(body.interval_hours))
    
    try:
        scheduler.remove_job("hourly_sync")
    except:
        pass
        
    scheduler.add_job(
        scheduled_sync,
        'interval',
        hours=body.interval_hours,
        id="hourly_sync",
        replace_existing=True,
    )
    return {"status": "updated", "interval_hours": body.interval_hours}

@app.get("/scheduler/config")
def get_scheduler_config():
    from database import get_setting
    interval = int(get_setting("scheduler_interval", "8"))
    return {"interval_hours": interval}

# ─── Articles ────────────────────────────────────────────────────────────────

@app.post("/articles/push")
async def push_articles(articles: List[dict]):
    conn = get_db_connection()
    cursor = conn.cursor()
    count = 0
    for a in articles:
        if a.get("link"):
            try:
                if USE_POSTGRES:
                    cursor.execute(
                        'INSERT INTO pending_articles (title, url, category, raw_content, source_name, image_url) VALUES (%s, %s, %s, %s, %s, %s) ON CONFLICT (url) DO NOTHING',
                        (a.get("title"), a.get("link"), "News", a.get("contentSnippet", ""), a.get("source", "Unknown"), a.get("image"))
                    )
                else:
                    prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
                    cursor.execute(
                        f'{prefix} INTO pending_articles (title, url, category, raw_content, source_name, image_url) VALUES ({PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER})',
                        (a.get("title"), a.get("link"), "News", a.get("contentSnippet", ""), a.get("source", "Unknown"), a.get("image"))
                    )
                count += 1
            except:
                pass
    conn.commit(); conn.close()
    return {"status": "Success", "inserted": count}

@app.get("/articles")
def get_articles(limit: int = 20, offset: int = 0):
    conn = get_db_connection(); cursor = conn.cursor()
    cursor.execute(
        f'''SELECT id, title, url, category, raw_content, source_name, image_url, created_at, tags 
           FROM pending_articles 
           WHERE image_url IS NOT NULL AND image_url != ''
           ORDER BY created_at DESC LIMIT {PLACEHOLDER} OFFSET {PLACEHOLDER}''',
        (limit, offset)
    )
    rows = cursor.fetchall(); conn.close()
    return [{"id": r[0], "title": r[1], "url": r[2], "category": r[3], "summary": r[4], "source_name": r[5], "image_url": r[6], "created_at": r[7], "tags": json.loads(r[8]) if r[8] else []} for r in rows]

@app.get("/categories/{category}/articles")
def get_articles_by_category(category: str, limit: int = 50, offset: int = 0):
    print(f"[API] GET /categories/{category}/articles interest='{category}'")
    conn = get_db_connection(); cursor = conn.cursor()
    
    # Keyword search logic: spargem interesul in cuvinte cheie (ex: "AI & Tools" -> ["ai", "tools"])
    keywords = [k.strip().lower() for k in category.replace("&", " ").replace(",", " ").split() if len(k.strip()) > 1]
    if not keywords: keywords = [category.lower().strip()]
    
    # Construim clauza WHERE dinamic pentru fiecare cuvant cheie
    where_clauses = []
    params = []
    
    # Calculam si un relevance_score cumulat
    score_calculation = "score"
    
    for kw in keywords:
        where_clauses.append(f"(LOWER(category) LIKE {PLACEHOLDER} OR LOWER(tags) LIKE {PLACEHOLDER} OR LOWER(title) LIKE {PLACEHOLDER})")
        like_val = f"%{kw}%"
        params.append(like_val); params.append(like_val); params.append(like_val)
        
        # Bonusuri pentru potriviri specifice pe fiecare cuvant
        score_calculation += f" + (CASE WHEN LOWER(tags) LIKE {PLACEHOLDER} THEN 100 ELSE 0 END)"
        score_calculation += f" + (CASE WHEN LOWER(category) LIKE {PLACEHOLDER} THEN 50 ELSE 0 END)"
        params.append(like_val); params.append(like_val)

    where_sql = " OR ".join(where_clauses)
    
    query = f'''SELECT title, url, category, raw_content, source_name, image_url, created_at, 
                       ({score_calculation}) as relevance_score, 
                       tags
                FROM pending_articles
                WHERE {where_sql}
                ORDER BY relevance_score DESC, created_at DESC
                LIMIT {PLACEHOLDER} OFFSET {PLACEHOLDER}'''
    
    params.extend([limit, offset])
    cursor.execute(query, params)
    rows = cursor.fetchall(); conn.close()
    
    return [
        {
            "title":        r[0] or "",
            "link":         r[1] or "",
            "category":     r[2] or "",
            "summary":      r[3] or "",
            "source":       r[4] or "",
            "image":        r[5],
            "publish_date": r[6] or "",
            "score":        r[7] or 70,
            "tags":         json.loads(r[8]) if r[8] else []
        }
        for r in rows
    ]

@app.post("/articles/mark-seen")
def mark_articles_seen(body: MarkSeenRequest):
    """Marcheaza articole ca vazute pentru un device — vor fi excluse din /digest 3 zile."""
    if not body.deviceId or not body.urls:
        return {"status": "noop", "inserted": 0}
    conn = get_db_connection(); cursor = conn.cursor()
    
    if USE_POSTGRES:
        for u in body.urls:
            if u:
                cursor.execute(
                    '''INSERT INTO device_seen (device_id, url, seen_at)
                       VALUES (%s, %s, CURRENT_TIMESTAMP)
                       ON CONFLICT (device_id, url) DO UPDATE SET seen_at = CURRENT_TIMESTAMP''',
                    (body.deviceId, u)
                )
    elif USE_MYSQL:
        for u in body.urls:
            if u:
                cursor.execute(
                    '''INSERT INTO device_seen (device_id, url, seen_at)
                       VALUES (%s, %s, CURRENT_TIMESTAMP)
                       ON DUPLICATE KEY UPDATE seen_at = CURRENT_TIMESTAMP''',
                    (body.deviceId, u)
                )
    else:
        cursor.executemany(
            '''INSERT INTO device_seen (device_id, url, seen_at)
               VALUES (?, ?, CURRENT_TIMESTAMP)
               ON CONFLICT(device_id, url) DO UPDATE SET seen_at = CURRENT_TIMESTAMP''',
            [(body.deviceId, u) for u in body.urls if u]
        )
    
    # Curatam intrarile mai vechi de 7 zile
    if USE_POSTGRES:
        cursor.execute("DELETE FROM device_seen WHERE seen_at < CURRENT_TIMESTAMP - INTERVAL '7 days'")
    elif USE_MYSQL:
        cursor.execute("DELETE FROM device_seen WHERE seen_at < DATE_SUB(NOW(), INTERVAL 7 DAY)")
    else:
        cursor.execute("DELETE FROM device_seen WHERE seen_at < datetime('now', '-7 days')")
    
    conn.commit(); conn.close()
    return {"status": "ok", "marked": len(body.urls), "purged_old": True}

@app.get("/articles/by-tag")
def get_articles_by_tag(tag: str, limit: int = 20, offset: int = 0):
    # Reutilizam aceeasi logica de keyword search si pentru tag-uri
    return get_articles_by_category(tag, limit, offset)

@app.post("/reset-data")
async def reset_data():
    """Endpoint pentru a goli baza de date și a pune date noi de test."""
    print("[API] POST /reset-data - Incepere resetare completa...")
    init_db(clear_existing=True) 
    cache_service.clear_all_cache()
    return {"status": "Database and Redis re-initialized with fresh seeds."}

@app.get("/digest")
async def get_digest(deviceId: Optional[str] = Query(None)):
    print(f"[API] GET /digest deviceId={deviceId}")
    
    # 1. Rate Limit
    if deviceId and not cache_service.check_rate_limit(f"digest:{deviceId}", limit=10, period=60):
        raise HTTPException(status_code=429, detail="Rate limit exceeded. Please wait a minute.")

    # 2. Check Cache
    if deviceId:
        cached_data = cache_service.get_cached_digest(deviceId)
        if cached_data:
            print(f"[Redis] Serving cached digest for {deviceId}")
            return cached_data

    conn = get_db_connection(); cursor = conn.cursor()

    favorites = []
    if deviceId:
        cursor.execute(f"SELECT favoriteCategories FROM user_profile WHERE deviceId = {PLACEHOLDER}", (deviceId,))
        row = cursor.fetchone()
        if row and row[0]:
            favorites = row[0].split(",")

    if deviceId:
        if USE_POSTGRES:
            cursor.execute(
                '''SELECT a.title, a.url, a.category, a.raw_content, a.source_name, a.image_url, a.created_at, a.score, a.tags,
                          CASE WHEN EXISTS (
                              SELECT 1 FROM device_seen s
                              WHERE s.device_id = %s AND s.url = a.url
                              AND s.seen_at >= CURRENT_TIMESTAMP - INTERVAL '3 days'
                          ) THEN 1 ELSE 0 END AS is_seen
                   FROM pending_articles a
                   WHERE a.image_url IS NOT NULL AND a.image_url != ''
                   ORDER BY is_seen ASC, a.created_at DESC LIMIT 150''',
                (deviceId,)
            )
        elif USE_MYSQL:
            cursor.execute(
                '''SELECT a.title, a.url, a.category, a.raw_content, a.source_name, a.image_url, a.created_at, a.score, a.tags,
                          CASE WHEN EXISTS (
                              SELECT 1 FROM device_seen s
                              WHERE s.device_id = %s AND s.url = a.url
                              AND s.seen_at >= DATE_SUB(NOW(), INTERVAL 3 DAY)
                          ) THEN 1 ELSE 0 END AS is_seen
                   FROM pending_articles a
                   WHERE a.image_url IS NOT NULL AND a.image_url != ''
                   ORDER BY is_seen ASC, a.created_at DESC LIMIT 150''',
                (deviceId,)
            )
        else:
            cursor.execute(
                '''SELECT a.title, a.url, a.category, a.raw_content, a.source_name, a.image_url, a.created_at, a.score, a.tags,
                          CASE WHEN EXISTS (
                              SELECT 1 FROM device_seen s
                              WHERE s.device_id = ? AND s.url = a.url
                              AND s.seen_at >= datetime('now', '-3 days')
                          ) THEN 1 ELSE 0 END AS is_seen
                   FROM pending_articles a
                   WHERE a.image_url IS NOT NULL AND a.image_url != ''
                   ORDER BY is_seen ASC, a.created_at DESC LIMIT 150''',
                (deviceId,)
            )
    else:
        cursor.execute(
            '''SELECT title, url, category, raw_content, source_name, image_url, created_at, score, tags
               FROM pending_articles
               WHERE image_url IS NOT NULL AND image_url != ''
               ORDER BY created_at DESC LIMIT 150'''
        )
    rows = cursor.fetchall(); conn.close()
    if not rows:
        return {
            "digest_date": __import__("datetime").datetime.now().strftime("%Y-%m-%d"),
            "news": [],
            "message": "No articles"
        }

    from datetime import datetime
    articles_pre_scored = [
        {
            "title":        r[0],
            "link":         r[1],
            "category":     r[2],
            "summary":      r[3],
            "source":       r[4],
            "image":        r[5],
            "publish_date": r[6].strftime('%Y-%m-%d %H:%M:%S') if isinstance(r[6], datetime) else r[6],
            "score":        r[7] if r[7] is not None else 70,
            "tags":         json.loads(r[8]) if r[8] else []
        }
        for r in rows
    ]

    ranked = rank_articles(articles_pre_scored, favorites)
    response_data = {
        "digest_date": __import__("datetime").datetime.now().strftime("%Y-%m-%d"),
        "news": ranked,
    }
    
    # 3. Save to Cache
    if deviceId:
        cache_service.set_cached_digest(deviceId, response_data)
        
    return response_data

# ─── User profile ────────────────────────────────────────────────────────────

@app.post("/profile")
def upsert_profile(body: UserProfileSync):
    """Upsert per-device favorites used for ranking in /digest."""
    import time
    favs = ",".join([c.strip() for c in body.favoriteCategories if c and c.strip()])
    conn = get_db_connection(); cursor = conn.cursor()
    
    if USE_POSTGRES:
        cursor.execute(
            '''INSERT INTO user_profile (deviceId, name, favoriteCategories, interestsScore, lastActive, language)
               VALUES (%s, %s, %s, 100, %s, %s)
               ON CONFLICT (deviceId) DO UPDATE SET
                 name = EXCLUDED.name,
                 favoriteCategories = EXCLUDED.favoriteCategories,
                 language = EXCLUDED.language,
                 lastActive = EXCLUDED.lastActive''',
            (body.deviceId, body.name, favs, int(time.time() * 1000), body.language)
        )
    elif USE_MYSQL:
        cursor.execute(
            '''INSERT INTO user_profile (deviceId, name, favoriteCategories, interestsScore, lastActive, language)
               VALUES (%s, %s, %s, 100, %s, %s)
               ON DUPLICATE KEY UPDATE
                 name = VALUES(name),
                 favoriteCategories = VALUES(favoriteCategories),
                 language = VALUES(language),
                 lastActive = VALUES(lastActive)''',
            (body.deviceId, body.name, favs, int(time.time() * 1000), body.language)
        )
    else:
        cursor.execute(
            '''INSERT INTO user_profile (deviceId, name, favoriteCategories, interestsScore, lastActive, language)
               VALUES (?, ?, ?, 100, ?, ?)
               ON CONFLICT(deviceId) DO UPDATE SET
                 name = excluded.name,
                 favoriteCategories = excluded.favoriteCategories,
                 language = excluded.language,
                 lastActive = excluded.lastActive''',
            (body.deviceId, body.name, favs, int(time.time() * 1000), body.language)
        )
    conn.commit(); conn.close()
    return {"status": "ok", "deviceId": body.deviceId, "favorites": favs, "language": body.language}

@app.get("/profile/{deviceId}")
def get_profile(deviceId: str):
    conn = get_db_connection(); cursor = conn.cursor()
    cursor.execute(
        f"SELECT deviceId, name, favoriteCategories, interestsScore, lastActive, language FROM user_profile WHERE deviceId = {PLACEHOLDER}",
        (deviceId,)
    )
    row = cursor.fetchone(); conn.close()
    if not row:
        return {"deviceId": deviceId, "favoriteCategories": [], "name": "Reader", "language": "en"}
    return {
        "deviceId": row[0],
        "name": row[1],
        "favoriteCategories": [c for c in (row[2] or "").split(",") if c.strip()],
        "interestsScore": row[3],
        "lastActive": row[4],
        "language": row[5] or "en"
    }

# ─── Categories ──────────────────────────────────────────────────────────────

@app.get("/categories")
def list_categories(deviceId: Optional[str] = Query(None)):
    conn = get_db_connection(); cursor = conn.cursor()
    dev_id = deviceId or "global"

    active_val = True if USE_POSTGRES else 1
    cursor.execute(f"SELECT DISTINCT category FROM rss_sources WHERE is_active = {PLACEHOLDER} AND (device_id = {PLACEHOLDER} OR device_id = 'global' OR device_id IS NULL)", (active_val, dev_id))
    cats_from_sources = {row[0] for row in cursor.fetchall()}

    cursor.execute(f"SELECT category FROM category_config WHERE (device_id = {PLACEHOLDER} OR device_id = 'global' OR device_id IS NULL)", (dev_id,))
    cats_from_config = {row[0] for row in cursor.fetchall()}

    all_cats = cats_from_sources | cats_from_config
    result = []
    for cat in all_cats:
        cursor.execute(f"SELECT url, is_active FROM rss_sources WHERE category = {PLACEHOLDER} AND (device_id = {PLACEHOLDER} OR device_id = 'global' OR device_id IS NULL)", (cat, dev_id))
        feeds = [{"url": r[0], "is_active": bool(r[1])} for r in cursor.fetchall()]

        cursor.execute(f"SELECT COUNT(*) FROM pending_articles WHERE category = {PLACEHOLDER}", (cat,))
        article_count = cursor.fetchone()[0]

        cursor.execute(f"SELECT serp_query, serp_region, serp_enabled FROM category_config WHERE category = {PLACEHOLDER} AND (device_id = {PLACEHOLDER} OR device_id = 'global' OR device_id IS NULL)", (cat, dev_id))
        serp_row = cursor.fetchone()
        serp = {
            "query":   serp_row[0] if serp_row else None,
            "region":  serp_row[1] if serp_row else "us",
            "enabled": bool(serp_row[2]) if serp_row else False,
        }

        result.append({
            "name":          cat,
            "feeds":         feeds,
            "article_count": article_count,
            "serp":          serp,
        })

    conn.close()
    return result

@app.post("/categories")
def create_category(body: CategoryCreate):
    conn = get_db_connection(); cursor = conn.cursor()
    dev_id = body.deviceId or "global"
    
    if USE_POSTGRES:
        cursor.execute(f"INSERT INTO category_config (category, device_id) VALUES (%s, %s) ON CONFLICT (category, device_id) DO NOTHING", (body.name, dev_id))
    else:
        prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
        cursor.execute(f"{prefix} INTO category_config (category, device_id) VALUES ({PLACEHOLDER}, {PLACEHOLDER})", (body.name, dev_id))
    
    for url in body.rss_urls:
        if url.strip():
            if USE_POSTGRES:
                cursor.execute(
                    "INSERT INTO rss_sources (url, category, device_id) VALUES (%s, %s, %s) ON CONFLICT (url, category, device_id) DO NOTHING",
                    (url.strip(), body.name, dev_id)
                )
            else:
                prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
                cursor.execute(
                    f"{prefix} INTO rss_sources (url, category, device_id) VALUES ({PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER})",
                    (url.strip(), body.name, dev_id)
                )
    conn.commit(); conn.close()
    return {"status": "created"}

@app.delete("/categories/{category}")
def delete_category(category: str, deviceId: Optional[str] = Query(None)):
    conn = get_db_connection(); cursor = conn.cursor()
    dev_id = deviceId or "global"
    if USE_POSTGRES:
        cursor.execute("DELETE FROM category_config WHERE category = %s AND device_id = %s", (category, dev_id))
        cursor.execute("UPDATE rss_sources SET is_active = FALSE WHERE category = %s AND device_id = %s", (category, dev_id))
    else:
        cursor.execute(f"DELETE FROM category_config WHERE category = {PLACEHOLDER} AND device_id = {PLACEHOLDER}", (category, dev_id))
        cursor.execute(f"UPDATE rss_sources SET is_active = 0 WHERE category = {PLACEHOLDER} AND device_id = {PLACEHOLDER}", (category, dev_id))
    conn.commit(); conn.close()
    return {"status": "deleted"}

# ─── Feeds per category ───────────────────────────────────────────────────────

@app.post("/categories/{category}/feeds")
def add_feed(category: str, body: RssFeedRequest):
    conn = get_db_connection(); cursor = conn.cursor()
    dev_id = body.deviceId or "global"
    if USE_POSTGRES:
        cursor.execute(f"INSERT INTO category_config (category, device_id) VALUES (%s, %s) ON CONFLICT (category, device_id) DO NOTHING", (category, dev_id))
        cursor.execute(
            f"INSERT INTO rss_sources (url, category, device_id, is_active) VALUES (%s, %s, %s, TRUE) ON CONFLICT (url, category, device_id) DO UPDATE SET is_active = TRUE",
            (body.url, category, dev_id)
        )
    else:
        prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
        cursor.execute(f"{prefix} INTO category_config (category, device_id) VALUES ({PLACEHOLDER}, {PLACEHOLDER})", (category, dev_id))
        cursor.execute(
            f"{prefix} INTO rss_sources (url, category, device_id, is_active) VALUES ({PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, 1)",
            (body.url, category, dev_id)
        )
        cursor.execute(
            f"UPDATE rss_sources SET is_active = 1 WHERE url = {PLACEHOLDER} AND category = {PLACEHOLDER} AND device_id = {PLACEHOLDER}",
            (body.url, category, dev_id)
        )
    conn.commit(); conn.close()
    return {"status": "added"}

@app.put("/categories/{category}/feeds/toggle")
def toggle_feed(category: str, body: RssFeedRequest):
    conn = get_db_connection(); cursor = conn.cursor()
    dev_id = body.deviceId or "global"
    print(f"[API] PUT /categories/{category}/feeds/toggle url={body.url} deviceId={dev_id}")
    
    cursor.execute(f"SELECT is_active FROM rss_sources WHERE url = {PLACEHOLDER} AND category = {PLACEHOLDER} AND device_id = {PLACEHOLDER}", (body.url, category, dev_id))
    row = cursor.fetchone()
    if not row:
        print(f"[API] Feed NOT FOUND for url={body.url}, category={category}, deviceId={dev_id}")
        conn.close()
        raise HTTPException(404, "Feed not found")
    
    new_state = body.is_active if body.is_active is not None else (not row[0] if USE_POSTGRES else (0 if row[0] else 1))
    sql_state = new_state if USE_POSTGRES else (1 if new_state else 0)
    print(f"[API] Toggling feed to {new_state}")
    cursor.execute(
        f"UPDATE rss_sources SET is_active = {PLACEHOLDER} WHERE url = {PLACEHOLDER} AND category = {PLACEHOLDER} AND device_id = {PLACEHOLDER}",
        (sql_state, body.url, category, dev_id)
    )
    conn.commit(); conn.close()
    return {"status": "toggled", "is_active": bool(new_state)}

@app.delete("/categories/{category}/feeds")
def remove_feed(category: str, body: RssFeedRequest):
    conn = get_db_connection(); cursor = conn.cursor()
    dev_id = body.deviceId or "global"
    cursor.execute(
        f"DELETE FROM rss_sources WHERE url = {PLACEHOLDER} AND category = {PLACEHOLDER} AND device_id = {PLACEHOLDER}",
        (body.url, category, dev_id)
    )
    conn.commit(); conn.close()
    return {"status": "removed"}

# ─── SERP config ──────────────────────────────────────────────────────────────

@app.put("/categories/{category}/serp")
def update_serp_config(category: str, body: SerpConfigUpdate):
    conn = get_db_connection(); cursor = conn.cursor()
    dev_id = body.deviceId or "global"
    
    if USE_POSTGRES:
        cursor.execute(f"INSERT INTO category_config (category, device_id) VALUES (%s, %s) ON CONFLICT (category, device_id) DO NOTHING", (category, dev_id))
    else:
        prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
        cursor.execute(f"{prefix} INTO category_config (category, device_id) VALUES ({PLACEHOLDER}, {PLACEHOLDER})", (category, dev_id))
    
    cursor.execute(
        f"UPDATE category_config SET serp_query = {PLACEHOLDER}, serp_region = {PLACEHOLDER}, serp_enabled = {PLACEHOLDER} WHERE category = {PLACEHOLDER} AND device_id = {PLACEHOLDER}",
        (body.query, body.region, 1 if body.enabled else 0, category, dev_id)
    )
    conn.commit(); conn.close()
    return {"status": "updated"}

@app.post("/categories/{category}/serp/fetch")
async def fetch_serp(category: str, background_tasks: BackgroundTasks):
    conn = get_db_connection(); cursor = conn.cursor()
    cursor.execute(f"SELECT serp_query, serp_region, serp_enabled FROM category_config WHERE category = {PLACEHOLDER}", (category,))
    row = cursor.fetchone()
    conn.close()

    if not row or not row[2]:
        raise HTTPException(400, "SERP not enabled for this category")

    query  = row[0]
    region = row[1] or "us"
    if not query:
        raise HTTPException(400, "No SERP query configured")

    background_tasks.add_task(_fetch_serp_articles, category, query, region)
    return {"status": "fetch started", "mode": "serpapi" if SERP_API_KEY else "google_news_rss"}

async def _fetch_serp_articles(category: str, query: str, region: str):
    if SERP_API_KEY:
        await _fetch_via_serpapi(category, query, region)
    else:
        _fetch_via_google_news_rss(category, query, region)

async def _fetch_via_serpapi(category: str, query: str, region: str):
    try:
        async with httpx.AsyncClient(timeout=15) as client:
            resp = await client.get(
                "https://serpapi.com/search",
                params={
                    "engine":   "google_news",
                    "q":        query,
                    "gl":       region.upper(),
                    "hl":       region.lower(),
                    "api_key":  SERP_API_KEY,
                }
            )
            data = resp.json()
    except Exception as e:
        print(f"[SERP] SerpAPI error: {e}. Falling back to Google News RSS.")
        _fetch_via_google_news_rss(category, query, region)
        return

    news_results = data.get("news_results", [])
    if not news_results:
        print(f"[SERP] No results from SerpAPI, falling back to Google News RSS.")
        _fetch_via_google_news_rss(category, query, region)
        return

    articles = []
    for item in news_results:
        title = item.get("title", "")
        url   = item.get("link", "")
        source = item.get("source", {}).get("name", "SerpAPI") if isinstance(item.get("source"), dict) else str(item.get("source", "SerpAPI"))
        snippet = item.get("snippet", "")
        thumbnail = item.get("thumbnail", None)
        if url:
            articles.append((title, url, category, snippet, source, thumbnail))

    _insert_articles(articles)
    print(f"[SERP] Inserted {len(articles)} articles via SerpAPI for '{category}'")

_FALLBACK_RSS_SOURCES = [
    "google_news",
    "https://techcrunch.com/feed/",
    "https://www.theverge.com/rss/index.xml",
    "https://feeds.arstechnica.com/arstechnica/index",
    "https://hnrss.org/frontpage",
    "https://www.wired.com/feed/rss",
    "https://www.zdnet.com/news/rss.xml",
    "https://feeds.bbci.co.uk/news/technology/rss.xml",
    "https://feeds.reuters.com/reuters/technologyNews",
    "https://www.cnet.com/rss/news/",
    "https://www.engadget.com/rss.xml",
    "https://www.gizmodo.com/rss",
    "https://www.digitaltrends.com/feed/",
    "https://www.slashdot.org/slashdot.rss",
    "https://www.venturebeat.com/feed/",
    "https://www.recode.net/rss/index.xml",
    "https://www.thenextweb.com/feed/",
    "https://www.mashable.com/feeds/rss/all",
    "https://www.infoworld.com/index.rss",
    "https://www.computerworld.com/index.rss",
    "https://www.networkworld.com/index.rss",
    "https://www.cio.com/index.rss",
    "https://www.techradar.com/rss",
    "https://www.tomsguide.com/feeds/all",
    "https://www.pcworld.com/index.rss",
    "https://www.macworld.com/index.rss",
    "https://www.androidcentral.com/feed",
    "https://www.imore.com/feed",
    "https://www.windowscentral.com/feed",
    "https://www.9to5mac.com/feed/",
    "https://www.9to5google.com/feed/",
    "https://www.forbes.com/innovation/feed/",
    "https://www.bloomberg.com/technology/rss",
    "https://www.nytimes.com/svc/collections/v1/publish/https://www.nytimes.com/section/technology/rss.xml",
    "https://www.wsj.com/xml/rss/3_7455.xml",
    "https://www.startupcafe.ro/rss.xml",
    "https://www.digi24.ro/rss/stiri/sci-tech",
    "https://www.hotnews.ro/rss/science",
    "https://www.go4it.ro/feed/",
    "https://www.economica.net/rss",
    "https://www.profit.ro/rss",
    "https://www.zf.ro/rss",
    "https://autolatest.ro/feed/",
    "https://www.biziday.ro/feed/",
]

def _fetch_via_google_news_rss(category: str, query: str, region: str):
    """Fallback: cauta articole din mai multe surse RSS publice + Google News."""
    import feedparser
    from utils import clean_html

    all_articles = []
    clean_query = query.replace("&", " ").replace("+", " ").replace("  ", " ").strip()
    
    for source in _FALLBACK_RSS_SOURCES:
        try:
            is_google = (source == "google_news")
            if is_google:
                feed_url    = google_news_rss_url(clean_query, region)
                source_name = "Google News"
            else:
                feed_url    = source
                source_name = source.split("/")[2].replace("feeds.", "").replace("www.", "")

            feed = feedparser.parse(feed_url)
            count = 0
            # Limitam la 15 articole per sursa fallback pentru a nu satura DB-ul cu date vechi
            for entry in feed.entries[:15]:
                url = entry.get("link", "")
                if not url: continue
                title   = clean_html(entry.get("title", ""))
                summary = clean_html(entry.get("summary", "") + " " + entry.get("description", ""))
                if len(summary) > 500: summary = summary[:500] + "..."
                src = clean_html(entry.get("source", {}).get("title", source_name) if isinstance(entry.get("source"), dict) else source_name)
                
                # IMPORTANT: Daca e Google News (bazat pe query exact), folosim categoria ceruta.
                # Daca e o sursa fixa (ex: MacWorld), incercam sa detectam daca e relevanta pentru query.
                if is_google or (clean_query.lower() in title.lower() or clean_query.lower() in summary.lower()):
                    all_articles.append((title, url, category, summary, src, None))
                    count += 1
            print(f"[SERP fallback] {source_name}: {count} articole validate pentru '{category}'")
        except Exception as e:
            print(f"[SERP fallback] Eroare la {source}: {e}")
            continue

    if all_articles:
        _insert_articles(all_articles)
        print(f"[SERP fallback] Total inserat: {len(all_articles)} articole pentru '{category}'")

def _insert_articles(articles: list):
    if not articles:
        return
    from ai_service import enrich_pending_articles

    # Deduplicare rapidă cu Redis
    unique_articles = []
    for a in articles:
        url = a[1]
        if not cache_service.is_url_seen(url):
            unique_articles.append(a)
            cache_service.mark_url_processed(url)
    
    if not unique_articles:
        return

    conn = get_db_connection(); cursor = conn.cursor()
    if USE_POSTGRES:
        cursor.executemany(
            'INSERT INTO pending_articles (title, url, category, raw_content, source_name, image_url) VALUES (%s, %s, %s, %s, %s, %s) ON CONFLICT (url) DO NOTHING',
            unique_articles
        )
    else:
        prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
        cursor.executemany(
            f'{prefix} INTO pending_articles (title, url, category, raw_content, source_name, image_url) VALUES ({PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER})',
            unique_articles
        )
    conn.commit(); conn.close()

    categories = {a[2] for a in unique_articles if a[2]}
    for cat in categories:
        enrich_pending_articles(cat)

@app.get("/sync")
async def trigger_sync():
    """Trigger manual sync of all active RSS sources."""
    await scheduled_sync()
    return {"status": "Sync triggered successfully"}

if __name__ == "__main__":
    import uvicorn
    from database import init_db
    init_db()
    uvicorn.run(app, host="0.0.0.0", port=8085)
