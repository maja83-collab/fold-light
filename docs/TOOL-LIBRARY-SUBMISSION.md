# Tool Library submission checklist — Fold Light

Step-by-step for Victor. Tick items as you go.

## 1. Code ready

- [ ] App tested on real LP3 (trips, events, QR, copy, keyboard)
- [ ] `examples/fold/lighttool.toml` — correct `id`, `label`, `versionCode`, `versionName`
- [ ] Bump `versionCode` for each new submission build
- [ ] `./gradlew :examples:fold:assembleRelease` succeeds

## 2. Public GitHub repository

- [ ] Create **public** repo (fork of light-sdk with `examples/fold`, or dedicated repo)
- [ ] Remove secrets: `local.properties`, tokens, keys (use `local.properties.example` only)
- [ ] Root README links to `examples/fold/README.md`
- [ ] Commit hash you want Light to build from is pushed to `main`

## 3. Developer program

- [ ] Sign up: [lightphonethings.com](https://www.lightphonethings.com/)
- [ ] Join GitHub / Discord / channel when invited
- [ ] **Developer Mode** enabled on Dashboard for your LP3

## 4. Personal toolbox (your phone only)

```powershell
cd C:\FoldLight
.\gradlew :examples:fold:installDebug
```

- [ ] Fold Light appears in toolbox and runs on LP3

This is **not** Tool Library — only your dev install.

## 5. Submit to Tool Library

Light reviews manually; exact form may vary. Prepare:

- [ ] Public repo URL
- [ ] Commit SHA to build
- [ ] Short description (from `examples/fold/README.md`)
- [ ] Screenshots or short screen recording on LP3
- [ ] Permissions justification (CAMERA for QR)

**Where to send:** developer program channel or new GitHub issue on [lightphone/light-sdk](https://github.com/lightphone/light-sdk):

**Issue title:**
```
Tool Library submission: Fold Light — offline trip planner
```

**Issue body template:**

```markdown
## Tool Library submission

**Name:** Fold Light  
**Tool ID:** `com.thelightphone.fold`  
**Repository:** [YOUR_GITHUB_URL]  
**Commit:** [FULL_COMMIT_SHA]  
**Version:** 1.0.14 (versionCode 15)

### Description

Offline trip planner for Light Phone III. Trips, day-by-day events (flights, hotels, transport, places), packing list, QR ticket storage. All data local; no network.

### Permissions

- `CAMERA` — scan boarding pass / ticket QR codes

### Testing

Tested on Light Phone III hardware. Developer Mode sideload build installs and runs from toolbox.

### Docs

Tool README: `examples/fold/README.md`

### Known platform note

Copy-to-clipboard works; paste into Directory/Directions from SDK tools does not (separate issue filed).
```

## 6. Clipboard / Directory (parallel)

- [ ] File issue using [GITHUB-ISSUE-clipboard-paste.md](./GITHUB-ISSUE-clipboard-paste.md)
- [ ] Add your repo URL in the issue body before submitting

## Notes

| Question | Answer |
|----------|--------|
| Can I add Fold to toolbox myself? | Yes — Developer Mode + `installDebug` |
| Can I add to Tool Library myself? | No — Light builds, signs, and publishes |
| Is broken Directory paste a blocker? | Unlikely — document as known platform limit; file separate issue |
| Package ID `com.thelightphone.fold` | Ask Light if you need your own namespace (e.g. `com.yourname.fold`) |
