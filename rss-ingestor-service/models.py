from pydantic import BaseModel
from typing import List, Optional

class RSSIngestRequest(BaseModel):
    urls: List[str]
    category: str = "General"
    deviceId: Optional[str] = None

class CategoryCreate(BaseModel):
    name: str
    rss_urls: List[str] = []
    deviceId: Optional[str] = None

class RssFeedRequest(BaseModel):
    url: str
    deviceId: Optional[str] = None
    is_active: Optional[bool] = None

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
