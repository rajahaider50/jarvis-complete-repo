"""The skill book: reusable how-to knowledge (the same SKILL.md idea the app already uses).

A skill is a named body of instructions the assistant can follow later. The server can *write* a new
skill from a task the owner points at ("is tarah ki dynamic website banane ka tareeqa seekho"), which
is exactly the background learning the owner asked for.
"""
from __future__ import annotations

import re

from .config import Config
from .llm import Llm, LlmUnavailable
from .store import Store

SKILL_SYSTEM = (
    "You write short, practical SKILL.md files for a phone assistant that controls a phone and can call "
    "AI APIs. Answer in markdown only, no preamble. Start with '# <Skill name>' then '## When to use', "
    "'## Steps' (numbered, concrete), '## Checks' (how to verify it really worked) and '## Traps' "
    "(what usually goes wrong). Be honest: if the task note is too thin to learn from, write "
    "'# Not enough information' and one line saying what is missing."
)


def slug(name: str) -> str:
    s = re.sub(r"[^a-z0-9\u0600-\u06ff]+", "-", (name or "").lower()).strip("-")
    return s[:60] or "skill"


class SkillBook:
    def __init__(self, store: Store, cfg: Config, llm: Llm) -> None:
        self.store = store
        self.cfg = cfg
        self.llm = llm

    def save(self, name: str, body: str, source: str = "app") -> int:
        return self.store.upsert_skill(name, body, source=source)

    def get(self, name: str) -> dict | None:
        return self.store.skill(name)

    def list(self, limit: int = 200) -> list[dict]:
        return self.store.skills(limit=limit)

    def delete(self, name: str) -> bool:
        return self.store.delete_skill(name)

    def learn(self, task: str, observation: str = "", name: str = "") -> dict:
        """Turn a task (+ what was observed) into a stored skill. Returns {name, body, id, ai}."""
        title = (name or task).strip()
        prompt = f"Task: {task.strip()}\n\nWhat was seen / known:\n{(observation or '(nothing recorded)').strip()[:4000]}"
        try:
            body = self.llm.chat([{"role": "system", "content": SKILL_SYSTEM}, {"role": "user", "content": prompt}],
                                 temperature=0.3, max_tokens=1200)
            ai = True
        except LlmUnavailable:
            body = (f"# {title}\n\n## When to use\n{task.strip()}\n\n## What was recorded\n"
                    f"{(observation or '(nothing recorded)').strip()[:2000]}\n\n"
                    "## Note\nNo model was configured on the server, so this is the raw record, not a distilled skill.")
            ai = False
        name_out = slug(title)
        sid = self.store.upsert_skill(name_out, body, source="learn")
        return {"id": sid, "name": name_out, "body": body, "ai": ai}
