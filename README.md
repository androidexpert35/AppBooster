<p align="center">
  <img src="docs/images/logo.png" width="192" alt="OptiDroid App Icon"/>
</p>

<h1 align="center">OptiDroid</h1>

<p align="center">
  <strong>Supercharge your Android device performance — one tap at a time.</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#how-it-works">How It Works</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#building">Building</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#license">License</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose"/>
  <img src="https://img.shields.io/badge/Min_SDK-26-brightgreen" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/Target_SDK-36-blue" alt="Target SDK"/>
  <img src="https://img.shields.io/badge/Architecture-MVVM_+_Clean-orange" alt="Architecture"/>
  <img src="https://img.shields.io/badge/License-Apache_2.0-green" alt="License"/>
</p>

---

## ✨ What is OptiDroid?

**OptiDroid** is a modern Android app that optimizes the ART (Android Runtime) compilation of every installed app on your device. It leverages `dex2oat` — Android's ahead-of-time compiler — to re-compile apps with aggressive optimization profiles, resulting in **faster app launches**, **smoother scrolling**, and **better overall performance**.

> Think of it as defragmenting your phone's app layer — apps run native-speed code instead of interpreted bytecode.

---

## 🚀 Features

### 🔍 Smart Analysis
- **Pre-scan all installed apps** to determine which need optimization
- **Intelligent skip logic** — already-optimized and recently compiled apps are skipped
- **Detailed per-app compilation status** including compiler filter, timestamps, and OAT file presence

### ⚡ One-Tap Optimization
- **Batch optimization** of all apps that need it
- **Real-time progress tracking** with per-app status
- **Live activity feed** showing optimization logs as they happen
- **Background execution** via WorkManager with foreground notification

### 🎛️ Configurable Optimization Modes
| Mode | Description | Best For |
|------|-------------|----------|
| `speed-profile` | Profile-guided compilation — optimizes hot code paths | Daily use, balanced |
| `speed` (Full) | Aggressively compiles everything ahead-of-time | Maximum performance |

### 🔐 Privileged Access via Shizuku
- **No root required** — uses [Shizuku](https://shizuku.rikka.app/) for privileged shell access
- **Guided setup wizard** walks you through enabling Shizuku step by step
- **Automatic permission handling** with clear status indicators

### 🎨 Beautiful Material 3 UI
- **Material Design 3 Expressive** with dynamic color theming
- **Smooth 60fps animations** on all state transitions
- **Dark mode support** out of the box
- **Localization ready** (English & Italian)

---

## 📱 Screenshots

> *Coming soon — PRs welcome!*

<!-- 
<p align="center">
  <img src="docs/screenshots/shizuku_setup.png" width="250"/>
  <img src="docs/screenshots/dashboard.png" width="250"/>
  <img src="docs/screenshots/settings.png" width="250"/>
</p>
-->

---

## 🧠 How It Works

```
┌─────────────────────────────────────────────────────────┐
│  1. Setup    │  Enable Shizuku for privileged access     │
├──────────────┼───────────────────────────────────────────┤
│  2. Analyze  │  Scan all apps → find what needs work     │
├──────────────┼───────────────────────────────────────────┤
│  3. Optimize │  Run `cmd package compile` on each app    │
├──────────────┼───────────────────────────────────────────┤
│  4. Enjoy    │  Faster launches & smoother performance   │
└─────────────────────────────────────────────────────────┘
```

Under the hood, OptiDroid:

1. **Connects** to a privileged shell via Shizuku (or wireless ADB)
2. **Analyzes** every installed package's compilation status using `dumpsys package` and OAT file metadata
3. **Filters** out apps that are already optimized, system apps, or recently compiled
4. **Executes** `cmd package compile -m <mode> -f <package>` for each app that needs optimization
5. **Streams** real-time progress and logs back to the UI via `StateFlow`

---

## 🏁 Getting Started

### Prerequisites

| Requirement | Details |
|-------------|---------|
| **Android Device** | Android 8.0+ (API 26) |
| **Shizuku** | Install from [Google Play](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or [GitHub](https://github.com/RikkaApps/Shizuku) |
| **ADB** (for Shizuku) | One-time setup via `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh` |

### Quick Start

1. **Install Shizuku** and start the Shizuku service
2. **Install OptiDroid** (build from source or grab a release)
3. **Grant permission** — OptiDroid will guide you through the Shizuku setup
4. **Tap Analyze** to scan your apps
5. **Tap Optimize** and watch the magic happen ✨

---

## 🏗️ Architecture

OptiDroid follows **Clean Architecture** with **MVVM + Unidirectional Data Flow (UDF)**.

```
┌──────────────────────────────────────────────────────────┐
│                    Presentation Layer                      │
│  ┌──────────────┐  ┌───────────┐  ┌───────────────────┐  │
│  │  Composables  │←─│ ViewModels│←─│ UiModels/UiState  │  │
│  │  (Screens)    │──│ (State +  │  │ (Immutable)       │  │
│  │               │  │  Events)  │  │                   │  │
│  └──────────────┘  └─────┬─────┘  └───────────────────┘  │
│                          │                                 │
├──────────────────────────┼─────────────────────────────────┤
│                    Domain Layer (Pure Kotlin)               │
│  ┌──────────────┐  ┌─────┴─────┐  ┌───────────────────┐  │
│  │  Use Cases    │  │Repository │  │  Domain Models     │  │
│  │  (invoke())   │  │Interfaces │  │  (Entities)        │  │
│  └──────────────┘  └─────┬─────┘  └───────────────────┘  │
│                          │                                 │
├──────────────────────────┼─────────────────────────────────┤
│                    Data Layer                               │
│  ┌──────────────┐  ┌─────┴─────┐  ┌───────────────────┐  │
│  │  Repository   │  │   Data    │  │  Shizuku / ADB    │  │
│  │  Impls        │  │  Sources  │  │  Shell Clients    │  │
│  └──────────────┘  └───────────┘  └───────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### Project Structure

```
com.tony.appbooster/
├── 📂 data/              # Repository implementations, shell clients
│   ├── client/           # ADB & Shizuku shell client implementations
│   ├── repository/       # Concrete repository classes
│   └── scheduler/        # WorkManager schedulers
├── 📂 domain/            # Pure Kotlin — zero Android dependencies
│   ├── client/           # Shell client interfaces
│   ├── model/            # Domain entities (Progress, Analysis, Config)
│   ├── repository/       # Repository contracts
│   ├── scheduler/        # Work scheduler interfaces
│   └── usecase/          # Business logic (Analyze, Optimize, Connect)
├── 📂 presentation/      # UI layer (Compose + ViewModels)
│   ├── navigation/       # Navigation graph & Screen definitions
│   ├── screen/           # Composable screens (Dashboard, Settings, Shizuku)
│   ├── ui/               # Theme, components, design system
│   ├── viewmodel/        # ViewModels + UiModels
│   └── worker/           # WorkManager workers for background optimization
└── 📂 di/                # Hilt dependency injection modules
```

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin 2.3 |
| **UI Framework** | Jetpack Compose + Material 3 Expressive |
| **Architecture** | MVVM + Clean Architecture + UDF |
| **DI** | Hilt (constructor injection) |
| **Async** | Coroutines + StateFlow / SharedFlow |
| **Navigation** | Navigation Compose |
| **Background Work** | WorkManager with foreground service |
| **Storage** | DataStore Preferences |
| **Shell Access** | Shizuku API / Dadb (wireless ADB) |
| **Testing** | JUnit 4, MockK, Turbine |
| **Build System** | Gradle with Version Catalog |

---

## 🔨 Building

### Prerequisites

- **Android Studio Ladybug** (2024.3+) or newer
- **JDK 17+**
- **Android SDK 36**

### Build & Run

```bash
# Clone the repository
git clone https://github.com/yourusername/OptiDroid.git
cd OptiDroid

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew runUnitTests

# Run all tests (requires connected device)
./gradlew runAllTests
```

### Build Variants

| Variant | Description |
|---------|-------------|
| `debug` | Development build with debugging enabled |
| `release` | Optimized build (ProGuard rules included) |

### GitHub Actions CI

The repository includes a GitHub Actions workflow at `.github/workflows/android-ci.yml` with three jobs:

| Job | Trigger | What it does |
|-----|---------|--------------|
| **Unit tests** | Every push & PR | Runs `./gradlew runUnitTests` (the existing Gradle task) |
| **Signed release build** | Push to `master` only, after tests pass | Builds signed APK + AAB |
| **Publish GitHub Release** | After signed build succeeds | Creates a GitHub Release with the **merged PR body as changelog** and attaches the APK & AAB |

The release tag is derived automatically from `versionName` and `versionCode` in `app/build.gradle.kts` (e.g. `v1.0-1`).

> **Changelog**: When you merge a PR into `master`, the CI extracts the PR description and uses it as the release notes. If no matching PR is found (e.g. a direct push), it falls back to the last 10 commit messages.

Configure these repository secrets:

| Secret | Purpose |
|--------|---------|
| `GH_RELEASE_KEYSTORE_BASE64` | Base64-encoded contents of the release keystore file |
| `GH_RELEASE_KEY_ALIAS` | Alias of the release key inside the keystore |
| `GH_RELEASE_KEY_PASSWORD` | Password for the selected key alias |
| `GH_RELEASE_STORE_PASSWORD` | Password for the keystore itself |

All four secrets are **required** for the release & publish jobs. Unit tests still run on every push and PR regardless of secrets.

---

## 🧪 Testing

OptiDroid uses a modern testing stack with clear naming conventions:

```kotlin
@Test
fun `given apps need optimization when optimize invoked then returns success`()
```

```bash
# Unit tests only (no device needed)
./gradlew runUnitTests

# Instrumented tests (requires device/emulator)
./gradlew runInstrumentedTests

# Full suite
./gradlew runAllTests
```

**Reports** are generated at:
- Unit tests → `app/build/reports/tests/testDebugUnitTest/index.html`
- Instrumented → `app/build/reports/androidTests/connected/debug/index.html`

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Guidelines

- Follow the [coding guidelines](.github/copilot-instructions.md) for architecture and style
- Write KDoc for all public APIs
- Include unit tests for new use cases and ViewModels
- Use Material 3 Expressive components with proper animations
- Keep the domain layer free of Android dependencies

---

## 📄 License

```
Copyright 2025 Tony

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 🙏 Acknowledgments

- **[Shizuku](https://github.com/RikkaApps/Shizuku)** — Privileged API access without root
- **[Dadb](https://github.com/nicholasgasior/dadb)** — Pure Kotlin ADB client
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)** — Modern declarative UI
- **[Material Design 3](https://m3.material.io/)** — Design system & components

---

<p align="center">
  Made with ❤️ and Kotlin
</p>

