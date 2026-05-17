import sqlite3
import mysql.connector
import psycopg2
import os
import time
import json
from config import (
    DB_PATH, USE_MYSQL, MYSQL_HOST, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE, MYSQL_PORT,
    USE_POSTGRES, POSTGRES_HOST, POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DATABASE, POSTGRES_PORT
)

PLACEHOLDER = "%s" if (USE_MYSQL or USE_POSTGRES) else "?"

def get_db_connection():
    if USE_POSTGRES:
        return psycopg2.connect(
            host=POSTGRES_HOST,
            user=POSTGRES_USER,
            password=POSTGRES_PASSWORD,
            database=POSTGRES_DATABASE,
            port=POSTGRES_PORT
        )
    elif USE_MYSQL:
        return mysql.connector.connect(
            host=MYSQL_HOST,
            user=MYSQL_USER,
            password=MYSQL_PASSWORD,
            database=MYSQL_DATABASE,
            port=MYSQL_PORT,
            buffered=True
        )
    else:
        return sqlite3.connect(DB_PATH)

def init_db(clear_existing: bool = False):
    if not (USE_MYSQL or USE_POSTGRES):
        os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)

    conn = get_db_connection()
    cursor = conn.cursor()

    if clear_existing:
        print("[DB] Clearing existing tables...")
        tables = ["pending_articles", "rss_sources", "category_config", "device_seen", "user_profile", "settings"]
        for t in tables:
            try: cursor.execute(f"DROP TABLE IF EXISTS {t} CASCADE")
            except: pass
        conn.commit()

    # Adaptare syntax pentru MySQL vs PostgreSQL vs SQLite
    auto_inc = "SERIAL" if USE_POSTGRES else ("AUTO_INCREMENT" if USE_MYSQL else "AUTOINCREMENT")
    text_type = "TEXT" # TEXT works in both Postgres and SQLite; MySQL uses LONGTEXT but TEXT is usually enough.
    pk_type = "" if USE_POSTGRES else ("INT" if USE_MYSQL else "INTEGER")
    timestamp_def = "CURRENT_TIMESTAMP"

    url_len = 1000 # Increased for Postgres
    cat_len = 100
    dev_len = 100

    # PostgreSQL specific PK syntax
    pk_pending = f"id {auto_inc} PRIMARY KEY" if USE_POSTGRES else f"id {pk_type} PRIMARY KEY {auto_inc}"

    queries = [
        f'''
        CREATE TABLE IF NOT EXISTS pending_articles (
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
            created_at TIMESTAMP DEFAULT {timestamp_def}
        )
        ''',
        f'''
        CREATE TABLE IF NOT EXISTS rss_sources (
            id {"SERIAL" if USE_POSTGRES else f"{pk_type} PRIMARY KEY {auto_inc}"},
            url VARCHAR({url_len}),
            category VARCHAR({cat_len}),
            device_id VARCHAR({dev_len}),
            is_active BOOLEAN DEFAULT TRUE,
            UNIQUE(url, category, device_id)
        )
        ''',
        f'''
        CREATE TABLE IF NOT EXISTS category_config (
            category    VARCHAR({cat_len}),
            device_id   VARCHAR({dev_len}),
            serp_query  TEXT,
            serp_region VARCHAR(50) DEFAULT 'us',
            serp_enabled INTEGER DEFAULT 0,
            PRIMARY KEY (category, device_id)
        )
        ''',
        f'''
        CREATE TABLE IF NOT EXISTS user_profile (
            deviceId           VARCHAR({dev_len}) NOT NULL PRIMARY KEY,
            name               TEXT NOT NULL,
            favoriteCategories TEXT NOT NULL,
            interestsScore     INTEGER NOT NULL,
            lastActive         BIGINT NOT NULL,
            language           VARCHAR(10) DEFAULT 'en'
        )
        ''',
        f'''
        CREATE TABLE IF NOT EXISTS settings (
            key VARCHAR(255) PRIMARY KEY,
            value TEXT
        )
        ''',
        f'''
        CREATE TABLE IF NOT EXISTS device_seen (
            device_id VARCHAR({dev_len}) NOT NULL,
            url VARCHAR({url_len}) NOT NULL,
            seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (device_id, url)
        )
        '''
    ]

    for q in queries:
        try:
            # PostgreSQL doesn't like backticks
            if USE_POSTGRES:
                q = q.replace("`key`", "key").replace("`value`", "value")
            cursor.execute(q)
        except Exception as e:
            print(f"Error executing query: {e}")

    # Index
    try:
        if USE_POSTGRES:
            cursor.execute('CREATE INDEX IF NOT EXISTS idx_device_seen_at ON device_seen(device_id, seen_at)')
        elif USE_MYSQL:
            cursor.execute('CREATE INDEX idx_device_seen_at ON device_seen(device_id, seen_at)')
        else:
            cursor.execute('CREATE INDEX IF NOT EXISTS idx_device_seen_at ON device_seen(device_id, seen_at)')  
    except:
        pass

    # Seteaza intervalul default la 2 ore
    if USE_POSTGRES:
        cursor.execute("INSERT INTO settings (key, value) VALUES ('scheduler_interval', '2') ON CONFLICT (key) DO NOTHING")
    elif USE_MYSQL:
        cursor.execute("INSERT IGNORE INTO settings (`key`, `value`) VALUES ('scheduler_interval', '2')")       
    else:
        cursor.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('scheduler_interval', '2')")

    conn.commit()
    cursor.close()
    conn.close()
    print("[DB] Initialization complete.")

def get_setting(key: str, default: str = None) -> str:
    conn = get_db_connection()
    cursor = conn.cursor()
    if USE_POSTGRES:
        cursor.execute("SELECT value FROM settings WHERE key = %s", (key,))
    elif USE_MYSQL:
        cursor.execute("SELECT `value` FROM settings WHERE `key` = %s", (key,))
    else:
        cursor.execute("SELECT value FROM settings WHERE key = ?", (key,))
    res = cursor.fetchall()
    row = res[0] if res else None
    cursor.close()
    conn.close()
    return row[0] if row else default

def set_setting(key: str, value: str):
    conn = get_db_connection()
    cursor = conn.cursor()
    if USE_POSTGRES:
        cursor.execute("INSERT INTO settings (key, value) VALUES (%s, %s) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value", (key, value))
    elif USE_MYSQL:
        cursor.execute("INSERT INTO settings (`key`, `value`) VALUES (%s, %s) ON DUPLICATE KEY UPDATE `value` = %s", (key, value, value))
    else:
        cursor.execute("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", (key, value))
    conn.commit()
    cursor.close()
    conn.close()
