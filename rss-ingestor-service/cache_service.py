import redis
import json
import time
import re
import hashlib
from config import REDIS_HOST, REDIS_PORT
from logging_config import get_logger

logger = get_logger(__name__)

try:
    redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0, decode_responses=True)
    redis_client.ping()
    logger.info("Redis connected at %s:%s", REDIS_HOST, REDIS_PORT)
except Exception as e:
    logger.warning("Cannot connect to Redis at %s:%s: %s", REDIS_HOST, REDIS_PORT, e)
    redis_client = None

_PROCESSED_URLS_KEY  = "processed_urls_zset"
_TITLE_FP_KEY        = "title_fingerprints_zset"
_URL_TTL_DAYS        = 7
_TITLE_TTL_DAYS      = 7


def _r():
    if redis_client is None:
        raise RuntimeError("Redis unavailable")
    return redis_client


# ─── Digest Cache ─────────────────────────────────────────────────────────────

def get_cached_digest(device_id: str):
    try:
        cached = _r().get(f"digest:{device_id}")
        if cached:
            return json.loads(cached)
    except Exception as e:
        logger.error("Error getting cached digest: %s", e)
    return None


def set_cached_digest(device_id: str, data: dict, expire_seconds: int = 900):
    try:
        _r().setex(f"digest:{device_id}", expire_seconds, json.dumps(data))
    except Exception as e:
        logger.error("Error setting cached digest: %s", e)


def invalidate_digest(device_id: str):
    """Force refresh on next /digest call for this device."""
    try:
        _r().delete(f"digest:{device_id}")
    except Exception:
        pass


# ─── URL Deduplication ────────────────────────────────────────────────────────

def is_url_seen(url: str) -> bool:
    try:
        score = _r().zscore(_PROCESSED_URLS_KEY, url)
        if score is None:
            return False
        return score > time.time() - (_URL_TTL_DAYS * 86400)
    except:
        return False


def mark_url_processed(url: str):
    try:
        r = _r()
        r.zadd(_PROCESSED_URLS_KEY, {url: time.time()})
        if int(time.time()) % 100 == 0:
            r.zremrangebyscore(_PROCESSED_URLS_KEY, "-inf", time.time() - (_URL_TTL_DAYS * 86400))
    except:
        pass


# ─── Title Fingerprint Deduplication ─────────────────────────────────────────

def _title_fingerprint(title: str) -> str:
    """Normalize title and return a short hash for near-duplicate detection."""
    normalized = re.sub(r'[^\w\s]', '', title.lower())
    normalized = re.sub(r'\s+', ' ', normalized).strip()
    return hashlib.md5(normalized[:80].encode()).hexdigest()[:16]


def is_title_duplicate(title: str) -> bool:
    """Returns True if a very similar title was recently processed."""
    if not title:
        return False
    try:
        fp = _title_fingerprint(title)
        score = _r().zscore(_TITLE_FP_KEY, fp)
        if score is None:
            return False
        return score > time.time() - (_TITLE_TTL_DAYS * 86400)
    except:
        return False


def mark_title_processed(title: str):
    try:
        r = _r()
        fp = _title_fingerprint(title)
        r.zadd(_TITLE_FP_KEY, {fp: time.time()})
        if int(time.time()) % 200 == 0:
            r.zremrangebyscore(_TITLE_FP_KEY, "-inf", time.time() - (_TITLE_TTL_DAYS * 86400))
    except:
        pass


# ─── Distributed Lock ─────────────────────────────────────────────────────────

def set_processing_lock(lock_name: str, timeout: int = 300) -> bool:
    try:
        return bool(_r().set(f"lock:{lock_name}", "true", ex=timeout, nx=True))
    except:
        return True


def release_processing_lock(lock_name: str):
    try:
        _r().delete(f"lock:{lock_name}")
    except:
        pass


# ─── Rate Limiting ────────────────────────────────────────────────────────────

def check_rate_limit(key: str, limit: int = 10, period: int = 60) -> bool:
    try:
        r = _r()
        rk = f"ratelimit:{key}"
        current = r.get(rk)
        if current and int(current) >= limit:
            return False
        pipe = r.pipeline()
        pipe.incr(rk)
        pipe.expire(rk, period)
        pipe.execute()
        return True
    except:
        return True


# ─── Job Status ───────────────────────────────────────────────────────────────

def get_job_status(job_id: str) -> str:
    try:
        return _r().get(f"job_status:{job_id}")
    except:
        return None


def set_job_status(job_id: str, status: str, expire: int = 3600):
    try:
        _r().setex(f"job_status:{job_id}", expire, status)
    except:
        pass


# ─── Maintenance ──────────────────────────────────────────────────────────────

def clear_all_cache():
    try:
        _r().flushdb()
        logger.info("All cache and deduplication data cleared.")
    except Exception as e:
        logger.error("Error flushing Redis DB: %s", e)
