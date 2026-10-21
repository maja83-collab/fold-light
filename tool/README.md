# Fold Light

Offline trip planner for **Light Phone III**, built with the [Light SDK](https://github.com/lightphone/light-sdk).

Plan flights, hotels, transport, and places — day by day, entirely on your phone. No cloud, no accounts, no sharing.

## Features

- **Trips** — create, rename, and delete trips
- **Events by day** — flight, hotel, bus, train, taxi, car rental, place
- **Day view** — swipe between days; compact list of events per day
- **INFO** — full details for a day (time, route, address, notes, QR tickets)
- **This week** — overview of all days in a trip
- **What to pack** — editable packing list per trip
- **QR tickets** — scan or paste boarding passes / tickets (up to 3 per event); view at check-in
- **Copy** — copy address, route, or time to the clipboard (tap a field → **COPY**)

## Privacy

- All data stays **on device** (local JSON via DataStore)
- No network access required
- No analytics or third-party SDKs in the tool module

## Permissions

| Permission | Why |
|------------|-----|
| `CAMERA` | Scan QR codes on tickets |

## Tool metadata

| Field | Value |
|-------|--------|
| Tool ID | `com.thelightphone.fold` |
| Label | Fold Light |
| Version | 1.0.15 (versionCode 16) |

## Build & install (development)

From the repository root:

```bash
# Copy and fill in local.properties (see local.properties.example)
# Light builds :tool — this repo maps :tool → examples/fold
./gradlew :tool:installDebug
```

Requires a Light Phone III with USB debugging, or the Light SDK emulator.

**Developer Mode** on the [Light Dashboard](https://www.lightphonethings.com/) is needed to sideload and run custom SDK tools in the toolbox.

## Known limitations

- **Clipboard / Directory / Directions:** Copy places text on the Android clipboard and paste works in Android-layer apps. Paste into LightOS **Directory** and **Directions** search fields may not work from third-party SDK tools — this appears to be a platform limitation, not a Fold Light bug. See [clipboard issue draft](../../docs/GITHUB-ISSUE-clipboard-paste.md).

## Intentionally excluded (Light ethos)

- Cloud sync or backup
- Real-time sharing with other users
- Social features, feeds, or messaging
- In-app browser or maps

## Screens (overview)

1. **Home** — trip list
2. **Trip / day** — date, event titles, settings · this week · info
3. **INFO** — full event details and actions (edit, delete, QR)
4. **Settings** — rename trip, packing list, delete trip, add event

## Tool Library submission

This tool is intended for the Light **Tool Library**. Source is open; builds for the library are signed by Light from a public commit.

**Author:** Victor (Fold Light)  
**SDK:** [lightphone/light-sdk](https://github.com/lightphone/light-sdk)

## License

Same as the parent Light SDK repository (see root `LICENSE`).
