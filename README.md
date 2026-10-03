# 👻 GhostTap

> **A pocket-sized ESP32-C3 Wi-Fi security & analysis tool** — firmware +
> Android controller, connected over BLE.

GhostTap pairs an ESP32-C3 running custom ESP-IDF firmware with a Jetpack
Compose Android app. The phone is the command center: scan networks, launch
authorized attacks, capture handshakes to .pcap/.hccapx — and have fun with a
clock, music visualizer, drawing canvas and AI-generated OLED animations.

```
┌──────────────────┐    BLE     ┌──────────────────┐
│  Android app     │ ◄────────► │  ESP32-C3        │
│  dashboard + AI  │            │  attacks + OLED  │
└──────────────────┘            └──────────────────┘
```

## 🖼️ Screens & hardware

| ✏️ Draw | 📝 Text | 🎵 Music Viz | 🔌 Hardware |
|---|---|---|---|
| <img src="https://github.com/user-attachments/assets/189ab1d9-48c4-4bf0-8555-d0a3f908bec5" width="180"> | <img src="https://github.com/user-attachments/assets/bc7bd3b1-a5cb-4d1d-ba30-d6963b0b6dd9" width="180"> | <img src="https://github.com/user-attachments/assets/c1dce5f3-f780-48e9-b0b0-851c0f3c3d2f" width="180"> | <img src="https://github.com/user-attachments/assets/ad62578a-57f6-4b7e-af05-d429533985c2" width="180"> |
| Pixel canvas with stroke size, shapes & eraser — mirrored live on the OLED | Giant scrolling text with size, animation & effects | Audio-reactive visualizers: Classic Bars, Fluid Wave, Peak Meter | ESP32-C3 + SSD1306 OLED over I2C (SDA 5, SCL 6) |

The app keeps a live **OLED simulator** at the top of every screen, so what
you see is exactly what the device shows — plus **AI mode**: type a prompt,
Gemini writes the animation, preview it on the phone, then stream it to the
device.

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
2. **Install the app** — grab the signed
   [app-release.apk](https://github.com/testplay-byte/GhostTap/releases/download/latest/app-release.apk)
   from Releases, or build it with CI (`./gradlew assembleDebug` runs on
   GitHub Actions).
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
- ✅ CI: firmware builds + signed APKs on every push ([Releases](https://github.com/testplay-byte/GhostTap/releases))
- ⏳ Backlog: ViewModels split, scoped-storage tightening

## 📄 License

Not decided yet (see `docs/HISTORY.md` roadmap).

---

*Screenshots courtesy of the original
[ESPOLED_APP](https://github.com/Confused-Creature-180/ESPOLED_APP) preview.*
