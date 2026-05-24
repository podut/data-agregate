import time
from fastapi import APIRouter
from database import db_conn, PLACEHOLDER
from models import UserProfileSync, ProfileResponse, StatusResponse
from config import USE_MYSQL, USE_POSTGRES
import cache_service
from logging_config import get_logger

logger = get_logger(__name__)
router = APIRouter(prefix="/profile", tags=["Profile"])


@router.post("", response_model=StatusResponse)
def upsert_profile(body: UserProfileSync):
    favs = ",".join([c.strip() for c in body.favoriteCategories if c and c.strip()])
    with db_conn() as conn:
        cursor = conn.cursor()
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
    # Invalidate digest cache so next call reflects new favorites
    cache_service.invalidate_digest(body.deviceId)
    logger.info("[Profile] Upserted profile for %s", body.deviceId)
    return {"status": "ok"}


@router.get("/{deviceId}", response_model=ProfileResponse)
def get_profile(deviceId: str):
    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()
        cursor.execute(
            f"SELECT deviceId, name, favoriteCategories, interestsScore, lastActive, language "
            f"FROM user_profile WHERE deviceId = {PLACEHOLDER}",
            (deviceId,)
        )
        row = cursor.fetchone()
    if not row:
        return ProfileResponse(deviceId=deviceId, name="Reader", favoriteCategories=[], language="en")
    return ProfileResponse(
        deviceId=row[0],
        name=row[1],
        favoriteCategories=[c for c in (row[2] or "").split(",") if c.strip()],
        interestsScore=row[3],
        lastActive=row[4],
        language=row[5] or "en",
    )
