# 📱 GhostTap — Android App Guide

> Structure and flows of the controller app. Build steps live in
> [guides/BUILD.md](BUILD.md); wire format in
> [reference/BLE-PROTOCOL.md](BLE-PROTOCOL.md).

Package: `com.ghosttap.app` · Kotlin · Jetpack Compose · Material 3 ·
minSdk 31 / targetSdk 36 · Gemini via OkHttp + Gson.

---

## Module layout

```
app/app/src/main/java/com/ghosttap/app/
├── MainActivity.kt               # Navigation host
├── BleManager.kt                 # BLE singleton: scan/connect/queue/notifications
├── data/
│   └── GeminiService.kt          # Gemini API client
├── logic/
│   └── AnimationScriptEngine.kt  # Script interpreter
└── ui/
    ├── ScanScreen.kt             # Device discovery
    ├── AdvHandshakeScreen.kt     # Handshake capture detail
    ├── theme/                    # Color / Theme / Type
    ├── Control*.kt               # Normal mode, split by concern:
    │   ├── ControlScreen.kt      #   main screen composable
    │   ├── ControlSettings.kt    #   DeviceCommand sealed class, settings, slider
    │   ├── ControlCanvas.kt      #   drawing canvas + bitmap rendering
    │   ├── ControlSimulator.kt   #   OLED simulator preview
    │   ├── ControlWidgets.kt     #   TextControl and small widgets
    │   └── ControlExtras.kt      #   extras screen, clock/viz/snake items
    └── Hacker*.kt                # Hacker mode, split by concern:
        ├── HackerModeScreen.kt   #   screen shell + temperature bubble + settings
        ├── HackerTheme.kt        #   hacker palette colors
        ├── HackerDashboard.kt    #   attack selection dashboard
        ├── HackerHandshake.kt    #   handshake running + results screens
        ├── HackerScanner.kt      #   network scanner + detail cards
        ├── HackerFiles.kt        #   capture file manager + beacon SSID settings
        └── HackerCommon.kt       #   shared cards, signal bars
```

## Key singletons & flows

### BleManager

- Scans for devices named **GhostTap** (or ESP32) carrying our service UUID
- Connects, discovers services, subscribes to notifications
- **Command queue:** writes are serialized so BLE never floods
- Parses notifications by prefix (`SCAN_RESULT:`, `HS_FRAME:`, `HS_STATUS:`,
  `TEMP:`, `FILE_*`, `LOG:`) into StateFlows the UI observes

### Hacker Mode flow

1. Screen opens → sends `MODE:ADVANCED`
2. Scan → `SCAN_RESULT:` items fill the network list (monospace BSSIDs)
3. Target selected → attack chosen (type → method → duration)
4. `ATTACK:<TYPE>:<METHOD>:<INDEX>:<DURATION>` sent
5. Live progress from `HS_STATUS:`; frames accumulate into a local .pcap
6. Files tab lists/inspects/saves captures

### AI animation flow

1. Prompt in Control screen → `GeminiService` calls the Gemini API
   (key entered at runtime, never stored in code)
2. Response is C-style draw code → `AnimationScriptEngine` tokenizes and
   evaluates it (variables `t`, `x`, `y`; functions map to Canvas calls)
3. Local preview renders on a Canvas
4. With **Live Preview** on, each call is mirrored to the device as
   `CIRCLE:x:y:r`-style BLE commands

### Navigation

`MainActivity` hosts routes: Scan → Control / HackerMode (→ AdvHandshake).

## Permissions (AndroidManifest)

- `BLUETOOTH_SCAN` (neverForLocation) + `BLUETOOTH_CONNECT` (API 31+)
- Legacy `BLUETOOTH`/`BLUETOOTH_ADMIN` capped at API 30
- Fine/coarse location (legacy scan visibility)
- Internet (Gemini)
- Storage for capture-file save/export (scoped-storage aware)

## Known refinement targets (backlog)

- `ControlScreen.kt` (4,020 lines) and `HackerModeScreen.kt` (2,685 lines)
  are candidates for splitting into smaller composables/ViewModels
- No ViewModels yet — state lives in composables + BleManager flows
- `MANAGE_EXTERNAL_STORAGE` is broad; tighten to scoped storage later
