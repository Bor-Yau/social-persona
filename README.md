# Social Persona Engine (AI 网友模拟器)

<p align="center">
  <a href="./LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License"></a>
  <a href="#"><img src="https://img.shields.io/badge/python-3.12+-blue.svg" alt="Python"></a>
  <a href="#"><img src="https://img.shields.io/badge/java-21+-orange.svg" alt="Java"></a>
  <a href="#"><img src="https://img.shields.io/badge/node-18+-green.svg" alt="Node"></a>
</p>

---

A **relationship-state-driven** AI social simulation system. Not a chatbot — an AI persona that lives inside your IM client, with its own personality, memories, motivations, and evolving feelings toward you.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Documentation](#documentation)
- [Acknowledgments](#acknowledgments)
- [License](#license)

## Features

**Persona Creation via Interview or Form.** Two creation modes: let the *Matchmaker* — a 7-stage conversational interview agent — guide you through personality questions like a consultant; or use the *Manual Create* form to fill in every parameter directly. The result is a full persona profile (25+ dimensions) including Big Five traits, attachment dimensions, conflict style, communication habits, and a complete life archive.

**Relationship State Machine.** Every persona maintains a continuously evolving relationship with its human user — trust, closeness, tension, emotional energy, and contact urge — all shaped by interaction history. This is not a static character prompt; the persona's attitude toward you changes with every conversation.

**Long-Term Vector Memory.** Built on mem0 with ChromaDB for local vector storage. Personas remember key moments, emotional peaks, and personal details across sessions. Each persona has its own isolated memory namespace.

**Intrinsic Motivation.** Personas have their own needs, moods, and daily rhythms. They do not just wait for your messages — the event scheduler generates proactive messages triggered by routine events, emotional states, and the natural passage of time.

**Real IM Platform Integration.** All interactions happen through QQ — not a custom chat widget. The persona connects via NapCat (OneBot V11 WebSocket), making the experience indistinguishable from chatting with a real person.

**Image Generation (Optional).** Personas can generate and send images (selfies, scenery, memes) using an external image API. Supports OpenAI-compatible image endpoints.

## Architecture

```
┌──────────┐     ┌──────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌──────────────┐
│   User   │────▶│  NapCat  │────▶│  Java Manager   │────▶│  Python Core    │────▶│  LLM API     │
│   (QQ)   │◀────│ (OneBot) │◀────│  (Spring Boot)  │◀────│  (FastAPI)      │◀────│  (OpenAI SDK) │
└──────────┘     └──────────┘     └────────┬────────┘     └────────┬────────┘     └──────────────┘
                                           │                       │
                                           ▼                       ▼
                                    ┌────────────┐          ┌──────────────┐
                                    │   SQLite   │          │ ChromaDB     │
                                    │ (Personas, │          │ (mem0        │
                                    │  Events,   │          │  long-term   │
                                    │  Sessions) │          │  memory)     │
                                    └────────────┘          └──────────────┘
```

### Interaction Flow

1. **Persona Creation** — Open the web admin panel → two options: Matchmaker (7-stage conversational interview) or Manual Create (form with all parameters) → persona profile confirmed → persona deployed
2. **Daily Chat** — User sends QQ message → NapCat forwards to Java via WebSocket → Java calls Python for LLM response → Python queries mem0 for relevant memories → generates reply with split markers → Java simulates typing delay → sends via QQ
3. **Proactive Messaging** — Event scheduler fires at configured times → Java calls Python to generate event-triggered messages → scheduled messages queued → Java scans and dispatches at appropriate times
4. **Memory Consolidation** — After each reply, an inner thought is generated → at end of day (sleep event), a daily reflection summarizes the day → key memories written to long-term storage

## Prerequisites

| Component | Version | Required | Notes |
|-----------|---------|----------|-------|
| [Java (JDK)](https://adoptium.net/) | 21+ | Yes | Spring Boot backend |
| [Maven](https://maven.apache.org/) | 3.6+ | Yes | Java build tool |
| [Python](https://www.python.org/) | 3.12+ | Yes | LLM orchestration + memory |
| [Node.js](https://nodejs.org/) | 18+ | Yes | Frontend admin panel |
| Redis | — | No | Falls back to in-memory cache if unavailable |
| [NapCat QQ](https://github.com/NapNeko/NapCatQQ) | — | Yes | QQ bridge; user installs separately |
| LLM API Key | — | Yes | Any OpenAI-compatible provider (DeepSeek, OpenAI, Anthropic, custom endpoint) |

## Quick Start

### 1. Install dependencies

```bash
# Python
cd python-core
python -m venv .venv
.venv\Scripts\activate        # Windows
pip install -r requirements.txt

# Frontend
cd frontend
npm install
```

### 2. Configure the LLM provider

When you first open `http://localhost:5173`, an onboarding wizard will guide you through LLM provider configuration, image model setup, and QQ account binding — all from the web UI. You can also manage providers anytime in **Settings**.

To configure manually, copy the template and edit the JSON file:

```bash
copy java-manager\data\system_config.example.json java-manager\data\system_config.json
```

Edit `system_config.json`:

| Field | Description |
|-------|-------------|
| `provider` | Chat LLM provider: `deepseek`, `openai`, `anthropic`, or `custom` |
| `apiKeyEncrypted` | Your API key, Base64-encoded |
| `baseUrl` | API endpoint (auto-filled for known providers) |
| `model` | Model name (e.g., `deepseek-chat`, `gpt-4o`) |
| `qq` | Your QQ account number |
| `imageProvider` | Image generation provider (optional) |
| `imageApiKeyEncrypted` | Image API key, Base64-encoded (optional) |

### 3. Launch

Double-click `start.bat`. On first launch, the script automatically:
1. Installs Python dependencies
2. Downloads the embedding model (`BAAI/bge-small-zh-v1.5`, ~47 MB) — cached locally afterward
3. Starts all services: Python → Java → Frontend

Open `http://localhost:5173` to access the admin panel.

To start services individually:

```bash
cd python-core
python -m uvicorn main:app --host 127.0.0.1 --port 8000

cd java-manager
mvn spring-boot:run

cd frontend
npm run dev
```

### 4. Connect QQ

1. Download and install [NapCat QQ](https://github.com/NapNeko/NapCatQQ)
2. Configure OneBot WebSocket to `ws://127.0.0.1:8080/ws/onebot`
3. Create a persona via the admin panel → Matchmaker interview

### 5. Stop

```bash
# Stop all services (Windows)
双击 stop.bat
```

## Project Structure

```
├── java-manager/                     # Spring Boot backend
│   └── src/main/java/com/socialpersona/
│       ├── matchmaker/               # 7-stage interview engine
│       ├── persona/                  # Persona CRUD + life archive
│       ├── message/                  # QQ message routing
│       ├── sim/                      # Simulation controller
│       └── event/                    # Event scheduler
├── python-core/                      # FastAPI core
│   ├── api/                          # REST endpoints
│   ├── engine/
│   │   ├── prompt_templates.py       # Interview prompt templates
│   │   ├── memory.py                 # mem0 + ChromaDB engine
│   │   └── llm/                      # LLM provider adapters
│   ├── main.py
│   ├── startup_check.py              # Model auto-download on launch
│   └── requirements.txt
├── frontend/                         # React admin panel
├── start.bat                         # One-click startup
├── stop.bat                          # Stop all services
└── .github/workflows/ci.yml          # CI pipeline
```

## Configuration

### Supported LLM Providers

The system uses OpenAI SDK for compatibility. Supported providers:

| Provider | Chat | Image Generation |
|----------|------|------------------|
| DeepSeek | Yes | No |
| OpenAI | Yes | Yes (DALL-E) |
| Anthropic | Yes | No |
| Custom (OpenAI-compatible) | Yes | Yes |

Provider metadata is managed in `java-manager/src/main/resources/providers.json`.

### Image Generation

When `imageProvider` is configured:

1. The LLM persona is informed it can send images
2. When the persona wants to send an image, Java calls Python's `/api/image/generate`
3. The generated image is saved locally and sent to QQ via OneBot CQ code

Image generation runs in two modes:
- **Sync**: Blocking — generates image first, then sends (used for proactive messages)
- **Async** (planned): Non-blocking — sends a stalling text first, generates in background

> **Note:** The admin panel UI defaults to Chinese. Select your preferred language on first visit (a popup will appear), or change it later in Settings. The admin panel fully supports both English and Chinese.

## Documentation

- [docs/CONTEXT.md](docs/CONTEXT.md) — Domain model, terminology, and architectural boundaries
- [docs/database-schema.md](docs/database-schema.md) — Complete database schema reference

## Acknowledgments

This project builds on ideas and infrastructure from:

| Project | Role | License |
|---------|------|---------|
| [mem0](https://github.com/mem0ai/mem0) | Long-term memory engine (direct dependency) | Apache 2.0 |
| [Generative Agents](https://github.com/joonspk-research/generative_agents) | Core simulation concepts (Stanford, Park et al.) | Apache 2.0 |
| [MetaGPT](https://github.com/geekan/MetaGPT) | Multi-agent architecture patterns | MIT |
| [LangGraph](https://github.com/langchain-ai/langgraph) | Stateful agent orchestration patterns | MIT |
| [NapCatQQ](https://github.com/NapNeko/NapCatQQ) | QQ communication bridge | See its repo |

## License

[Apache License 2.0](LICENSE)
