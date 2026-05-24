import sqlite3
import os
from contextlib import contextmanager

from config import (
    DB_PATH, USE_MYSQL, MYSQL_HOST, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE, MYSQL_PORT,
    USE_POSTGRES, POSTGRES_HOST, POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DATABASE, POSTGRES_PORT
)
from logging_config import get_logger

logger = get_logger(__name__)

PLACEHOLDER = "%s" if (USE_MYSQL or USE_POSTGRES) else "?"

# ─── Connection Pool ──────────────────────────────────────────────────────────

_pg_pool = None
_mysql_pool = None


def init_pool():
    """Initialize the connection pool. Must be called once at startup."""
    global _pg_pool, _mysql_pool
    if USE_POSTGRES:
        from psycopg2 import pool as pg_pool
        _pg_pool = pg_pool.ThreadedConnectionPool(
            minconn=2, maxconn=20,
            host=POSTGRES_HOST, user=POSTGRES_USER,
            password=POSTGRES_PASSWORD, database=POSTGRES_DATABASE, port=POSTGRES_PORT
        )
        logger.info("PostgreSQL connection pool initialized (2–20 connections)")
    elif USE_MYSQL:
        import mysql.connector.pooling as mysql_pool
        _mysql_pool = mysql_pool.MySQLConnectionPool(
            pool_name="data_agregate",
            pool_size=10,
            host=MYSQL_HOST, user=MYSQL_USER,
            password=MYSQL_PASSWORD, database=MYSQL_DATABASE, port=MYSQL_PORT
        )
        logger.info("MySQL connection pool initialized (10 connections)")


def _get_conn():
    if USE_POSTGRES:
        if _pg_pool:
            return _pg_pool.getconn()
        import psycopg2
        return psycopg2.connect(
            host=POSTGRES_HOST, user=POSTGRES_USER,
            password=POSTGRES_PASSWORD, database=POSTGRES_DATABASE, port=POSTGRES_PORT
        )
    elif USE_MYSQL:
        if _mysql_pool:
            return _mysql_pool.get_connection()
        import mysql.connector
        return mysql.connector.connect(
            host=MYSQL_HOST, user=MYSQL_USER, password=MYSQL_PASSWORD,
            database=MYSQL_DATABASE, port=MYSQL_PORT, buffered=True
        )
    else:
        return sqlite3.connect(DB_PATH)


def _return_conn(conn):
    if USE_POSTGRES and _pg_pool:
        _pg_pool.putconn(conn)
    else:
        conn.close()


def get_db_connection():
    """Raw connection — prefer db_conn() context manager instead."""
    return _get_conn()


@contextmanager
def db_conn(readonly: bool = False):
    """
    Context manager for DB access.
    Commits on success, rolls back on error, always returns connection to pool.
    Use readonly=True for SELECT-only blocks to skip the commit.
    """
    conn = _get_conn()
    try:
        yield conn
        if not readonly:
            conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        _return_conn(conn)


# ─── Schema ───────────────────────────────────────────────────────────────────

def init_db(clear_existing: bool = False):
    if not (USE_MYSQL or USE_POSTGRES):
        os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)

    with db_conn() as conn:
        cursor = conn.cursor()

        if clear_existing:
            logger.info("[DB] Clearing existing tables...")
            tables = [
                "user_feed_subscriptions", "rss_feeds",
                "pending_articles", "rss_sources", "category_config",
                "device_seen", "user_profile", "settings",
            ]
            for t in tables:
                try:
                    if USE_POSTGRES or USE_MYSQL:
                        cursor.execute(f"DROP TABLE IF EXISTS {t} CASCADE")
                    else:
                        cursor.execute(f"DROP TABLE IF EXISTS {t}")
                except Exception as e:
                    logger.warning("Could not drop table %s: %s", t, e)

        auto_inc  = "SERIAL" if USE_POSTGRES else ("AUTO_INCREMENT" if USE_MYSQL else "AUTOINCREMENT")
        text_type = "TEXT"
        pk_type   = "" if USE_POSTGRES else ("INT" if USE_MYSQL else "INTEGER")
        url_len, cat_len, dev_len = 1000, 100, 100

        pk_pending = f"id {auto_inc} PRIMARY KEY" if USE_POSTGRES else f"id {pk_type} PRIMARY KEY {auto_inc}"
        pk_sources = f"id {'SERIAL' if USE_POSTGRES else f'{pk_type} PRIMARY KEY {auto_inc}'}"

        pk_feeds = f"id {auto_inc} PRIMARY KEY" if USE_POSTGRES else f"id {pk_type} PRIMARY KEY {auto_inc}"

        queries = [
            # ── Global feed registry — one row per unique URL ──────────────────
            f'''CREATE TABLE IF NOT EXISTS rss_feeds (
                {pk_feeds},
                url         VARCHAR({url_len}) UNIQUE NOT NULL,
                category    VARCHAR({cat_len}),
                is_active   BOOLEAN DEFAULT TRUE,
                last_fetched TIMESTAMP
            )''',
            # ── Per-user feed subscriptions — many-to-many ────────────────────
            f'''CREATE TABLE IF NOT EXISTS user_feed_subscriptions (
                device_id VARCHAR({dev_len}) NOT NULL,
                feed_id   INTEGER NOT NULL,
                is_active BOOLEAN DEFAULT TRUE,
                PRIMARY KEY (device_id, feed_id)
            )''',
            f'''CREATE TABLE IF NOT EXISTS pending_articles (
                {pk_pending},
                title TEXT,
                url VARCHAR({url_len}) UNIQUE,
                category TEXT,
                raw_content {text_type},
                source_name TEXT,
                image_url TEXT,
                score INTEGER DEFAULT 70,
                processed INTEGER DEFAULT 0,
                tags TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )''',
            f'''CREATE TABLE IF NOT EXISTS rss_sources (
                {pk_sources},
                url VARCHAR({url_len}),
                category VARCHAR({cat_len}),
                device_id VARCHAR({dev_len}),
                is_active BOOLEAN DEFAULT TRUE,
                UNIQUE(url, category, device_id)
            )''',
            f'''CREATE TABLE IF NOT EXISTS category_config (
                category    VARCHAR({cat_len}),
                device_id   VARCHAR({dev_len}),
                serp_query  TEXT,
                serp_region VARCHAR(50) DEFAULT 'us',
                serp_enabled INTEGER DEFAULT 0,
                PRIMARY KEY (category, device_id)
            )''',
            f'''CREATE TABLE IF NOT EXISTS user_profile (
                deviceId           VARCHAR({dev_len}) NOT NULL PRIMARY KEY,
                name               TEXT NOT NULL,
                favoriteCategories TEXT NOT NULL,
                interestsScore     INTEGER NOT NULL,
                lastActive         BIGINT NOT NULL,
                language           VARCHAR(10) DEFAULT 'en'
            )''',
            f'''CREATE TABLE IF NOT EXISTS settings (
                key VARCHAR(255) PRIMARY KEY,
                value TEXT
            )''',
            f'''CREATE TABLE IF NOT EXISTS device_seen (
                device_id VARCHAR({dev_len}) NOT NULL,
                url VARCHAR({url_len}) NOT NULL,
                seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (device_id, url)
            )'''
        ]

        for q in queries:
            try:
                cursor.execute(q)
            except Exception as e:
                logger.error("Error creating table: %s", e)

        try:
            if USE_POSTGRES or not USE_MYSQL:
                cursor.execute('CREATE INDEX IF NOT EXISTS idx_device_seen_at    ON device_seen(device_id, seen_at)')
                cursor.execute('CREATE INDEX IF NOT EXISTS idx_articles_category ON pending_articles(category)')
                cursor.execute('CREATE INDEX IF NOT EXISTS idx_articles_processed ON pending_articles(processed)')
                cursor.execute('CREATE INDEX IF NOT EXISTS idx_rss_feeds_cat     ON rss_feeds(category)')
                cursor.execute('CREATE INDEX IF NOT EXISTS idx_user_feed_subs    ON user_feed_subscriptions(device_id)')
            else:
                for idx in [
                    'CREATE INDEX idx_device_seen_at    ON device_seen(device_id, seen_at)',
                    'CREATE INDEX idx_articles_category ON pending_articles(category)',
                    'CREATE INDEX idx_articles_processed ON pending_articles(processed)',
                    'CREATE INDEX idx_rss_feeds_cat     ON rss_feeds(category)',
                    'CREATE INDEX idx_user_feed_subs    ON user_feed_subscriptions(device_id)',
                ]:
                    try:
                        cursor.execute(idx)
                    except Exception as e:
                        logger.debug("[DB] Index already exists or skip: %s", e)
        except Exception as e:
            logger.error("[DB] Error creating indexes: %s", e)

        if USE_POSTGRES:
            cursor.execute("INSERT INTO settings (key, value) VALUES ('scheduler_interval', '2') ON CONFLICT (key) DO NOTHING")
            cursor.execute("INSERT INTO settings (key, value) VALUES ('default_language', 'ro') ON CONFLICT (key) DO NOTHING")
        elif USE_MYSQL:
            cursor.execute("INSERT IGNORE INTO settings (`key`, `value`) VALUES ('scheduler_interval', '2')")
            cursor.execute("INSERT IGNORE INTO settings (`key`, `value`) VALUES ('default_language', 'ro')")
        else:
            cursor.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('scheduler_interval', '2')")
            cursor.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('default_language', 'ro')")

    logger.info("[DB] Initialization complete.")


def get_setting(key: str, default: str = None) -> str:
    with db_conn(readonly=True) as conn:
        cursor = conn.cursor()
        if USE_POSTGRES:
            cursor.execute("SELECT value FROM settings WHERE key = %s", (key,))
        elif USE_MYSQL:
            cursor.execute("SELECT `value` FROM settings WHERE `key` = %s", (key,))
        else:
            cursor.execute("SELECT value FROM settings WHERE key = ?", (key,))
        row = cursor.fetchone()
        return row[0] if row else default


def migrate_to_shared_feeds():
    """
    One-time migration: copies legacy rss_sources rows into
    rss_feeds (global, deduped by URL) + user_feed_subscriptions (per-user).
    Idempotent — safe to call on every startup.
    """
    try:
        with db_conn(readonly=True) as conn:
            cursor = conn.cursor()
            if USE_POSTGRES:
                cursor.execute(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'rss_sources'"
                )
            elif USE_MYSQL:
                cursor.execute(
                    "SELECT COUNT(*) FROM information_schema.tables "
                    "WHERE table_schema = DATABASE() AND table_name = 'rss_sources'"
                )
            else:
                cursor.execute(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='rss_sources'"
                )
            if cursor.fetchone()[0] == 0:
                return

            cursor.execute("SELECT url, category, device_id, is_active FROM rss_sources")
            rows = cursor.fetchall()

        if not rows:
            return

        migrated = 0
        with db_conn() as conn:
            cursor = conn.cursor()
            for url, category, device_id, is_active in rows:
                if not url:
                    continue
                dev = device_id or "global"

                if USE_POSTGRES:
                    cursor.execute(
                        "INSERT INTO rss_feeds (url, category) VALUES (%s, %s) ON CONFLICT (url) DO NOTHING",
                        (url, category)
                    )
                    cursor.execute("SELECT id FROM rss_feeds WHERE url = %s", (url,))
                else:
                    prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
                    cursor.execute(
                        f"{prefix} INTO rss_feeds (url, category) VALUES ({PLACEHOLDER}, {PLACEHOLDER})",
                        (url, category)
                    )
                    cursor.execute(f"SELECT id FROM rss_feeds WHERE url = {PLACEHOLDER}", (url,))

                row = cursor.fetchone()
                if not row:
                    continue
                feed_id = row[0]

                active = bool(is_active) if USE_POSTGRES else (1 if is_active else 0)
                if USE_POSTGRES:
                    cursor.execute(
                        "INSERT INTO user_feed_subscriptions (device_id, feed_id, is_active) "
                        "VALUES (%s, %s, %s) ON CONFLICT (device_id, feed_id) DO NOTHING",
                        (dev, feed_id, bool(is_active))
                    )
                else:
                    prefix = "INSERT IGNORE" if USE_MYSQL else "INSERT OR IGNORE"
                    cursor.execute(
                        f"{prefix} INTO user_feed_subscriptions (device_id, feed_id, is_active) "
                        f"VALUES ({PLACEHOLDER}, {PLACEHOLDER}, {PLACEHOLDER})",
                        (dev, feed_id, active)
                    )
                migrated += 1

        if migrated:
            logger.info("[DB] Migrated %d rows rss_sources → rss_feeds/user_feed_subscriptions", migrated)
    except Exception as e:
        logger.warning("[DB] migrate_to_shared_feeds skipped: %s", e)


def set_setting(key: str, value: str):
    with db_conn() as conn:
        cursor = conn.cursor()
        if USE_POSTGRES:
            cursor.execute(
                "INSERT INTO settings (key, value) VALUES (%s, %s) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value",
                (key, value)
            )
        elif USE_MYSQL:
            cursor.execute(
                "INSERT INTO settings (`key`, `value`) VALUES (%s, %s) ON DUPLICATE KEY UPDATE `value` = %s",
                (key, value, value)
            )
        else:
            cursor.execute("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", (key, value))
