# Crew Fortune

> 很認真算，別太認真信。

Crew Fortune 是 Crew 系列的娛樂型算命 App。核心原則是：**用可重現、結構化的方式產生命理結果，再用幽默方式解讀。**

## Product principles

- 嚴謹計算、幽默回應：底層結果 deterministic，不讓 LLM 任意改分數或核心結論。
- 分享優先：每個結果都要適合直接分享給朋友。
- 輕鬆而不傷人：不預測死亡、重大疾病、懷孕、犯罪或災難。
- AI 是文案層：模型負責幽默轉譯，不負責憑空決定命運。
- Crew Agent Harness：共用 agent orchestration，不在 App 裡另造 agent loop。

## Fortune systems

Crew Fortune now intentionally exposes only two calculation systems:

1. **八字 / Four Pillars**
   - Four Pillars (年／月／日／時)
   - Day Master and weighted Five Elements
   - Hidden Stems and Ten Gods
   - Simplified Day Master strength indicator
   - Natal combinations / clashes / harms / punishments
   - Luck start, direction and 10-year Luck Pillars
   - Current Annual Pillar and its Ten-God relationship

2. **塔羅生命靈數 / Tarot Numerology**
   - Life Path number with 11/22/33 master numbers
   - Birthday number and Attitude number
   - Tarot School birth-card pair/triplet
   - Four Pinnacle numbers and timing
   - Four Challenge numbers
   - Three Period cycles
   - Current Personal Year and Personal Month

## Architecture

```text
User profile
    ↓
FortuneEngine (deterministic)
    ↓
FortuneResult (structured truth)
    ↓
Crew Agent Harness
    ↓
Humor / explanation model
    ↓
Share card / text
```

Harness dependency:

```groovy
implementation 'com.github.magic76:crew-agent-harness:v0.1.3'
```

The product owns its prompt, state and tools. The shared Harness owns orchestration.

## Package

`com.crewpocket.fortune`

## Status

Initial Android MVP scaffold in progress.


## Calculation conventions

### BaZi / Four Pillars

- Calendar engine: `cn.6tail:lunar:1.7.7`
- Late-Zi convention: `sect=2` (23:00 stays on the current civil day).
- Luck Pillars use gender and surrounding solar terms through lunar-java's `Yun` implementation.
- Current version uses the entered birthplace-local civil time and does **not** yet apply longitude / true-solar-time correction.
- Day Master strength is deliberately labeled as a simplified weighted model. It is not presented as a universal 格局／喜用神 verdict.
- Relationship detection covers common stem combinations and branch 六合／六沖／六害／三合／三刑／自刑 patterns.

### Tarot numerology

- Life Path uses the full birth date and preserves master numbers 11, 22 and 33.
- Birthday Number and Attitude Number are calculated separately.
- Tarot Birth Cards follow the Tarot School birth-card system: `MM + DD + century + YY`, producing the standard Major Arcana pair, with 19 → 10 → 1 as the three-card exception.
- The Fool is not used as a birth-card pair in this convention.
- Pinnacles and Challenges use standard birth-date numerology formulas.
- Personal Year follows the calendar-year method; Personal Month derives from the Personal Year plus the current month.

All readings are positioned as entertainment and reflection. Gemini writes the interpretation; deterministic calculators remain the source of truth.


## v0.3.2 UX and report depth

- Gemini BYOK key persists locally through app restarts and normal app upgrades.
- Birth date and birth time use Android date/time pickers instead of free-form typing.
- The last successfully calculated profile is restored on the next launch.
- Users can save up to 12 reusable presets containing name, birth date, birth time, gender and fortune system.
- Deterministic fallback reports now include multiple interpretive chapters instead of a few stock sentences.
- Gemini report output expands to overview, personality/talents, career/wealth, relationships, current timing, human-language translation, punchline and practical advice.
- Gemini text generation output budget is increased so the structured calculation is not compressed into a horoscope-length answer.


## v0.6.0 live fortune teacher

- Result pages include a **老師跟我講解** action.
- Voice conversation uses Gemini 3.8 Live with native audio.
- The teacher receives only locally calculated deterministic facts; it cannot recalculate birth data or substitute defaults.
- The first explanation is designed as a 60–90 second spoken summary, followed by open voice Q&A.
- Voice persona follows the selected AI style: strict, normal or funny.
- Microphone permission is requested only when starting the voice teacher.


## v0.6.1 state, media audio and operation history

- Gemini Live teacher playback uses Android media audio (`USAGE_MEDIA`, normal audio mode), so volume follows media volume rather than call volume.
- `MainActivity` handles orientation/screen-size configuration changes without recreation, preserving the visible result and active Live teacher during rotation.
- `onSaveInstanceState` provides a second recovery path for inputs, selected mode/style, deterministic result and AI copy if Android later recreates the activity.
- A persistent local operation log keeps the most recent 200 important user/app actions. It never stores the Gemini API key or full voice transcript content.
- The top-bar **記錄** entry displays and can clear local operation history.


## v0.7.0 BaZi result tabs and question-ready facts

BaZi results are split into five tabs instead of one long page:

- **總覽** — Day Master, strength, Four Pillars, Five Elements summary, current Luck Pillar, current Annual Pillar and the main interpretation.
- **命盤** — the original detailed chart remains available: Four Pillars, hidden stems, Ten Gods, Five Elements, weighted elements, Na Yin, life stages, natal interactions, Ming Gong and Shen Gong.
- **大運・流年** — detailed Luck Pillars plus an annual timeline from the previous year through the next 15 years. Each row can be opened to inspect deterministic Ten Gods, elements, natal interactions and theme tags.
- **主題分析** — wealth, career and relationship evidence profiles. These expose the source facts instead of inventing opaque fortune scores.
- **老師解讀** — the full AI/local interpretation, Gemini Live teacher entry point and suggested follow-up questions.

The deterministic BaZi facts now also include:
- `annualTimeline`
- enriched `luckPillars`
- `wealthProfile`
- `careerProfile`
- `relationshipProfile`

Gemini text and Live teacher prompts are instructed to use these structures for questions such as the next ten years, wealth, career and relationship timing. They still cannot recalculate the chart or substitute defaults.


## v0.8.0 unified fortune information architecture

Both fortune systems now use the same result-page structure:

- **總覽** — the few facts a user should understand first.
- **本命** — the complete deterministic source data for that system.
- **流年** — time-based cycles with readable summaries and tap-to-inspect evidence.
- **主題** — work/resources, relationships and other domain interpretations tied back to source facts.
- **解讀** — the full local/AI written report.

A shared footer always appears below the tabs for both systems:

- **老師跟我講解**
- **分享結果**

The fortune-system selector is now inside the profile card, above basic data. The selected system is visually highlighted.

### BaZi readability

BaZi Luck Pillars and annual timeline rows now include deterministic plain-language summaries in addition to Gan-Zhi, Ten Gods, elements and natal interactions. Detail dialogs translate internal field names into user-facing Traditional Chinese labels.

### Tarot timing

Tarot Numerology now exposes:

- `personalYearTimeline`: previous year through the next 9 years.
- `personalMonthTimeline`: all 12 months of the current Personal Year.
- Human-readable Personal Year and Personal Month summaries.

Gemini text and Live teacher prompts use these timelines for follow-up questions instead of only knowing the current year.


## v0.9.0 expanded AI reports and resilient JSON parsing

AI interpretation now uses a richer report schema instead of compressing work, wealth and timing into two short fields.

Generated sections:
- `overview`
- `personality`
- `career`
- `wealth`
- `relationships`
- `currentCycle`
- `longTerm`
- `keyYears`
- `translation`
- `punchline`
- `advice`
- `shareText`

BaZi long-term interpretation is required to synthesize the upcoming decade from deterministic `annualTimeline` plus Luck Pillars. Tarot long-term interpretation uses upcoming Personal Years together with Pinnacle / Challenge context.

### AI quality and format recovery

- Gemini output budget is increased to 9000 tokens.
- The parser accepts JSON inside markdown fences and JSON surrounded by extra text.
- Legacy `analysis`, `careerWealth` and `timing` fields remain readable for backward compatibility.
- Only `title` and `overview/analysis` are core parser requirements. Non-core missing fields use safe fallbacks or remain optional.
- A parsed but substantially under-sized report triggers one expansion retry.
- Malformed JSON triggers one restricted JSON-only retry.
- A second failure falls back to the deterministic local report without crashing.
- Operation history records the exact parser reason, Gemini finish reason, response length and a redacted 300-character response preview. API keys are never logged.

The deterministic fortune calculators are unchanged by this release.


## v0.9.2 image sharing

Result sharing now creates a local 1080×1350 PNG card instead of sending the full report as text.

The share card contains only a concise summary:
- mode and brand
- core natal identity
- current cycle
- next three years
- one memorable AI/local interpretation line

BaZi cards show Day Master / strength, current Luck Pillar and upcoming annual rows.
Tarot cards show Personality Card / Soul Card / Life Path, current Personal Year and the next three Personal Years.

Privacy rules:
- full name is not shown
- birth date and birth time are not shown
- raw deterministic facts are never embedded
- name/date/time references inside the quote are redacted before rendering

The PNG is generated in app cache and shared through Android FileProvider with temporary read permission. No server or public URL is used.


## v0.9.3 launcher icon

Crew Fortune now uses the approved purple-and-gold cat / crystal-ball artwork as the Android launcher icon.

- Legacy launchers use the same square artwork.
- Android 8.0+ uses an adaptive icon wrapper with a deep-purple background.
- `android:icon` and `android:roundIcon` are both configured in the manifest.
- The source artwork remains visually consistent with the in-app Crew Fortune branding.


## v0.9.4 launcher alignment

The adaptive launcher foreground is now centered and rendered at 132dp inside the 108dp adaptive-icon viewport. This intentionally crops the baked outer margin from the artwork so Samsung/Android launcher masks no longer show the Crew Fortune icon as a smaller inset tile.


## v0.9.5 adaptive launcher artwork

Android 8.0+ now uses a dedicated adaptive-icon foreground instead of the full poster-style logo.

- Foreground keeps the centered cat, crystal ball, 八字 and 塔羅 motifs.
- The bottom Crew Fortune / 命運研究所 wordmark is excluded from the adaptive foreground because launchers already render the app label separately.
- The artwork fills the 108dp adaptive viewport directly, avoiding the previous double-inset effect on Samsung launchers.
- Legacy launchers continue using the full square artwork.
