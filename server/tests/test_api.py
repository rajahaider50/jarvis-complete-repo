"""Real API tests for the JARVIS companion server.

Run with:  cd server && python -m pytest -q
No network is used: the model is either absent (honest fallback paths are tested) or replaced by a
fake chat function (the AI path is tested without calling anyone).
"""
from __future__ import annotations

import json

import pytest

from jarvis_server import Config, create_app


@pytest.fixture()
def app(tmp_path, monkeypatch):
    monkeypatch.setenv("JARVIS_DATA", str(tmp_path))
    monkeypatch.setenv("JARVIS_LEARNER", "0")
    monkeypatch.delenv("JARVIS_LLM_KEY", raising=False)
    monkeypatch.delenv("JARVIS_TOKEN", raising=False)
    cfg = Config()
    a = create_app(cfg, db_path=str(tmp_path / "test.db"), start_learner=False)
    a.config["TESTING"] = True
    return a


@pytest.fixture()
def client(app):
    return app.test_client()


def test_health_reports_real_counts(client):
    r = client.get("/health")
    assert r.status_code == 200
    d = r.get_json()
    assert d["ok"] is True and d["service"] == "jarvis-server"
    assert d["memories"] == 0 and d["skills"] == 0
    assert d["config"]["llm_ready"] is False          # no key in tests: must not pretend


def test_memory_add_search_and_dedupe(client):
    assert client.post("/api/memories", json={"text": "Owner's phone is a Pixel 7", "kind": "fact",
                                             "tags": "device"}).status_code == 201
    client.post("/api/memories", json={"text": "Owner builds websites with dynamic 3D effects"})
    # duplicate text must not create a second row
    client.post("/api/memories", json={"text": "Owner's phone is a Pixel 7"})
    assert client.get("/health").get_json()["memories"] == 2

    hits = client.post("/api/recall", json={"query": "which phone does the owner use"}).get_json()["items"]
    assert hits and "Pixel" in hits[0]["text"]

    empty = client.post("/api/memories", json={"text": "   "})
    assert empty.status_code == 400 and empty.get_json()["ok"] is False


def test_learn_job_is_processed_and_kept_honest(app, client):
    r = client.post("/api/learn", json={"text": "A dynamic 3D website uses requestAnimationFrame and CSS transforms",
                                        "task": "learn how to build a dynamic 3D website", "skill": True})
    assert r.status_code == 202
    jid = r.get_json()["job"]

    rt = app.extensions["jarvis"]
    assert rt.store.count_queued_jobs() == 1
    assert rt.learner.run_once() == 1                      # the real learner code path

    job = client.get(f"/api/jobs/{jid}").get_json()["job"]
    assert job["status"] == "done"
    assert "memories=" in job["result"] and "skill=" in job["result"]
    # no model configured -> the raw note is kept, marked as raw (never invented)
    mems = client.get("/api/memories").get_json()["items"]
    assert any(m["kind"] == "raw" and m["source"] == "learn" for m in mems)
    sk = client.get("/api/skills").get_json()["items"]
    assert sk and sk[0]["name"].startswith("learn-how-to-build")


def test_distil_parses_model_json(app, monkeypatch):
    rt = app.extensions["jarvis"]

    def fake_chat(messages, temperature=0.2, max_tokens=700):
        assert "JSON only" in messages[0]["content"]
        return '```json\n{"lessons":[{"text":"Dynamic sites animate with requestAnimationFrame","kind":"lesson",' \
               '"tags":"web,animation"},{"text":"","kind":"lesson"}]}\n```'

    monkeypatch.setattr(rt.llm, "chat", fake_chat)
    ids = rt.brain.distil("raw note", source="learn")
    assert len(ids) == 1                                   # the empty lesson is dropped
    items = rt.brain.list()
    assert items[0]["text"].startswith("Dynamic sites")
    assert items[0]["kind"] == "lesson" and items[0]["source"] == "learn"


def test_affect_moves_on_real_events(client):
    start = client.get("/api/affect").get_json()
    assert start["mood"] == "calm" or start["mood"] == "content"
    client.post("/api/affect", json={"kind": "call_fail", "note": "mic went silent"})
    bad = client.get("/api/affect").get_json()
    assert bad["state"]["valence"] < start["state"]["valence"]
    client.post("/api/affect", json={"kind": "praise", "weight": 2})
    good = client.get("/api/affect").get_json()
    assert good["state"]["valence"] > bad["state"]["valence"]
    assert good["state"]["trust"] > bad["state"]["trust"]
    assert client.post("/api/affect", json={"kind": "not_a_real_event"}).status_code == 400


def test_skill_put_get_delete(client):
    assert client.put("/api/skills/make-tea", json={"body": "# Make tea\n1. boil water"}).status_code == 200
    assert client.get("/api/skills/make-tea").get_json()["skill"]["body"].startswith("# Make tea")
    assert client.get("/api/skills/nope").status_code == 404
    assert client.delete("/api/skills/make-tea").get_json()["ok"] is True


def test_sdui_pages_use_only_whitelisted_parts(client):
    d = client.get("/api/pages").get_json()
    assert d["ok"] and len(d["pages"]) >= 3
    for p in d["pages"]:
        assert p["id"] and p["title"] and p["blocks"]
    assert client.get("/api/pages/brain").get_json()["page"]["id"] == "brain"
    assert client.get("/api/pages/nope").status_code == 404


def test_sync_accepts_app_data_and_returns_the_brain(client):
    r = client.post("/api/sync", json={
        "memories": [{"text": "Owner prefers Urdu for voice replies", "kind": "preference"}],
        "skills": [{"name": "call-recovery", "body": "# Call recovery"}],
        "events": [{"kind": "call_ok", "weight": 1}],
    }).get_json()
    assert r["accepted"] == {"memories": 1, "skills": 1, "events": 1}
    assert r["memories"] and r["skills"]
    assert r["affect"]["state"]["valence"] >= 0
    assert r["learner"]["enabled"] is False


def test_token_auth_when_configured(tmp_path, monkeypatch):
    monkeypatch.setenv("JARVIS_DATA", str(tmp_path))
    monkeypatch.setenv("JARVIS_LEARNER", "0")
    monkeypatch.setenv("JARVIS_TOKEN", "s3cret")
    cfg = Config()
    app = create_app(cfg, db_path=str(tmp_path / "auth.db"), start_learner=False)
    c = app.test_client()
    assert c.get("/health").status_code == 200                       # health stays open
    assert c.get("/api/state").status_code == 401
    assert c.post("/api/memories", json={"text": "x"}).status_code == 401
    ok = c.get("/api/state", headers={"X-Jarvis-Token": "s3cret"})
    assert ok.status_code == 200 and ok.get_json()["ok"] is True


def test_job_failure_is_recorded_not_hidden(app):
    rt = app.extensions["jarvis"]
    jid = rt.store.add_job("definitely-not-a-kind", {})
    # the learner retries a bad job (JARVIS_LEARNER_TRIES) and only then marks it failed
    for _ in range(rt.cfg.learner_max_tries):
        assert rt.learner.run_once() == 1
    job = rt.store.job(jid)
    assert job["status"] == "failed" and "unknown job kind" in job["error"]
    assert job["tries"] == rt.cfg.learner_max_tries


def test_dashboard_and_state_are_served(client):
    assert client.get("/").status_code == 200
    st = client.get("/api/state").get_json()
    assert st["counts"]["memories"] == 0
    assert json.dumps(st)  # serialisable
