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
