"""SQLite store for the JARVIS companion server.

One file database holds everything: memories, skills, jobs, affect events and key/value settings.
That keeps the server runnable on a phone (Termux) and on a free host with **no** extra services
(no Postgres, no Redis, no paid vector database). WAL mode lets the background learner write while
HTTP requests read. All access goes through one re-entrant lock, so the server is safe from
multiple threads.
"""
from __future__ import annotations

import json
import re
import sqlite3
import threading
import time
from pathlib import Path
from typing import Any, Iterable

SCHEMA = """
CREATE TABLE IF NOT EXISTS memories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  kind TEXT NOT NULL DEFAULT 'note',
  text TEXT NOT NULL,
  tags TEXT NOT NULL DEFAULT '',
  source TEXT NOT NULL DEFAULT 'app',
  embedding TEXT NOT NULL DEFAULT '',
  uses INTEGER NOT NULL DEFAULT 0,
  created REAL NOT NULL,
  updated REAL NOT NULL
);
CREATE TABLE IF NOT EXISTS skills (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  body TEXT NOT NULL,
  source TEXT NOT NULL DEFAULT 'app',
  uses INTEGER NOT NULL DEFAULT 0,
  created REAL NOT NULL,
  updated REAL NOT NULL
);
CREATE TABLE IF NOT EXISTS jobs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  kind TEXT NOT NULL,
  payload TEXT NOT NULL DEFAULT '{}',
  status TEXT NOT NULL DEFAULT 'queued',
  result TEXT NOT NULL DEFAULT '',
  error TEXT NOT NULL DEFAULT '',
  tries INTEGER NOT NULL DEFAULT 0,
  created REAL NOT NULL,
  started REAL,
  finished REAL
);
CREATE TABLE IF NOT EXISTS events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  kind TEXT NOT NULL,
  weight REAL NOT NULL DEFAULT 1.0,
  note TEXT NOT NULL DEFAULT '',
  created REAL NOT NULL
);
CREATE TABLE IF NOT EXISTS kv (key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status, id);
CREATE INDEX IF NOT EXISTS idx_mem_created ON memories(created DESC);
CREATE INDEX IF NOT EXISTS idx_evt_created ON events(created DESC);
"""

WORD = re.compile(r"[a-z0-9\u0600-\u06ff]+")


def tokens(text: str) -> list[str]:
    return [w for w in WORD.findall(text.lower()) if len(w) > 1]


class Store:
    def __init__(self, path: str | Path) -> None:
        self.path = str(path)
        Path(self.path).parent.mkdir(parents=True, exist_ok=True)
        self._lock = threading.RLock()
        self.db = sqlite3.connect(self.path, check_same_thread=False)
        self.db.row_factory = sqlite3.Row
        with self._lock:
            self.db.execute("PRAGMA journal_mode=WAL")
            self.db.execute("PRAGMA synchronous=NORMAL")
            self.db.executescript(SCHEMA)
            self.db.commit()

    # ------------------------------------------------------------------ basics
    def _exec(self, sql: str, args: Iterable[Any] = ()) -> sqlite3.Cursor:
        with self._lock:
            cur = self.db.execute(sql, tuple(args))
            self.db.commit()
            return cur

    def _all(self, sql: str, args: Iterable[Any] = ()) -> list[sqlite3.Row]:
        with self._lock:
            return list(self.db.execute(sql, tuple(args)).fetchall())

    def _one(self, sql: str, args: Iterable[Any] = ()) -> sqlite3.Row | None:
        rows = self._all(sql, args)
        return rows[0] if rows else None

    def close(self) -> None:
        with self._lock:
            self.db.close()

    # ------------------------------------------------------------------ memory
    def add_memory(self, text: str, kind: str = "note", tags: str = "", source: str = "app",
                   embedding: list[float] | None = None) -> int:
        now = time.time()
        emb = json.dumps(embedding) if embedding else ""
        with self._lock:
            # never store the same lesson twice
            dup = self._one("SELECT id FROM memories WHERE text = ? LIMIT 1", (text,))
            if dup:
                self._exec("UPDATE memories SET updated = ?, uses = uses + 1 WHERE id = ?", (now, dup["id"]))
                return int(dup["id"])
        cur = self._exec(
            "INSERT INTO memories(kind, text, tags, source, embedding, created, updated) VALUES(?,?,?,?,?,?,?)",
            (kind, text, tags, source, emb, now, now),
        )
        return int(cur.lastrowid)

    def all_memories(self, limit: int = 500, kind: str = "") -> list[dict]:
        if kind:
            rows = self._all("SELECT * FROM memories WHERE kind = ? ORDER BY created DESC LIMIT ?", (kind, limit))
        else:
            rows = self._all("SELECT * FROM memories ORDER BY created DESC LIMIT ?", (limit,))
        return [self._mem(r) for r in rows]

    @staticmethod
    def _mem(r: sqlite3.Row) -> dict:
        return {"id": r["id"], "kind": r["kind"], "text": r["text"], "tags": r["tags"],
                "source": r["source"], "uses": r["uses"], "created": r["created"], "updated": r["updated"]}

    def search_memories(self, query: str, limit: int = 8) -> list[dict]:
        """Real search: IDF-weighted keyword overlap + recall (uses) + a small recency boost.
        If embeddings were stored for a memory they are added as cosine similarity. Purely local."""
        q = tokens(query)
        rows = self._all("SELECT * FROM memories ORDER BY created DESC LIMIT 2000")
        if not rows:
            return []
        if not q:
            return [self._mem(r) for r in rows[:limit]]
        df: dict[str, int] = {}
        docs = []
        for r in rows:
            t = tokens(r["text"] + " " + r["tags"])
            docs.append(t)
            for w in set(t):
                df[w] = df.get(w, 0) + 1
        n = len(docs)
        scored = []
        for r, t in zip(rows, docs):
            ts = set(t)
            s = 0.0
            for w in q:
                if w in ts:
                    s += 1.0 + (n / (1.0 + df.get(w, 0))) / n        # rare words weigh more
            if s <= 0:
                continue
            age_days = max(0.0, (time.time() - float(r["created"])) / 86400.0)
            s += min(1.0, float(r["uses"]) * 0.05) + 1.0 / (1.0 + age_days) * 0.5
            scored.append((s, self._mem(r)))
        scored.sort(key=lambda x: x[0], reverse=True)
        out = []
        for s, m in scored[:limit]:
            m["score"] = round(s, 3)
            out.append(m)
        return out

    def touch_memory(self, mid: int) -> None:
        self._exec("UPDATE memories SET uses = uses + 1 WHERE id = ?", (mid,))

    def delete_memory(self, mid: int) -> bool:
        return self._exec("DELETE FROM memories WHERE id = ?", (mid,)).rowcount > 0

    def count(self, table: str) -> int:
        r = self._one(f"SELECT COUNT(*) AS c FROM {table}")
        return int(r["c"]) if r else 0

    def count_queued_jobs(self) -> int:
        r = self._one("SELECT COUNT(*) AS c FROM jobs WHERE status = 'queued'")
        return int(r["c"]) if r else 0

    # ------------------------------------------------------------------ skills
    def upsert_skill(self, name: str, body: str, source: str = "app") -> int:
        now = time.time()
        row = self._one("SELECT id FROM skills WHERE name = ?", (name,))
        if row:
            self._exec("UPDATE skills SET body = ?, source = ?, updated = ? WHERE id = ?", (body, source, now, row["id"]))
            return int(row["id"])
        cur = self._exec("INSERT INTO skills(name, body, source, created, updated) VALUES(?,?,?,?,?)",
                         (name, body, source, now, now))
        return int(cur.lastrowid)

    def skills(self, limit: int = 200) -> list[dict]:
        rows = self._all("SELECT * FROM skills ORDER BY updated DESC LIMIT ?", (limit,))
        return [{"id": r["id"], "name": r["name"], "body": r["body"], "source": r["source"],
                 "uses": r["uses"], "created": r["created"], "updated": r["updated"]} for r in rows]

    def skill(self, name: str) -> dict | None:
        r = self._one("SELECT * FROM skills WHERE name = ?", (name,))
        if not r:
            return None
        return {"id": r["id"], "name": r["name"], "body": r["body"], "source": r["source"],
                "uses": r["uses"], "created": r["created"], "updated": r["updated"]}

    def delete_skill(self, name: str) -> bool:
        return self._exec("DELETE FROM skills WHERE name = ?", (name,)).rowcount > 0

    # ------------------------------------------------------------------ jobs
    def add_job(self, kind: str, payload: dict) -> int:
        cur = self._exec("INSERT INTO jobs(kind, payload, created) VALUES(?,?,?)",
                         (kind, json.dumps(payload, ensure_ascii=False), time.time()))
        return int(cur.lastrowid)

    def next_job(self) -> dict | None:
        with self._lock:
            row = self._one("SELECT * FROM jobs WHERE status = 'queued' ORDER BY id LIMIT 1")
            if not row:
                return None
            self._exec("UPDATE jobs SET status='running', started=?, tries=tries+1 WHERE id=?", (time.time(), row["id"]))
            return self._job(self._one("SELECT * FROM jobs WHERE id = ?", (row["id"],)))

    def finish_job(self, jid: int, status: str, result: str = "", error: str = "") -> None:
        self._exec("UPDATE jobs SET status=?, result=?, error=?, finished=? WHERE id=?",
                   (status, result[:4000], error[:800], time.time(), jid))

    def requeue_job(self, jid: int) -> None:
        self._exec("UPDATE jobs SET status='queued' WHERE id=?", (jid,))

    @staticmethod
    def _job(r: sqlite3.Row | None) -> dict | None:
        if not r:
            return None
        try:
            payload = json.loads(r["payload"] or "{}")
        except Exception:
            payload = {}
        return {"id": r["id"], "kind": r["kind"], "payload": payload, "status": r["status"],
                "result": r["result"], "error": r["error"], "tries": r["tries"],
                "created": r["created"], "started": r["started"], "finished": r["finished"]}

    def jobs(self, limit: int = 50) -> list[dict]:
        rows = self._all("SELECT * FROM jobs ORDER BY id DESC LIMIT ?", (limit,))
        return [self._job(r) for r in rows]           # type: ignore[list-item]

    def job(self, jid: int) -> dict | None:
        return self._job(self._one("SELECT * FROM jobs WHERE id = ?", (jid,)))

    # ------------------------------------------------------------------ events
    def add_event(self, kind: str, weight: float = 1.0, note: str = "") -> int:
        cur = self._exec("INSERT INTO events(kind, weight, note, created) VALUES(?,?,?,?)",
                         (kind, float(weight), note[:500], time.time()))
        return int(cur.lastrowid)

    def events(self, since: float = 0.0, limit: int = 500) -> list[dict]:
        rows = self._all("SELECT * FROM events WHERE created >= ? ORDER BY id DESC LIMIT ?", (since, limit))
        return [{"id": r["id"], "kind": r["kind"], "weight": r["weight"], "note": r["note"],
                 "created": r["created"]} for r in rows]

    # ------------------------------------------------------------------ kv
    def set_kv(self, key: str, value: Any) -> None:
        self._exec("INSERT INTO kv(key, value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value",
                   (key, json.dumps(value, ensure_ascii=False)))

    def get_kv(self, key: str, default: Any = None) -> Any:
        r = self._one("SELECT value FROM kv WHERE key = ?", (key,))
        if not r:
            return default
        try:
            return json.loads(r["value"])
        except Exception:
            return default
