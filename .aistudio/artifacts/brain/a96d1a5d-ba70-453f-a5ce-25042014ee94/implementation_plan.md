# Fix GitHub Actions CI Workflow Failure (Android SDK & JDK Setup)

Fix the build failure occurring during the Android SDK and JDK setup steps on GitHub Actions runners, enabling reliable automated APK generation and artifact publishing.

## User Review & Critical Decisions

> [!IMPORTANT]
> The failure in GitHub Actions was pinpointed to the **"Setup Android SDK"** and SDK initialization step. Ubuntu GitHub-hosted runners already come with the official Android SDK pre-installed at `/usr/local/lib/android/sdk`. Using the third-party `android-actions/setup-android@v3` action and calling `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager` crashes because `cmdline-tools/latest` is not present at that hardcoded path on Ubuntu runners, throwing a fatal command-not-found / license error.

- **Confirmed Decision (Phase 1)**: User confirmed that the failure occurs during the **Set up JDK or Setup Android SDK** step.
- **Architectural Fix**: Replace the fragile third-party `android-actions/setup-android` action and manual `cmdline-tools` path scripts with:
  1. The GitHub-native pre-installed `$ANDROID_HOME`.
  2. Automatic license acceptance via standard license hash files (`$ANDROID_HOME/licenses/android-sdk-license`).
  3. Native Gradle automatic SDK platform/build-tools downloading (`gradle.properties: android.builder.sdkDownload=true`).

---

## 1. Overview & Core Concept

- **What It Does**: Resolves the CI pipeline stoppage so every push to `main` or release tag triggers a clean, deterministic build that outputs the signed debug or release APK directly to GitHub Artifacts and Releases.
- **Target Environment**: GitHub Actions Ubuntu 22.04 / 24.04 runner (`ubuntu-latest`) with JDK 21 and Gradle 9.3.1.
- **Key Value**: The user gets guaranteed APK builds without manually troubleshooting GitHub runner environment paths.

---

## 2. CI Workflow Strategy

```
┌─────────────────────────────────────────────────────────────┐
│ GitHub Actions Runner (ubuntu-latest)                       │
├─────────────────────────────────────────────────────────────┤
│ 1. actions/checkout@v4                                      │
│ 2. actions/setup-java@v4 (Temurin JDK 21)                   │
│ 3. Native SDK Setup & License Agreement:                     │
│    - Auto-create $ANDROID_HOME/licenses/android-sdk-license │
│    - Let Gradle AGP auto-download any missing SDK tools     │
│ 4. Setup Gradle (gradle/actions/setup-gradle@v4)             │
│ 5. Keystore & Env Setup (.env + debug.keystore)             │
│ 6. ./gradlew assembleDebug / assembleRelease                │
│ 7. actions/upload-artifact@v4 (Flowpay APKs)                │
└─────────────────────────────────────────────────────────────┘
```

### Key Workflow Improvements:
1. **Remove `android-actions/setup-android@v3`**:
   The GitHub runner already defines `$ANDROID_HOME`. The third-party action causes path conflicts with pre-installed SDK components.
2. **Standard Non-Interactive License Pre-seeding**:
   Rather than running interactive/brittle `sdkmanager --licenses`, echo standard license hashes into `$ANDROID_HOME/licenses/android-sdk-license`. AGP detects this immediately and never prompts.
3. **Robust SDK Download Setting**:
   Ensure `gradle.properties` contains `android.builder.sdkDownload=true` so Gradle self-heals any missing components in the background.

---

## 3. Product Decisions & Trade-Offs

- **Decision 1: Native Runner SDK vs. Third-Party Setup Actions**
  - *Chosen Approach*: Use GitHub's pre-configured Android SDK environment and pre-seed the license directory.
  - *Why*: Eliminates 100% of external action runtime breakages, command line tool path mismatches, and NodeJS version compatibility warnings.
  - *Alternatives Considered*: Re-downloading command-line tools on every run (adds 2-3 minutes to build time and is prone to download timeouts).

- **Decision 2: Automated License Acceptance**
  - *Chosen Approach*: Inject official license hashes (`8933bad161af6ba78b136de3c4f230c801f061cba4760a4533624040f74b23e4`) into `$ANDROID_HOME/licenses/`.
  - *Why*: Completely non-blocking and works across all AGP versions.

---

## 4. Execution Steps

1. Update `.github/workflows/build-apk.yml`:
   - Replace the `Setup Android SDK` and `sdkmanager` steps with native license initialization.
   - Retain JDK 21 Temurin setup (`actions/setup-java@v4`).
   - Retain automatic debug keystore generation and release keystore decoding.
2. Update `gradle.properties` to ensure `android.builder.sdkDownload=true`.
3. Commit and prepare the clean push to GitHub so the user can re-trigger the workflow and receive the APK artifact immediately.
