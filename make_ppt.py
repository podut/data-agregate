from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.util import Inches, Pt
import copy

# ── Palette ──────────────────────────────────────────────────────────────────
BG_DARK    = RGBColor(0x0F, 0x0F, 0x17)
PURPLE     = RGBColor(0x8A, 0x2B, 0xE2)
PURPLE_DIM = RGBColor(0x3D, 0x1A, 0x6E)
PURPLE_LT  = RGBColor(0xB1, 0x9C, 0xD9)
WHITE      = RGBColor(0xFF, 0xFF, 0xFF)
GRAY       = RGBColor(0x88, 0x88, 0xA0)
GREEN      = RGBColor(0x22, 0xC5, 0x5E)
CYAN       = RGBColor(0x06, 0xB6, 0xD4)
AMBER      = RGBColor(0xF5, 0x9E, 0x0B)
RED        = RGBColor(0xEF, 0x44, 0x44)
CARD_BG    = RGBColor(0x1E, 0x1E, 0x2E)


prs = Presentation()
prs.slide_width  = Inches(13.33)
prs.slide_height = Inches(7.5)

blank_layout = prs.slide_layouts[6]  # completely blank


# ── Helpers ──────────────────────────────────────────────────────────────────

def add_slide():
    slide = prs.slides.add_slide(blank_layout)
    # Dark background
    bg = slide.background
    fill = bg.fill
    fill.solid()
    fill.fore_color.rgb = BG_DARK
    return slide


def txb(slide, text, x, y, w, h,
        size=18, bold=False, color=WHITE,
        align=PP_ALIGN.LEFT, italic=False, wrap=True):
    tf_box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tf_box.text_frame
    tf.word_wrap = wrap
    p = tf.paragraphs[0]
    p.alignment = align
    run = p.add_run()
    run.text = text
    run.font.size  = Pt(size)
    run.font.bold  = bold
    run.font.italic = italic
    run.font.color.rgb = color
    return tf_box


def rect(slide, x, y, w, h, fill_color, alpha=None, radius=None):
    shape = slide.shapes.add_shape(
        1,  # MSO_SHAPE_TYPE.RECTANGLE
        Inches(x), Inches(y), Inches(w), Inches(h)
    )
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill_color
    shape.line.fill.background()
    return shape


def circle(slide, x, y, size, fill_color):
    shape = slide.shapes.add_shape(
        9,  # oval
        Inches(x), Inches(y), Inches(size), Inches(size)
    )
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill_color
    shape.line.fill.background()
    return shape


def accent_bar(slide, color=PURPLE):
    rect(slide, 0, 0, 0.06, 7.5, color)


def bullet_slide(slide, items, x, y, w, color=GRAY, size=16):
    for i, (icon, text) in enumerate(items):
        txb(slide, icon, x, y + i * 0.52, 0.4, 0.5, size=size, color=PURPLE)
        txb(slide, text, x + 0.42, y + i * 0.52, w, 0.5, size=size, color=color)


def tag_box(slide, text, x, y, bg=PURPLE_DIM, fg=PURPLE_LT, size=12):
    r = rect(slide, x, y, len(text) * 0.085 + 0.25, 0.32, bg)
    txb(slide, text, x + 0.08, y + 0.02, len(text) * 0.085 + 0.1, 0.3,
        size=size, color=fg, bold=True)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 1 — Title
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

# Big purple glow circle (decorative)
circle(s, 8.5, -0.5, 5, RGBColor(0x3D, 0x1A, 0x6E))
circle(s, 9.5, 2.0, 2.5, RGBColor(0x1A, 0x0A, 0x2E))

txb(s, "DataAgregate", 1.0, 1.8, 8, 1.6, size=60, bold=True, color=WHITE)
txb(s, "PRO", 1.0, 3.1, 3, 0.6, size=28, bold=True, color=PURPLE)

txb(s, "Personalized AI-Powered News Aggregation Platform",
    1.0, 3.9, 9, 0.8, size=20, color=GRAY)

# Tech tags row
tags = ["FastAPI", "Python", "PostgreSQL", "Redis", "Gemini AI", "Android", "Jetpack Compose", "Docker"]
xpos = 1.0
for tag in tags:
    tag_box(s, tag, xpos, 5.2)
    xpos += len(tag) * 0.085 + 0.45

txb(s, "2026", 11.8, 6.9, 1.2, 0.4, size=12, color=GRAY, align=PP_ALIGN.RIGHT)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 2 — Architecture Overview
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "System Architecture", 0.5, 0.25, 10, 0.7, size=32, bold=True, color=WHITE)
txb(s, "End-to-end flow from RSS sources to the user's personalized feed",
    0.5, 0.9, 12, 0.5, size=14, color=GRAY)

# 3 boxes: RSS Sources → Backend → Android App
boxes = [
    (0.6,  2.0, 3.4, 3.8, CARD_BG, CYAN,   "RSS Sources / SERP",
     ["arstechnica.com", "wired.com", "bbc.com", "techcrunch.com", "+ Google News RSS"]),
    (5.0,  1.5, 4.0, 4.5, CARD_BG, PURPLE, "Backend  (FastAPI)",
     ["Ingest & Dedup", "AI Enrichment (Gemini)", "Feed Ranking", "Redis Cache", "PostgreSQL"]),
    (9.6,  2.0, 3.4, 3.8, CARD_BG, GREEN,  "Android App",
     ["Onboarding", "For You Feed", "Trending Now", "Saved Articles", "Settings"]),
]
for bx, by, bw, bh, bg, accent, title, items in boxes:
    rect(s, bx, by, bw, bh, bg)
    rect(s, bx, by, bw, 0.06, accent)
    txb(s, title, bx + 0.15, by + 0.15, bw - 0.3, 0.5, size=14, bold=True, color=WHITE)
    for i, item in enumerate(items):
        txb(s, f"▸  {item}", bx + 0.2, by + 0.75 + i * 0.52, bw - 0.4, 0.5, size=12, color=GRAY)

# Arrows
txb(s, "→", 4.1, 3.5, 0.7, 0.6, size=28, bold=True, color=PURPLE, align=PP_ALIGN.CENTER)
txb(s, "→", 9.0, 3.5, 0.7, 0.6, size=28, bold=True, color=GREEN,  align=PP_ALIGN.CENTER)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 3 — Backend Tech Stack
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Backend — Tech Stack", 0.5, 0.25, 10, 0.7, size=32, bold=True, color=WHITE)

cards = [
    (0.5,  1.2, 2.9, "FastAPI",     PURPLE, "Async REST API\nAuto OpenAPI docs\nPydantic v2 models"),
    (3.7,  1.2, 2.9, "PostgreSQL",  CYAN,   "Primary database\nConnection pool (2–20)\nFull-text indexes"),
    (6.9,  1.2, 2.9, "Redis",       AMBER,  "Digest cache\nRate limiting\nURL dedup (ZADD)"),
    (10.1, 1.2, 2.9, "Docker",      GREEN,  "Multi-stage build\ndocker-compose\nHealthcheck"),
    (0.5,  4.3, 2.9, "APScheduler", RED,    "Sync every N hours\nDaily cleanup 03:00\nAsync jobs"),
    (3.7,  4.3, 2.9, "feedparser",  CYAN,   "RSS / Atom parsing\nasyncio.to_thread\nTitle dedup MD5"),
    (6.9,  4.3, 2.9, "Gemini AI",   PURPLE, "Article enrichment\nScore 0–100\nCategory + Tags"),
    (10.1, 4.3, 2.9, "httpx",       GREEN,  "Async HTTP client\nSERP fallback\nImage scraping"),
]
for cx, cy, cw, title, color, desc in cards:
    rect(s, cx, cy, cw, 2.7, CARD_BG)
    rect(s, cx, cy, cw, 0.06, color)
    txb(s, title, cx + 0.15, cy + 0.15, cw - 0.3, 0.5, size=15, bold=True, color=WHITE)
    txb(s, desc,  cx + 0.15, cy + 0.75, cw - 0.3, 1.8, size=12, color=GRAY)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 4 — API Endpoints
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Backend — API Endpoints", 0.5, 0.25, 10, 0.7, size=32, bold=True, color=WHITE)

col1 = [
    ("GET",    "/digest",                    "Personalized feed + seen filtering + Redis cache"),
    ("GET",    "/trending",                  "Popular articles (device_seen COUNT DISTINCT)"),
    ("POST",   "/articles/mark-seen",        "Mark seen → update interest profile"),
    ("GET",    "/articles",                  "Paginated article list"),
    ("GET",    "/categories/{cat}/articles", "Category-filtered articles"),
    ("GET",    "/articles/by-tag",           "Tag-based search"),
]
col2 = [
    ("POST",   "/profile",                   "Upsert user profile + invalidate cache"),
    ("GET",    "/profile/{deviceId}",        "Get profile by device"),
    ("POST",   "/categories/{cat}/feeds",    "Subscribe to RSS feed (shared global pool)"),
    ("DELETE", "/categories/{cat}/feeds",    "Unsubscribe from feed"),
    ("POST",   "/ingest",                    "Trigger RSS + SERP ingest"),
    ("GET",    "/health",                    "DB + Redis health check"),
]

method_colors = {"GET": GREEN, "POST": PURPLE, "DELETE": RED, "PUT": AMBER}

for i, (method, path, desc) in enumerate(col1):
    y = 1.3 + i * 0.85
    tag_box(s, method, 0.5, y + 0.02, method_colors.get(method, GRAY), WHITE, 11)
    txb(s, path, 1.4, y, 4.0, 0.4, size=12, bold=True, color=WHITE)
    txb(s, desc, 1.4, y + 0.38, 4.5, 0.4, size=11, color=GRAY)

for i, (method, path, desc) in enumerate(col2):
    y = 1.3 + i * 0.85
    tag_box(s, method, 7.0, y + 0.02, method_colors.get(method, GRAY), WHITE, 11)
    txb(s, path, 7.9, y, 4.0, 0.4, size=12, bold=True, color=WHITE)
    txb(s, desc, 7.9, y + 0.38, 4.8, 0.4, size=11, color=GRAY)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 5 — Database Schema
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Backend — Database Schema", 0.5, 0.25, 10, 0.7, size=32, bold=True, color=WHITE)

tables = [
    (0.5,  1.1, "pending_articles", PURPLE,
     ["id, title, url (UNIQUE)", "category, raw_content", "source_name, image_url",
      "score (0–100), tags (JSON)", "processed, created_at"]),
    (4.5,  1.1, "rss_feeds  ★ new", CYAN,
     ["id, url (UNIQUE)", "category, is_active", "last_fetched",
      "→ global, no duplicates", ""]),
    (8.2,  1.1, "user_feed_subscriptions  ★ new", GREEN,
     ["device_id, feed_id (PK)", "is_active",
      "→ many-to-many", "→ per-user toggle", ""]),
    (0.5,  4.2, "device_seen", AMBER,
     ["device_id, url (PK)", "seen_at", "→ TTL 7 days", "→ trending signal", ""]),
    (4.5,  4.2, "user_profile", PURPLE,
     ["deviceId (PK), name", "favoriteCategories", "language, interestsScore", "lastActive", ""]),
    (8.2,  4.2, "category_config", CYAN,
     ["category, device_id (PK)", "serp_query, serp_region", "serp_enabled", "", ""]),
]
for tx, ty, title, color, fields in tables:
    tw = 4.0 if "subscriptions" in title else 3.5
    rect(s, tx, ty, tw, 3.0, CARD_BG)
    rect(s, tx, ty, tw, 0.05, color)
    txb(s, title, tx + 0.12, ty + 0.1, tw - 0.2, 0.45, size=12, bold=True, color=WHITE)
    for i, f in enumerate(fields[:5]):
        if f:
            txb(s, f"• {f}", tx + 0.12, ty + 0.65 + i * 0.45, tw - 0.2, 0.42, size=11, color=GRAY)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 6 — AI Pipeline
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Backend — AI Enrichment Pipeline", 0.5, 0.25, 11, 0.7, size=32, bold=True, color=WHITE)
txb(s, "Every article is enriched by AI before being served to users",
    0.5, 0.9, 12, 0.45, size=14, color=GRAY)

# Pipeline steps
steps = [
    (0.4,  "①  Raw Article",    CARD_BG, CYAN,   "title + raw_content\ncreated_at, source"),
    (2.9,  "②  AI Cascade",     CARD_BG, PURPLE, "Gemini 2.0 Flash\n→ 1.5 Flash fallback\n→ DeepSeek fallback"),
    (5.4,  "③  Enriched",       CARD_BG, GREEN,  "score 0–100\ncategory (1 word)\ntags [3–5], summary"),
    (7.9,  "④  Image Scrape",   CARD_BG, AMBER,  "OG image extraction\nasync parallel\nwith AI call"),
    (10.4, "⑤  DB Update",      CARD_BG, CYAN,   "UPDATE pending_articles\nprocessed = 1\ncache invalidated"),
]
for sx, title, bg, accent, desc in steps:
    rect(s, sx, 1.6, 2.4, 2.8, bg)
    rect(s, sx, 1.6, 2.4, 0.06, accent)
    txb(s, title, sx + 0.12, 1.7, 2.2, 0.5, size=13, bold=True, color=WHITE)
    txb(s, desc,  sx + 0.12, 2.3, 2.2, 1.8, size=12, color=GRAY)

# Arrows between steps
for ax in [3.25, 5.75, 8.25, 10.75]:
    pass  # skip — visual clutter

txb(s, "→  →  →  →", 2.7, 2.8, 8.0, 0.6, size=22, color=PURPLE_LT, align=PP_ALIGN.CENTER)

# Interest profile update
rect(s, 0.5, 5.0, 12.3, 2.1, CARD_BG)
rect(s, 0.5, 5.0, 12.3, 0.05, PURPLE)
txb(s, "Adaptive Interest Profile", 0.7, 5.1, 6, 0.45, size=14, bold=True, color=WHITE)
txb(s, "update_interest_profile(device_id)  — called on every mark-seen event",
    0.7, 5.55, 11, 0.45, size=12, color=GRAY)
txb(s, "Reads 14-day device_seen history  →  ranks categories by opens  →  updates favoriteCategories  →  invalidates digest cache",
    0.7, 5.95, 12, 0.9, size=12, color=PURPLE_LT)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 7 — Feed Ranking Algorithm
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Backend — Feed Ranking Algorithm", 0.5, 0.25, 11, 0.7, size=32, bold=True, color=WHITE)

rect(s, 0.5, 1.1, 12.3, 2.0, CARD_BG)
rect(s, 0.5, 1.1, 12.3, 0.05, PURPLE)
txb(s, "FeedRanker  (utils.py)", 0.7, 1.2, 6, 0.45, size=14, bold=True, color=WHITE)
txb(s, "final_score  =  base_score  +  recency_bonus  +  interest_boost  −  diversity_penalty",
    0.7, 1.7, 11.5, 0.5, size=14, color=PURPLE_LT, bold=True)

components = [
    ("base_score",        PURPLE, "0–100", "AI quality score assigned during enrichment"),
    ("recency_bonus",     GREEN,  "+0..+20", "Half-life 12h: new articles get max +20, decays exponentially"),
    ("interest_boost",    CYAN,   "+50",   "Category matches user's favoriteCategories → +50 points"),
    ("diversity_penalty", RED,    "−5/art.", "Same category: −5 per extra article  |  Same source: −8 per extra article"),
]
for i, (name, color, val, desc) in enumerate(components):
    y = 3.4 + i * 0.85
    rect(s, 0.5, y, 2.4, 0.65, RGBColor(0x1A, 0x1A, 0x2E))
    txb(s, name, 0.65, y + 0.08, 2.2, 0.5, size=13, bold=True, color=color)
    txb(s, val,  3.1,  y + 0.08, 1.2, 0.5, size=14, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    txb(s, desc, 4.5,  y + 0.1,  8.0, 0.5, size=13, color=GRAY)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 8 — Shared RSS Pool
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Backend — Shared RSS Feed Pool", 0.5, 0.25, 10, 0.7, size=32, bold=True, color=WHITE)
txb(s, "Multiple users subscribing to the same feed = fetched only once",
    0.5, 0.9, 12, 0.45, size=14, color=GRAY)

# Before / After
txb(s, "BEFORE", 1.2, 1.5, 4, 0.5, size=16, bold=True, color=RED, align=PP_ALIGN.CENTER)
txb(s, "AFTER", 7.8, 1.5, 4, 0.5, size=16, bold=True, color=GREEN, align=PP_ALIGN.CENTER)

rect(s, 0.5, 2.0, 5.5, 4.8, CARD_BG)
rect(s, 0.5, 2.0, 5.5, 0.05, RED)
before_items = [
    "rss_sources (url, category, device_id)",
    "UNIQUE(url, category, device_id)",
    "",
    "User A adds arstechnica → 1 row",
    "User B adds arstechnica → 2 rows",
    "User C adds arstechnica → 3 rows",
    "",
    "Scheduler: fetches URL  ×3  ← WASTE",
]
for i, t in enumerate(before_items):
    color = RED if "WASTE" in t else GRAY
    bold = "WASTE" in t
    txb(s, t, 0.7, 2.15 + i * 0.52, 5.1, 0.5, size=12, color=color, bold=bold)

rect(s, 6.8, 2.0, 5.9, 4.8, CARD_BG)
rect(s, 6.8, 2.0, 5.9, 0.05, GREEN)
after_items = [
    "rss_feeds (url UNIQUE)  +  user_feed_subscriptions",
    "",
    "User A adds arstechnica → 1 row in rss_feeds",
    "                           + 1 row in subscriptions",
    "User B adds arstechnica → 0 new rows in rss_feeds",
    "                           + 1 row in subscriptions",
    "",
    "Scheduler: SELECT DISTINCT → fetches URL  ×1  ✓",
]
for i, t in enumerate(after_items):
    color = GREEN if "×1" in t else GRAY
    bold = "×1" in t
    txb(s, t, 7.0, 2.15 + i * 0.52, 5.5, 0.5, size=12, color=color, bold=bold)

txb(s, "→", 6.1, 4.1, 0.7, 0.6, size=28, bold=True, color=PURPLE, align=PP_ALIGN.CENTER)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 9 — Android Tech Stack
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Android — Tech Stack", 0.5, 0.25, 10, 0.7, size=32, bold=True, color=WHITE)

cards = [
    (0.5,  1.2, "Jetpack Compose", PURPLE, "100% declarative UI\nMaterial3 components\nAnimations + gestures"),
    (3.7,  1.2, "Hilt DI",         CYAN,   "Constructor injection\n@HiltViewModel\n@Named bindings"),
    (6.9,  1.2, "Ktor Client",     GREEN,  "Async HTTP\nKotlin Serialization\nJSON deserialization"),
    (10.1, 1.2, "Room Database",   AMBER,  "Local persistence\nFlow-based queries\nOffline support"),
    (0.5,  4.3, "DataStore",       RED,    "Onboarding state\nDevice UUID\nPreferences"),
    (3.7,  4.3, "Coil3",           CYAN,   "Async image loading\nShimmer placeholder\nCaching"),
    (6.9,  4.3, "Navigation",      PURPLE, "Type-safe routes\nBackstack management\nDeep links"),
    (10.1, 4.3, "StateFlow",       GREEN,  "Reactive UI state\nviewModelScope\ncollectAsState()"),
]
for cx, cy, title, color, desc in cards:
    rect(s, cx, cy, 2.9, 2.7, CARD_BG)
    rect(s, cx, cy, 2.9, 0.06, color)
    txb(s, title, cx + 0.15, cy + 0.15, 2.6, 0.5, size=14, bold=True, color=WHITE)
    txb(s, desc,  cx + 0.15, cy + 0.75, 2.6, 1.8, size=12, color=GRAY)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 10 — App Screens & Features
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Android — App Screens & Features", 0.5, 0.25, 11, 0.7, size=32, bold=True, color=WHITE)

screens = [
    ("Onboarding", PURPLE,
     ["2-step interest selection (16 categories)", "Language choice (Română / English)",
      "UUID device ID (SharedPreferences)", "Sync profile to backend on complete",
      "Resettable from Settings"]),
    ("News Feed", CYAN,
     ["Hero Carousel — top 5 personalized articles", "Pull-to-refresh gesture",
      "Trending Now — horizontal row (social signal)", "For You — filtered by interests",
      "Session-based seen tracking (no premature marks)"]),
    ("Explore / Categories", GREEN,
     ["Browse by category", "Category articles list", "Search across all content", "", ""]),
    ("Saved", AMBER,
     ["Bookmark any article", "Room local persistence", "Offline readable", "", ""]),
    ("Settings", RED,
     ["Dark / Light theme toggle", "Language selector", "Current interests chips",
      "Reset interests → onboarding", "Profile name edit"]),
]
for i, (title, color, items) in enumerate(screens):
    cx = 0.5 + i * 2.56
    rect(s, cx, 1.1, 2.4, 6.0, CARD_BG)
    rect(s, cx, 1.1, 2.4, 0.06, color)
    txb(s, title, cx + 0.1, 1.15, 2.2, 0.5, size=13, bold=True, color=WHITE)
    for j, item in enumerate(items):
        if item:
            txb(s, f"• {item}", cx + 0.1, 1.8 + j * 0.95, 2.2, 0.9, size=11, color=GRAY)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 11 — Trending Algorithm
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Android — Trending Now Feature", 0.5, 0.25, 11, 0.7, size=32, bold=True, color=WHITE)

# SQL box
rect(s, 0.5, 1.1, 8.0, 3.6, RGBColor(0x0D, 0x0D, 0x1A))
rect(s, 0.5, 1.1, 8.0, 0.05, CYAN)
sql = (
    "SELECT a.title, a.url, a.category, ...,\n"
    "       COUNT(DISTINCT s.device_id) AS open_count\n"
    "FROM pending_articles a\n"
    "JOIN device_seen s ON a.url = s.url\n"
    "WHERE a.image_url IS NOT NULL\n"
    "  AND s.seen_at >= NOW() - INTERVAL '48 hours'\n"
    "GROUP BY a.id\n"
    "ORDER BY open_count DESC, a.created_at DESC\n"
    "LIMIT 20"
)
txb(s, sql, 0.7, 1.25, 7.7, 3.3, size=12, color=CYAN)

# Explanation
rect(s, 9.0, 1.1, 3.8, 3.6, CARD_BG)
rect(s, 9.0, 1.1, 3.8, 0.05, RED)
txb(s, "How it works", 9.15, 1.2, 3.5, 0.45, size=13, bold=True, color=WHITE)
points = [
    "Counts distinct users who opened each article",
    "48-hour rolling window",
    "Excludes already-seen articles (per device)",
    "Fallback to top AI score if < 5 social results",
    "Refreshed on every pull-to-refresh",
]
for i, p in enumerate(points):
    txb(s, f"▸  {p}", 9.15, 1.75 + i * 0.6, 3.5, 0.55, size=11, color=GRAY)

# Android side
rect(s, 0.5, 5.0, 12.3, 2.1, CARD_BG)
rect(s, 0.5, 5.0, 12.3, 0.05, RED)
txb(s, "Android integration", 0.7, 5.1, 6, 0.45, size=14, bold=True, color=WHITE)
txb(s, "NewsFeedViewModel.loadTrending()  →  GET /trending?deviceId=&hours=48",
    0.7, 5.55, 9.5, 0.45, size=12, color=GRAY)
txb(s, "LazyRow  ←  TrendingCard (200dp wide, image + category chip + title + source)",
    0.7, 5.95, 11, 0.8, size=12, color=PURPLE_LT)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 12 — Deployment
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

txb(s, "Deployment — Docker Compose", 0.5, 0.25, 10, 0.7, size=32, bold=True, color=WHITE)

services = [
    ("data-agregate-api",   PURPLE, "dataagregate-app",    "Port 8085\nPython 3.11-slim\nMulti-stage build"),
    ("data-agregate-db",    CYAN,   "postgres:16-alpine",  "Primary DB\nThreadedConnectionPool\n2–20 connections"),
    ("data-agregate-redis", GREEN,  "redis:7-alpine",      "Digest cache\nURL dedup ZADD\nRate limiting"),
]
for i, (name, color, image, desc) in enumerate(services):
    cx = 0.5 + i * 4.4
    rect(s, cx, 1.3, 4.0, 3.5, CARD_BG)
    rect(s, cx, 1.3, 4.0, 0.06, color)
    txb(s, name,  cx + 0.15, 1.4,  3.7, 0.5, size=14, bold=True, color=WHITE)
    txb(s, image, cx + 0.15, 1.95, 3.7, 0.4, size=12, color=color)
    txb(s, desc,  cx + 0.15, 2.45, 3.7, 1.8, size=12, color=GRAY)

# Startup sequence
rect(s, 0.5, 5.0, 12.3, 2.1, CARD_BG)
rect(s, 0.5, 5.0, 12.3, 0.05, PURPLE)
txb(s, "Startup sequence (lifespan)", 0.7, 5.1, 8, 0.45, size=14, bold=True, color=WHITE)
startup = "init_pool()  →  init_db()  →  migrate_to_shared_feeds()  →  fix_missing_data()  →  scheduler.start()"
txb(s, startup, 0.7, 5.6, 11.8, 0.5, size=13, color=PURPLE_LT)
txb(s, "Healthcheck: GET /health  →  DB ping + Redis ping + articles count",
    0.7, 6.1, 11, 0.6, size=12, color=GRAY)


# ═══════════════════════════════════════════════════════════════════════════════
# SLIDE 13 — Summary
# ═══════════════════════════════════════════════════════════════════════════════
s = add_slide()
accent_bar(s)

circle(s, 8.0, -0.5, 6, RGBColor(0x3D, 0x1A, 0x6E))

txb(s, "DataAgregate PRO", 0.7, 1.2, 8, 1.0, size=44, bold=True, color=WHITE)
txb(s, "Built for scale. Personalized by AI. Shared by design.",
    0.7, 2.3, 9, 0.6, size=18, color=PURPLE_LT)

highlights = [
    (GREEN,  "Shared RSS pool",          "Zero duplication — same URL fetched once regardless of subscribers"),
    (PURPLE, "AI-enriched articles",     "Gemini cascade: score, category, tags, summary, translated title"),
    (CYAN,   "Adaptive interests",       "Reading history → auto-updates favoriteCategories every mark-seen"),
    (AMBER,  "Trending social signal",   "COUNT(DISTINCT device_id) on device_seen → popular articles surface"),
    (RED,    "Pull-to-refresh",          "Native gesture → fresh digest + trending in one swipe"),
    (GREEN,  "Onboarding + reset",       "Interest chips + language → persisted in DataStore + synced to backend"),
]
for i, (color, title, desc) in enumerate(highlights):
    y = 3.2 + i * 0.65
    circle(s, 0.7, y + 0.05, 0.25, color)
    txb(s, title, 1.15, y, 3.2, 0.55, size=13, bold=True, color=WHITE)
    txb(s, desc,  4.5,  y, 8.5, 0.55, size=13, color=GRAY)


# ── Save ─────────────────────────────────────────────────────────────────────
path = r"C:\Users\Podut\Desktop\Proiecte\DataAgregate\DataAgregate_Presentation.pptx"
prs.save(path)
print(f"Saved: {path}")
print(f"Slides: {len(prs.slides)}")
