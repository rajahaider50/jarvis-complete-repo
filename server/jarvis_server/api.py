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


CONTROL_HTML = """<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>JARVIS control</title>
<style>
 body{background:#0e0d0b;color:#f2e9d8;font:15px/1.55 system-ui;margin:0;padding:16px}
 h1{color:#d8b46a;font-size:20px;margin:0 0 2px}
 h2{color:#d8b46a;font-size:12px;margin:0 0 6px;letter-spacing:.08em;text-transform:uppercase}
 small{color:#9c9285} a{color:#e9cf95}
 .b{background:#1b1814;border:1px solid #3a3128;border-radius:14px;padding:12px;margin:10px 0}
 input,textarea{background:#12100d;border:1px solid #3a3128;border-radius:10px;color:#f2e9d8;padding:9px;width:100%;box-sizing:border-box;font:inherit}
 button{background:linear-gradient(180deg,#e9cf95,#c9a24f);border:0;border-radius:10px;color:#1b1814;font-weight:600;padding:10px 14px;margin-top:8px;cursor:pointer}
 button.ghost{background:#241f19;color:#e9cf95;border:1px solid #4a3f31}
 .row{display:flex;gap:8px;flex-wrap:wrap} .row>*{flex:1 1 130px}
 pre{white-space:pre-wrap;word-break:break-word;margin:8px 0 0;font-size:12px;color:#cbbfa6}
 .ok{color:#8fce8f}.bad{color:#e08b7d}
 table{width:100%;border-collapse:collapse;font-size:12px} td,th{text-align:left;padding:4px 6px;border-bottom:1px solid #2a241d}
</style></head><body>
<h1>JARVIS brain server · control</h1>
<small>Every number below comes from the server's own SQLite database. Writes need the token.</small>

<div class="b"><h2>Token</h2>
  <div class="row">
    <input id="tok" type="password" placeholder="JARVIS_TOKEN (kept only in this browser)">
    <button onclick="saveTok()">Save</button>
    <button class="ghost" onclick="clearTok()">Clear</button>
  </div>
  <small id="tokstate"></small>
</div>

<div class="b"><h2>Live state</h2><pre id="state">…</pre></div>

<div class="b"><h2>Add a memory</h2>
  <textarea id="mtext" rows="2" placeholder="e.g. Owner builds dynamic 3D websites"></textarea>
  <div class="row"><input id="mkind" value="fact" placeholder="kind (fact / preference / lesson)">
  <input id="mtags" placeholder="tags (comma separated)"></div>
  <button onclick="addMem()">Save memory</button>
</div>

<div class="b"><h2>Teach JARVIS (background learning)</h2>
  <textarea id="ltask" rows="2" placeholder="e.g. learn how to build a dynamic 3D website"></textarea>
  <input id="lobs" placeholder="what was observed or known (optional)">
  <div class="row"><button onclick="queueLearn()">Queue learning job</button>
  <button class="ghost" onclick="loadJobs()">Refresh queue</button></div>
  <div id="jobs"></div>
</div>

<div class="b"><h2>Mood (real events)</h2>
  <div class="row">
    <button class="ghost" onclick="ev('call_ok')">call worked</button>
    <button class="ghost" onclick="ev('call_fail')">call failed</button>
    <button class="ghost" onclick="ev('tool_ok')">action worked</button>
    <button class="ghost" onclick="ev('praise')">praise</button>
    <button class="ghost" onclick="ev('anger')">upset</button>
  </div>
  <pre id="affect">…</pre>
</div>

<div class="b"><h2>Recall</h2>
  <input id="q" placeholder="ask the memory…">
  <button onclick="recall()">Search</button>
  <pre id="hits"></pre>
</div>
<small>API: <a href="/health">/health</a> · /api/state · /api/memories · /api/recall · /api/learn · /api/jobs · /api/affect · /api/skills</small>
<script>
var $ = function(id){ return document.getElementById(id); };
function tok(){ return localStorage.getItem('jarvis_token') || ''; }
function saveTok(){ localStorage.setItem('jarvis_token', $('tok').value.trim()); paintTok(); refresh(); }
function clearTok(){ localStorage.removeItem('jarvis_token'); $('tok').value = ''; paintTok(); refresh(); }
function paintTok(){ $('tokstate').textContent = tok() ? 'token saved in this browser' : 'no token saved - write actions will answer HTTP 401'; }
async function api(path, opts){
  var o = Object.assign({headers:{'Accept':'application/json'}}, opts || {});
  if (tok()) o.headers['X-Jarvis-Token'] = tok();
  if (o.body) o.headers['Content-Type'] = 'application/json';
  var r = await fetch(path, o);
  var t = await r.text();
  var j = null; try { j = JSON.parse(t); } catch (e) {}
  return {ok:r.ok, code:r.status, body:j, raw:t};
}
async function refresh(){
  var h = await api('/health'), s = await api('/api/state'), a = await api('/api/affect');
  var lines = [];
  lines.push('health      ' + (h.ok ? 'ok  v' + (h.body.version || '?') : 'FAILED'));
  if (s.body && s.body.counts) lines.push('memories    ' + s.body.counts.memories + '      skills ' + s.body.counts.skills + '      jobs ' + s.body.counts.jobs + '      events ' + s.body.counts.events);
  if (s.body && s.body.learner) lines.push('learner     ' + (s.body.learner.running ? 'on' : 'off') + ' / ' + s.body.learner.interval_s + 's      queued ' + s.body.learner.queued + '      done ' + s.body.learner.done + '      failed ' + s.body.learner.failed);
  if (!s.ok) lines.push('state       HTTP ' + s.code + (s.code === 401 ? '   (paste the token above)' : ''));
  $('state').textContent = lines.join('\\n');
  if (a.body && a.body.state) {
    var st = a.body.state;
    $('affect').textContent = 'mood ' + a.body.mood + '   valence ' + st.valence + '   arousal ' + st.arousal +
      '   energy ' + st.energy + '   trust ' + st.trust + '   focus ' + st.focus + '\\n' + (a.body.note || '');
  }
}
function esc(s){ var d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
async function addMem(){
  var t = $('mtext').value.trim(); if (!t) { alert('write something first'); return; }
  var r = await api('/api/memories', {method:'POST', body:JSON.stringify({text:t, kind:$('mkind').value || 'fact', tags:$('mtags').value})});
  alert(r.ok ? 'saved (id ' + r.body.id + ')' : 'failed: HTTP ' + r.code + ' ' + ((r.body && r.body.error) || r.raw.slice(0,120)));
  if (r.ok) { $('mtext').value = ''; refresh(); }
}
async function queueLearn(){
  var t = $('ltask').value.trim(); if (!t) { alert('write the task first'); return; }
  var r = await api('/api/learn', {method:'POST', body:JSON.stringify({task:t, observation:$('lobs').value, skill:true})});
  alert(r.ok ? 'queued job #' + r.body.job + ' (' + r.body.queued + ' waiting) - the learner picks it up within 15s' : 'failed: HTTP ' + r.code);
  if (r.ok) { $('ltask').value = ''; setTimeout(loadJobs, 17000); }
}
async function loadJobs(){
  var r = await api('/api/jobs?limit=10');
  if (!r.ok || !r.body) { $('jobs').innerHTML = '<pre class="bad">queue: HTTP ' + r.code + '</pre>'; return; }
  var rows = (r.body.items || []).map(function(j){
    var cls = j.status === 'done' ? 'ok' : (j.status === 'failed' ? 'bad' : '');
    return '<tr><td>#' + j.id + '</td><td>' + j.kind + '</td><td class="' + cls + '">' + j.status + '</td><td>' + esc(j.result || j.error || '') + '</td></tr>';
  }).join('');
  $('jobs').innerHTML = '<table><tr><th>job</th><th>kind</th><th>status</th><th>result / error</th></tr>' + rows + '</table>';
}
async function ev(kind){
  var r = await api('/api/affect', {method:'POST', body:JSON.stringify({kind:kind, weight:1, note:'from the control page'})});
  if (!r.ok) alert('failed: HTTP ' + r.code);
  refresh();
}
async function recall(){
  var r = await api('/api/recall', {method:'POST', body:JSON.stringify({query:$('q').value, limit:8})});
  if (!r.ok || !r.body) { $('hits').textContent = 'HTTP ' + r.code; return; }
  var items = (r.body.items || []).map(function(m){ return '[' + m.kind + '] ' + m.text; });
  $('hits').textContent = items.length ? items.join('\\n') : 'nothing remembered yet';
}
$('tok').value = tok();
paintTok(); refresh(); loadJobs();
setInterval(refresh, 5000);
</script>
</body></html>
"""


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
/api/jobs · /api/affect · /api/pages · /api/sync</small></div>
<div class=b><small><a style="color:#e9cf95" href="/control">open the control page &rarr;</a></small></div>"""
        return Response(html, mimetype="text/html")

    @app.get("/control")
    def control():
        return Response(CONTROL_HTML, mimetype="text/html")

    return app
