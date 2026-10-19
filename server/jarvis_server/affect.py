"""Honest affect model — JARVIS's "mood".

This is **not** fake biology and not a random animation. It is a small, documented state machine that
reacts to things that really happened on the phone and decays back to a calm baseline with time:

    valence  -1 .. +1   how good things are going right now
    arousal   0 ..  1   how "activated" JARVIS is (many events in a short time)
    energy    0 ..  1   drains while working, refills while idle
    trust     0 ..  1   grows when the assistant's actions actually succeed
    focus     0 ..  1   grows while a task is being worked on, decays when idle

Every event moves the state by a fixed, listed amount (see EVENTS). Between events the state decays
towards calm with a half-life (`JARVIS_AFFECT_HALFLIFE`, default 6 h). The app shows the true numbers
and the derived label; there is no hidden randomness.
"""
from __future__ import annotations

import math
import time

from .config import Config
from .store import Store

BASE = {"valence": 0.15, "arousal": 0.25, "energy": 0.70, "trust": 0.50, "focus": 0.35}

# kind -> (valence, arousal, energy, trust, focus, label)
EVENTS: dict[str, tuple[float, float, float, float, float, str]] = {
    "call_ok":        (0.25, 0.10, -0.05, 0.03, 0.05, "a call went well"),
    "call_fail":      (-0.35, 0.30, -0.10, -0.10, 0.05, "a call failed"),
    "tool_ok":        (0.10, 0.05, -0.02, 0.05, 0.05, "an action worked"),
    "tool_fail":      (-0.15, 0.15, -0.03, -0.06, 0.05, "an action failed"),
    "praise":         (0.45, 0.20, 0.05, 0.08, 0.02, "the owner was pleased"),
    "anger":          (-0.50, 0.40, -0.05, -0.10, 0.05, "the owner was upset"),
    "learn_done":     (0.20, 0.05, -0.08, 0.04, 0.15, "a learning job finished"),
    "learn_fail":     (-0.15, 0.10, -0.05, -0.04, 0.05, "a learning job failed"),
    "task_start":     (0.05, 0.15, -0.05, 0.00, 0.30, "a task started"),
    "task_done":      (0.30, 0.05, -0.10, 0.06, -0.20, "a task finished"),
    "task_fail":      (-0.20, 0.20, -0.08, -0.05, 0.05, "a task failed"),
    "user_talk":      (0.08, 0.10, 0.02, 0.01, 0.10, "the owner spoke"),
    "repeat":         (-0.10, 0.05, 0.00, 0.00, 0.10, "the same thing had to be explained again"),
    "idle":           (0.00, -0.20, 0.10, 0.00, -0.15, "long quiet time"),
    "sleep":          (0.00, -0.30, 0.25, 0.00, -0.30, "resting"),
    "error":          (-0.25, 0.25, -0.05, -0.08, 0.10, "a technical error happened"),
}

LIMITS = {"valence": (-1.0, 1.0), "arousal": (0.0, 1.0), "energy": (0.0, 1.0),
          "trust": (0.0, 1.0), "focus": (0.0, 1.0)}


def label(state: dict) -> str:
    v, a, e = state["valence"], state["arousal"], state["energy"]
    if e < 0.25:
        return "tired"
    if v <= -0.35 and a >= 0.45:
        return "upset"
    if v <= -0.25:
        return "concerned"
    if v >= 0.55 and a >= 0.55:
        return "excited"
    if v >= 0.45:
        return "pleased"
    if a >= 0.6:
        return "alert"
    if v >= 0.20:
        return "content"
    return "calm"


class Affect:
    def __init__(self, store: Store, cfg: Config) -> None:
        self.store = store
        self.cfg = cfg
        self.state = dict(BASE)
        self.updated = time.time()
        self.recent: list[dict] = []
        self._load()

    # ------------------------------------------------------------------ persistence
    def _load(self) -> None:
        saved = self.store.get_kv("affect")
        if isinstance(saved, dict) and "state" in saved:
            self.state = {k: float(saved["state"].get(k, BASE[k])) for k in BASE}
            self.updated = float(saved.get("updated", time.time()))
            self.recent = list(saved.get("recent", []))[-20:]

    def _save(self) -> None:
        self.store.set_kv("affect", {"state": self.state, "updated": self.updated, "recent": self.recent[-20:]})

    # ------------------------------------------------------------------ time decay
    def _decay(self) -> None:
        now = time.time()
        dt_h = max(0.0, (now - self.updated) / 3600.0)
        if dt_h <= 0:
            return
        k = 0.5 ** (dt_h / max(0.1, self.cfg.affect_half_life_h))
        for key in BASE:
            self.state[key] = BASE[key] + (self.state[key] - BASE[key]) * k
        # energy always drifts up while nothing is happening (idle charges the battery, so to speak)
        self.state["energy"] = min(1.0, self.state["energy"] + 0.12 * dt_h)
        self.updated = now

    # ------------------------------------------------------------------ events
    def event(self, kind: str, weight: float = 1.0, note: str = "") -> dict:
        row = EVENTS.get(kind)
        if row is None:
            raise KeyError(f"unknown affect event '{kind}'")
        self._decay()
        dv, da, de, dtr, df, _ = row
        w = max(0.1, min(5.0, float(weight)))
        self.state["valence"] += dv * w
        self.state["arousal"] += da * w
        self.state["energy"] += de * w
        self.state["trust"] += dtr * w
        self.state["focus"] += df * w
        for k, (lo, hi) in LIMITS.items():
            self.state[k] = round(min(hi, max(lo, self.state[k])), 4)
        self.recent.append({"kind": kind, "weight": w, "note": note[:200], "at": time.time()})
        self.recent = self.recent[-20:]
        self.store.add_event(kind, w, note)
        self._save()
        return self.snapshot()

    def snapshot(self) -> dict:
        self._decay()
        self._save()
        return {
            "state": dict(self.state),
            "mood": label(self.state),
            "updated": self.updated,
            "recent": self.recent[-8:],
            "legend": {k: EVENTS[k][5] for k in ("call_ok", "call_fail", "praise", "anger", "learn_done")},
            "decay_hours": self.cfg.affect_half_life_h,
            "note": ("Deterministic affect model: fixed per-event deltas, decay back to baseline with the "
                     "given half-life. Not biology, not random."),
        }

    def recall_line(self) -> str:
        s = self.snapshot()
        st = s["state"]
        return (f"mood={s['mood']} valence={st['valence']:.2f} arousal={st['arousal']:.2f} "
                f"energy={st['energy']:.2f} trust={st['trust']:.2f} focus={st['focus']:.2f}")

    def closeness(self) -> float:
        """Simple 0..1 measure of how well things are going — used by the app to tint the orb."""
        self._decay()
        st = self.state
        return round(max(0.0, min(1.0, 0.5 + st["valence"] * 0.35 + st["trust"] * 0.15
                                    + math.tanh(st["focus"]) * 0.0)), 3)
