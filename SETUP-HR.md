# FOLD — Light Phone putni planer

Offline planer putovanja za Light Phone III. Baziran na [Light SDK](https://github.com/lightphone/light-sdk).

**Tvoja Expo app** ostaje u `C:\TravelPlanner` — ova mapa je potpuno odvojena.

---

## Što je u projektu

| Dio | Svrha |
|-----|--------|
| `tool/` | **Tvoj FOLD** — putovanja, karte (QR), copy adresa |
| `examples/authenticator/` | Lightov primjer (za učenje / usporedbu na emulatoru) |
| `sdk/` | Light SDK biblioteke (potrebne za build) |
| `docs/system_app/` | Upute za LightOS emulator |

Uklonjeno iz projekta: weather primjer, ui-demo, default tool scaffold — da ostane čisto za FOLD.

---

## FOLD značajke

- Putovanja i događaji po danima (let, hotel, bus, vlak, taxi, auto, mjesto)
- **What to pack** lista
- **QR karte** — skeniraj boarding pass / ulaznicu, prikaži na check-inu (do 3 po događaju)
- **Copy** adrese/naslova → paste u Directory ili Directions
- Sve **lokalno** na telefonu (bez clouda i dijeljenja)

---

# KORAK 0 — Kad instaliraš Android Studio

### A) Emulator (jednom)

1. Android Studio → **Device Manager** → **Create Device**
2. Postavke:
   - API **34** (Android 14)
   - **Bez Google Play** (AOSP image)
   - Ekran **1080 × 1240**, ~3.92"
3. Pokreni emulator

### B) `local.properties`

U korijenu `C:\FoldLight` napravi datoteku `local.properties`:

```properties
sdk.dir=C\:\\Users\\TVOJE_IME\\AppData\\Local\\Android\\Sdk
gpr.user=TVOJ_GITHUB_USERNAME
gpr.key=TVOJ_GITHUB_TOKEN
```

GitHub token: Settings → Developer settings → Personal access tokens → `read:packages`.

### C) Otvori projekt

File → Open → `C:\FoldLight` → pričekaj Gradle sync.

---

## Kako vidjeti Light primjere na emulatoru

### Authenticator (u ovom projektu)

1. Gore desno: run configuration **`:examples:authenticator`**
2. Odaberi emulator → **Run** (zeleni play)
3. Na emulatoru se otvara Authenticator — skenira QR za 2FA

Ovo je dobar uzor za QR skeniranje (isto kao FOLD ticket scan).

### Weather (nije u ovom projektu)

Weather primjer smo maknuli da projekt ostane čist. Ako ga želiš vidjeti:

1. U drugom folderu: `git clone https://github.com/lightphone/light-sdk.git C:\LightSDK-examples`
2. Otvori taj projekt u Android Studio
3. Run **`:examples:weather`**
4. Unesi grad (npr. “Zagreb”) — koristi **Open-Meteo**, ne yr.no

Kad upoznaš izgled, vrati se na `C:\FoldLight` i radi na FOLD-u.

### LightOS emulator (opcionalno, kasnije)

Za puniji Light doživljaj: `docs/system_app/README.md`  
Za FOLD na emulatoru u `tool/lighttool.toml` stavi:

```toml
serverPackage = "com.thelightphone.sdk.emulator"
```

---

## KORAK 1 — Pokreni svoj FOLD

1. Run configuration: **`:examples:fold`**
2. Run na emulatoru
3. Probaj:
   - New trip → dodaj hotel s adresom
   - Dugi pritisak na adresu → COPY
   - Dodaj let → Scan ticket QR → View ticket

---

## Build iz terminala

```powershell
cd C:\FoldLight
.\gradlew :examples:fold:installDebug
```

---

## Sljedeći koraci (Light Phone)

1. Dotjerati UI ako treba
2. Javni GitHub repo
3. Prijava na [Light developer program](https://www.lightphonethings.com/)
