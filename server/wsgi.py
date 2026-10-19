"""WSGI entry point for gunicorn / free hosts:  gunicorn wsgi:app"""
from __future__ import annotations

from jarvis_server import create_app

app = create_app()
