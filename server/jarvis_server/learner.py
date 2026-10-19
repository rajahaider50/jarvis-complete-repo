"""Background learner: the part that keeps working while the owner is busy.

It is a plain daemon thread that polls the job table, so it needs no Celery/Redis (which would not run
on a phone). Job kinds:

  learn      payload {text, task, observation, name, skill}  -> distill memories (+ optional skill)
  summarise  payload {text, title}                           -> one summary memory
  recall     payload {query}                                 -> a warm answer cached as a memory

Every finished job writes its real result into the job row, so the phone can show exactly what happened.
"""
from __future__ import annotations

import threading
import time

from .config import Config
from .memory import Brain
from .skills import SkillBook
from .store import Store


class Learner:
    def __init__(self, store: Store, brain: Brain, skills: SkillBook, cfg: Config) -> None:
        self.store = store
        self.brain = brain
        self.skills = skills
        self.cfg = cfg
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None
        self.running = False
        self.last_tick = 0.0
        self.done = 0
        self.failed = 0

    # ------------------------------------------------------------------ lifecycle
    def start(self) -> None:
        if self._thread or not self.cfg.learner_enabled:
            return
        self._thread = threading.Thread(target=self._loop, name="jarvis-learner", daemon=True)
        self._thread.start()
        self.running = True

    def stop(self) -> None:
        self._stop.set()
        t = self._thread
        if t:
            t.join(timeout=2)
        self.running = False

    def _loop(self) -> None:
        while not self._stop.is_set():
            try:
                while self.run_once():
                    pass
            except Exception:                                     # noqa: BLE001 - a bad job must not kill the loop
                self.failed += 1
            self.last_tick = time.time()
            self._stop.wait(self.cfg.learner_interval)

    # ------------------------------------------------------------------ work
    def run_once(self) -> int:
        job = self.store.next_job()
        if not job:
            return 0
        self.store.set_kv("learner_last", {"job": job["id"], "kind": job["kind"], "at": time.time()})
        try:
            result = self._handle(job)
            self.store.finish_job(job["id"], "done", result=result)
            self.done += 1
        except Exception as e:                                    # noqa: BLE001 - recorded on the job row
            if job["tries"] < self.cfg.learner_max_tries:
                self.store.requeue_job(job["id"])
            else:
                self.store.finish_job(job["id"], "failed", error=f"{type(e).__name__}: {e}")
                self.failed += 1
        return 1

    def _handle(self, job: dict) -> str:
        p = job["payload"] or {}
        if job["kind"] == "learn":
            text = str(p.get("text") or "").strip()
            task = str(p.get("task") or "").strip()
            obs = str(p.get("observation") or "").strip()
            name = str(p.get("name") or "").strip()
            ids: list[int] = []
            if text:
                ids += self.brain.distil(text, source="learn")
            if task:
                ids += self.brain.distil(f"Task: {task}\nObservation: {obs or '(none)'}", source="learn")
            out = [f"memories={len(ids)}"]
            if p.get("skill"):
                sk = self.skills.learn(task or text, obs, name)
                out.append(f"skill={sk['name']} ai={sk['ai']}")
            return "; ".join(out)

        if job["kind"] == "summarise":
            text = str(p.get("text") or "").strip()
            title = str(p.get("title") or "summary").strip()
            ids = self.brain.distil(f"[{title}] {text}", source="summarise")
            return f"memories={len(ids)}"

        if job["kind"] == "recall":
            hits = self.brain.recall(str(p.get("query") or ""), limit=5, touch=False)
            return f"hits={len(hits)}"

        raise ValueError(f"unknown job kind '{job['kind']}'")

    # ------------------------------------------------------------------ status
    def status(self) -> dict:
        return {
            "enabled": self.cfg.learner_enabled,
            "running": self.running,
            "interval_s": self.cfg.learner_interval,
            "done": self.done,
            "failed": self.failed,
            "queued": self.store.count_queued_jobs(),
            "last_tick": self.last_tick,
            "last": self.store.get_kv("learner_last"),
        }
