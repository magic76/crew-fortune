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

Crew Fortune currently exposes three deterministic calculation systems:

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

3. **印度星盤 / Vedic Astrology / Jyotish**
   - Sidereal Zodiac with Lahiri / Chitrapaksha ayanamsa
   - Whole Sign Houses
   - Lagna, Sun, Moon, Mercury, Venus, Mars, Jupiter, Saturn, Mean Rahu and Ketu
   - Sidereal longitude, Rashi, house, Nakshatra and Pada
   - House lords, classical Graha Drishti, conjunctions, basic dignity and reliable retrograde flags
   - Vimshottari Mahadasha and Antardasha timelines with current periods

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


## v0.10.0 engaging fortune experience

The AI layer now focuses on recognition and follow-up instead of only producing a long report.

### Three strongest observations

AI output includes `topTraits`, exactly three concise fact-grounded observations designed to answer “what feels most like me?” first.

Both BaZi and Tarot overview tabs show these observations before the long AI interpretation.

### One-tap follow-up questions

AI output includes four personalized `followUps` based on the deterministic facts. They are shown as compact “你一定會想問” actions below the result.

Tapping a follow-up:
- opens the Gemini Live teacher
- carries the selected question into the session
- skips the generic 60–90 second opening
- asks for a direct conclusion plus 2–4 deterministic pieces of evidence

Microphone permission flow preserves the selected question across the permission prompt.

### Distinct AI personas

The three AI styles now use different narrative structures rather than only different temperatures:

- **嚴謹**: evidence → interpretation → practical implication
- **普通**: plain conclusion → concrete facts → practical meaning
- **風趣**: recognizable observation → evidence → explanation → one restrained dry punchline

The Live teacher uses the same persona distinction.

This release does not change deterministic BaZi or Tarot calculation rules.


## v0.11.0 interactive fortune UX

The result experience now focuses on moving from “看到結果” to “理解依據” to “直接追問老師”.

### Home branding
- The Crew Fortune cat / crystal-ball logo now appears in the home top bar.
- Tapping the logo opens a compact About card with the app version and slogan.

### Important year highlights
- Both BaZi and Tarot timeline tabs show a deterministic “重要年份 · 先看這幾個” section before the full timeline.
- BaZi highlights use existing Ten-God, interaction, career, wealth and relationship signals.
- Tarot highlights use the existing Personal Year cycle themes.
- The UI explicitly avoids calling this a luck score or good/bad ranking.
- Every highlighted year exposes its evidence and a one-tap “問老師” action.

### Evidence behind the top three observations
- AI output now includes `topTraitEvidence`, aligned 1:1 with the three `topTraits`.
- Tapping a “最像你的 3 件事” card opens the deterministic/fact-grounded evidence.
- The evidence dialog can immediately hand the same context to Gemini Live.

### Context-aware teacher actions
- Individual Luck Pillars, annual years and Tarot months can hand their exact context to the Live teacher.
- Work, wealth/resources and relationship topic cards have dedicated “問老師” actions.
- Bottom follow-up questions change with the current tab instead of always showing the same four questions.

### Clearer AI generation state
The loading banner reflects real generation progress:
1. organizing natal/cycle signals
2. writing the full interpretation
3. repairing / expanding content when a retry is required

This release does not change deterministic BaZi or Tarot calculation rules.


## v0.12.0 deterministic Vedic astrology

Crew Fortune adds **印度星盤 / Vedic Astrology / Jyotish** as the third fortune system while keeping the same product rule as BaZi and Tarot:

```text
Birth date + exact local birth time + deterministic birth place
    ↓
VedicAstrologyCalculator
    ↓
FortuneFacts
    ↓
shared five-tab result UI
    ↓
Gemini interpretation / Gemini Live teacher
```

Gemini never calculates or repairs the chart. It receives the completed deterministic facts and may only explain them.

### Ephemeris and license

The astronomical position layer uses:

```groovy
implementation 'io.github.cosinekitty:astronomy:2.1.19'
```

Astronomy Engine is MIT licensed and is used only for astronomical positions / sidereal time. Crew Fortune owns the astrology convention layer.

Swiss Ephemeris was evaluated because it is a strong fit for Lahiri/sidereal astrology, but its GPL-or-Professional dual license would change the distribution obligations of the Android app unless a Professional License is purchased. It is therefore **not bundled** in v0.12.0. Swiss Ephemeris is used only as an external reference source for regression fixtures.

### Fixed v1 calculation convention

- Zodiac: **Sidereal**
- Ayanamsa: **Lahiri / Chitrapaksha**
- Houses: **Whole Sign**
- Nodes: **Mean Rahu / Ketu**, exactly 180° apart
- Lagna: calculated from birth UTC, latitude, longitude, Greenwich apparent sidereal time and obliquity, then converted with the same Lahiri ayanamsa
- Graha Drishti:
  - all seven classical planets: 7th
  - Mars: additional 4th / 8th
  - Jupiter: additional 5th / 9th
  - Saturn: additional 3rd / 10th
  - no disputed special Rahu/Ketu aspects in v1
- Conjunction: same sidereal sign and within 8°
- Dignity: sign-level exaltation / debilitation / own-sign for the seven classical planets; node dignity is intentionally unassigned
- Vimshottari: 120-year sequence derived from the Moon Nakshatra at birth; Mahadasha and Antardasha expose start/end dates and ages

The calculation details include a `methodVersion` and a `calculationConvention` object so changes remain auditable and testable.

### Birth place input

A Vedic chart never substitutes the phone's current location for the birthplace.

The first release intentionally uses a low-maintenance deterministic input model:

- optional city/display label
- required latitude
- required longitude
- required IANA timezone ID such as `Asia/Taipei`, or an explicit UTC offset when necessary

The city label is not used for calculation and is never sent to AI to guess coordinates. Historical timezone rules are resolved from the supplied timezone. DST gaps are rejected; ambiguous repeated DST times require an explicit offset instead of guessing.

The Android UI now includes city search backed by Open-Meteo geocoding. A selected result fills the city display label, WGS84 latitude/longitude and IANA timezone together. The chart engine still consumes only those deterministic fields, and manual advanced inputs remain available as a fallback.

### Shared result UI

Vedic uses the same five result tabs as the existing modes. The natal tab also includes a deterministic South Indian Whole Sign chart visualization, and the timing tab includes a visual Vimshottari Mahadasha timeline with the current period highlighted and a today marker.


- **總覽** — Lagna, Moon, Sun, Moon Nakshatra, current Mahadasha / Antardasha, top traits and AI summary
- **本命** — 12 houses, 9 grahas, Rashi, Nakshatra / Pada, house lords, Drishti, conjunctions and dignity
- **流年** — v1 is explicitly a Vimshottari Dasha page; Gochar/transits are not silently mixed in
- **主題** — personality/talent, career, wealth, relationships and family/children, each tied to deterministic evidence and the current Dasha
- **解讀** — full local/AI interpretation plus grounded Gemini Live follow-up

Important-period cards expose the underlying Dasha facts and can hand the exact context to the Live teacher.

### AI and Live teacher grounding

For Vedic mode, text AI and Gemini Live may cite only:

- Lagna
- planets
- houses / house lords
- Nakshatra / Pada
- Drishti / conjunctions
- dignity / retrograde facts
- Mahadasha / Antardasha timelines and current periods
- deterministic topic-evidence profiles

If the required evidence is absent, the response must say the current chart facts are insufficient. The prompts explicitly forbid recalculation, changing ayanamsa/house system/node convention, filling missing values or inventing transit claims.

STRICT / NORMAL / FUNNY remain supported. FUNNY still follows observation → chart evidence → serious explanation → one restrained dry punchline.

### Intentionally omitted from v0.12.0

These features are not calculated until a reliable, tested implementation is added:

- Navamsa D9
- Yogas
- Shadbala
- Ashtakavarga
- Gochar / transit forecasting

The app and prompts explicitly disclose those omissions rather than approximating them.

### Share card and privacy

The existing 1080×1350 PNG renderer is reused. Vedic cards contain Lagna, Moon/Nakshatra, current Mahadasha/Antardasha, upcoming Dasha periods and one AI/local line.

The card does **not** expose:

- full name
- birth date
- birth time
- city name
- latitude / longitude
- raw deterministic JSON

### Verification

Unit coverage includes:

- external Swiss Ephemeris Lahiri reference values for a fixed J2000/London fixture
- Lagna and nine-graha sidereal longitudes
- Rashi / Nakshatra / Pada
- 12 Whole Sign houses and house lords
- Mean Rahu/Ketu opposition
- retrograde facts
- Vimshottari current Mahadasha / Antardasha
- DST ambiguity rejection instead of timezone guessing
- FortuneEngine integration
- grounded Live teacher rules
- Vedic share-card privacy
- explicit omission boundaries for unimplemented Vedic layers

Existing BaZi and Tarot deterministic calculation rules are unchanged.
