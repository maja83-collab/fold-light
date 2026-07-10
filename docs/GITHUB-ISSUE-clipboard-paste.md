# GitHub issue draft — clipboard paste in Directory / Directions

Open at: **https://github.com/lightphone/light-sdk/issues/new**

---

## Title

```
Clipboard from SDK tools not available for paste in Directory / Directions
```

---

## Body (copy below)

```markdown
### Summary

Third-party SDK tools can copy text to the clipboard, and that text pastes correctly in **Android-layer** apps on Light Phone III. The same clipboard content does **not** paste into LightOS **Directory** or **Directions** search fields.

This blocks a common travel workflow: copy a hotel address or place name from a trip planner tool → paste into Directory or Directions.

### Tool / app

- **Tool:** Fold Light (offline trip planner)
- **Package:** `com.thelightphone.fold`
- **Repo:** [add your public GitHub URL here]

### Device

- Light Phone III
- LightOS (production, not emulator)

### Steps to reproduce

1. Open any SDK-built tool that copies text (e.g. Fold Light → trip event → tap address → **COPY**).
2. Confirm paste works in an Android-layer app (e.g. browser or notes on the Android layer).
3. Open LightOS **Directory** or **Directions**.
4. Focus the search field and attempt paste (long-press → paste, or keyboard paste).

### Expected behavior

Pasted text from the system/tool clipboard appears in the Directory or Directions search field, same as copy/paste within LightOS messaging (per [Keyboard & Texting](https://support.thelightphone.com/hc/en-us/articles/360030790152-Keyboard-Texting) docs).

### Actual behavior

Paste does not insert the copied text. The clipboard content is valid (verified via Android-layer paste).

### Why this matters for SDK tools

Trip and navigation tools often document “copy address → paste in Directory/Directions” as the integration path. There is no `LightServiceMethod` for opening Directory/Directions with a query, and `geo:` intents do not resolve on LP3.

A shared clipboard between SDK tools and LightOS built-in tools would unblock this without custom intents per tool.

### Suggested directions

- Document expected clipboard behavior for tool developers, **or**
- Add a supported API (e.g. set shared LightOS clipboard, or intent to open Directory/Directions with initial search text)

Happy to help test any fix on LP3 hardware.
```

---

## Optional shorter version

```markdown
**Fold Light** (`com.thelightphone.fold`) copies addresses via the SDK clipboard API. Paste works on the Android layer but **not** in Directory or Directions on LP3. Request: tool clipboard should be readable in LightOS Directory/Directions, or provide an official integration API.
```
