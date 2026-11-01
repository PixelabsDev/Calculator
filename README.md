# 📱 Pixelabs Calculator

<div align="center">

![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/PixelabsDev/Calculator-/deploy.yml?branch=main&label=CI%2FCD%20Release&logo=githubactions&logoColor=white&style=for-the-badge)
![Google Play Track](https://img.shields.io/badge/Google%20Play-Internal%20Testing-34A853?style=for-the-badge&logo=googleplay&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-00C853?style=for-the-badge&logo=shieldcheck&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

<p align="center">
  <b>A modern, privacy-first, offline Android calculator crafted with Jetpack Compose, Material You design, and an enterprise zero-touch CI/CD release pipeline.</b>
</p>

[**Pixelabs Website**](https://pixelabs.in) • [**Legal & Privacy Portal**](https://policies.pixelabs.in/) • [**Features**](#-features) • [**Architecture**](#-architecture--tech-stack) • [**DevOps & CI/CD**](#-enterprise-devops--cicd) • [**Building**](#-building--testing)

</div>

---

## ✨ Features

### 🎨 Material You & Expressive Motion
- **Dynamic Theming**: Adapts seamlessly to System, Dark, and Light themes with Material 3 color harmony.
- **Organic Stadium Oval Keypad**: Smooth horizontal stadium/capsule key geometries engineered for thumb ergonomics.
- **Fluid Spring Animations**: Micro-interactions powered by Compose physics springs and animated elevation.
- **Haptic Feedback**: Tactile vibrations distinguishing light inputs from heavy evaluations (`=`, `AC`).

### 📐 Powerful Scientific Engine
- **Trigonometry**: Sine, Cosine, Tangent, and their inverse functions ($\arcsin$, $\arccos$, $\arctan$).
- **Angle Modes**: Instant toggle between Radians (`RAD`) and Degrees (`DEG`).
- **Advanced Mathematics**: Natural logarithms ($\ln$), common logarithms ($\log$), powers ($x^y$), square roots ($\sqrt{x}$), factorials ($!$), constants ($\pi$, $e$).
- **Smart Expression Parser**: Implicit multiplication (`2(3)`, `2π`), percentage evaluations, and automatic parenthesis balancing.

### 🔄 Adaptive Dual-Orientation Architecture
- **Portrait Experience**: Focused daily calculation keypad with a smooth expandable scientific drawer and history sheet.
- **Ergonomic Landscape Experience**:
  - Dedicated top horizontal display with auto-scrolling expression input and real-time evaluated preview.
  - **3 + 4 Grid Architecture**: 3 columns of scientific functions paired with 4 columns of primary numeric operators.
  - **Collapsible Scientific Panel**: Smoothly toggle between full scientific mode and an expanded basic numeric keypad with fluid width transitions.

### 📜 Calculation History
- Fast local storage persisting past calculations across app sessions.
- Tap any past expression to recall it directly into the active calculation canvas.
- Quick history wipe with haptic confirmation.

### 🔒 Privacy-First Guarantee
- **100% Offline Core**: Zero internet permissions required for standard math calculations.
- **Zero Background Tracking**: No third-party ad networks, no analytics trackers, no telemetry SDKs.
- **Voluntary User Feedback**: User-initiated feedback modal with explicit opt-in consent and optional diagnostic toggles.

---

## 🏗 Architecture & Tech Stack

```
Calculator-
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt               # Jetpack Compose UI & Adaptive Layouts
│   │   ├── calculator/
│   │   │   ├── CalculatorViewModel.kt    # State management & expression orchestration
│   │   │   └── ExpressionParser.kt       # Deterministic recursive-descent math engine
│   │   └── data/
│   │       └── HistoryEntry.kt           # Local calculation history data model
│   └── src/test/                         # Comprehensive unit & Robolectric test suite
├── scripts/
│   ├── compute-version.py               # Monotonic SemVer calculation engine
│   └── generate-changelog.py            # Automated Conventional Commit changelog
└── .github/workflows/
    └── deploy.yml                       # Production-grade Release CI/CD workflow
```

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose (BOM 2024.09.00) |
| **Design System** | Material Design 3 (Material You) |
| **Architecture** | MVVM (Model-View-ViewModel) + Single Source of Truth |
| **Asynchronous** | Kotlin Coroutines & `StateFlow` |
| **Testing** | JUnit 4, Robolectric, Roborazzi Screenshot Testing |
| **Build System** | Gradle 8.7 + Android Gradle Plugin 8.5.2 (Configuration Cache enabled) |

---

## 🚀 Enterprise DevOps & CI/CD

This repository features a **zero-touch, production-grade automated release pipeline** configured via GitHub Actions ([`.github/workflows/deploy.yml`](.github/workflows/deploy.yml)):

```mermaid
flowchart LR
    A["Push to main"] --> B["Quality Gate\n(Unit Tests + Lint)"]
    B --> C["Security Audit\n(Trivy CVE Scan)"]
    C --> D["Dynamic SemVer\n(Git Rev-List + Conventional Commits)"]
    D --> E["Deterministic Build\n(Signed AAB & Universal APK)"]
    E --> F["Size Watchdog\n(< 5 MB Limit)"]
    F --> G["Google Play Console\n(Internal Testing Track)"]
    F --> H["GitHub Release\n(Release Notes + Artifacts)"]
```

### Pipeline Quality Gates
1. **Automated Dynamic Versioning**:
   - `versionCode` is monotonically computed from git history: `git rev-list --count HEAD`.
   - `versionName` follows Semantic Versioning (`MAJOR.MINOR.PATCH`) deduced from commit conventions (`feat` $\to$ minor, `fix` $\to$ patch, `BREAKING CHANGE` $\to$ major).
2. **Strict Quality Gates**:
   - Compiles and runs `./gradlew testReleaseUnitTest` with fail-fast execution.
   - Executes `./gradlew lintRelease` to prevent fatal lint issues.
   - Runs Trivy vulnerability scanner to flag dependency CVEs.
   - AAB Size Watchdog validates bundle footprint (< 5 MB threshold).
3. **Automated Distribution**:
   - Bundles and signs release with Google Play upload key.
   - Automatically publishes the release to the **Google Play Console Internal Testing track**.
   - Generates a GitHub Release with compiled Universal APK and Android App Bundle (`.aab`).

---

## 🛠 Building & Testing

### Prerequisites
- **JDK 17** (OpenJDK or Amazon Corretto)
- **Android SDK** (API Level 34, Build Tools 34.0.0)

### Local Build Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/PixelabsDev/Calculator-.git
   cd Calculator-
   ```

2. **Configure SDK Path**:
   Create a `local.properties` file in the root directory:
   ```properties
   sdk.dir=/path/to/your/android-sdk
   ```

3. **Run Unit Tests**:
   ```bash
   ./gradlew testReleaseUnitTest
   ```

4. **Run Android Lint**:
   ```bash
   ./gradlew lintRelease
   ```

5. **Assemble Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📄 Privacy Policy & Compliance

Pixelabs Calculator is engineered with privacy as a foundational principle. It is fully compliant with Google Play Store Developer policies and international data protection standards:
- **100% Offline Calculations**: All formulas, trigonometry, arithmetic, and evaluations run locally on the device CPU with zero internet permissions.
- **Zero Background Tracking**: No third-party ad networks, no analytics trackers, and zero telemetry SDKs.
- **Voluntary User Feedback**: In-app feedback submissions require explicit, voluntary user opt-in consent with transparent diagnostics toggles.
- **Central Legal & Trust Center**: [https://policies.pixelabs.in/](https://policies.pixelabs.in/)
- **Calculator Privacy Policy**: [https://policies.pixelabs.in/calculator/privacy.html](https://policies.pixelabs.in/calculator/privacy.html)
- **Calculator Terms & Conditions**: [https://policies.pixelabs.in/calculator/terms.html](https://policies.pixelabs.in/calculator/terms.html)
- **Google Play Direct URL**: [https://policies.pixelabs.in/privacy.html](https://policies.pixelabs.in/privacy.html) (auto-redirect)

---

## 👨‍💻 Author & Organization

Crafted with precision by **Pixelabs**.
- **Website**: [https://pixelabs.in](https://pixelabs.in)
- **Email**: [hello@pixelabs.in](mailto:hello@pixelabs.in)
- **Developer**: Pixelabs Utility Division

---

## ⚖️ License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
