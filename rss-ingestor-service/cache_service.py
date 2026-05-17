import redis
import json
from config import REDIS_HOST, REDIS_PORT

# Initialize Redis client
# Decode_responses=True helps handle strings instead of bytes
redis_client = redis.Redis(
    host=REDIS_HOST, 
    port=REDIS_PORT, 
    db=0, 
    decode_responses=True
)

def get_cached_digest(device_id: str):
    """Retrieve cached digest for a specific device."""
    try:
        key = f"digest:{device_id}"
        cached = redis_client.get(key)
        if cached:
            return json.loads(cached)
    except Exception as e:
        print(f"[Redis] Error getting cached digest: {e}")
    return None

def set_cached_digest(device_id: str, data: dict, expire_seconds: int = 900):
    """Cache digest for 15 minutes (default)."""
    try:
        key = f"digest:{device_id}"
        redis_client.setex(key, expire_seconds, json.dumps(data))
    except Exception as e:
        print(f"[Redis] Error setting cached digest: {e}")

def is_url_seen(url: str) -> bool:
    """Check if URL was recently processed using a Redis Set."""
    try:
        return redis_client.sismember("processed_urls", url)
    except:
        return False

def mark_url_processed(url: str, expire_days: int = 7):
    """Add URL to processed set and handle expiration."""
    try:
        redis_client.sadd("processed_urls", url)
        # Setele nu au TTL per element, dar putem folosi un set temporar 
        # sau o cheie separată pentru a expira setul complet dacă devine prea mare
    except:
        pass

def set_processing_lock(lock_name: str, timeout: int = 300) -> bool:
    """Set a distributed lock to prevent concurrent processing."""
    return redis_client.set(f"lock:{lock_name}", "true", ex=timeout, nx=True)

def release_processing_lock(lock_name: str):
    """Release the distributed lock."""
    redis_client.delete(f"lock:{lock_name}")

def get_job_status(job_id: str) -> str:
    """Get the status of a long-running job."""
    return redis_client.get(f"job_status:{job_id}")

def set_job_status(job_id: str, status: str, expire: int = 3600):
    """Set the status of a long-running job."""
    redis_client.setex(f"job_status:{job_id}", expire, status)

def check_rate_limit(key: str, limit: int = 10, period: int = 60) -> bool:
    """
    Simple rate limiter. 
    Returns True if request is allowed, False if limit exceeded.
    """
    try:
        current = redis_client.get(f"ratelimit:{key}")
        if current and int(current) >= limit:
            return False
        
        pipe = redis_client.pipeline()
        pipe.incr(f"ratelimit:{key}")
        pipe.expire(f"ratelimit:{key}", period)
        pipe.execute()
        return True
    except:
        return True # Default to allow if Redis fails
def clear_all_cache():
    """Wipe all keys, including rate limits, digests, and processed URLs."""
    try:
        redis_client.flushdb()
        print("[Redis] All cache and deduplication data cleared.")
    except Exception as e:
        print(f"[Redis] Error flushing DB: {e}")
