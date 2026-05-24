import re
import json
import asyncio
from abc import ABC, abstractmethod
from datetime import datetime

import google.generativeai as genai
import httpx
from google.generativeai import GenerationConfig

from config import GEMINI_API_KEY, DEEPSEEK_API_KEY, DIGEST_PROMPT
from utils import clean_html
from logging_config import get_logger

logger = get_logger(__name__)

if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)

_JSON_CONFIG = GenerationConfig(response_mime_type="application/json", temperature=0.1)
_GENERIC_CATEGORIES = {"general", "news", "unknown", "uncategorized", ""}

_ENRICH_PROMPT = """Ești un evaluator de articole de știri. Returnează DOAR un obiect JSON valid cu exact aceste câmpuri:
- "title": șir de caractere, titlul original tradus în limba {language}
- "score": număr întreg între 0 și 100 (scorul de calitate + relevanță)
- "summary": șir de caractere, maximum 200 de caractere (un rezumat concis de 1-2 propoziții tradus în limba {language})
- "category": șir de caractere, O SINGURĂ etichetă scurtă de categorie în limba engleză (de ex. "AI", "Dev", "Startups", "Security", "Science", "Business", "Tools", "Politics", "Health", "Climate").
- "tags": o listă de 3-5 cuvinte cheie scurte (șiruri de caractere) care descriu subiectele specifice ale articolului. Câmp MANDATORIU.

IMPORTANT: Câmpurile "title" și "summary" TREBUIE să fie în limba {language}.

Articol:
Titlu: {title}
Categorie: {category}
Conținut: {content}"""

# ─── AI Provider abstraction ─────────────────────────────────────────────────

class AIProvider(ABC):
    @property
    @abstractmethod
    def name(self) -> str: ...

    @abstractmethod
    async def call(self, prompt: str) -> str: ...


class GeminiProvider(AIProvider):
    def __init__(self, model_name: str):
        self.model_name = model_name

    @property
    def name(self) -> str:
        return f"Gemini/{self.model_name}"

    async def call(self, prompt: str) -> str:
        if not GEMINI_API_KEY:
            raise RuntimeError("Gemini API key missing")
        model = genai.GenerativeModel(self.model_name, generation_config=_JSON_CONFIG)
        response = await asyncio.to_thread(model.generate_content, prompt)
        return response.text


class DeepSeekProvider(AIProvider):
    _URL   = "https://api.deepseek.com/chat/completions"
    _MODEL = "deepseek-chat"

    @property
    def name(self) -> str:
        return "DeepSeek"

    async def call(self, prompt: str) -> str:
        if not DEEPSEEK_API_KEY:
            raise RuntimeError("DeepSeek API key missing")
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
        }
        payload = {
            "model": self._MODEL,
            "messages": [{"role": "user", "content": prompt}],
            "response_format": {"type": "json_object"},
            "temperature": 0.1,
        }
        async with httpx.AsyncClient(timeout=60.0) as client:
            resp = await client.post(self._URL, headers=headers, json=payload)
            resp.raise_for_status()
            return resp.json()["choices"][0]["message"]["content"]


class AICascade:
    """Tries each provider in order, falls back to next on failure."""

    def __init__(self, providers: list[AIProvider]):
        self.providers = providers

    async def call(self, prompt: str) -> str:
        errors = []
        for provider in self.providers:
            try:
                logger.debug("[AI Cascade] Trying %s", provider.name)
                return await provider.call(prompt)
            except Exception as e:
                msg = f"{provider.name} failed: {e}"
                logger.warning("[AI Cascade] %s", msg)
                errors.append(msg)
        raise RuntimeError(f"All AI providers failed: {'; '.join(errors)}")


def _build_cascade() -> AICascade:
    providers: list[AIProvider] = []
    if GEMINI_API_KEY:
        for m in ["gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro"]:
            providers.append(GeminiProvider(m))
    if DEEPSEEK_API_KEY:
        providers.append(DeepSeekProvider())
    return AICascade(providers)


# ─── Parsers ─────────────────────────────────────────────────────────────────

def _strip_md(raw: str) -> str:
    raw = raw.strip()
    raw = re.sub(r'^```json\s*', '', raw, flags=re.IGNORECASE)
    raw = re.sub(r'^```\s*', '', raw, flags=re.IGNORECASE)
    raw = re.sub(r'\s*```$', '', raw)
    return raw.strip()


def _parse_enrich(raw: str, fallback_content: str, fallback_title: str = "") -> dict:
    try:
        data = json.loads(_strip_md(raw))
    except Exception:
        words = [w.strip() for w in (fallback_title or "").split() if len(w) > 4][:3]
        return {
            "title": fallback_title,
            "score": 70,
            "summary": (fallback_content or "")[:200],
            "category": "News",
            "tags": words,
        }

    title = data.get("title", fallback_title)
    if not isinstance(title, str):
        title = str(title)

    try:
        score = max(0, min(100, int(float(data.get("score", 70)))))
    except Exception:
        score = 70

    summary = data.get("summary", fallback_content or "")
    if not isinstance(summary, str):
        summary = str(summary)

    category = data.get("category", "News")
    if not isinstance(category, str):
        category = str(category)

    raw_tags = data.get("tags", [])
    if not isinstance(raw_tags, list):
        raw_tags = []
    tags = [str(t).strip()[:20] for t in raw_tags if t]
    if not tags:
        tags = [w.strip().upper() for w in (fallback_title or "").split() if len(w) > 4][:3]

    return {
        "title":    title.strip() or fallback_title,
        "score":    score,
        "summary":  summary[:200],
        "category": category.strip()[:30],
        "tags":     tags[:5],
    }


def _parse_digest(raw: str) -> dict | None:
    try:
        data = json.loads(_strip_md(raw))
        if not isinstance(data, dict) or "news" not in data:
            logger.warning("[AI] _parse_digest: missing 'news' key in response")
            return None
        validated = []
        for item in data["news"]:
            if not isinstance(item, dict):
                continue
            try:
                score = max(0, min(100, int(float(item.get("score", 70)))))
            except Exception:
                score = 70
            validated.append({
                "title":        str(item.get("title", "")),
                "summary":      str(item.get("summary", ""))[:400],
                "link":         str(item.get("link", "")),
                "source":       str(item.get("source", "")),
                "category":     str(item.get("category", "")),
                "image":        item.get("image") or None,
                "publish_date": str(item.get("publish_date", "")),
                "score":        score,
                "tags":         [str(t).strip()[:20] for t in item.get("tags", []) if t][:5],
            })
        data["news"] = validated
        return data
    except Exception as e:
        logger.error("[AI] _parse_digest error: %s", e)
        return None


# ─── Public API ──────────────────────────────────────────────────────────────

async def enrich_article_async(article_id: int, title: str, content: str, category: str, language: str = "en") -> dict:
    lang_map = {"en": "English", "ro": "Romanian"}
    target_lang = lang_map.get(language, language)

    prompt = _ENRICH_PROMPT.format(
        language=target_lang,
        title=(title or "")[:200],
        category=(category or ""),
        content=(content or "")[:500],
    )

    try:
        cascade = _build_cascade()
        text = await cascade.call(prompt)
        return _parse_enrich(text, content, title)
    except Exception as e:
        logger.error("[AI Cascade] Critical failure for #%d: %s", article_id, e)
        return {
            "title":    title,
            "score":    60,
            "summary":  (content or "")[:200],
            "category": category or "News",
            "tags":     [w.strip().upper() for w in (title or "").split() if len(w) > 4][:3],
        }


async def generate_digest(articles_input: list) -> dict | None:
    prompt = f"{DIGEST_PROMPT}\n\nArticles:\n{json.dumps(articles_input, ensure_ascii=False)}"
    try:
        cascade = _build_cascade()
        text = await cascade.call(prompt)
        return _parse_digest(text)
    except Exception as e:
        logger.error("[AI] generate_digest failed: %s", e)
        return None


async def process_article_chunk(chunk_rows: list, user_lang: str = "en"):
    from database import db_conn, PLACEHOLDER
    from utils import scrape_image

    ids = [r[0] for r in chunk_rows]
    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()
        placeholders = ",".join([PLACEHOLDER] * len(ids))
        cursor.execute(
            f"SELECT id, url, image_url FROM pending_articles WHERE id IN ({placeholders})", ids
        )
        db_data = {r[0]: {"url": r[1], "image_url": r[2]} for r in cursor.fetchall()}

    async def process_single(article_id, title, content, cat):
        url = db_data[article_id]["url"]
        existing_img = db_data[article_id]["image_url"]
        enrich_task = enrich_article_async(article_id, title, content, cat, user_lang)
        scrape_task = scrape_image(url) if not existing_img and url else None

        if scrape_task:
            ai_res, scraped_img = await asyncio.gather(enrich_task, scrape_task)
        else:
            ai_res = await enrich_task
            scraped_img = existing_img
        return article_id, ai_res, scraped_img

    tasks = [process_single(*row) for row in chunk_rows]
    results = await asyncio.gather(*tasks)

    with db_conn() as conn:
        cursor = conn.cursor()
        for article_id, ai_res, final_img in results:
            try:
                orig_cat = next((r[3] for r in chunk_rows if r[0] == article_id), "")
                new_category = ai_res.get("category", "").strip()
                should_update_cat = (
                    new_category
                    and orig_cat.lower() in _GENERIC_CATEGORIES
                    and new_category.lower() not in _GENERIC_CATEGORIES
                )
                tags_json = json.dumps(ai_res.get("tags", []))
                translated_title = ai_res.get("title", "")

                if should_update_cat:
                    cursor.execute(
                        f"UPDATE pending_articles SET title={PLACEHOLDER}, score={PLACEHOLDER}, raw_content={PLACEHOLDER}, "
                        f"category={PLACEHOLDER}, tags={PLACEHOLDER}, image_url={PLACEHOLDER}, processed=1 WHERE id={PLACEHOLDER}",
                        (translated_title, ai_res["score"], ai_res["summary"], new_category, tags_json, final_img, article_id)
                    )
                else:
                    cursor.execute(
                        f"UPDATE pending_articles SET title={PLACEHOLDER}, score={PLACEHOLDER}, raw_content={PLACEHOLDER}, "
                        f"tags={PLACEHOLDER}, image_url={PLACEHOLDER}, processed=1 WHERE id={PLACEHOLDER}",
                        (translated_title, ai_res["score"], ai_res["summary"], tags_json, final_img, article_id)
                    )
                logger.debug("[AI+Scrape] #%d done (score=%d)", article_id, ai_res["score"])
            except Exception as e:
                logger.error("[AI chunk] Error saving #%d: %s", article_id, e)


async def _process_all_chunks(rows: list, user_lang: str = "en", chunk_size: int = 10):
    for i in range(0, len(rows), chunk_size):
        await process_article_chunk(rows[i:i + chunk_size], user_lang)


async def enrich_pending_articles(category: str = None, device_id: str = None):
    from database import db_conn, PLACEHOLDER

    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()
        user_lang = None
        if device_id:
            cursor.execute(
                f"SELECT language FROM user_profile WHERE deviceId = {PLACEHOLDER}", (device_id,)
            )
            row = cursor.fetchone()
            if row and row[0]:
                user_lang = row[0]
                
        if not user_lang:
            cursor.execute(
                f"SELECT value FROM settings WHERE key = {PLACEHOLDER}", ("default_language",)
            )
            row = cursor.fetchone()
            user_lang = row[0] if row and row[0] else "ro"

        query = "SELECT id, title, raw_content, category FROM pending_articles WHERE processed = 0"
        params = []
        if category:
            query += f" AND category = {PLACEHOLDER}"
            params.append(category)
        cursor.execute(query, params)
        rows = cursor.fetchall()

    if rows:
        await _process_all_chunks(rows, user_lang)


def fix_missing_data():
    """Processes articles that missed enrichment. Runs in background thread on startup."""
    import threading

    def run_fix():
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        loop.run_until_complete(enrich_pending_articles())

    threading.Thread(target=run_fix, daemon=True).start()
    logger.info("[AI] fix_missing_data started in background thread.")


def get_fallback_digest(rows: list) -> dict:
    return {
        "digest_date": datetime.now().strftime("%Y-%m-%d"),
        "news": [
            {
                "title":        r[0],
                "summary":      clean_html(r[3])[:400],
                "image":        r[5],
                "link":         r[1],
                "source":       r[4],
                "category":     r[2],
                "score":        70,
                "publish_date": r[6],
                "tags":         json.loads(r[8]) if len(r) > 8 and r[8] else [],
            }
            for r in rows
        ],
    }


async def update_interest_profile(device_id: str):
    """
    Recalculates user interests from reading history (device_seen + pending_articles).
    Called automatically when mark-seen is received.
    """
    from database import db_conn, PLACEHOLDER
    from config import USE_POSTGRES, USE_MYSQL

    with db_conn() as conn:
        cursor = conn.cursor()

        if USE_POSTGRES:
            cursor.execute(
                '''SELECT a.category, COUNT(*) AS reads
                   FROM device_seen s
                   JOIN pending_articles a ON s.url = a.url
                   WHERE s.device_id = %s
                   AND s.seen_at >= CURRENT_TIMESTAMP - INTERVAL '14 days'
                   GROUP BY a.category
                   ORDER BY reads DESC
                   LIMIT 10''',
                (device_id,)
            )
        else:
            cursor.execute(
                f'''SELECT a.category, COUNT(*) AS reads
                   FROM device_seen s
                   JOIN pending_articles a ON s.url = a.url
                   WHERE s.device_id = {PLACEHOLDER}
                   AND s.seen_at >= datetime('now', '-14 days')
                   GROUP BY a.category
                   ORDER BY reads DESC
                   LIMIT 10''',
                (device_id,)
            )

        top_cats = [row[0] for row in cursor.fetchall() if row[0]]
        if not top_cats:
            return

        # Merge with existing favorites to avoid wiping manual selections
        cursor.execute(
            f"SELECT favoriteCategories FROM user_profile WHERE deviceId = {PLACEHOLDER}",
            (device_id,)
        )
        row = cursor.fetchone()
        existing = [c for c in (row[0] or "").split(",") if c.strip()] if row else []

        # Union: reading-history categories first, then existing manual selections
        merged = list(dict.fromkeys(top_cats + existing))[:15]
        favs = ",".join(merged)

        cursor.execute(
            f"UPDATE user_profile SET favoriteCategories = {PLACEHOLDER}, interestsScore = {PLACEHOLDER} WHERE deviceId = {PLACEHOLDER}",
            (favs, min(100, len(top_cats) * 10), device_id)
        )
        logger.info("[Profile] Updated interests for %s: %s", device_id, favs)

        # Invalidate cached digest so next call gets fresh ranking
        import cache_service as cs
        cs.invalidate_digest(device_id)
