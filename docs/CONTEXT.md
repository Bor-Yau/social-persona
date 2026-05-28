# Architecture & Domain Model

This document describes the core domain concepts, component responsibilities, and architectural boundaries of the Social Persona Engine.

## Core Concepts

### Persona

A complete personality profile for an AI character. Created through the Matchmaker interview process and stored in the `personas` table. Contains:

- **Big Five personality traits** (`big_five_json`) — openness, conscientiousness, extraversion, agreeableness, neuroticism
- **Attachment dimensions** (`attachment_anxiety`, `attachment_avoidance`) — continuous 0–1 values replacing discrete attachment types
- **Communication style** — social rhythm, conflict style, input method, typing speed, typing fragmentation patterns
- **Life context** (`character_current_context`) — current occupation, location, daily routine, stressors
- **Life archive** (`character_life_archives`) — complete personal history: childhood, adolescence, key life events
- **Sample chats** — 3 conversation samples generated during Matchmaker session to validate persona authenticity
- **Image style** — visual appearance description and image generation style prompt

### Relationship State

Runtime variables that evolve with every interaction. Stored per persona in `relationship_state`:

| Variable | Range | Description |
|----------|-------|-------------|
| `trust` | 0–100 | Falls slowly, rises slowly; requires consistent positive interactions |
| `closeness` | 0–100 | Sensitive in 20–60 range, slow-growth in 60–85; 85+ requires qualitative leaps |
| `tension` | 0+ | Accumulates from conflict, decays naturally over time |
| `emotional_energy` | 0–100 | Current emotional capacity; low values mean withdrawn/silent |
| `tension_pressure` | 0+ | Drives proactive contact urge; accumulates faster for high `initiative_tendency` personas |
| `contact_urge` | 0–1 | Single value summarizing current desire to reach out |

### Events

Time-anchored moments in a persona's daily life. Events drive the simulation forward:

| Event Type | Behavior |
|------------|----------|
| `routine` | Triggers LLM to generate contextual messages at scheduled times |
| `moment` | One-off significant events (emotional peaks, special occasions) |
| `sleep` | Transitions persona to SLEEPING state; triggers daily reflection before generating tomorrow's events |

Events are generated daily via the Python `/api/event/generate` endpoint, with lazy loading on persona startup if no events exist for the current day.

### Matchmaker

A 7-stage conversational agent that interviews the user to build a persona profile:

| Stage | Purpose |
|-------|---------|
| `basic_profile` | Name, gender, age, occupation, relationship start point, world time origin |
| `style_anchor` | Personality traits, Big Five dimensions, communication style |
| `boundary_probe` | Conflict handling, emotional boundaries, sensitivity triggers |
| `attachment_explore` | Attachment anxiety/avoidance, self-esteem stability |
| `system_detail` | Social rhythm, typing speed, initiative tendency, image preferences, appearance, life stage, location |
| `sample_confirm` | Generate and validate 3 sample conversations; final confirmation |
| `confirm` | Build and persist the complete persona configuration |

Session state is stored in `matchmaker_sessions` and managed by a Java state machine.

### Memory System

- **Short-term**: Conversation history maintained in the LLM context window
- **Long-term**: Vector memory via [mem0](https://github.com/mem0ai/mem0) with local ChromaDB storage. Memories are persona-isolated
- **Inner thoughts**: After each reply, the LLM generates a private thought (attitude: positive/negative/mixed/neutral) that guides relationship state updates and memory decisions
- **Daily reflection**: On sleep events, the day's conversations and inner thoughts are consolidated into key memories

## Component Architecture

### Java Manager (Spring Boot 3)

**Responsibilities:**
- HTTP API for frontend admin panel and QQ message routing
- Matchmaker session state machine (7-stage interview flow)
- Persona CRUD and life archive management
- Relationship state engine with non-linear curves and heartbeat decay
- Event scheduler and proactive message dispatch
- Message splitting (by `[SPLIT]` markers), typing delay simulation, burst group management
- Sleep/wake state machine
- Redis caching layer (auto-fallback to in-memory)

**Does NOT:**
- Call any LLM directly
- Parse or generate natural language
- Execute AI tool capabilities

### Python Core (FastAPI)

**Responsibilities:**
- All LLM calls (message replies, event generation, Matchmaker interviews, memory operations)
- Long-term memory storage and retrieval (mem0 + ChromaDB)
- Image generation (via external image API)
- Embedding model management (BAAI/bge-small-zh-v1.5)
- Provider abstraction — supports any OpenAI-compatible API

**IPC Endpoints:**

| Endpoint | Trigger | Frequency |
|----------|---------|-----------|
| `POST /api/message` | User sends message | Per message |
| `POST /api/event/trigger` | Event fires | 5–15/day |
| `POST /api/event/generate` | Daily at midnight | 1/day |
| `POST /api/matchmaker` | Matchmaker interview turn | Multi-turn during creation |
| `POST /api/image/generate` | Persona sends image | On demand |

### Frontend (React + Vite)

Admin panel for persona management and Matchmaker interview interface. No built-in chat window — all messaging happens through QQ.

## Data Flow

```
User message (QQ)
  → NapCat OneBot WebSocket
    → Java WebSocket handler
      → Validate persona state (sleeping? blocked?)
      → POST /api/message to Python
        → Python retrieves relevant memories from mem0
        → Python constructs system prompt with persona config + relationship state + memories
        → Python calls LLM API
      ← Python returns reply items (text/sticker/image) + inner thought + relationship deltas
    → Java applies relationship deltas to RelationshipEngine
    → Java saves inner thought to event log
    → Java splits text by [SPLIT] markers
    → Java sends each item with typing delay via OneBot WebSocket
```

## State Machine

Personas transition through these states:

```
ACTIVE ←→ SLEEPING
  │
  └────→ BLOCKED → ARCHIVED (irreversible)
```

- **ACTIVE**: Normal operation. Receives messages, generates replies, events fire
- **SLEEPING**: After sleep events. Messages are queued but not responded to. Transitions back to ACTIVE on the next non-sleep event
- **BLOCKED**: User has blocked the persona. No message processing
- **ARCHIVED**: Permanent archival state. Data preserved but persona is inactive

## Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Backend | Spring Boot 3 + MyBatis-Plus | REST API, state management, scheduling |
| Database | SQLite | Lightweight embedded database |
| Cache | Redis (optional) + Spring Cache | Persona/relationship state caching |
| LLM Core | Python + FastAPI | All AI inference |
| Memory | mem0 + ChromaDB | Vector-based long-term memory |
| Frontend | React + Vite + TailwindCSS | Admin panel |
| IM Bridge | NapCat (OneBot V11 over WebSocket) | QQ connectivity |
| LLM SDK | openai (Python SDK) | Multi-provider abstraction |
| Embedding | sentence-transformers + BAAI/bge-small-zh-v1.5 | Text vectorization |

## Security

- API keys stored with AES-256-GCM encryption in SQLite
- Encryption key generated on first launch, stored locally
- Sensitive fields never printed in logs
- WebSocket connections restricted to localhost
