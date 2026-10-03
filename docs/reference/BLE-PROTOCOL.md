# 📡 GhostTap — BLE Protocol Reference

> All traffic runs over one BLE GATT characteristic. Commands are plain-text
> strings from the app to the device; notifications are plain-text strings
> from the device to the app. Everything is UTF-8 text — easy to debug with
> any BLE terminal.

## Connection details

| Item | Value |
|---|---|
| Device name | **GhostTap** (also accept any ESP32 variant for older builds) |
| Service UUID | `00004faf-0000-1000-8000-00805f9b34fb` |
| Characteristic UUID | `beb5483e-36e1-4688-b7f5-ea07361b26a8` |
| Properties | Read / Write / Notify |

---

## Commands (App → Device)

### Modes

| Command | Effect |
|---|---|
| `MODE:NORMAL` | Clock / animation mode |
| `MODE:ADVANCED` | Hacker / attack mode |

### Wi-Fi scanning

| Command | Effect |
|---|---|
| `SCAN:START` | Start AP scan |
| `SCAN:STOP` | Stop AP scan |

### Attacks — legacy form

| Command | Effect |
|---|---|
| `ATTACK:DOS` | Deauth attack (needs target config) |
| `ATTACK:HANDSHAKE` | Handshake capture |
| `ATTACK:BEACON` | Beacon swarm |
| `ATTACK:STOP` | Stop current attack |

### Attacks — v2 form (used by Hacker Mode dashboard)

```
ATTACK:<TYPE>:<METHOD>:<INDEX>:<DURATION>
```

- `TYPE`: `DOS` · `HANDSHAKE` · `BEACON`
- `METHOD`: integer ID —
  - DOS: `0` Rogue AP · `1` Broadcast · `2` Combined
  - HANDSHAKE: `0` Rogue AP · `1` Broadcast · `2` Passive
- `INDEX`: scan-result index of the target AP
- `DURATION`: seconds (`0` = infinite)

### Target config

| Command | Effect |
|---|---|
| `SET_SSID:<ssid>` | Set target SSID |
| `SWARM_SSIDS:<a,b,c>` | Set beacon-swarm SSIDs (comma-separated) |
| `GET_SWARM` | Request current swarm list |
| `SET_SWARM:<list>` | Update swarm list |

### Display & drawing (OLED canvas)

| Command | Effect |
|---|---|
| `CLEAR` | Clear screen |
| `UPDATE` | Flush draw buffer to display |
| `PX:x:y` / `PIXEL:x:y` | Set pixel |
| `LINE:x1:y1:x2:y2` | Draw line |
| `RECT:x:y:w:h` | Draw rectangle |
| `CIRCLE:x:y:r` | Draw circle |
| `BRUSH:<n>` | Select brush size |
| `ROW:<data>` | Write a row of pixels |
| `BUF:<data>` | Write raw buffer chunk |

### Script streaming (AI animations)

| Command | Effect |
|---|---|
| `SCRIPT_LINE:<code>` | One line of C-like draw script, executed on device |
| `SAVE_PCAP` | Save the in-RAM capture to a file |

### Device features

| Command | Effect |
|---|---|
| `TIME:FMT:<…>` | Set clock display format |
| `SYNC_TIME:<epoch>` | Sync device clock |
| `BOOT_ANIM:<n>` | Select boot animation |
| `SNAKE:<cmd>` | Snake game control |
| `MUSIC:VIZ:<mode>` | Music visualizer mode |

---

## Notifications (Device → App)

| Prefix | Format | Meaning |
|---|---|---|
| `SCAN_RESULT:` | `SCAN_RESULT:SSID:BSSID:RSSI:CH` | Discovered access point |
| `HS_FRAME:` | `HS_FRAME:<hex>` | Raw EAPOL frame bytes (app saves to .pcap) |
| `HS_STATUS:` | `HS_STATUS:Frames:Mask:Clients:Size` | Handshake capture progress |
| `TEMP:` | `TEMP:<celsius>` | Temperature reading |
| `FILE_START:` | `FILE_START:<name>:<size>` | File transfer begins |
| `FILE_DATA:` | `FILE_DATA:<chunk>` | File transfer chunk |
| `FILE_ERROR:` | `FILE_ERROR:<msg>` | File transfer failed |
| `LOG:` | `LOG:<message>` | Debug/info log line |

---

## Notes

- The app serializes writes through a **command queue** — commands are never
  sent faster than the device link can take them (prevents BLE flooding).
- `HS_FRAME:` payloads are hex-encoded; the app reassembles them into a
  local `.pcap` file in capture order.
- On attack cleanup the device restores the AP MAC before tearing down the
  AP interface (order matters — see ARCHITECTURE.md).
