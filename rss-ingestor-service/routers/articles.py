import json
from datetime import datetime
from typing import List, Optional

from fastapi import APIRouter, HTTPException, Query
from database import db_conn, init_db, PLACEHOLDER
from models import (
    ArticleResponse, ArticleListResponse, DigestResponse,
    MarkSeenRequest, StatusResponse,
)
from utils import rank_articles
from ai_service import update_interest_profile
from config import USE_MYSQL, USE_POSTGRES
import cache_service
from logging_config import get_logger

logger = get_logger(__name__)
router = APIRouter(tags=["Articles"])


@router.post("/articles/push")
async def push_articles(articles: List[dict]):
    valid = [
        (a.get("title"), a.get("link"), "News",
         a.get("contentSnippet", ""), a.get("source", "Unknown"), a.get("image"))
        for a in articles if a.get("link")
    ]
    if not valid:
        return {"status": "Success", "inserted": 0}

    count = 0
    with db_conn() as conn:
        cursor = conn.cursor()
        for row in valid:
            try:
                if USE_POSTGRES:
                    cursor.execute(
                        'INSERT INTO pending_articles (title, url, category, raw_content, source_name, image_url) '
                        'VALUES (%s, %s, %s, %s, %s, %s) ON CONFLICT (url) DO NOTHING',
                        row
                    )
                else:
                    prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
                    cursor.execute(
                        f'{prefix} INTO pending_articles (title, url, category, raw_content, source_name, image_url) '
                        f'VALUES ({PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER})',
                        row
                    )
                count += 1
            except Exception as e:
                logger.warning("[push_articles] Failed to insert %s: %s", row[1], e)
    return {"status": "Success", "inserted": count}


@router.get("/articles", response_model=List[ArticleListResponse])
def get_articles(limit: int = 20, offset: int = 0):
    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()
        cursor.execute(
            f'''SELECT id, title, url, category, raw_content, source_name, image_url, created_at, tags
               FROM pending_articles
               WHERE image_url IS NOT NULL AND image_url != ''
               ORDER BY created_at DESC LIMIT {PLACEHOLDER} OFFSET {PLACEHOLDER}''',
            (limit, offset)
        )
        rows = cursor.fetchall()
    return [
        ArticleListResponse(
            id=r[0], title=r[1] or "", url=r[2] or "", category=r[3] or "",
            summary=r[4] or "", source_name=r[5] or "", image_url=r[6],
            created_at=str(r[7] or ""), tags=json.loads(r[8]) if r[8] else [],
        )
        for r in rows
    ]


@router.get("/categories/{category}/articles", response_model=List[ArticleResponse])
def get_articles_by_category(category: str, limit: int = 50, offset: int = 0):
    logger.info("[API] GET /categories/%s/articles", category)
    keywords = [k.strip().lower() for k in category.replace("&", " ").replace(",", " ").split() if len(k.strip()) > 1]
    if not keywords:
        keywords = [category.lower().strip()]

    where_clauses, params, score_parts = [], [], ["score"]

    for kw in keywords:
        like_val = f"%{kw}%"
        where_clauses.append(
            f"(LOWER(category) LIKE {PLACEHOLDER} OR LOWER(tags) LIKE {PLACEHOLDER} OR LOWER(title) LIKE {PLACEHOLDER})"
        )
        params += [like_val, like_val, like_val]
        score_parts.append(f"(CASE WHEN LOWER(tags) LIKE {PLACEHOLDER} THEN 100 ELSE 0 END)")
        score_parts.append(f"(CASE WHEN LOWER(category) LIKE {PLACEHOLDER} THEN 50 ELSE 0 END)")
        params += [like_val, like_val]

    where_sql = " OR ".join(where_clauses)
    score_sql  = " + ".join(score_parts)
    query = (
        f"SELECT title, url, category, raw_content, source_name, image_url, created_at, "
        f"({score_sql}) as relevance_score, tags "
        f"FROM pending_articles WHERE {where_sql} "
        f"ORDER BY relevance_score DESC, created_at DESC "
        f"LIMIT {PLACEHOLDER} OFFSET {PLACEHOLDER}"
    )
    params += [limit, offset]

    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()
        cursor.execute(query, params)
        rows = cursor.fetchall()

    return [
        ArticleResponse(
            title=r[0] or "", link=r[1] or "", category=r[2] or "",
            summary=r[3] or "", source=r[4] or "", image=r[5],
            publish_date=str(r[6] or ""), score=r[7] or 70,
            tags=json.loads(r[8]) if r[8] else [],
        )
        for r in rows
    ]


@router.post("/articles/mark-seen", response_model=StatusResponse)
async def mark_articles_seen(body: MarkSeenRequest):
    if not body.deviceId or not body.urls:
        return {"status": "noop"}

    valid_urls = [u for u in body.urls if u]
    with db_conn() as conn:
        cursor = conn.cursor()
        if USE_POSTGRES:
            for u in valid_urls:
                cursor.execute(
                    'INSERT INTO device_seen (device_id, url, seen_at) VALUES (%s, %s, CURRENT_TIMESTAMP) '
                    'ON CONFLICT (device_id, url) DO UPDATE SET seen_at = CURRENT_TIMESTAMP',
                    (body.deviceId, u)
                )
            cursor.execute("DELETE FROM device_seen WHERE seen_at < CURRENT_TIMESTAMP - INTERVAL '7 days'")
        elif USE_MYSQL:
            for u in valid_urls:
                cursor.execute(
                    'INSERT INTO device_seen (device_id, url, seen_at) VALUES (%s, %s, CURRENT_TIMESTAMP) '
                    'ON DUPLICATE KEY UPDATE seen_at = CURRENT_TIMESTAMP',
                    (body.deviceId, u)
                )
            cursor.execute("DELETE FROM device_seen WHERE seen_at < DATE_SUB(NOW(), INTERVAL 7 DAY)")
        else:
            cursor.executemany(
                'INSERT INTO device_seen (device_id, url, seen_at) VALUES (?, ?, CURRENT_TIMESTAMP) '
                'ON CONFLICT(device_id, url) DO UPDATE SET seen_at = CURRENT_TIMESTAMP',
                [(body.deviceId, u) for u in valid_urls]
            )
            cursor.execute("DELETE FROM device_seen WHERE seen_at < datetime('now', '-7 days')")

    # Async: update interest profile from reading history
    await update_interest_profile(body.deviceId)
    return {"status": "ok"}


@router.get("/articles/by-tag", response_model=List[ArticleResponse])
def get_articles_by_tag(tag: str, limit: int = 20, offset: int = 0):
    return get_articles_by_category(tag, limit, offset)


@router.get("/trending", response_model=List[ArticleResponse])
def get_trending(
    deviceId: Optional[str] = Query(None),
    hours: int = Query(48, description="Look-back window in hours"),
    limit: int = Query(20),
    categories: Optional[str] = Query(None, description="Comma-separated interest categories to filter by"),
):
    """
    Articles ranked by composite score: (distinct opens × 10) + AI score.
    Filtered to the requesting user's interest categories when provided.
    Falls back to top-scored recent articles when social signal is thin.
    """
    logger.info("[API] GET /trending deviceId=%s hours=%d categories=%s", deviceId, hours, categories)

    cat_list = [c.strip().lower() for c in categories.split(",") if c.strip()] if categories else []

    if cat_list:
        ph_cats = ",".join([PLACEHOLDER] * len(cat_list))
        cat_filter       = f"AND LOWER(a.category) IN ({ph_cats})"
        cat_filter_plain = f"AND LOWER(category) IN ({ph_cats})"
    else:
        cat_filter       = ""
        cat_filter_plain = ""

    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()

        if USE_POSTGRES:
            params = [hours] + cat_list + [limit]
            cursor.execute(
                f'''SELECT a.title, a.url, a.category, a.raw_content, a.source_name,
                          a.image_url, a.created_at, a.score, a.tags,
                          COUNT(DISTINCT s.device_id) AS open_count
                   FROM pending_articles a
                   JOIN device_seen s ON a.url = s.url
                   WHERE a.image_url IS NOT NULL AND a.image_url != ''
                   AND s.seen_at >= CURRENT_TIMESTAMP - INTERVAL '%s hours'
                   {cat_filter}
                   GROUP BY a.id, a.title, a.url, a.category, a.raw_content,
                            a.source_name, a.image_url, a.created_at, a.score, a.tags
                   ORDER BY (COUNT(DISTINCT s.device_id) * 10 + COALESCE(a.score, 70)) DESC, a.created_at DESC
                   LIMIT %s''',
                params
            )
        elif USE_MYSQL:
            params = [hours] + cat_list + [limit]
            cursor.execute(
                f'''SELECT a.title, a.url, a.category, a.raw_content, a.source_name,
                          a.image_url, a.created_at, a.score, a.tags,
                          COUNT(DISTINCT s.device_id) AS open_count
                   FROM pending_articles a
                   JOIN device_seen s ON a.url = s.url
                   WHERE a.image_url IS NOT NULL AND a.image_url != ''
                   AND s.seen_at >= DATE_SUB(NOW(), INTERVAL %s HOUR)
                   {cat_filter}
                   GROUP BY a.id
                   ORDER BY (COUNT(DISTINCT s.device_id) * 10 + COALESCE(a.score, 70)) DESC, a.created_at DESC
                   LIMIT %s''',
                params
            )
        else:
            params = [f'-{hours} hours'] + cat_list + [limit]
            cursor.execute(
                f'''SELECT a.title, a.url, a.category, a.raw_content, a.source_name,
                          a.image_url, a.created_at, a.score, a.tags,
                          COUNT(DISTINCT s.device_id) AS open_count
                   FROM pending_articles a
                   JOIN device_seen s ON a.url = s.url
                   WHERE a.image_url IS NOT NULL AND a.image_url != ''
                   AND s.seen_at >= datetime('now', ?)
                   {cat_filter}
                   GROUP BY a.url
                   ORDER BY (COUNT(DISTINCT s.device_id) * 10 + COALESCE(a.score, 70)) DESC, a.created_at DESC
                   LIMIT ?''',
                params
            )

        rows = cursor.fetchall()

        # Fallback: pad with top-scored recent articles in same categories
        if len(rows) < 5:
            existing_urls = {r[1] for r in rows}
            if USE_POSTGRES:
                params_fb = cat_list + [limit]
                cursor.execute(
                    f'''SELECT title, url, category, raw_content, source_name,
                              image_url, created_at, score, tags, 0 AS open_count
                       FROM pending_articles
                       WHERE image_url IS NOT NULL AND image_url != ''
                       {cat_filter_plain}
                       ORDER BY score DESC, created_at DESC
                       LIMIT %s''',
                    params_fb
                )
            else:
                params_fb = cat_list + [limit]
                cursor.execute(
                    f'''SELECT title, url, category, raw_content, source_name,
                               image_url, created_at, score, tags, 0 AS open_count
                        FROM pending_articles
                        WHERE image_url IS NOT NULL AND image_url != ''
                        {cat_filter_plain}
                        ORDER BY score DESC, created_at DESC
                        LIMIT {PLACEHOLDER}''',
                    params_fb
                )
            fallback = [r for r in cursor.fetchall() if r[1] not in existing_urls]
            rows = list(rows) + fallback
            rows = rows[:limit]

    # Exclude articles already seen by this device
    if deviceId and rows:
        seen_urls: set[str] = set()
        with db_conn(readonly=True) as conn:
            cursor = conn.cursor()
            if USE_POSTGRES:
                cursor.execute(
                    "SELECT url FROM device_seen WHERE device_id = %s AND seen_at >= CURRENT_TIMESTAMP - INTERVAL '7 days'",
                    (deviceId,)
                )
            else:
                cursor.execute(
                    f"SELECT url FROM device_seen WHERE device_id = {PLACEHOLDER} AND seen_at >= datetime('now', '-7 days')",
                    (deviceId,)
                )
            seen_urls = {r[0] for r in cursor.fetchall()}
        rows = [r for r in rows if r[1] not in seen_urls]

    return [
        ArticleResponse(
            title=r[0] or "", link=r[1] or "", category=r[2] or "",
            summary=r[3] or "", source=r[4] or "", image=r[5],
            publish_date=str(r[6] or ""), score=r[7] or 70,
            tags=json.loads(r[8]) if r[8] else [],
        )
        for r in rows
    ]


@router.post("/reset-data", response_model=StatusResponse)
async def reset_data():
    logger.warning("[API] POST /reset-data — full reset requested")
    init_db(clear_existing=True)
    cache_service.clear_all_cache()
    return {"status": "Database and Redis re-initialized."}


@router.get("/digest", response_model=DigestResponse)
async def get_digest(
    deviceId: Optional[str] = Query(None),
    exclude_seen: bool = Query(False, description="When true, completely exclude seen articles instead of pushing them to bottom"),
):
    logger.info("[API] GET /digest deviceId=%s", deviceId)

    if deviceId and not cache_service.check_rate_limit(f"digest:{deviceId}", limit=10, period=60):
        raise HTTPException(status_code=429, detail="Rate limit exceeded. Please wait a minute.")

    if deviceId and not exclude_seen:
        cached = cache_service.get_cached_digest(deviceId)
        if cached:
            logger.debug("[Redis] Serving cached digest for %s", deviceId)
            return cached

    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()
        favorites = []
        if deviceId:
            cursor.execute(
                f"SELECT favoriteCategories FROM user_profile WHERE deviceId = {PLACEHOLDER}", (deviceId,)
            )
            row = cursor.fetchone()
            if row and row[0]:
                favorites = row[0].split(",")

        if deviceId:
            # exclude_seen=True → NOT EXISTS (JOIN) — completely removes seen articles
            # exclude_seen=False (default) → ORDER BY is_seen ASC — seen go to bottom
            if USE_POSTGRES:
                if exclude_seen:
                    cursor.execute(
                        '''SELECT a.title, a.url, a.category, a.raw_content, a.source_name, a.image_url,
                                  a.created_at, a.score, a.tags, 0 AS is_seen
                           FROM pending_articles a
                           WHERE a.image_url IS NOT NULL AND a.image_url != ''
                           AND NOT EXISTS (
                               SELECT 1 FROM device_seen s
                               WHERE s.device_id = %s AND s.url = a.url
                               AND s.seen_at >= CURRENT_TIMESTAMP - INTERVAL '3 days'
                           )
                           ORDER BY a.created_at DESC LIMIT 150''',
                        (deviceId,)
                    )
                else:
                    cursor.execute(
                        '''SELECT a.title, a.url, a.category, a.raw_content, a.source_name, a.image_url,
                                  a.created_at, a.score, a.tags,
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
                if exclude_seen:
                    cursor.execute(
                        '''SELECT a.title, a.url, a.category, a.raw_content, a.source_name, a.image_url,
                                  a.created_at, a.score, a.tags, 0 AS is_seen
                           FROM pending_articles a
                           WHERE a.image_url IS NOT NULL AND a.image_url != ''
                           AND NOT EXISTS (
                               SELECT 1 FROM device_seen s
                               WHERE s.device_id = %s AND s.url = a.url
                               AND s.seen_at >= DATE_SUB(NOW(), INTERVAL 3 DAY)
                           )
                           ORDER BY a.created_at DESC LIMIT 150''',
                        (deviceId,)
                    )
                else:
                    cursor.execute(
                        '''SELECT a.title, a.url, a.category, a.raw_content, a.source_name, a.image_url,
                                  a.created_at, a.score, a.tags,
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
                if exclude_seen:
                    cursor.execute(
                        '''SELECT a.title, a.url, a.category, a.raw_content, a.source_name, a.image_url,
                                  a.created_at, a.score, a.tags, 0 AS is_seen
                           FROM pending_articles a
                           WHERE a.image_url IS NOT NULL AND a.image_url != ''
                           AND NOT EXISTS (
                               SELECT 1 FROM device_seen s
                               WHERE s.device_id = ? AND s.url = a.url
                               AND s.seen_at >= datetime('now', '-3 days')
                           )
                           ORDER BY a.created_at DESC LIMIT 150''',
                        (deviceId,)
                    )
                else:
                    cursor.execute(
                        '''SELECT a.title, a.url, a.category, a.raw_content, a.source_name, a.image_url,
                                  a.created_at, a.score, a.tags,
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
        rows = cursor.fetchall()

    if not rows:
        return DigestResponse(digest_date=datetime.now().strftime("%Y-%m-%d"), news=[], message="No articles")

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
            "tags":         json.loads(r[8]) if r[8] else [],
        }
        for r in rows
    ]

    ranked = rank_articles(articles_pre_scored, favorites)
    response_data = DigestResponse(digest_date=datetime.now().strftime("%Y-%m-%d"), news=ranked)

    if deviceId:
        cache_service.set_cached_digest(deviceId, response_data.model_dump())

    return response_data
