from fastapi import APIRouter, BackgroundTasks
from scheduler_task import scheduler, scheduled_sync
from rss_service import process_feeds_task
from serp_service import fetch_for_category
from models import RSSIngestRequest, SchedulerConfig, StatusResponse
from database import set_setting, get_setting
import cache_service
from logging_config import get_logger

logger = get_logger(__name__)
router = APIRouter(tags=["Scheduler"])


@router.post("/ingest", response_model=StatusResponse)
async def ingest_rss(request: RSSIngestRequest, background_tasks: BackgroundTasks):
    cache_key = f"ingest:{request.deviceId or 'global'}:{request.category}"
    if not cache_service.check_rate_limit(cache_key, limit=5, period=60):
        from fastapi import HTTPException
        raise HTTPException(status_code=429, detail="Too many ingestion requests. Try again later.")

    logger.info("[API] POST /ingest category='%s' device='%s'", request.category, request.deviceId)
    background_tasks.add_task(process_feeds_task, request.urls, request.category)
    background_tasks.add_task(fetch_for_category, request.category, request.category, "us")
    return {"status": "Accepted"}


@router.post("/sync-all", response_model=StatusResponse)
async def sync_all():
    await scheduled_sync()
    return {"status": "Started"}


@router.get("/sync", response_model=StatusResponse)
async def trigger_sync():
    await scheduled_sync()
    return {"status": "Sync triggered successfully"}


@router.post("/scheduler/run-now", response_model=StatusResponse)
async def run_now():
    await scheduled_sync()
    return {"status": "done"}


@router.put("/scheduler/config")
def update_scheduler(body: SchedulerConfig):
    set_setting("scheduler_interval", str(body.interval_hours))
    try:
        scheduler.remove_job("hourly_sync")
    except Exception:
        pass
    scheduler.add_job(scheduled_sync, 'interval', hours=body.interval_hours, id="hourly_sync", replace_existing=True)
    logger.info("[SCHEDULER] Interval updated to %d hours", body.interval_hours)
    return {"status": "updated", "interval_hours": body.interval_hours}


@router.get("/scheduler/config")
def get_scheduler_config():
    interval = int(get_setting("scheduler_interval", "8"))
    return {"interval_hours": interval}


@router.put("/settings/language")
def update_default_language(language: str):
    set_setting("default_language", language)
    logger.info("[Settings] Default language updated to %s", language)
    return {"status": "updated", "default_language": language}


@router.get("/settings/language")
def get_default_language():
    return {"default_language": get_setting("default_language", "ro")}

