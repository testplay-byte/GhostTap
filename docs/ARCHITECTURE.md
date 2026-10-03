# 🏗️ GhostTap — Architecture

> GhostTap is a two-part ESP32-C3 Wi-Fi security & analysis tool:
> a **firmware** running on the ESP32-C3 and an **Android controller app**,
> talking to each other over Bluetooth Low Energy (BLE).

---

## 1. System overview

```
┌─────────────────────┐         BLE GATT          ┌──────────────────────────┐
│  Android app        │ ◄───────────────────────► │  ESP32-C3 firmware       │
│  (Kotlin/Compose)   │   commands + notifications│  (ESP-IDF, C)            │
└─────────────────────┘                           └──────────────────────────┘
        │                                                 │
        │  • dashboard / scanner / attack UI              │  • Wi-Fi frame injection
        │  • AI animation generator (Gemini)              │  • promiscuous capture
        │  • animation script engine + preview            │  • OLED rendering
        │  • capture file manager (.pcap/.hccapx)         │  • snake game / clock / viz
```

- **App → device:** text commands (`SCAN:START`, `ATTACK:DOS`, `LINE:x1:y1:x2:y2`, …)
- **Device → app:** text notifications (`SCAN_RESULT:…`, `HS_FRAME:…`, `LOG:…`)

---

## 2. Firmware layout (`firmware/`)

ESP-IDF project (target: `esp32c3`). Entry point `main/main.c`:

1. NVS init → 2. event loop → 3. OLED init + boot animation →
4. app_logic init → 5. BLE init (advertises as **GhostTap**) →
6. idle animation until a phone connects → 7. hand control to `app_logic`.

| Component | Role |
|---|---|
| `ble_comm` | GATT server + command/notification transport |
| `app_logic` | Routes all 31 BLE commands; owns clock, music-viz, snake, drawing modes |
| `attack_manager` | Attack state machine (READY/RUNNING/FINISHED) + per-attack modules |
| `attack_manager/attack_dos` | Deauth (Rogue AP / Broadcast / Combined) |
| `attack_manager/attack_handshake` | EAPOL capture (M1–M4), serializes to PCAP |
| `attack_manager/attack_pmkid` | PMKID extraction |
| `attack_manager/attack_beacon` | Beacon swarm (fake networks) |
| `wifi_controller` | STA/AP modes, sniffer, AP scanner, MAC spoof/restore |
| `frame_analyzer` | Promiscuous frame filter; detects EAPOL/PMKID frames |
| `pcap_serializer` | In-RAM .pcap writer |
| `hccapx_serializer` | .hccapx writer for hashcat-style cracking tools |
| `wsl_bypasser` | Deauth frame injection |
| `oled_display` | SSD1306 driver: scrolling lists, animated countdown, drawing ops |
| `snake_game` | 1x1 and 2x2 snake with BFS AI |

**Attack flow (handshake example):** `attack_handshake_start()` → sniffer mode +
frame filters for target BSSID → EAPOL frames parsed/serialized →
`HS_FRAME:` notifications to the app → optional deauth via `wsl_bypasser`
forces clients to reconnect.

**Safety ordering (important):** on cleanup, restore the AP MAC **first**, then
stop the AP interface — the reverse order crashes with `ESP_ERR_WIFI_MODE`.

---

## 3. App layout (`app/`)

Single Gradle module `app/` (Kotlin, Jetpack Compose, Material 3, min SDK 31).

| Path | Role |
|---|---|
| `MainActivity.kt` | Navigation host |
| `BleManager.kt` | BLE singleton: scan, connect, command queue (no flooding), notification routing |
| `ui/ScanScreen.kt` | Device discovery |
| `ui/ControlScreen.kt` | Clock, music viz, snake, drawing, AI animations |
| `ui/HackerModeScreen.kt` | Attack dashboard: scanner, attacks, files |
| `ui/AdvHandshakeScreen.kt` | Handshake capture detail view |
| `data/GeminiService.kt` | Gemini API client for animation generation |
| `logic/AnimationScriptEngine.kt` | Local interpreter for C-like draw scripts; previews on phone before sending |

**AI animation flow:** prompt → Gemini returns C-style code (`drawCircle(...)`)
→ `AnimationScriptEngine` evaluates it locally → live preview renders on a
Canvas → if "Live Preview" is on, each call is streamed to the device as a BLE
drawing command (`CIRCLE:x:y:r`).

---

## 4. BLE protocol

- **Service UUID:** `00004faf-0000-1000-8000-00805f9b34fb`
- **Characteristic:** `beb5483e-36e1-4688-b7f5-ea07361b26a8` (read/write/notify)
- Device advertises as **GhostTap**.

Full command & notification reference: see [`BLE-PROTOCOL.md`](reference/BLE-PROTOCOL.md).
