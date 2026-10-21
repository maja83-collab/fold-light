# Fold Light — nastavi sutra (posao)

Ovaj file je sažetak gdje smo stali i što dalje. Kopija projekta je i na USB-u (`E:\FoldLight`).

## Što je napravljeno

- Fold Light radi kao `:tool` modul (za Light Dashboard build)
- Emulator **lp3** (1080×1240) za preview bez LP3 uređaja
- Unutar tripa: **ADD EVENT**, **THIS WEEK**, **INFO** (settings je gore desno — zupčanik)
- Datumi se slažu kronološki (dan po dan, unutar dana po vremenu)
- Edit eventa (posebno hotel check-in/out) puni i sprema polja kako treba
- Datumi: `YYYY-MM-DD`, `YYYY-MM-DD HH:MM`, ili `DD.MM.YYYY`

## Sutra ujutro na poslu

### 1. Otvori projekt

**Ako imaš USB:**
1. Ukopčaj USB
2. Kopiraj `E:\FoldLight` na PC (npr. `C:\FoldLight`) — ili radi direktno s USB-a
3. Otvori folder u **Cursor** ili **Android Studio**

**Ako imaš GitHub (`maja83-collab/fold-light`):**
```powershell
cd C:\
git clone https://github.com/maja83-collab/fold-light.git FoldLight
cd FoldLight
git pull
```
Ako folder već postoji: `cd C:\FoldLight` pa `git pull`.

### 2. Cursor chat / ove upute

- **Ovaj file (`docs/NASTAVI-SUTRA.md`) i kod** — da, na USB-u i u folderu.
- **Cursor chat (ova konverzacija)** — **nije na USB-u**. Chat je u Cursoru / računu.
  - Na poslovnom PC-u: prijavi se istim **Cursor** računom → Agent / Chat history bi trebao pokazati stare razgovore (ako sync radi).
  - Ako chat ne vidiš: otvori ovaj file + `docs/DASHBOARD-SUBMISSION.md` — dovoljno za nastavak.

### 3. Pokreni preview

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd lp3
cd C:\FoldLight
.\gradlew.bat :tool:installDebug
```

Ili u Android Studiju: run **`:tool`** na device **lp3**.

### 4. Screenshot + Dashboard submission

1. Snimi screenshot (kamera na emulatoru) → Desktop
2. Push na GitHub ako treba
3. [dashboard.thelightphone.com](https://dashboard.thelightphone.com/) → Developer → Submit New Tool
4. Repo: `https://github.com/maja83-collab/fold-light`
5. Upload slike → Submit build (`main` + kratki changelog)

Detalji: `docs/DASHBOARD-SUBMISSION.md`

## Važne staze

| Što | Gdje |
|-----|------|
| App kod | `tool/` |
| Tool config | `tool/lighttool.toml` |
| Gradle `:tool` | `settings.gradle.kts` |
| Submission | `docs/DASHBOARD-SUBMISSION.md` |

## Još nije gotovo (opcionalno)

- Ikone / branding kad budeš htjela
- Push najnovijih UI izmjena na GitHub ako nisi pushala s kuće
- Screenshot + Submit build na Light Dashboard
