# Database Schema Reference

## personas

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `name` | TEXT | Persona display name |
| `big_five_json` | TEXT NOT NULL | Big Five personality traits (JSON) |
| `attachment_anxiety` | REAL | Attachment anxiety, 0–1 |
| `attachment_avoidance` | REAL | Attachment avoidance, 0–1 |
| `self_esteem_stability` | REAL | Self-esteem stability, 0–1 |
| `social_rhythm` | TEXT | Social rhythm descriptor |
| `conflict_style` | TEXT | Conflict handling style |
| `initiative_tendency` | REAL | Proactive contact tendency, 0–1 |
| `input_method` | TEXT | Input method descriptor |
| `typing_style_json` | TEXT | Typing fragmentation pattern (JSON) |
| `typing_speed` | REAL | Characters per second |
| `image_style_prompt` | TEXT | Image generation style (art style, palette, composition) |
| `character_appearance` | TEXT | Physical appearance (hair, eyes, height) |
| `image_enabled` | INTEGER | 0 = disabled, 1 = enabled |
| `api_key_encrypted` | TEXT | AES-256-GCM encrypted API key |
| `sample_chats_json` | TEXT | Sample conversation examples (JSON) |
| `character_initial_world_time` | TEXT | World time origin for the persona |
| `birthday` | TEXT | YYYY-MM-DD format |
| `character_current_context` | TEXT | Current life context (occupation, location, routine) |
| `ai_qq` | TEXT | QQ account used by this persona |
| `owner_qq` | TEXT | Human owner QQ account |
| `relationship_phase` | TEXT | Initial relationship: `stranger`/`acquaintance`/`friend`/`close_friend` |
| `status` | TEXT | `active`/`paused`/`archived` |
| `last_user_message_time` | TEXT | ISO 8601 timestamp of last user message |
| `created_at` | TEXT | Creation timestamp |
| `updated_at` | TEXT | Last update timestamp |

**Constraints:**
```sql
CHECK (status IN ('active', 'paused', 'archived'))
CHECK (relationship_phase IN ('stranger', 'acquaintance', 'friend', 'close_friend'))
CHECK (image_enabled IN (0, 1))
```

**Indexes:**
```sql
idx_personas_owner_qq ON personas(owner_qq)
idx_personas_ai_qq   ON personas(ai_qq)
idx_personas_status  ON personas(status)
```

## character_life_archives

| Column | Type | Description |
|--------|------|-------------|
| `persona_id` | TEXT PK | FK → personas |
| `archive_json` | TEXT | Full life history (childhood, adolescence, key events) |
| `created_at` | TEXT | |
| `updated_at` | TEXT | |

## relationship_state

| Column | Type | Description |
|--------|------|-------------|
| `persona_id` | TEXT PK | FK → personas |
| `trust` | REAL | Trust level, 0–100 |
| `closeness` | REAL | Closeness level, 0–100 |
| `tension` | REAL | Current tension (must be ≥ 0) |
| `emotional_energy` | REAL | Emotional energy, 0–100 |
| `tension_pressure` | REAL | Accumulated pressure driving contact urge |
| `contact_urge` | REAL | Contact urge, 0–1 |
| `last_heartbeat_at` | TEXT | Timestamp for lazy decay calculation |
| `updated_at` | TEXT | |

**Constraints:**
```sql
CHECK (trust BETWEEN 0 AND 100)
CHECK (closeness BETWEEN 0 AND 100)
CHECK (tension >= 0)
CHECK (emotional_energy BETWEEN 0 AND 100)
```

## daily_events

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `persona_id` | TEXT | FK → personas |
| `event_date` | TEXT | yyyy-MM-dd |
| `event_time` | TEXT | HH:mm:ss |
| `event_type` | TEXT | `routine`/`moment`/`sleep` |
| `description` | TEXT | Natural language description |
| `is_active` | INTEGER | 1 = active, 0 = cancelled |
| `created_at` | TEXT | |
| `updated_at` | TEXT | |

**Constraints:**
```sql
CHECK (event_type IN ('routine', 'moment', 'sleep'))
CHECK (is_active IN (0, 1))
```

**Indexes:**
```sql
idx_daily_events_scan ON daily_events(persona_id, event_date, is_active, event_time)
```

## scheduled_messages

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `persona_id` | TEXT | FK → personas |
| `scheduled_time` | TEXT | When to send |
| `actual_send_time` | TEXT | When actually sent |
| `burst_group_id` | TEXT | Burst group UUID (multi-message sequences) |
| `burst_order` | INTEGER | Position within burst group |
| `items_json` | TEXT | Send sequence (text/image items) |
| `inner_thought_json` | TEXT | Persona's private thought (JSON) |
| `mood` | TEXT | Mood label |
| `is_sent` | INTEGER | 0 = pending, 1 = sent |
| `created_at` | TEXT | |
| `updated_at` | TEXT | |

## event_log

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT PK | UUID |
| `persona_id` | TEXT | FK → personas |
| `event_type` | TEXT | Legacy field |
| `log_type` | TEXT | `event`/`inner_thought`/`reflection`/`state_snapshot`/`conversation_turn` |
| `description` | TEXT | Legacy field |
| `detail_json` | TEXT | Flexible detail payload (JSON) |
| `state_snapshot_json` | TEXT | Legacy field |
| `occurred_at` | TEXT | Event timestamp |
| `created_at` | TEXT | |

**Constraints:**
```sql
CHECK (log_type IN ('event', 'inner_thought', 'reflection', 'state_snapshot', 'conversation_turn'))
```

**Indexes:**
```sql
idx_event_log_query ON event_log(persona_id, log_type, created_at)
```

## matchmaker_sessions

| Column | Type | Description |
|--------|------|-------------|
| `session_id` | TEXT PK | UUID |
| `current_stage` | TEXT | Current interview stage |
| `collected_data_json` | TEXT | Accumulated extraction results |
| `history_json` | TEXT | Full conversation history |
| `persona_id` | TEXT | FK → personas (set on completion) |
| `status` | TEXT | `in_progress`/`completed`/`abandoned` |
| `created_at` | TEXT | |
| `updated_at` | TEXT | |

**Constraints:**
```sql
CHECK (current_stage IN ('basic_profile', 'style_anchor', 'boundary_probe',
    'attachment_explore', 'system_detail', 'sample_confirm'))
CHECK (status IN ('in_progress', 'completed', 'abandoned'))
```
