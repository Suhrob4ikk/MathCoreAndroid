# MathCore Android

A native Android application for higher mathematics practice. Covers seven subjects with theory, adaptive quizzes, a real-time duel system, daily challenges, and full cross-platform synchronization with the [MathCore Web](https://github.com/Suhrob4ikk/Calculus) application.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-1.6-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Supabase-2.x-3ECF8E?style=flat-square&logo=supabase&logoColor=white)](https://supabase.com)
[![Min SDK](https://img.shields.io/badge/Min_SDK-26-green?style=flat-square)](https://developer.android.com/about/versions/oreo)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

---

## Overview

MathCore is a cross-platform mathematics training system. The Android client and the web client share the same Supabase backend — results, XP, mistakes, leaderboards, and real-time duels are synchronized across both platforms in real time.

**Download:** [mathcore.apk](https://github.com/Suhrob4ikk/Calculus/raw/main/mathcore.apk)

---

## Features

### Study

| Feature | Description |
|---|---|
| 7 subjects | Integrals, Derivatives, Limits, Series, ODEs, Probability, Linear Algebra |
| 3 difficulty levels | Easy / Medium / Hard, each with its own question pool |
| Open-answer questions | Numeric input with smart normalization (`1.0 = 1 = 1,0`) |
| Theory | 85+ steps per subject with KaTeX-rendered formulas |
| Study mode | Practice without timer or result saving |
| Exam mode | Timed, randomized questions across all subjects |

### Progress & Ranking

| Feature | Description |
|---|---|
| XP system | +10 / +20 / +30 XP per correct answer by difficulty; +25 for 100% |
| Daily challenge | 10 questions seeded by date — identical for all users on both platforms |
| Global leaderboard | Best-per-combo aggregation, synced with web |
| Mistake tracker | Wrong answers saved to Supabase; repeat until resolved |
| Streaks | Consecutive-day activity counter |

### Duels

| Feature | Description |
|---|---|
| Real-time 1v1 | Supabase Realtime WebSocket — Phoenix protocol |
| Invites | Send a challenge by username; accepted in-app |
| Rematch | Immediate rematch flow after a duel ends |
| Cross-platform | Web and Android clients share the same duel channel protocol |

---

## Tech Stack

```
UI              Jetpack Compose + Material 3
Architecture    MVVM — ViewModel + StateFlow + Coroutines
Backend         Supabase (PostgreSQL, Auth, Storage, Realtime)
HTTP            Ktor Client with automatic token refresh
WebSocket       Ktor + Supabase Realtime (Phoenix protocol)
Serialization   kotlinx.serialization
Local storage   DataStore Preferences
Image loading   Coil Compose
Math rendering  KaTeX (WebView)
```

---

## Question Bank

| Subject | Easy | Medium | Hard | Total |
|---|---:|---:|---:|---:|
| Integrals | 75 | 30 | 20 | 125 |
| Derivatives | 75 | 40 | 20 | 135 |
| Limits | 102 | 66 | 30 | 198 |
| Series | 75 | 30 | 20 | 125 |
| ODEs | 75 | 30 | 20 | 125 |
| Probability | 76 | 30 | 20 | 126 |
| Linear Algebra | 75 | 30 | 20 | 125 |
| **Total** | **553** | **256** | **150** | **959** |

Questions are stored as JSON in `app/src/main/assets/questions/`. Daily challenge and duel question selection uses a deterministic seeded PRNG (`mulberry32` + Fisher-Yates), identical to the web implementation.

---

## Project Structure

```
app/src/main/
├── assets/
│   ├── questions/          # 21 JSON files (subject × difficulty)
│   └── katex/              # KaTeX renderer (offline)
└── java/com/mathcore/app/
    ├── data/
    │   ├── model/          # Profile, TestResult, LeaderboardEntry
    │   ├── repository/     # Auth, Results, Mistakes repositories
    │   ├── local/          # PreferencesManager (DataStore)
    │   ├── Question.kt     # Domain models + normalizeAnswer()
    │   └── TheoryData.kt   # Theory content
    ├── ui/
    │   ├── auth/           # Sign in / register
    │   ├── home/           # Home screen, daily challenge dialog
    │   ├── quiz/           # Quiz screen with KaTeX rendering
    │   ├── duel/           # Real-time duel lobby and game
    │   ├── profile/        # User profile + avatar upload
    │   ├── stats/          # Leaderboard (global + by section)
    │   ├── theory/         # Theory viewer
    │   ├── exam/           # Exam mode
    │   ├── mistakes/       # Mistake review
    │   └── search/         # User search + public profile
    ├── viewmodel/          # QuizViewModel, AuthViewModel, DuelViewModel, ExamViewModel
    └── util/
        ├── AppHttpClient.kt    # Ktor client + token refresh
        ├── XpUtils.kt          # Shared XP formula
        └── SoundManager.kt     # Audio feedback (AudioTrack)
```

---

## Getting Started

### Requirements

- Android Studio Hedgehog or later (includes JDK 21)
- Android SDK 26+
- A Supabase project (shared with the web client)

### Build

```bash
git clone https://github.com/Suhrob4ikk/MathCoreAndroid.git
cd MathCoreAndroid
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Configuration

Supabase credentials are in:

```
app/src/main/java/com/mathcore/app/data/repository/SupabaseClient.kt
```

```kotlin
object SupabaseConfig {
    const val URL     = "https://<project>.supabase.co"
    const val API_KEY = "<anon-key>"
    // ...
}
```

---

## Cross-Platform Synchronization

Both clients target the same Supabase instance. The following are guaranteed to be identical across platforms:

| Component | Mechanism |
|---|---|
| Daily questions | `mulberry32(hashCode(dateString))` seeded Fisher-Yates |
| Duel questions | `mulberry32(hashCode("${code}_duel_${section}_${difficulty}"))` |
| XP formula | `correctAnswers × (10/20/30) + (score == 100 ? 25 : 0)` |
| Answer normalization | trim → lowercase → remove whitespace → comma→dot → strip trailing zeros |
| Leaderboard ranking | Best result per `section+difficulty` combo, sum across combos |
| Mistake hashing | `Math.imul(31, h)` equivalent — first 120 chars of question text |

---

## Related

- [MathCore Web](https://github.com/Suhrob4ikk/Calculus) — the web client this app is paired with
