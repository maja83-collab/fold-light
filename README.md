# Fold Light — Light Phone III trip planner

Offline trip planner built on the [Light SDK](https://github.com/lightphone/light-sdk).

| Resource | Path |
|----------|------|
| **Tool README** | [tool/README.md](./tool/README.md) |
| **Dashboard submission** | [docs/DASHBOARD-SUBMISSION.md](./docs/DASHBOARD-SUBMISSION.md) |
| **Tool guidelines** | [TOOL_GUIDELINES.md](https://github.com/lightphone/light-sdk/blob/main/TOOL_GUIDELINES.md) |
| **Setup (Croatian)** | [SETUP-HR.md](./SETUP-HR.md) |

## Module layout

Light’s build server compiles **`:tool`**. In this repo, `:tool` is wired to `tool` (Fold Light).

```text
tool/   ← Fold Light source + lighttool.toml
settings.gradle.kts maps :tool → tool
```

## Quick start (emulator)

Light recommends a phone-sized AVD: **1080 × 1240**, **3.92"**, **API 34**, **no Google Play**.

```powershell
# Emulator named "lp3" (create once in Android Studio Device Manager if missing)
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd lp3

cd C:\FoldLight
.\gradlew :tool:installDebug
```

## Quick start (Android Studio)

1. File → Open → `C:\FoldLight`
2. Wait for Gradle sync
3. Run configuration: **`:tool`** (or `:examples:fold` if listed — same code via `:tool`)
4. Select device **lp3**
5. Run ▶

## Submit to Tool Library

See [docs/DASHBOARD-SUBMISSION.md](./docs/DASHBOARD-SUBMISSION.md). Short version:

1. Push to public GitHub
2. Dashboard → Developer Account → Submit New Tool
3. Submit build with a git commit / `main`
4. Wait for Light review
