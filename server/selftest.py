"""Self-test for the JARVIS brain server — no pytest needed, works on Termux.

Runs the REAL Flask app against a temporary SQLite database and checks every route:
health, memory add/search/dedupe, recall, a real learning job (learner.run_once),
skills CRUD, the affect model, the SDUI whitelist, sync, and token auth (401 vs 200).

Exit code 0 = all good. Any failure prints the reason and exits 1, so the start script can
show it in its summary table instead of pretending everything is fine.

    python3 selftest.py
"""
from __future__ import annotations

import os
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
LIBS = os.path.join(HERE, ".pylibs")
if os.path.isdir(LIBS):
    sys.path.insert(0, LIBS)
sys.path.insert(0, HERE)

TMP = tempfile.mkdtemp(prefix="jarvis-selftest-")
os.environ["JARVIS_DATA"] = TMP
os.environ["JARVIS_LEARNER"] = "0"
os.environ.pop("JARVIS_LLM_KEY", None)

from jarvis_server import Config, create_app  # noqa: E402

PASSED = 0
CHECKS = 0


def check(name: str, cond: bool, extra: str = "") -> None:
    global PASSED, CHECKS
    CHECKS += 1
    if not cond:
        print(f"FAIL  {name} {extra}", flush=True)
        raise SystemExit(1)
    PASSED += 1
    print(f"ok    {name} {extra}".rstrip(), flush=True)


def main() -> int:
    cfg = Config()
    app = create_app(cfg, db_path=os.path.join(TMP, "selftest.db"), start_learner=False)
    c = app.test_client()
    rt = app.extensions["jarvis"]

    h = c.get("/health").get_json()
    check("health answers", bool(h and h["ok"] and h["service"] == "jarvis-server"), f"v{h['version']}")
    check("health does not pretend to have a model", h["config"]["llm_ready"] is False)

    r = c.post("/api/memories", json={"text": "Owner's phone is a Pixel 7", "kind": "fact", "tags": "device"})
    check("memory added", r.status_code == 201, f"id={r.get_json()['id']}")
    c.post("/api/memories", json={"text": "Owner builds dynamic 3D websites"})
    c.post("/api/memories", json={"text": "Owner's phone is a Pixel 7"})
    check("duplicate memory not stored twice", c.get("/health").get_json()["memories"] == 2)
    rec = c.post("/api/recall", json={"query": "which phone does the owner use"}).get_json()
    check("recall finds the right memory", bool(rec["items"]) and "Pixel" in rec["items"][0]["text"])
    check("recall includes the mood line", "mood=" in rec["affect_line"])
    check("empty memory rejected", c.post("/api/memories", json={"text": "  "}).status_code == 400)

    r = c.post("/api/learn", json={"text": "A dynamic 3D website uses requestAnimationFrame",
                                   "task": "learn how to build a dynamic 3D website", "skill": True})
    check("learning job queued", r.status_code == 202, f"job={r.get_json()['job']}")
    jid = r.get_json()["job"]
    check("queue shows the job", rt.store.count_queued_jobs() == 1)
    check("learner processed it", rt.learner.run_once() == 1)
    job = c.get(f"/api/jobs/{jid}").get_json()["job"]
    check("job done with a real result", job["status"] == "done" and "skill=" in job["result"], job["result"])
    mems = c.get("/api/memories").get_json()["items"]
    check("raw note kept (no model key -> no invented lesson)",
          any(m["kind"] == "raw" and m["source"] == "learn" for m in mems))
    skills = c.get("/api/skills").get_json()["items"]
    check("skill written", bool(skills) and skills[0]["name"].startswith("learn-how-to-build"), skills[0]["name"])

    bad = rt.store.add_job("not-a-real-kind", {})
    for _ in range(rt.cfg.learner_max_tries):
        rt.learner.run_once()
    jb = rt.store.job(bad)
    check("bad job retried then failed with the real reason",
          jb["status"] == "failed" and "unknown job kind" in jb["error"], jb["error"][:40])

    check("skill saved", c.put("/api/skills/make-tea", json={"body": "# Make tea\n1. boil water"}).status_code == 200)
    check("skill read back", c.get("/api/skills/make-tea").get_json()["skill"]["body"].startswith("# Make tea"))
    check("missing skill -> 404", c.get("/api/skills/nope").status_code == 404)
    check("skill deleted", c.delete("/api/skills/make-tea").get_json()["ok"] is True)

    s0 = c.get("/api/affect").get_json()
    c.post("/api/affect", json={"kind": "call_fail", "note": "mic went silent"})
    s1 = c.get("/api/affect").get_json()
    check("a failure lowers valence", s1["state"]["valence"] < s0["state"]["valence"])
    c.post("/api/affect", json={"kind": "praise", "weight": 2})
    s2 = c.get("/api/affect").get_json()
    check("praise raises valence and trust",
          s2["state"]["valence"] > s1["state"]["valence"] and s2["state"]["trust"] > s1["state"]["trust"], s2["mood"])
    check("unknown event rejected", c.post("/api/affect", json={"kind": "nope"}).status_code == 400)

    pages = c.get("/api/pages").get_json()["pages"]
    check("SDUI pages served", len(pages) >= 3, str([p["id"] for p in pages]))
    check("one page by id", c.get("/api/pages/brain").get_json()["page"]["id"] == "brain")
    check("unknown page -> 404", c.get("/api/pages/nope").status_code == 404)

    sy = c.post("/api/sync", json={
        "memories": [{"text": "Owner prefers Urdu for voice replies", "kind": "preference"}],
        "skills": [{"name": "call-recovery", "body": "# Call recovery"}],
        "events": [{"kind": "call_ok", "weight": 1}],
    }).get_json()
    check("sync accepted everything", sy["accepted"] == {"memories": 1, "skills": 1, "events": 1}, str(sy["accepted"]))
    check("sync returns the brain", bool(sy["memories"]) and bool(sy["skills"]))

    cfg2 = Config()
    cfg2.token = "selftest-token"
    app2 = create_app(cfg2, db_path=os.path.join(TMP, "auth.db"), start_learner=False)
    c2 = app2.test_client()
    check("health stays open without a token", c2.get("/health").status_code == 200)
    check("api needs the token (401)", c2.get("/api/state").status_code == 401)
    check("wrong token -> 401", c2.get("/api/state", headers={"X-Jarvis-Token": "nope"}).status_code == 401)
    check("right token -> 200", c2.get("/api/state", headers={"X-Jarvis-Token": "selftest-token"}).status_code == 200)
    check("writing needs the token", c2.post("/api/memories", json={"text": "x"}).status_code == 401)

    check("dashboard html", c.get("/").status_code == 200)
    check("control page html", c.get("/control").status_code == 200)

    print(f"SELFTEST PASS {PASSED}/{CHECKS}", flush=True)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except SystemExit:
        raise
    except Exception as e:                                        # noqa: BLE001 - printed, never hidden
        import traceback
        traceback.print_exc()
        print(f"SELFTEST FAIL {type(e).__name__}: {e}", flush=True)
        raise SystemExit(1)
