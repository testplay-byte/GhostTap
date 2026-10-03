# 🕹️ GhostTap — Usage Guide

> Everyday operation, from pairing to attacks to the fun stuff. For build
> instructions see [BUILD.md](BUILD.md); for every wire command see
> [../reference/BLE-PROTOCOL.md](../reference/BLE-PROTOCOL.md).

---

## 1. Connecting

1. Power the ESP32-C3 — boot animation plays, then it advertises as **GhostTap**.
2. Open the app → grant Bluetooth/Location permissions → **Scan**.
3. Tap **GhostTap**. The OLED shows `CONNECTED!`.

The device shows the idle animation until a phone connects — that's the
"advertising" state.

---

## 2. Normal mode (Control tab)

| Feature | What happens |
|---|---|
| **Clock** | Device syncs time from the phone (`SYNC_TIME:`), renders it on the OLED |
| **Music visualizer** | Audio-reactive animation modes (`MUSIC:VIZ:`) |
| **Snake game** | Play 1x1 or 2x2 snake on the OLED; the device has a BFS AI mode too |
| **Drawing** | Draw on the phone canvas; strokes stream to the OLED as `LINE:`/`PX:`/`CIRCLE:` commands |
| **AI animations** | Type a prompt ("bouncing ball") → Gemini writes C-like draw code → preview locally → send live to the OLED |

### AI animation details

The generated script (e.g. `drawCircle(x, y, r)`) runs through the local
**AnimationScriptEngine**, which maps calls to Android Canvas for instant
preview. With **Live Preview** enabled, each call is also sent as a BLE
drawing command so the device mirrors the phone in real time.

---

## 3. Advanced mode (Hacker tab) — authorized use only

> ⚠️ **Legal:** deauth/beacon attacks and handshake capture disrupt networks
> you don't own. Use **only** on networks you own or have explicit written
> permission to test. This is an educational/security-audit tool.

Flow: **Type → Target → Method → Launch.**

### 3.1 Scan

Tap **SCAN** → results stream in as notifications (`SCAN_RESULT:`) into the
network list. Pick a target — BSSID + channel are captured automatically.

### 3.2 Attacks

| Attack | Methods | Notes |
|---|---|---|
| **Deauth (DOS)** | 0 Rogue AP · 1 Broadcast · 2 Combined | Kicks clients off the target AP |
| **Handshake capture** | 0 Rogue AP · 1 Broadcast · 2 Passive | Captures EAPOL M1–M4; deauth forces reconnects to speed it up |
| **Beacon swarm** | — | Broadcasts fake networks (configurable SSID list) |

Command form: `ATTACK:<TYPE>:<METHOD>:<INDEX>:<DURATION>` — duration in
seconds, `0` = infinite. The OLED shows a large animated countdown while an
attack runs.

### 3.3 Capture files

- EAPOL frames stream to the phone (`HS_FRAME:`) and are reassembled into a
  **.pcap** file locally.
- The **Files** tab lists captures with detail view + save.
- The device can also persist the capture itself (`SAVE_PCAP`) and transfer
  the file over BLE (`FILE_START:` / `FILE_DATA:` chunks).
- .pcap converts to **.hccapx** on-device for cracking tools — or analyze the
  .pcap in Wireshark on your computer.

### 3.4 Status feedback

During any attack the app shows live progress (`HS_STATUS:Frames:Mask:Clients:Size`),
and `LOG:` lines stream into the dashboard console.

---

## 4. OLED UI highlights

- **AP list screen:** horizontally scrolling SSIDs longer than 72 px,
  decorative header bars, `1/15` index, side-bar selection markers.
- **Attack countdown:** 3-digit scaled timer with a per-second vertical
  slide + pixel-fade transition ("glitch" feel).

---

## 5. Quick troubleshooting

| Problem | Check |
|---|---|
| Scan finds nothing | Location enabled? Bluetooth on? Device advertising (idle animation visible)? |
| Attack buttons disabled | Run a scan and select a target first |
| Handshake stalls | Switch handshake method to Broadcast (with deauth) or give Passive more time |
| App freezes during long ops | Progress indicators should be showing; if not, report it — UI responsiveness is a known focus area |
