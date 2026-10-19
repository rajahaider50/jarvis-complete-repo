"""HTTP API of the JARVIS companion server (Flask).

Design rules kept from the app's own rules:
  * no fake data — every number comes from the database, and when the model is missing the answer says so;
  * offline-safe — the app is never required to reach this server;
  * one shared secret — when JARVIS_TOKEN is set, every /api route needs `X-Jarvis-Token`.

Run:  python -m jarvis_server          (dev / Termux)
      gunicorn wsgi:app                (free hosts, see Procfile)
"""
from __future__ import annotations

import hmac
from typing import Any

from flask import Flask, Response, jsonify, request

from .affect import Affect, EVENTS
from .config import VERSION, Config
from .learner import Learner
from .llm import Llm
from .memory import Brain
from .sdui import default_pages
from .skills import SkillBook
from .store import Store


class Runtime:
    """Everything the server needs, built once."""

    def __init__(self, cfg: Config | None = None, db_path: str | None = None) -> None:
        self.cfg = cfg or Config()
        self.store = Store(db_path or self.cfg.db_path)
        self.llm = Llm(self.cfg)
        self.brain = Brain(self.store, self.cfg, self.llm)
        self.skills = SkillBook(self.store, self.cfg, self.llm)
        self.affect = Affect(self.store, self.cfg)
        self.learner = Learner(self.store, self.brain, self.skills, self.cfg)


def create_app(cfg: Config | None = None, db_path: str | None = None, start_learner: bool = True) -> Flask:
    rt = Runtime(cfg, db_path)
    app = Flask("jarvis_server")
    app.config["JSON_SORT_KEYS"] = False
    app.extensions["jarvis"] = rt
    if start_learner:
        rt.learner.start()

    # ------------------------------------------------------------------ auth
    @app.before_request
    def _auth() -> Any:
        if not rt.cfg.token or not request.path.startswith("/api"):
            return None
        given = request.headers.get("X-Jarvis-Token", "")
        if not hmac.compare_digest(given, rt.cfg.token):
            return jsonify({"ok": False, "error": "bad or missing X-Jarvis-Token"}), 401
        return None

    def body() -> dict:
        return request.get_json(silent=True) or {}

    # ------------------------------------------------------------------ basics
    @app.get("/health")
    def health():
        return jsonify({
            "ok": True, "service": "jarvis-server", "version": VERSION,
            "time": __import__("time").time(),
            "memories": rt.store.count("memories"), "skills": rt.store.count("skills"),
            "jobs_queued": rt.store.count_queued_jobs(), "mood": rt.affect.snapshot()["mood"],
            **{"config": rt.cfg.mask()},
        })

    @app.get("/api/state")
    def state():
        return jsonify({
            "ok": True,
            "counts": {"memories": rt.store.count("memories"), "skills": rt.store.count("skills"),
                       "jobs": rt.store.count("jobs"), "events": rt.store.count("events")},
            "affect": rt.affect.snapshot(),
            "learner": rt.learner.status(),
            "config": rt.cfg.mask(),
        })

    # ------------------------------------------------------------------ memory
    @app.get("/api/memories")
    def memories_list():
        q = (request.args.get("q") or "").strip()
        limit = min(200, int(request.args.get("limit", "50")))
        if q:
            return jsonify({"ok": True, "query": q, "items": rt.brain.recall(q, limit=limit, touch=False)})
        return jsonify({"ok": True, "items": rt.brain.list(limit=limit, kind=request.args.get("kind", ""))})

    @app.post("/api/memories")
    def memories_add():
        b = body()
        try:
            mid = rt.brain.remember(str(b.get("text", "")), kind=str(b.get("kind", "note")),
                                    tags=str(b.get("tags", "")), source=str(b.get("source", "app")))
        except ValueError as e:
            return jsonify({"ok": False, "error": str(e)}), 400
        return jsonify({"ok": True, "id": mid}), 201

    @app.delete("/api/memories/<int:mid>")
    def memories_del(mid: int):
        return jsonify({"ok": rt.brain.forget(mid)})

    @app.post("/api/recall")
    def recall():
        b = body()
        hits = rt.brain.recall(str(b.get("query", "")), limit=min(20, int(b.get("limit", 8))))
        return jsonify({"ok": True, "items": hits, "affect_line": rt.affect.recall_line()})

    # ------------------------------------------------------------------ skills
    @app.get("/api/skills")
    def skills_list():
        return jsonify({"ok": True, "items": rt.skills.list(limit=min(200, int(request.args.get("limit", "100"))))})

    @app.get("/api/skills/<name>")
    def skills_get(name: str):
        s = rt.skills.get(name)
        return (jsonify({"ok": True, "skill": s}) if s else (jsonify({"ok": False, "error": "not found"}), 404))

    @app.put("/api/skills/<name>")
    def skills_put(name: str):
        b = body()
        sid = rt.skills.save(name, str(b.get("body", "")), source=str(b.get("source", "app")))
        return jsonify({"ok": True, "id": sid})

    @app.delete("/api/skills/<name>")
    def skills_del(name: str):
        return jsonify({"ok": rt.skills.delete(name)})

    # ------------------------------------------------------------------ learning
    @app.post("/api/learn")
    def learn():
        b = body()
        text = str(b.get("text", "")).strip()
        task = str(b.get("task", "")).strip()
        if not text and not task:
            return jsonify({"ok": False, "error": "give 'text' or 'task'"}), 400
        jid = rt.store.add_job("learn", {
            "text": text, "task": task,
            "observation": str(b.get("observation", "")),
            "name": str(b.get("name", "")),
            "skill": bool(b.get("skill", True)),
        })
        rt.affect.event("task_start", 0.4, "learning queued")
        return jsonify({"ok": True, "job": jid, "queued": rt.store.count_queued_jobs()}), 202

    @app.post("/api/summarise")
    def summarise():
        b = body()
        jid = rt.store.add_job("summarise", {"text": str(b.get("text", "")), "title": str(b.get("title", "summary"))})
        return jsonify({"ok": True, "job": jid}), 202

    @app.get("/api/jobs")
    def jobs():
        return jsonify({"ok": True, "items": rt.store.jobs(limit=min(200, int(request.args.get("limit", "50")))),
                        "learner": rt.learner.status()})

    @app.get("/api/jobs/<int:jid>")
    def job(jid: int):
        j = rt.store.job(jid)
        return (jsonify({"ok": True, "job": j}) if j else (jsonify({"ok": False, "error": "not found"}), 404))

    # ------------------------------------------------------------------ affect
    @app.get("/api/affect")
    def affect_get():
        return jsonify({"ok": True, **rt.affect.snapshot(), "events": sorted(EVENTS.keys())})

    @app.post("/api/affect")
    def affect_post():
        b = body()
        kind = str(b.get("kind", ""))
        if kind not in EVENTS:
            return jsonify({"ok": False, "error": f"unknown event, use one of {sorted(EVENTS)}"}), 400
        return jsonify({"ok": True, **rt.affect.event(kind, float(b.get("weight", 1.0)), str(b.get("note", "")))})

    # ------------------------------------------------------------------ sdui
    @app.get("/api/pages")
    def pages():
        return jsonify({"ok": True, "pages": default_pages()})

    @app.get("/api/pages/<pid>")
    def page(pid: str):
        for p in default_pages():
            if p["id"] == pid:
                return jsonify({"ok": True, "page": p})
        return jsonify({"ok": False, "error": "unknown page"}), 404

    # ------------------------------------------------------------------ sync (app <-> server)
    @app.post("/api/sync")
    def sync():
        b = body()
        accepted = {"memories": 0, "skills": 0, "events": 0}
        for m in b.get("memories", [])[:500]:
            try:
                rt.brain.remember(str(m.get("text", "")), kind=str(m.get("kind", "note")),
                                 tags=str(m.get("tags", "")), source=str(m.get("source", "app")))
                accepted["memories"] += 1
            except ValueError:
                pass
        for s in b.get("skills", [])[:200]:
            if str(s.get("name", "")).strip():
                rt.skills.save(str(s["name"]), str(s.get("body", "")), source=str(s.get("source", "app")))
                accepted["skills"] += 1
        for e in b.get("events", [])[:500]:
            if str(e.get("kind", "")) in EVENTS:
                rt.affect.event(str(e["kind"]), float(e.get("weight", 1.0)), str(e.get("note", "")))
                accepted["events"] += 1
        return jsonify({
            "ok": True, "accepted": accepted,
            "memories": rt.store.all_memories(limit=100),
            "skills": rt.skills.list(limit=100),
            "affect": rt.affect.snapshot(),
            "learner": rt.learner.status(),
        })

    # ------------------------------------------------------------------ dashboard
    @app.get("/")
    def dashboard():
        s = rt.affect.snapshot()
        st = rt.store
        html = f"""<!doctype html><meta name=viewport content="width=device-width,initial-scale=1">
<title>JARVIS server</title>
<style>body{{background:#0e0d0b;color:#f2e9d8;font:15px/1.5 system-ui;margin:0;padding:24px}}
h1{{color:#d8b46a;margin:0 0 4px}}small{{color:#9c9285}}.b{{background:#1b1814;border:1px solid #3a3128;
border-radius:14px;padding:14px;margin:12px 0}}b{{color:#e9cf95}}</style>
<h1>JARVIS brain server</h1><small>v{VERSION} · {rt.cfg.mask()['data_dir']}</small>
<div class=b>memories <b>{st.count('memories')}</b> · skills <b>{st.count('skills')}</b> ·
jobs queued <b>{st.count_queued_jobs()}</b> · events <b>{st.count('events')}</b></div>
<div class=b>mood <b>{s['mood']}</b><br>{rt.affect.recall_line()}<br><small>{s['note']}</small></div>
<div class=b>model: <b>{rt.cfg.mask()['llm_model'] or 'none configured'}</b> ·
learner: <b>{'on' if rt.learner.running else 'off'}</b> ·
auth: <b>{'on' if rt.cfg.token else 'off (localhost only)'}</b></div>
<div class=b><small>API: /health · /api/state · /api/memories · /api/recall · /api/skills · /api/learn ·
/api/jobs · /api/affect · /api/pages · /api/sync</small></div>"""
        return Response(html, mimetype="text/html")

    return app
