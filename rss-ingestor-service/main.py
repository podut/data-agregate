from contextlib import asynccontextmanager
import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from logging_config import setup_logging, get_logger
from database import init_db, init_pool, migrate_to_shared_feeds, db_conn, PLACEHOLDER
from ai_service import fix_missing_data
from scheduler_task import scheduler, scheduled_sync, cleanup_old_articles
from models import HealthResponse
import cache_service

# Boot logging before anything else
setup_logging()
logger = get_logger(__name__)

# ─── Routers ─────────────────────────────────────────────────────────────────
from routers.articles    import router as articles_router
from routers.categories  import router as categories_router
from routers.profile     import router as profile_router
from routers.scheduler   import router as scheduler_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_pool()
    init_db()
    migrate_to_shared_feeds()

    from database import get_setting
    interval = int(get_setting("scheduler_interval", "8"))

    scheduler.add_job(scheduled_sync,        'interval', hours=interval, id="hourly_sync",    replace_existing=True)
    scheduler.add_job(cleanup_old_articles,  'cron',     hour=3, minute=0, id="daily_cleanup", replace_existing=True)
    scheduler.start()
    logger.info("[Lifespan] Scheduler started — sync every %dh, cleanup daily at 03:00", interval)

    try:
        fix_missing_data()
    except Exception as e:
        logger.error("[Lifespan] fix_missing_data error: %s", e)

    yield

    scheduler.shutdown()
    logger.info("[Lifespan] Shutdown complete.")


app = FastAPI(title="RSS Aggregator PRO", version="2.0.0", lifespan=lifespan)

# CORS configuration
allowed_origins = os.getenv("ALLOWED_ORIGINS", "http://localhost:3000").split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(articles_router)
app.include_router(categories_router)
app.include_router(profile_router)
app.include_router(scheduler_router)


# ─── Root & Health ────────────────────────────────────────────────────────────

@app.get("/")
def read_root():
    return {"status": "RSS Aggregator API is running", "version": "2.0.0"}


@app.get("/health", response_model=HealthResponse)
def health_check():
    status = HealthResponse(status="healthy", db="ok", redis="ok")

    try:
        with db_conn(readonly=True) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM pending_articles")
            status.articles_count = cursor.fetchone()[0]
    except Exception as e:
        logger.error("[Health] DB check failed: %s", e)
        status.db     = "error"
        status.status = "degraded"

    try:
        cache_service._r().ping()
    except Exception as e:
        logger.warning("[Health] Redis check failed: %s", e)
        status.redis  = "error"
        status.status = "degraded"

    return status


if __name__ == "__main__":
    import uvicorn
    init_db()
    host = os.getenv("API_HOST", "0.0.0.0")
    port = int(os.getenv("API_PORT", "8085"))
    uvicorn.run(app, host=host, port=port)
