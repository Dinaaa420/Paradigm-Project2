# PHP client

A small web page (and a CLI) that calls the Darija Translator REST service with cURL and HTTP Basic auth.

Requirements: PHP 8.1+ with the `curl` extension (`php -m | grep curl`).

```bash
# 1. Point it at the server (these are the defaults)
export TRANSLATOR_URL=http://localhost:8080
export TRANSLATOR_USER=student
export TRANSLATOR_PASSWORD=darija2026

# 2a. Web UI → open http://localhost:8000
php -S localhost:8000 -t public

# 2b. Command line
php cli.php "Where is the nearest pharmacy?"
php cli.php "See you tomorrow" --latin
```

On Windows PowerShell use `$env:TRANSLATOR_URL="http://localhost:8080"` instead of `export`.

| File | Role |
|---|---|
| `src/TranslatorClient.php` | Reusable client class (`translate()`, `health()`) |
| `public/index.php` | Form-based web UI, with a browser "Read aloud" button |
| `cli.php` | One-shot translation from the terminal |
