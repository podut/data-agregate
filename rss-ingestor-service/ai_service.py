import re
import json
import asyncio
import google.generativeai as genai
import httpx
from google.generativeai import GenerationConfig
from config import GEMINI_API_KEY, DEEPSEEK_API_KEY, DIGEST_PROMPT
from utils import clean_html
from datetime import datetime

if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)

# Ierarhia de modele pentru cascada
_GEMINI_MODELS = ["gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro"]
_DEEPSEEK_MODEL = "deepseek-chat"
_DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"

# Config care forteaza raspuns JSON pur
_JSON_CONFIG = GenerationConfig(
    response_mime_type="application/json",
    temperature=0.1,
)

_GENERIC_CATEGORIES = {"general", "news", "unknown", "uncategorized", ""}

_ENRICH_PROMPT = """You are a news article evaluator. Return ONLY a valid JSON object with exactly these fields:
- "title": string, the original title translated into {language}
- "score": integer between 0 and 100 (quality + relevance score)
- "summary": string, max 200 characters (concise 1-2 sentence summary translated into {language})
- "category": string, ONE short category label in English (e.g. "AI", "Dev", "Startups", "Security", "Science", "Business", "Tools", "Politics", "Health", "Climate"). 
- "tags": list of 3-5 short keywords (strings) that describe the article's specific topics. MANDATORY field.

IMPORTANT: The "title" and "summary" MUST be in {language}.

Article:
Title: {title}
Category: {category}
Content: {content}"""

# ─── Callers ─────────────────────────────────────────────────────────────────

async def _call_gemini_async(model_name: str, prompt: str) -> str:
    """Apel către Google Gemini."""
    if not GEMINI_API_KEY:
        raise Exception("Gemini API key missing")
    model = genai.GenerativeModel(model_name, generation_config=_JSON_CONFIG)
    response = await asyncio.to_thread(model.generate_content, prompt)
    return response.text

async def _call_deepseek_async(prompt: str) -> str:
    """Apel către DeepSeek (OpenAI compatible)."""
    if not DEEPSEEK_API_KEY:
        raise Exception("DeepSeek API key missing")
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {DEEPSEEK_API_KEY}"
    }
    payload = {
        "model": _DEEPSEEK_MODEL,
        "messages": [{"role": "user", "content": prompt}],
        "response_format": {"type": "json_object"},
        "temperature": 0.1
    }
    
    async with httpx.AsyncClient(timeout=60.0) as client:
        response = await client.post(_DEEPSEEK_URL, headers=headers, json=payload)
        response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"]

# ─── Cascade Logic ───────────────────────────────────────────────────────────

async def _ai_cascade_call(prompt: str, is_enrich: bool = True) -> str:
    """
    Implementează cascada: 
    1. Modele Gemini în ordine
    2. DeepSeek ca ultimă instanță AI
    """
    errors = []

    # Pasul 1: Încearcă Gemini (Flash -> Pro)
    if GEMINI_API_KEY:
        for model_name in _GEMINI_MODELS:
            try:
                print(f"[AI Cascade] Încercare Gemini: {model_name}")
                return await _call_gemini_async(model_name, prompt)
            except Exception as e:
                err_msg = f"Gemini {model_name} failed: {str(e)}"
                print(f"[AI Cascade] {err_msg}")
                errors.append(err_msg)

    # Pasul 2: Încearcă DeepSeek
    if DEEPSEEK_API_KEY:
        try:
            print(f"[AI Cascade] Încercare DeepSeek: {_DEEPSEEK_MODEL}")
            return await _call_deepseek_async(prompt)
        except Exception as e:
            err_msg = f"DeepSeek failed: {str(e)}"
            print(f"[AI Cascade] {err_msg}")
            errors.append(err_msg)

    raise Exception(f"AI Cascade failed completely. Errors: {'; '.join(errors)}")

# ─── Parsers ─────────────────────────────────────────────────────────────────

def _strip_md(raw: str) -> str:
    raw = raw.strip()
    raw = re.sub(r'^```json\s*', '', raw, flags=re.IGNORECASE)
    raw = re.sub(r'^```\s*',     '', raw, flags=re.IGNORECASE)
    raw = re.sub(r'\s*```$',     '', raw)
    return raw.strip()

def _parse_enrich(raw: str, fallback_content: str, fallback_title: str = "") -> dict:
    try:
        data = json.loads(_strip_md(raw))
    except Exception:
        words = [w.strip() for w in (fallback_title or "").split() if len(w) > 4][:3]
        return {"title": fallback_title, "score": 70, "summary": (fallback_content or "")[:200], "category": "News", "tags": words}

    title = data.get("title", fallback_title)
    if not isinstance(title, str): title = str(title)
    
    try:
        score = int(float(data.get("score", 70)))
    except: score = 70
    score = max(0, min(100, score))

    summary = data.get("summary", fallback_content or "")
    if not isinstance(summary, str): summary = str(summary)
    
    category = data.get("category", "News")
    if not isinstance(category, str): category = str(category)
    
    raw_tags = data.get("tags", [])
    if not isinstance(raw_tags, list): raw_tags = []
    tags = [str(t).strip()[:20] for t in raw_tags if t]
    if not tags:
        tags = [w.strip().upper() for w in (fallback_title or "").split() if len(w) > 4][:3]
    
    return {"title": title.strip() or fallback_title, "score": score, "summary": summary[:200], "category": category.strip()[:30], "tags": tags[:5]}

def _parse_digest(raw: str) -> dict | None:
    try:
        data = json.loads(_strip_md(raw))
        if not isinstance(data, dict) or "news" not in data: return None
        validated_news = []
        for item in data["news"]:
            if not isinstance(item, dict): continue
            try: score = int(float(item.get("score", 70)))
            except: score = 70
            validated_news.append({
                "title": str(item.get("title", "")),
                "summary": str(item.get("summary", ""))[:400],
                "link": str(item.get("link", "")),
                "source": str(item.get("source", "")),
                "category": str(item.get("category", "")),
                "image": item.get("image") or None,
                "publish_date": str(item.get("publish_date", "")),
                "score": max(0, min(100, score)),
                "tags": [str(t).strip()[:20] for t in item.get("tags", []) if t][:5]
            })
        data["news"] = validated_news
        return data
    except: return None

# ─── API Methods ─────────────────────────────────────────────────────────────

async def enrich_article_async(article_id: int, title: str, content: str, category: str, language: str = "en") -> dict:
    lang_map = {"en": "English", "ro": "Romanian"}
    target_lang = lang_map.get(language, language)

    prompt = _ENRICH_PROMPT.format(
        language = target_lang,
        title    = (title    or "")[:200],
        category = (category or ""),
        content  = (content  or "")[:500],
    )

    try:
        text = await _ai_cascade_call(prompt)
        return _parse_enrich(text, content, title)
    except Exception as e:
        print(f"[AI Cascade] Eșec critic pentru #{article_id}: {e}")
        # Hard fallback
        return {
            "title": title, "score": 60, "summary": (content or "")[:200], 
            "category": category or "News", 
            "tags": [w.strip().upper() for w in (title or "").split() if len(w) > 4][:3]
        }

async def generate_digest(articles_input: list) -> dict | None:
    prompt = f"{DIGEST_PROMPT}\n\nArticles:\n{json.dumps(articles_input, ensure_ascii=False)}"
    try:
        text = await _ai_cascade_call(prompt, is_enrich=False)
        return _parse_digest(text)
    except:
        return None

async def process_article_chunk(chunk_rows: list):
    from database import get_db_connection, PLACEHOLDER
    from utils import scrape_image
    
    ids = [r[0] for r in chunk_rows]
    conn = get_db_connection(); cursor = conn.cursor()
    placeholders = ",".join([PLACEHOLDER] * len(ids))
    cursor.execute(f"SELECT id, url, image_url FROM pending_articles WHERE id IN ({placeholders})", ids)
    db_data = {r[0]: {"url": r[1], "image_url": r[2]} for r in cursor.fetchall()}
    cursor.execute("SELECT language FROM user_profile LIMIT 1")
    row = cursor.fetchone()
    user_lang = row[0] if row and row[0] else "en"
    conn.close()

    async def process_single_full(article_id, title, content, cat):
        url = db_data[article_id]["url"]
        existing_img = db_data[article_id]["image_url"]
        enrich_task = enrich_article_async(article_id, title, content, cat, user_lang)
        scrape_task = scrape_image(url) if not existing_img and url else None
        
        if scrape_task:
            ai_res, scraped_img = await asyncio.gather(enrich_task, scrape_task)
        else:
            ai_res = await enrich_task
            scraped_img = existing_img
        return article_id, title, content, cat, ai_res, scraped_img

    tasks = [process_single_full(*row) for row in chunk_rows]
    results = await asyncio.gather(*tasks)
    
    conn = get_db_connection(); cursor = conn.cursor()
    try:
        for article_id, title, content, cat, ai_res, final_img in results:
            try:
                new_category = ai_res.get("category", "").strip()
                should_update_cat = (new_category and cat.lower() in _GENERIC_CATEGORIES and new_category.lower() not in _GENERIC_CATEGORIES)
                tags_json = json.dumps(ai_res.get("tags", []))
                translated_title = ai_res.get("title", title)
                
                if should_update_cat:
                    cursor.execute(f"UPDATE pending_articles SET title = {PLACEHOLDER}, score = {PLACEHOLDER}, raw_content = {PLACEHOLDER}, category = {PLACEHOLDER}, tags = {PLACEHOLDER}, image_url = {PLACEHOLDER}, processed = 1 WHERE id = {PLACEHOLDER}",
                        (translated_title, ai_res["score"], ai_res["summary"], new_category, tags_json, final_img, article_id))
                else:
                    cursor.execute(f"UPDATE pending_articles SET title = {PLACEHOLDER}, score = {PLACEHOLDER}, raw_content = {PLACEHOLDER}, tags = {PLACEHOLDER}, image_url = {PLACEHOLDER}, processed = 1 WHERE id = {PLACEHOLDER}",
                        (translated_title, ai_res["score"], ai_res["summary"], tags_json, final_img, article_id))
                print(f"[AI+Scrape] #{article_id} finalizat via Cascade (scor={ai_res['score']})")
            except Exception as e: print(f"[AI chunk] Error saving #{article_id}: {e}")
        conn.commit()
    finally: conn.close()

async def _process_all_chunks(rows: list, chunk_size: int = 10):
    for i in range(0, len(rows), chunk_size):
        await process_article_chunk(rows[i:i + chunk_size])

async def enrich_pending_articles(category: str = None):
    from database import get_db_connection, PLACEHOLDER
    conn = get_db_connection(); cursor = conn.cursor()
    query = "SELECT id, title, raw_content, category FROM pending_articles WHERE processed = 0"
    params = [category] if category else []
    if category: query += f" AND category = {PLACEHOLDER}"
    cursor.execute(query, params)
    rows = cursor.fetchall(); conn.close()
    if rows: await _process_all_chunks(rows)

def fix_missing_data():
    """Procesează articolele care au scăpat de faza de enrichment."""
    import threading
    def run_fix():
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        loop.run_until_complete(enrich_pending_articles())
    
    # Rulăm într-un thread separat pentru a nu bloca pornirea FastAPI
    threading.Thread(target=run_fix, daemon=True).start()
    print("[AI] fix_missing_data started in background thread.")

def get_fallback_digest(rows: list) -> dict:
    return {
        "digest_date": datetime.now().strftime("%Y-%m-%d"),
        "news": [
            {
                "title": r[0], "summary": clean_html(r[3])[:400], "image": r[5], "link": r[1], 
                "source": r[4], "category": r[2], "score": 70, "publish_date": r[6],
                "tags": json.loads(r[8]) if len(r) > 8 and r[8] else []
            } for r in rows
        ],
    }
