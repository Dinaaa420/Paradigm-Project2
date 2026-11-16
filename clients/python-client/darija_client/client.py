"""Python client for the Darija Translator REST service."""

from __future__ import annotations

import os
from dataclasses import dataclass

import requests


class TranslatorError(Exception):
    """The service returned an error or could not be reached."""

    def __init__(self, message: str, status: int | None = None):
        super().__init__(message)
        self.status = status


@dataclass(frozen=True)
class Translation:
    translation: str
    script: str
    source_language: str
    provider: str
    model: str
    cached: bool
    duration_ms: int


class TranslatorClient:
    """Calls ``POST /api/translate`` with HTTP Basic authentication."""

    def __init__(self, base_url: str, username: str, password: str, timeout: float = 60):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.session.auth = (username, password)
        self.session.headers["Accept"] = "application/json"

    @classmethod
    def from_env(cls) -> "TranslatorClient":
        return cls(
            os.environ.get("TRANSLATOR_URL", "http://localhost:8080"),
            os.environ.get("TRANSLATOR_USER", "student"),
            os.environ.get("TRANSLATOR_PASSWORD", "darija2026"),
        )

    def translate(self, text: str, source_language: str = "auto", script: str = "arabic") -> Translation:
        body = self._request("POST", "/api/translate",
                             json={"text": text, "sourceLanguage": source_language, "script": script})
        return Translation(
            translation=body["translation"],
            script=body.get("script", script),
            source_language=body.get("sourceLanguage", source_language),
            provider=body.get("provider", "?"),
            model=body.get("model", "?"),
            cached=bool(body.get("cached", False)),
            duration_ms=int(body.get("durationMs", 0)),
        )

    def health(self) -> dict:
        return self._request("GET", "/api/health", auth=False)

    def _request(self, method: str, path: str, auth: bool = True, **kwargs) -> dict:
        try:
            response = self.session.request(
                method, self.base_url + path, timeout=self.timeout,
                **({} if auth else {"auth": None}), **kwargs)
        except requests.RequestException as e:
            raise TranslatorError(f"Cannot reach the translator at {self.base_url} ({e.__class__.__name__})") from e

        if response.status_code == 401:
            raise TranslatorError("Wrong username or password.", 401)
        try:
            body = response.json()
        except ValueError:
            body = {}
        if not response.ok:
            raise TranslatorError(body.get("message") or f"HTTP {response.status_code}", response.status_code)
        return body
