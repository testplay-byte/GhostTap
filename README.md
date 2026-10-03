# 👻 GhostTap

> **A pocket-sized ESP32-C3 Wi-Fi security & analysis tool** — firmware +
> Android controller, connected over BLE.

GhostTap pairs an ESP32-C3 running custom ESP-IDF firmware with a Jetpack
Compose Android app. The phone is the command center: scan networks, launch
authorized attacks, capture handshakes to .pcap/.hccapx, and have fun with a
clock, music visualizer, snake game and AI-generated OLED animations.

```
┌──────────────────┐    BLE     ┌──────────────────┐
│  Android app     │ ◄────────► │  ESP32-C3        │
│  dashboard + AI  │            │  attacks + OLED  │
└──────────────────┘            └──────────────────┘
```

---

## ⚠️ Authorized use only

Deauthentication attacks, beacon flooding and handshake capture **disrupt
networks**. Use GhostTap **only** on networks you own or have explicit
written permission to test. It is built for security education and auditing
your own gear.

---

## 📦 What's in the box

| Folder | What it is |
|---|---|
| [`firmware/`](firmware/) | ESP-IDF project for ESP32-C3: deauth/DOS, handshake + PMKID capture, beacon swarm, pcap/hccapx serialization, SSD1306 OLED UI, snake game |
| [`app/`](app/) | Android controller (`com.ghosttap.app`): BLE command center, attack dashboard, capture file manager, Gemini-powered animation generator |
| [`docs/`](docs/) | Architecture, BLE protocol reference, build & usage guides, component deep-dives |
| [`assets/`](assets/) | Images and binary assets |

## 🚀 Quick start

1. **Flash the firmware** — see [`docs/guides/BUILD.md`](docs/guides/BUILD.md)
   (ESP-IDF, `idf.py build flash`; OLED on SDA=GPIO5/SCL=GPIO6).
2. **Install the app** — build the APK (`./gradlew assembleDebug`) or grab one
   from Releases once CI is live.
3. **Pair** — the device advertises as **GhostTap**; connect from the app's
   Scan screen.
4. **Play** — Control tab for clock/snake/AI animations, Hacker tab for
   scanning and (authorized) attacks. Full walkthrough:
   [`docs/guides/USAGE.md`](docs/guides/USAGE.md).

## 📚 Documentation

| Doc | Contents |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | System map, both halves, data flows |
| [`docs/reference/BLE-PROTOCOL.md`](docs/reference/BLE-PROTOCOL.md) | Every command and notification on the wire |
| [`docs/reference/FIRMWARE-COMPONENTS.md`](docs/reference/FIRMWARE-COMPONENTS.md) | Each firmware component explained |
| [`docs/reference/APP-GUIDE.md`](docs/reference/APP-GUIDE.md) | App structure and flows |
| [`docs/guides/BUILD.md`](docs/guides/BUILD.md) | Building firmware + app, first pairing, troubleshooting |
| [`docs/guides/USAGE.md`](docs/guides/USAGE.md) | Operating the device day-to-day |
| [`docs/HISTORY.md`](docs/HISTORY.md) | Origin, migration changelog, roadmap |

## 🛠️ Tech stack

- **Firmware:** C / ESP-IDF v5.x, target ESP32-C3 (RISC-V), FreeRTOS
- **App:** Kotlin, Jetpack Compose, Material 3, OkHttp + Gson (Gemini API)
- **Link:** BLE GATT (service `4faf…`, characteristic `beb5…`)

## 🗺️ Status

- ✅ Feature-complete two-part tool (firmware + app), refined layout & docs
- ⏳ GitHub Actions CI (firmware + APK builds) — added in the GitHub phase
- ⏳ Repo publish — pending repo details

## 📄 License

Not decided yet (see `docs/HISTORY.md` roadmap).
