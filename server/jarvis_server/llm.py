"""Smallest possible OpenAI-compatible client: chat + embeddings, standard library only.

Why stdlib: the server must run on a phone (Termux) and on tiny free hosts without compiling anything.
Any provider that speaks the OpenAI format works — Google's Gemini compatibility endpoint, Groq,
OpenRouter, a local llama.cpp/ollama server, ... If no key is set, every call raises
[LlmUnavailable] and callers fall back to an honest non-AI path instead of inventing an answer.
"""
from __future__ import annotations

import json
import urllib.error
import urllib.request

from .config import Config


class LlmUnavailable(RuntimeError):
    """Raised when no key is configured or the provider refused the call. Never faked away."""


class Llm:
    def __init__(self, cfg: Config) -> None:
        self.cfg = cfg
        self.last_error = ""

    # ------------------------------------------------------------------ chat
    def chat(self, messages: list[dict], temperature: float = 0.2, max_tokens: int = 700) -> str:
        if not self.cfg.llm_ready:
            raise LlmUnavailable("no JARVIS_LLM_KEY configured")
        body = {
            "model": self.cfg.llm_model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        data = self._post("/chat/completions", body)
        try:
            return (data["choices"][0]["message"]["content"] or "").strip()
        except (KeyError, IndexError, TypeError) as e:
            raise LlmUnavailable(f"unexpected chat response: {str(data)[:200]}") from e

    # ------------------------------------------------------------------ embeddings
    def embed(self, texts: list[str]) -> list[list[float]]:
        if not (self.cfg.llm_ready and self.cfg.llm_embed_model):
            raise LlmUnavailable("embeddings not configured")
        data = self._post("/embeddings", {"model": self.cfg.llm_embed_model, "input": texts})
        return [list(map(float, d["embedding"])) for d in data["data"]]

    # ------------------------------------------------------------------ transport
    def _post(self, path: str, body: dict) -> dict:
        req = urllib.request.Request(
            self.cfg.llm_base + path,
            data=json.dumps(body).encode("utf-8"),
            method="POST",
            headers={
                "Content-Type": "application/json",
                "Authorization": "Bearer " + self.cfg.llm_key,
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=self.cfg.llm_timeout) as resp:
                return json.loads(resp.read().decode("utf-8") or "{}")
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", "replace")[:300]
            self.last_error = f"HTTP {e.code}: {detail}"
            raise LlmUnavailable(self.last_error) from e
        except Exception as e:                                    # noqa: BLE001 - reported, not hidden
            self.last_error = f"{type(e).__name__}: {e}"
            raise LlmUnavailable(self.last_error) from e
