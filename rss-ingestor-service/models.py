from pydantic import BaseModel, field_validator
from typing import List, Optional


# ─── Request Models ───────────────────────────────────────────────────────────

class RSSIngestRequest(BaseModel):
    urls: List[str]
    category: str = "General"
    deviceId: Optional[str] = None


class CategoryCreate(BaseModel):
    name: str
    rss_urls: List[str] = []
    deviceId: Optional[str] = None

    @field_validator("name")
    @classmethod
    def name_not_empty(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("Category name cannot be empty")
        if len(v) > 100:
            raise ValueError("Category name too long (max 100 chars)")
        return v


class RssFeedRequest(BaseModel):
    url: str
    deviceId: Optional[str] = None
    is_active: Optional[bool] = None

    @field_validator("url")
    @classmethod
    def url_not_empty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("URL cannot be empty")
        return v.strip()


class SerpConfigUpdate(BaseModel):
    query: str
    deviceId: Optional[str] = None
    region: str = "us"
    enabled: bool = True


class UserProfileSync(BaseModel):
    deviceId: str
    favoriteCategories: List[str] = []
    name: str = "Reader"
    language: str = "en"


class SchedulerConfig(BaseModel):
    interval_hours: int = 1


class MarkSeenRequest(BaseModel):
    deviceId: str
    urls: List[str]


# ─── Response Models ──────────────────────────────────────────────────────────

class ArticleResponse(BaseModel):
    title: str
    link: str
    category: str
    summary: str
    source: str
    image: Optional[str] = None
    publish_date: str
    score: int
    tags: List[str] = []


class ArticleListResponse(BaseModel):
    id: int
    title: str
    url: str
    category: str
    summary: str
    source_name: str
    image_url: Optional[str] = None
    created_at: str
    tags: List[str] = []


class DigestResponse(BaseModel):
    digest_date: str
    news: List[ArticleResponse]
    message: Optional[str] = None


class FeedInfoResponse(BaseModel):
    url: str
    is_active: bool


class SerpInfo(BaseModel):
    query: Optional[str]
    region: str
    enabled: bool


class CategoryResponse(BaseModel):
    name: str
    feeds: List[FeedInfoResponse]
    article_count: int
    serp: SerpInfo


class ProfileResponse(BaseModel):
    deviceId: str
    name: str
    favoriteCategories: List[str]
    interestsScore: Optional[int] = None
    lastActive: Optional[int] = None
    language: str = "en"


class HealthResponse(BaseModel):
    status: str
    db: str
    redis: str
    articles_count: Optional[int] = None


class StatusResponse(BaseModel):
    status: str
