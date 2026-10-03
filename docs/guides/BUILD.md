# 🛠️ GhostTap — Build Guide

> How to build both halves of GhostTap. Per the project rule, **heavy builds
> run on GitHub Actions, not locally** — but this guide works anywhere
> (local machine, CI, or a friend's Linux box).

---

## 1. Firmware (ESP-IDF)

**Requirements:** ESP-IDF v5.x installed and exported (`idf.py` on PATH),
ESP32-C3 board, USB cable.

```bash
cd firmware

# 1. Configure target (first time only)
idf.py set-target esp32c3

# 2. (Optional) adjust defaults
idf.py menuconfig

# 3. Build
idf.py build

# 4. Flash + monitor (replace port)
idf.py -p COM5 flash monitor
```

**Hardware defaults:** OLED SSD1306 on I2C — SDA `GPIO5`, SCL `GPIO6`.
Change in `firmware/main/main.c` if your wiring differs.

**Output:** `firmware/build/ghosttap-firmware.bin`

### In CI (GitHub Actions)

A workflow builds the firmware on every push touching `firmware/**` and
uploads the `.bin` as an artifact. See `.github/workflows/` once the repo is
live (added in the GitHub phase).

---

## 2. Android app

**Requirements:** JDK 17, Android SDK (compileSdk 36, minSdk 31). Android
Studio recommended, but the CLI works too.

```bash
cd app

# Debug APK
./gradlew assembleDebug

# Release APK (unsigned unless signing is configured)
./gradlew assembleRelease
```

**Outputs:** `app/app/build/outputs/apk/debug|release/`

> ⚠️ `local.properties` (with your SDK path) is machine-specific and
> git-ignored — Gradle creates it or Android Studio supplies it.

### In CI (GitHub Actions)

The Android workflow builds both APKs on pushes touching `app/**` and
uploads them as artifacts; releases attach them to a rolling tag.

---

## 3. First run — pairing the two halves

1. **Flash the firmware** onto the ESP32-C3. It boots into a boot animation,
   then advertises over BLE as **GhostTap**.
2. **Install the app**, grant Bluetooth + Location permissions.
3. Open the app → **Scan** → pick **GhostTap** from the list.
4. Once connected the OLED shows `CONNECTED!` and hands control to app logic.
5. From the app: *Control* tab = clock / music viz / snake / drawing / AI
   animations. *Hacker* tab = scanner + attacks + capture files.

---

## 4. Troubleshooting

| Symptom | Fix |
|---|---|
| Device not in scan list | Make sure the OLED shows the idle animation (advertising active); name filter now matches `GhostTap` |
| BLE connects then drops | Long-blocking OLED/Wi-Fi ops can starve BLE — keep attack durations sane |
| `ESP_ERR_WIFI_MODE` crash after attack | Cleanup order bug — MAC restore must happen before AP teardown (fixed in this codebase; check `wifi_controller`) |
| Handshake never completes | Trigger deauth to force reconnection; passive method needs a client to roam on its own |
| Gemini errors | API key is entered at runtime (not stored in code); check network + key validity |
