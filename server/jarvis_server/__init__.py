"""JARVIS companion server — the owner's own backend.

Run it on the phone with Termux (see docs/SERVER_SETUP_TERMUX.md) or on a free host
(see docs/SERVER_DEPLOY_FREE.md). The architecture and the roadmap are in docs/SERVER_SYSTEM.md.
"""
from .api import Runtime, create_app
from .config import VERSION, Config

__all__ = ["create_app", "Runtime", "Config", "VERSION"]
