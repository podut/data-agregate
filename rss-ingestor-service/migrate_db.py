import sqlite3
import os

DB_PATH = "../data_agregate_local.db"

def migrate():
    print(f"Connecting to {DB_PATH}...")
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # Check current columns
    cursor.execute("PRAGMA table_info(pending_articles)")
    columns = [col[1] for col in cursor.fetchall()]
    print(f"Current columns: {columns}")
    
    # Add columns if missing
    new_cols = [
        ("score",     "INTEGER DEFAULT 70"),
        ("processed", "INTEGER DEFAULT 0"),
        ("tags",      "TEXT"),
    ]
    
    for col, definition in new_cols:
        if col not in columns:
            print(f"Adding column {col}...")
            cursor.execute(f"ALTER TABLE pending_articles ADD COLUMN {col} {definition}")
        else:
            print(f"Column {col} already exists.")
            
    # Fix rss_sources UNIQUE constraint if needed
    # (sqlite doesn't support easy ALTER TABLE for constraints, so we leave it if it works)
    
    conn.commit()
    conn.close()
    print("Migration finished.")

if __name__ == "__main__":
    migrate()
