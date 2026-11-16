"""Command-line app.

    python -m darija_client "Good morning!"            # one-shot
    python -m darija_client --latin "See you tomorrow"  # Latin-script Darija
    echo "Thank you very much" | python -m darija_client
    python -m darija_client                             # interactive mode
"""

from __future__ import annotations

import argparse
import sys

from .client import TranslatorClient, TranslatorError

LANGUAGES = ["auto", "English", "French", "Spanish", "German", "Italian", "Modern Standard Arabic"]


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="darija", description="Translate text into Moroccan Darija.")
    p.add_argument("text", nargs="*", help="text to translate (omit for interactive mode or stdin)")
    p.add_argument("-s", "--source", default="auto", help="source language (default: auto-detect)")
    p.add_argument("--latin", action="store_true", help="output Darija in Latin script (3, 7, 9...)")
    p.add_argument("-v", "--verbose", action="store_true", help="show provider, model and timing")
    p.add_argument("--url", help="server URL (default: $TRANSLATOR_URL or http://localhost:8080)")
    p.add_argument("--user", help="username (default: $TRANSLATOR_USER)")
    p.add_argument("--password", help="password (default: $TRANSLATOR_PASSWORD)")
    return p


def make_client(args: argparse.Namespace) -> TranslatorClient:
    client = TranslatorClient.from_env()
    if args.url or args.user or args.password:
        user, password = client.session.auth
        client = TranslatorClient(args.url or client.base_url, args.user or user, args.password or password)
    return client


def show(client: TranslatorClient, text: str, args: argparse.Namespace) -> None:
    result = client.translate(text, args.source, "latin" if args.latin else "arabic")
    print(result.translation)
    if args.verbose:
        timing = "cached" if result.cached else f"{result.duration_ms} ms"
        print(f"  ({result.provider} / {result.model}, {timing})", file=sys.stderr)


def interactive(client: TranslatorClient, args: argparse.Namespace) -> None:
    health = client.health()
    print(f"Darija Translator · connected to {client.base_url} ({health.get('provider')} / {health.get('model')})")
    print("Type text and press Enter. Commands: :latin  :arabic  :source <lang>  :quit\n")
    while True:
        try:
            line = input("› ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            return
        if not line:
            continue
        if line in (":q", ":quit", ":exit"):
            return
        if line == ":latin":
            args.latin = True
            print("  output: Latin script")
            continue
        if line == ":arabic":
            args.latin = False
            print("  output: Arabic script")
            continue
        if line.startswith(":source"):
            args.source = line.partition(" ")[2].strip() or "auto"
            print(f"  source language: {args.source}")
            continue
        try:
            show(client, line, args)
        except TranslatorError as e:
            print(f"  ✗ {e}", file=sys.stderr)


def main(argv: list[str] | None = None) -> int:
    # Windows consoles default to a legacy code page; make sure Arabic prints.
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")

    args = build_parser().parse_args(argv)
    client = make_client(args)
    try:
        if args.text:
            show(client, " ".join(args.text), args)
        elif not sys.stdin.isatty():
            show(client, sys.stdin.read(), args)
        else:
            interactive(client, args)
    except TranslatorError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
