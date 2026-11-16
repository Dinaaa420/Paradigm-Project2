#!/usr/bin/env bash
# Builds (if needed) and starts the server, loading settings from .env when present.
set -euo pipefail
cd "$(dirname "$0")"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

if [ ! -f target/darija-translator.jar ]; then
  mvn -q package -DskipTests
fi

exec java -jar target/darija-translator.jar
