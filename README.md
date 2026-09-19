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
