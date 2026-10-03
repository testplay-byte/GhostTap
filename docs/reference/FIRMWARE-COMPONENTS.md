# 🧭 GhostTap — Firmware Component Guide

> Deep-dive into every firmware component: what it owns, what it exposes,
> and how it talks to its neighbors. Read ARCHITECTURE.md first for the map.

Source of truth: `firmware/components/` — line counts are approximate.

---

## app_logic (~1,590 lines)

The router and mode owner. Parses every incoming BLE command string
(`strncmp` dispatch — 31 command prefixes) and fans out to the right
component.

- Modes: NORMAL (clock/animations) ↔ ADVANCED (attacks)
- Owns: clock rendering, music visualizer, snake integration, drawing
  command execution, script interpreter (`clear()`, `drawLine(`,
  `drawRect(`, `drawCircle(`, `drawPixel(`, simple var declarations)
- Sends notifications back via ble_comm

**Extend it:** add a new `strncmp` branch, call your component, done.

## attack_manager (~1,180 lines total)

State machine (`attack.c`: READY → RUNNING → FINISHED) plus one module per
attack:

| Module | Role |
|---|---|
| `attack_dos.c` | Deauth: Rogue AP / Broadcast / Combined methods |
| `attack_handshake.c` | Sniffer + EAPOL capture (M1–M4), serializes frames, optional deauth assist |
| `attack_pmkid.c` | PMKID extraction from association frames |
| `attack_beacon.c` | Beacon swarm with configured SSID list |
| `attack_method.c` | Method-ID mapping shared by the v2 command format |

**Add an attack:** create `attack_new.c/h` with `start()/stop()/timeout()`,
register in `attack.c`'s switch, add a method ID in `attack_method.c`.

## wifi_controller (~640 lines)

Hardware abstraction over esp_wifi:

- `wifi_controller.c` — STA/AP mode management, channel switching,
  MAC spoofing (`wifictl_set_ap_mac`) and restore
  (`wifictl_restore_ap_mac` — checks AP-active state first)
- `ap_scanner.c` — blocking AP scan producing `SCAN_RESULT:` notifications
- `sniffer.c` — promiscuous mode + channel hopping for capture
- `Kconfig` — configurable options

⚠️ Cleanup order matters: restore MAC **before** stopping the AP interface,
or you get `ESP_ERR_WIFI_MODE` crashes.

## frame_analyzer (~340 lines)

- `frame_analyzer.c` — promiscuous callback; filters by BSSID/type,
  dispatches interesting frames
- `frame_analyzer_parser.c` — 802.11 header parsing; recognizes EAPOL
  key frames and PMKID-carrying frames
- Feeds `attack_handshake` / `attack_pmkid` via callbacks

## pcap_serializer (~180 lines)

Writes a valid .pcap stream into RAM (global header + per-packet records).
Frames arrive already parsed; serializer handles capture headers and
buffering. `SAVE_PCAP` triggers persistence/transfer.

## hccapx_serializer (~310 lines)

Converts captured handshake data into the **hccapx** binary format
(hashcat). Consumes the same EAPOL data the pcap serializer sees.

## wsl_bypasser (~210 lines)

Builds and injects raw deauth frames (the name is inherited from the
upstream project this firmware was inspired by). Called by attack modules
to force clients off an AP.

## ble_comm (~470 lines)

GATT server: advertises as **GhostTap**, exposes service
`4faf…` / characteristic `beb5…` (read/write/notify). Receives commands
(upstream to app_logic) and sends notifications downstream. Connection
state is queryable (`ble_comm_is_connected()`).

## oled_display (~1,800 lines + fonts)

SSD1306 over I2C (SDA GPIO5, SCL GPIO6). Thread-safe (recursive mutex).

Notable features:

- `oled_draw_string_scrolling` — horizontal scroll for strings wider than 72 px
- `oled_show_ap_list` — decorated AP browser (header bars, index, side markers)
- `oled_draw_large_digit` / `oled_show_attack_status` — 3-digit countdown with
  vertical slide + pixel-fade transitions
- Boot screen + idle orbital animation (`oled_anim_circles`)
- Drawing primitives used by app_logic's canvas commands

## snake_game (~590 lines)

Two playable variants sharing `snake_common.c`:

- `snake_1x1` — classic cells
- `snake_2x2` — larger blocks
- BFS pathfinding AI mode (device plays itself)

---

## Startup sequence (main.c)

```
NVS init → event loop → OLED init → boot animation (~50 frames)
→ app_logic_init → BLE init ("GhostTap") → idle orbit animation
→ [BLE connected] → "CONNECTED!" splash → app_logic owns the display
```
