# Crew Fortune

> 很認真算，別太認真信。

Crew Fortune 是 Crew 系列的娛樂型算命 App。核心原則是：**用可重現、結構化的方式產生命理結果，再用幽默方式解讀。**

## Product principles

- 嚴謹計算、幽默回應：底層結果 deterministic，不讓 LLM 任意改分數或核心結論。
- 分享優先：每個結果都要適合直接分享給朋友。
- 輕鬆而不傷人：不預測死亡、重大疾病、懷孕、犯罪或災難。
- AI 是文案層：模型負責幽默轉譯，不負責憑空決定命運。
- Crew Agent Harness：共用 agent orchestration，不在 App 裡另造 agent loop。

## MVP modes

1. 今日運勢
2. 隱藏人格
3. 發財命
4. 戀愛 Bug
5. 朋友／情侶合盤

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


## v0.2 fortune calculation engines

### BaZi / Four Pillars

`BA_ZI` accepts a Gregorian birth date plus the local civil birth time.

- Calendar engine: `cn.6tail:lunar:1.7.7`
- Outputs year/month/day/time pillars, day master, visible five-element counts, hidden stems, ten gods, Na Yin, life stages, Ming Gong and Shen Gong.
- Month/year pillars follow the library's exact solar-term-based Gan-Zhi calculations.
- Late-Zi convention is explicitly pinned to `sect=2`: 23:00 does not advance the day pillar.
- v0.2 uses the birthplace's local civil clock time as entered. It does **not** yet apply true-solar-time/longitude correction.
- Five-element balance is a simple visible-element distribution indicator, not a luck score and not a 喜用神 calculation.

### Tarot numerology

`TAROT_NUMEROLOGY` uses a disclosed single-card birth-card convention:

1. Add every digit of `yyyy-MM-dd`.
2. While the result is above 22, sum its digits again.
3. 1–21 map to the Rider-Waite-Smith Major Arcana; 22 maps to 0 / The Fool.
4. Life Path reduces separately while preserving master numbers 11, 22 and 33.
5. Two-digit Major Arcana values 10–21 also expose a reduced soul-card companion.

Different tarot-numerology schools use different reduction conventions, so the method is versioned in the structured facts instead of being presented as universal.
