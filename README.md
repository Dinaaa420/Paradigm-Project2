# Darija Translator: LLM-powered RESTful Web Service (Jakarta REST + Gemini) - Dina DAOUDI 86582

A REST service that translates English, French or any other language into **Moroccan Arabic (Darija, الدارجة)**
using a Large Language Model. The service is secured with HTTP Basic authentication and used by four clients:
a **Chrome extension** (side panel), a **PHP** web app, a **Python** CLI and a **React Native** mobile app.

```
POST /api/translate        {"text": "Where is the train station?"}
→ 200 {"translation": "فين كاينة لاڭار؟", "targetLanguage": "ary", ...}
```

| | |
|---|---|
| Language | Java 17+ (server), JavaScript, PHP 8, Python 3 |
| REST API | **Jakarta RESTful Web Services 3.1** (Jersey implementation) |
| Security | **Jakarta Authentication 3.0**: custom `ServerAuthModule`, HTTP Basic |
| Container | **Embedded Apache Tomcat 10.1** (no application server to install) |
| LLM | **Google Gemini** API (free tier); optional local model with **Ollama** |
| JSON | Jackson |
| Build | Maven (server), npm / Expo (mobile) |
| Config | Environment variables, loaded at startup |

<p align="center">
  <img src="docs/screenshots/sidepanel.png" width="300" alt="Chrome side panel">
  &nbsp;&nbsp;
</p>

## Features

**Server**
- `TranslatorResource.translate()` takes a text and returns its Darija translation (JSON, plain text or `GET` query).
- Every request first goes through a **Jakarta Authentication** module (`BasicAuthServerAuthModule`).
  Without valid credentials the answer is **401** with a `WWW-Authenticate: Basic` challenge. `/api/health` is public.
- The prompt asks for genuine Darija, not Modern Standard Arabic ("بغيت" not "أريد", "دابا" not "الآن").
- Output in **Arabic script** or **Latin script** ("3, 7, 9" chat alphabet, e.g. `wach 3ndek chi 9ahwa?`).
- **Cache**: repeated translations don't use up the free-tier quota.
- **Automatic retry** (1 s, 2 s, 4 s) when Gemini answers "model is experiencing high demand" (HTTP 503).
- Swappable LLM through one setting: **Gemini**, **Ollama** (local lightweight model), or **mock** (offline, no key).
- JSON errors for every case: 400 (bad input), 401, 429 (quota), 502/503 (model problem).

**Chrome extension** (Manifest V3)
- Select text on any page, right-click and choose **Translate "…" to Darija**. The **side panel** opens, the
  selection is pasted in automatically, and the translation appears.
- **Auto-paste selection**: selecting text is enough, no right-click needed.
- 🔊 **Read aloud** (text-to-speech) and 🎤 **voice-to-voice** translation (speak → translate → spoken Darija).
- Arabic/Latin switch, copy button, history, settings page with **Test connection**.
- Dark purple theme (#6C3082, #8B1C62, #702963).

**Other clients**
- **PHP**: web page with a form and Read aloud, plus a CLI (`php cli.php "Hello"`).
- **Python**: CLI app with one-shot, pipe and interactive modes.
- **React Native (Expo)**: Paste & translate, Arabic/Latin switch, Read aloud, history. The password is stored
  in the phone's secure keystore.

## Project structure

```
darija-translator/
├── server/                                         Maven project (REST service)
│   ├── pom.xml
│   ├── .env.example                                example configuration
│   ├── run.sh / run.bat                            build + start scripts
│   └── src/
│       ├── main/java/ma/darija/translator/
│       │   ├── Main.java                           entry point
│       │   ├── TranslatorServer.java               embedded Tomcat setup
│       │   ├── config/
│       │   │   └── AppConfig.java                  reads environment variables
│       │   ├── security/                           Jakarta Authentication
│       │   │   ├── BasicAuthServerAuthModule.java  ServerAuthModule (HTTP Basic)
│       │   │   ├── JakartaAuthenticationInitializer.java  registers the module
│       │   │   ├── UserStore.java                  users + password check
│       │   │   ├── BasicCredentials.java           parses the Authorization header
│       │   │   └── CorsHeaders.java
│       │   ├── rest/                               Jakarta REST
│       │   │   ├── TranslatorApplication.java      @ApplicationPath("/api")
│       │   │   ├── TranslatorResource.java         @Path("/translate"): translate()
│       │   │   ├── HealthResource.java             @Path("/health")
│       │   │   ├── CorsFilter.java
│       │   │   ├── ExceptionMappers.java           errors → JSON
│       │   │   └── dto/                            TranslationRequest / Response, ErrorResponse
│       │   ├── service/
│       │   │   ├── TranslationService.java         validation, cache, output cleaning
│       │   │   ├── PromptBuilder.java              the LLM instructions
│       │   │   └── Script.java                     ARABIC / LATIN
│       │   └── llm/
│       │       ├── LlmClient.java                  interface
│       │       ├── GeminiClient.java               Google Gemini REST API
│       │       ├── OllamaClient.java               local model
│       │       ├── MockLlmClient.java              offline, used by tests
│       │       ├── AbstractHttpLlmClient.java      HTTP + retry logic
│       │       └── LlmClientFactory.java
│       └── test/java/…                             21 JUnit tests
│
├── chrome-extension/                               Chrome extension (Manifest V3)
│   ├── manifest.json
│   ├── background.js                               service worker: context menu, side panel
│   ├── content/selection.js                        reports selected text
│   ├── sidepanel/                                  side panel UI (html, css, js)
│   ├── options/                                    settings page
│   ├── permission/                                 microphone permission page
│   ├── shared/api.js, speech.js                    REST calls, text-to-speech, voice input
│   └── icons/
│
├── clients/
│   ├── php-client/                                 PHP web page + CLI
│   ├── python-client/                              Python CLI + tests
│   └── mobile-app/                                 React Native (Expo) app
│
├── docs/
│   ├── uml/                                        UML diagrams 
│   ├── screenshots/
│   └── postman_collection.json                     Postman tests (import in Postman)
│
├── .gitignore
└── README.md
```

## API

| Method & path | Auth | Body / params | Response |
|---|---|---|---|
| `POST /api/translate` | Basic | JSON `{"text", "sourceLanguage"?, "script"?}` | translation JSON |
| `POST /api/translate` | Basic | `text/plain` body | Darija text |
| `GET /api/translate?text=…&script=…` | Basic | query parameters | translation JSON |
| `GET /api/health` | public | n/a | `{"status":"UP", "provider", "model"}` |

- `sourceLanguage` defaults to `auto` (the model detects the language). Examples: `English`, `French`.
- `script` is `arabic` (default) or `latin`.
- The response contains `translation`, `sourceLanguage`, `targetLanguage` (`ary`, the ISO 639-3 code for
  Moroccan Arabic), `script`, `provider`, `model`, `cached`, `durationMs`, `user`, `timestamp`.

| Error | Meaning |
|---|---|
| 400 | Empty text, more than 5000 characters, or a bad `script` value |
| 401 | Missing or wrong username/password |
| 429 | Gemini free-tier quota reached |
| 502 | Gemini rejected the request (bad key or model name) |
| 503 | Gemini busy or unreachable (after 3 automatic retries) |

## Requirements
- JDK **17** or newer
- Maven 3.9+ (bundled with IntelliJ IDEA)
- A free Gemini API key: <https://aistudio.google.com/apikey> (optional; without it the server runs in mock mode)
- Google Chrome (extension), PHP 8 (PHP client), Python 3 (Python client), Node.js + Expo Go (mobile app)

## Build

```bash
cd server
mvn clean package          # builds target/darija-translator.jar and runs the 21 tests
```

In IntelliJ: open the `server` folder, then **Maven panel → Lifecycle → package**.

## Run

**Server**
```bash
java -jar server/target/darija-translator.jar
```
In IntelliJ: open `Main.java` and click ▶. Check it at <http://localhost:8080/api/health>.

**Gemini key**: set the `GEMINI_API_KEY` environment variable before starting. In IntelliJ:
**Run → Edit Configurations → Main → Environment variables** → `GEMINI_API_KEY=your-key`.

Default account: **`student` / `darija2026`**.

**Chrome extension**
1. Open `chrome://extensions` and turn on **Developer mode**.
2. Click **Load unpacked** and select the `chrome-extension` folder.
3. On the settings page, click **Test connection**.
4. On any page: select text, right-click and choose **Translate "…" to Darija**.

**Clients**
```bash
# PHP → open http://localhost:8000
cd clients/php-client && php -S localhost:8000 -t public

# Python
cd clients/python-client && pip install -r requirements.txt
python -m darija_client "Good morning, how are you?"
python -m darija_client          # interactive mode

# Mobile → scan the QR code with Expo Go
cd clients/mobile-app && npm install && npx expo start
```

### Testing with Postman / cURL
- **Postman**: **Import** `docs/postman_collection.json`. You get 8 ready-made requests (translation, Latin script,
  no credentials → 401, wrong password → 401, empty text → 400) with automatic checks.
- **cURL**: copy the commands from `docs/curl-examples.md`.

```bash
curl -u student:darija2026 -H "Content-Type: application/json" \
     -d '{"text":"Good morning, how are you?"}' http://localhost:8080/api/translate
```

### Configuration without recompiling
All settings are environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `GEMINI_API_KEY` | (none) | Gemini key; when set, the provider becomes Gemini |
| `LLM_PROVIDER` | `gemini` if a key is set, else `mock` | `gemini`, `ollama` or `mock` |
| `GEMINI_MODEL` | `gemini-flash-latest` | e.g. `gemini-flash-lite-latest` when the main model is busy |
| `OLLAMA_BASE_URL` / `OLLAMA_MODEL` | `http://localhost:11434` / `qwen2.5:3b` | local LLM |
| `TRANSLATOR_USERS` | `student:darija2026` | `user:pass,user2:{SHA-256}<hex>` |
| `PORT` | `8080` | HTTP port |
| `MAX_TEXT_LENGTH` | `5000` | max characters per request |

### Local LLM (Ollama)
```bash
ollama pull qwen2.5:3b
# then start the server with LLM_PROVIDER=ollama
```
No key and no quota, but small local models write weaker Darija than Gemini.

### Using the mobile app on a real phone
1. Find your computer's IP with `ipconfig`, e.g. `192.168.1.20`.
2. Allow port 8080 through the firewall.
3. In the app's ⚙︎ Settings, set the server URL to `http://192.168.1.20:8080`.

The phone and the computer must be on the same Wi-Fi.

## How a request works
1. A client sends `POST /api/translate` with `Authorization: Basic …`.
2. Embedded Tomcat calls `BasicAuthServerAuthModule.validateRequest()`. It checks the credentials and passes the
   caller to the container (`CallerPrincipalCallback`), or answers 401.
3. Jakarta REST routes the request to `TranslatorResource.translate()`.
4. `TranslationService` validates the text, checks the cache, builds the prompt and calls the `LlmClient`.
5. `GeminiClient` calls the Gemini API, retrying if it is busy. The answer is cleaned and returned as JSON.

## UML diagrams
In `docs/uml/` :
- `class-diagram`: server classes and relationships
- `deployment-diagram`: Chrome, PHP, Python, phone, server, Gemini / Ollama
- `sequence-translate`: translating selected text from the side panel
- `use-case-diagram`: actors and use cases

## Tests
```bash
cd server && mvn test                                  # 21 tests (security, service, real HTTP calls)
cd clients/python-client && python -m unittest -v      # 6 tests
```

## AI PROMPTS USED

Claude Code AI was used for this project. Here are the prompts used:

"Set up a Java Maven project inside a `server/` folder under the base package `ma.darija.translator`. Configure `pom.xml` with Jakarta RESTful Web Services (Jersey), Jakarta Authentication (JASPIC), Jackson for JSON, JUnit 5 for testing, and an embedded Apache Tomcat server. Create `Main.java` as the entry point, `TranslatorServer.java` to programmatically configure and start embedded Tomcat, and `AppConfig.java` inside `config/` to read environment variables from `.env` (like port, API keys, and credentials), along with `.env.example`, `run.sh`, and `run.bat`."

"Inside `ma.darija.translator.llm`, design a clean Strategy/Factory pattern for calling LLMs so I can switch between cloud, local, and test providers: an `LlmClient` interface, `AbstractHttpLlmClient.java` handling shared HTTP calls and retry logic, `GeminiClient.java` to call the Google Gemini REST API, `OllamaClient.java` to support a local lightweight LLM via Ollama, `MockLlmClient.java` for offline unit/integration testing, and `LlmClientFactory.java` to instantiate the right client based on `AppConfig`."

"Now create the `ma.darija.translator.service` package with a `Script` enum (`ARABIC`, `LATIN`) so users can request Darija in either Arabic script or Arabizi/Latin script, `PromptBuilder.java` to generate tailored system/user instructions for natural Moroccan Darija based on the chosen `Script`, and `TranslationService.java` to validate input text, check an in-memory cache to avoid redundant LLM calls, invoke the `LlmClient`, and clean up the returned output."

"Implement the Jakarta REST layer inside `ma.darija.translator.rest`: `TranslatorApplication.java` annotated with `@ApplicationPath("/api")`, DTOs in `rest/dto/` (`TranslationRequest`, `TranslationResponse`, and `ErrorResponse`), `TranslatorResource.java` mapped to `@Path("/translate")` with a `POST` `translate()` method calling `TranslationService`, `HealthResource.java` mapped to `@Path("/health")` to verify server and LLM provider status, and `CorsFilter.java` and `ExceptionMappers.java` to map validation, auth, and server errors into clean JSON `ErrorResponse` payloads."

"Secure the `/api/translate` endpoint using the Jakarta Authentication Specification (JASPIC) rather than basic servlet filters. Inside `ma.darija.translator.security`, create `BasicCredentials.java` to decode and parse the Base64 `Authorization: Basic` header, `UserStore.java` to store valid users and verify passwords, `BasicAuthServerAuthModule.java` implementing `ServerAuthModule` to validate requests and allow unauthenticated CORS preflight `OPTIONS` requests (using `CorsHeaders.java`), and `JakartaAuthenticationInitializer.java` to programmatically register the `ServerAuthModule` with embedded Tomcat's `AuthConfigFactory`."

"Write a comprehensive suite of ~21 JUnit tests in `server/src/test/java` covering `BasicCredentials`, `UserStore`, `PromptBuilder`, `TranslationService` (using `MockLlmClient`), and end-to-end REST endpoint tests for `/api/translate` and `/api/health`. Also generate a `docs/postman_collection.json` file with pre-configured requests for valid translations (Arabic and Latin script), invalid auth (`401`), and bad requests (`400`)."

"Build a Manifest V3 Chrome extension inside `chrome-extension/`: `manifest.json` declaring `sidePanel`, `contextMenus`, `storage`, and `activeTab` permissions, `background.js` service worker that creates a right-click context menu item ('Translate to Darija'), opens the side panel, and passes selected webpage text to it, `content/selection.js` to detect and report highlighted text on any webpage, `sidepanel/` (HTML, CSS, JS) to display the auto-pasted text, script toggle (`ARABIC` / `LATIN`), and translated output, and an `options/` page so the user can configure the server URL and Basic Auth credentials."

"Create `chrome-extension/shared/api.js` to handle authenticated `fetch` calls to `/api/translate`, and `chrome-extension/shared/speech.js` to add Text-to-Speech (Read Aloud) and Speech-to-Text (voice input). Since Chrome extension side panels often block microphone permission prompts directly, add a dedicated `permission/` page that requests mic access cleanly when the user clicks voice input for the first time."

"Inside `clients/php-client/`, create both a clean PHP web interface (`index.php`) and a CLI script that send HTTP Basic Auth `POST` requests via PHP `cURL` to `/api/translate`, supporting both Arabic and Latin script output."

"Inside `clients/python-client/`, build an interactive Python CLI client using `requests` (reading config/credentials from environment variables or arguments) that calls `/api/translate` with Basic Auth, plus unit tests using `pytest`/`unittest` with mocked HTTP responses."

"Inside `clients/mobile-app/`, create a React Native (Expo) mobile application with a settings screen/modal for the API URL and Basic Auth credentials, a text input box, Arabic/Latin script selector, voice input, and `expo-speech` integration to read the Darija translation aloud."

"Generate PlantUML files in `docs/uml/` for a Class Diagram reflecting the exact packages (`config`, `security`, `rest`, `service`, `llm`) and classes, a Deployment Diagram showing Embedded Tomcat (`TranslatorServer`), external Gemini API / local Ollama nodes, and the four client applications (Chrome Extension, PHP, Python, Expo Mobile App), and a Sequence Diagram tracing an authenticated `POST /api/translate` request from the Chrome Extension through `BasicAuthServerAuthModule`, `TranslatorResource`, `TranslationService`, and `GeminiClient`. Finally, write the root `README.md` documenting the full monorepo layout, setup steps for the server and all clients, and instructions for importing `docs/postman_collection.json`."

