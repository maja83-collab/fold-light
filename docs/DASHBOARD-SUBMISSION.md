# How to submit Fold Light (Dashboard)

Light now builds tools from the `:tool` module in your public repo.
In this project, `:tool` points at `examples/fold` (same app).

## Before you submit

1. Push the latest code to **https://github.com/maja83-collab/fold-light** (`main`).
2. Confirm `examples/fold/lighttool.toml` has:
   - unique `id` (currently `com.thelightphone.fold`)
   - bumped `versionCode` for every new build
3. At least one screenshot of the app on LP3 / emulator (Dashboard requires an image).

## Dashboard steps

1. Open **https://dashboard.thelightphone.com/**
2. **Settings → Account → Developer Account** (enable Developer Mode if needed)
3. **Manage Custom Tools → Submit New Tool**
4. Paste your GitHub repo: `https://github.com/maja83-collab/fold-light`
5. Package name is read from `tool` / `lighttool.toml` on the default branch — cannot change later
6. Add at least one image
7. Open the tool detail page → **Submit build**
8. Enter git ref (commit SHA or `main`) + short changelog
9. Wait for build → download APK if successful → status becomes “pending approval”
10. Light emails feedback if needed

Support: **tools@thelightphone.com**

## Local preview (no Light Phone)

```powershell
# 1) Start LP3-sized emulator (1080×1240) — already created as "lp3"
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd lp3

# 2) (Once, if GitHub Packages token fails) publish keyboard locally:
cd C:\light-keyboard
git checkout v0.0.19
.\gradlew.bat ":ui:publishToMavenLocal" "-PprojectVersion=0.0.19"

# 3) Install Fold
cd C:\FoldLight
.\gradlew.bat :tool:installDebug
```

Or in Android Studio: open `C:\FoldLight` → run configuration **`:tool`** → device **lp3**.

**Camera note:** LP3 front camera sits in the **top bezel**, not a punch-hole on the screen — UI does not need a cutout inset.
