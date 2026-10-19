"""Configuration for the JARVIS companion server.

Everything is read from environment variables so the same code runs on Termux, on a laptop and on a
free host. Defaults are safe: no token (open on localhost), learner on, no LLM key (learning then
keeps the raw text instead of pretending to be smart).
"""
from __future__ import annotations

import os
from pathlib import Path

VERSION = "0.1.0"


def _env(name: str, default: str = "") -> str:
    return (os.environ.get(name) or default).strip()


class Config:
    def __init__(self) -> None:
        # --- storage ---
        self.data_dir = Path(_env("JARVIS_DATA", str(Path.cwd() / "data"))).expanduser()
        self.db_path = self.data_dir / "jarvis.db"

        # --- auth (shared secret; the app sends X-Jarvis-Token) ---
        self.token = _env("JARVIS_TOKEN")

        # --- LLM (any OpenAI-compatible endpoint, including Gemini's compat API) ---
        self.llm_base = _env("JARVIS_LLM_BASE", "https://generativelanguage.googleapis.com/v1beta/openai").rstrip("/")
        self.llm_key = _env("JARVIS_LLM_KEY")
        self.llm_model = _env("JARVIS_LLM_MODEL", "gemini-2.0-flash")
        self.llm_embed_model = _env("JARVIS_EMBED_MODEL", "")   # empty = keyword search only (honest fallback)
        self.llm_timeout = int(_env("JARVIS_LLM_TIMEOUT", "60"))

        # --- background learner ---
        self.learner_enabled = _env("JARVIS_LEARNER", "1") == "1"
        self.learner_interval = max(2, int(_env("JARVIS_LEARNER_INTERVAL", "15")))
        self.learner_batch = max(1, int(_env("JARVIS_LEARNER_BATCH", "3")))
        self.learner_max_tries = max(1, int(_env("JARVIS_LEARNER_TRIES", "3")))

        # --- affect engine ---
        self.affect_half_life_h = float(_env("JARVIS_AFFECT_HALFLIFE", "6"))

        self.data_dir.mkdir(parents=True, exist_ok=True)

    @property
    def llm_ready(self) -> bool:
        return bool(self.llm_key)

    def mask(self) -> dict:
        """What may be shown to the phone (never the keys themselves)."""
        return {
            "version": VERSION,
            "llm_ready": self.llm_ready,
            "llm_model": self.llm_model if self.llm_ready else "",
            "embeddings": bool(self.llm_embed_model and self.llm_ready),
            "auth_required": bool(self.token),
            "learner": self.learner_enabled,
            "data_dir": str(self.data_dir),
        }
