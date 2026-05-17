import os
from dotenv import load_dotenv

# Încarcă .env dacă există
load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
SERP_API_KEY   = os.getenv("SERP_API_KEY", "")

# Detectăm dacă rulăm în Docker
IS_DOCKER = os.path.exists("/.dockerenv") or os.environ.get("IS_DOCKER") == "true"

DB_PATH = "/app/data/data_agregate_local.db" if IS_DOCKER else "../data_agregate_local.db"
CSV_PATH = "/app/data/aggregated_news.csv" if IS_DOCKER else "../aggregated_news.csv"

# MySQL Config
MYSQL_HOST = os.getenv("MYSQL_HOST", "mysql")
MYSQL_USER = os.getenv("MYSQL_USER", "user_agregate")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "user_password")
MYSQL_DATABASE = os.getenv("MYSQL_DATABASE", "data_agregate")
MYSQL_PORT = int(os.getenv("MYSQL_PORT", "3306"))

# Postgres Config
POSTGRES_HOST = os.getenv("POSTGRES_HOST", "localhost")
POSTGRES_USER = os.getenv("POSTGRES_USER", "postgres")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "postgres")
POSTGRES_DATABASE = os.getenv("POSTGRES_DATABASE", "data_agregate")
POSTGRES_PORT = int(os.getenv("POSTGRES_PORT", "5432"))

# Redis Config
REDIS_HOST = os.getenv("REDIS_HOST", "redis")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))

USE_POSTGRES = os.getenv("USE_POSTGRES", "true").lower() == "true"
USE_MYSQL = os.getenv("USE_MYSQL", "false").lower() == "true"

DIGEST_PROMPT = """You are the backend intelligence of a personalized RSS reader app.
Input: Multiple RSS feed items as JSON array.
Task: Create a personalized digest. Select max 100 articles.
Return ONLY valid JSON."""

SCHEDULER_HOUR = int(os.getenv("SCHEDULE_HOUR", "9"))
SCHEDULER_MINUTE = int(os.getenv("SCHEDULE_MINUTE", "0"))
