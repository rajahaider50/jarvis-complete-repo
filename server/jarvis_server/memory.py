"""The brain's memory: long-term lessons, facts and episodes.

`distil()` is the interesting part: it takes a raw thing that happened (a task, a call log line, a
chat snippet) and asks the model for a few atomic, reusable lessons. With no model configured it
keeps a trimmed raw note marked `source=raw` — honest, and still useful.
"""
from __future__ import annotations

import json
import re

from .config import Config
from .llm import Llm, LlmUnavailable
from .store import Store

DISTIL_SYSTEM = (
    "You turn raw notes into durable, reusable memory for a phone assistant. "
    "Answer with JSON only: {\"lessons\":[{\"text\":\"...\",\"kind\":\"fact|preference|lesson|episode\",\"tags\":\"a,b\"}]}. "
    "Each lesson must be one short sentence, self-contained, no pronouns like 'it', and only what is really "
    "supported by the note. If the note contains nothing durable, answer {\"lessons\":[]}."
)


class Brain:
    def __init__(self, store: Store, cfg: Config, llm: Llm) -> None:
        self.store = store
        self.cfg = cfg
        self.llm = llm

    # ------------------------------------------------------------------ write
    def remember(self, text: str, kind: str = "note", tags: str = "", source: str = "app") -> int:
        text = (text or "").strip()
        if not text:
            raise ValueError("empty memory text")
        emb = None
        if self.cfg.llm_embed_model:
            try:
                emb = self.llm.embed([text])[0]
            except LlmUnavailable:
                emb = None
        return self.store.add_memory(text, kind=kind, tags=tags, source=source, embedding=emb)

    # ------------------------------------------------------------------ read
    def recall(self, query: str, limit: int = 8, touch: bool = True) -> list[dict]:
        hits = self.store.search_memories(query, limit=limit)
        if touch:
            for h in hits[:3]:
                self.store.touch_memory(int(h["id"]))
        return hits

    def list(self, limit: int = 200, kind: str = "") -> list[dict]:
        return self.store.all_memories(limit=limit, kind=kind)

    def forget(self, mid: int) -> bool:
        return self.store.delete_memory(mid)

    # ------------------------------------------------------------------ distil
    def distil(self, text: str, source: str = "server", default_kind: str = "lesson") -> list[int]:
        """Raw note -> a few stored memories. Returns the new/updated memory ids."""
        note = (text or "").strip()
        if not note:
            return []
        lessons = self._ask(note)
        if lessons is None:
            # No model (or it failed): keep one trimmed raw note so nothing is lost and nothing is faked.
            trimmed = re.sub(r"\s+", " ", note)[:600]
            return [self.remember(trimmed, kind="raw", source=source)]
        ids = []
        for item in lessons:
            t = str(item.get("text", "")).strip()
            if not t:
                continue
            ids.append(self.remember(t, kind=str(item.get("kind") or default_kind),
                                     tags=str(item.get("tags") or ""), source=source))
        return ids

    def _ask(self, note: str) -> list[dict] | None:
        try:
            raw = self.llm.chat(
                [{"role": "system", "content": DISTIL_SYSTEM},
                 {"role": "user", "content": note[:6000]}],
                temperature=0.1, max_tokens=700,
            )
        except LlmUnavailable:
            return None
        data = _json_block(raw)
        if data is None:
            return None
        lessons = data.get("lessons")
        return lessons if isinstance(lessons, list) else None


def _json_block(text: str) -> dict | None:
    """The model may wrap JSON in prose or fences; pull the first object out."""
    t = text.strip()
    t = re.sub(r"^```(?:json)?|```$", "", t, flags=re.M).strip()
    start = t.find("{")
    end = t.rfind("}")
    if start < 0 or end <= start:
        return None
    try:
        obj = json.loads(t[start:end + 1])
    except Exception:
        return None
    return obj if isinstance(obj, dict) else None
