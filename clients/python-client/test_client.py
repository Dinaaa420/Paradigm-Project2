"""Unit tests against a tiny fake of the REST service (no Java server needed)."""

import base64
import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer

from darija_client import TranslatorClient, TranslatorError

GOOD_AUTH = "Basic " + base64.b64encode(b"student:darija2026").decode()


class FakeService(BaseHTTPRequestHandler):
    def _send(self, status, body):
        data = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        self._send(200, {"status": "UP", "provider": "fake", "model": "fake-1"})

    def do_POST(self):
        if self.headers.get("Authorization") != GOOD_AUTH:
            return self._send(401, {"error": "unauthorized"})
        req = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        if not req["text"].strip():
            return self._send(400, {"error": "bad_request", "message": "text must not be empty"})
        out = "salam" if req["script"] == "latin" else "سلام"
        self._send(200, {"translation": out, "script": req["script"], "sourceLanguage": req["sourceLanguage"],
                         "provider": "fake", "model": "fake-1", "cached": False, "durationMs": 3})

    def log_message(self, *args):
        pass


class ClientTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = HTTPServer(("127.0.0.1", 0), FakeService)
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()
        cls.url = f"http://127.0.0.1:{cls.server.server_port}"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()

    def test_translate(self):
        r = TranslatorClient(self.url, "student", "darija2026").translate("Hello")
        self.assertEqual(r.translation, "سلام")
        self.assertEqual(r.provider, "fake")

    def test_latin(self):
        r = TranslatorClient(self.url, "student", "darija2026").translate("Hello", script="latin")
        self.assertEqual(r.translation, "salam")

    def test_bad_password(self):
        with self.assertRaises(TranslatorError) as ctx:
            TranslatorClient(self.url, "student", "nope").translate("Hello")
        self.assertEqual(ctx.exception.status, 401)

    def test_server_error_message(self):
        with self.assertRaisesRegex(TranslatorError, "must not be empty"):
            TranslatorClient(self.url, "student", "darija2026").translate("  ")

    def test_health_is_public(self):
        self.assertEqual(TranslatorClient(self.url, "x", "y").health()["status"], "UP")

    def test_unreachable(self):
        with self.assertRaisesRegex(TranslatorError, "Cannot reach"):
            TranslatorClient("http://127.0.0.1:1", "a", "b", timeout=2).translate("Hello")


if __name__ == "__main__":
    unittest.main()
