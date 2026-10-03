# 📜 GhostTap — History & Roadmap

> Where the project came from and what's next. Kept for context; the
> original docs this summarizes live in `old/` (DOCUMENTATION.MD, PLAN.MD, UI.MD).

## Origin

GhostTap started as a two-part ESP32-C3 Wi-Fi tool: ESP-IDF firmware with
Wi-Fi analysis/attack/capture features and an SSD1306 OLED, plus a Jetpack
Compose Android controller ("MYPP") talking over BLE. The "The Other Side"
plan (old/PLAN.MD) added the cyberpunk Hacker Mode dashboard, the v2 attack
command format (`ATTACK:TYPE:METHOD:INDEX:DURATION`), on-demand AP handling
and MAC-restore ordering fixes.

## What this migration changed (2026-10)

- Project renamed **GhostTap** everywhere (app label, package
  `com.ghosttap.app`, Gradle project, BLE device name, firmware project name)
- Repository reorganized: `firmware/` + `app/` + `docs/` + `assets/`
- Loose uppercase root docs converted into the structured `docs/` tree
- Junk removed: JVM crash logs (`hs_err_pid*`, `replay_pid*`), machine
  -specific `local.properties`
- Old paths in docs (e.g. `c:\Users\khurr\Desktop\MYPP\...`) replaced with
  repo-relative references

Nothing was removed functionally — every feature is preserved.

## Roadmap

- [x] Split mega-screens (2026-10-04): ControlScreen 4,020 lines → 6 files by
  concern; HackerModeScreen 2,685 lines → 7 files. Declaration-parity verified
  against the original (20 + 24 top-level symbols, no additions, no losses).
- [ ] GitHub Actions workflows: ESP-IDF firmware build + Android APK builds
- [ ] Signing config for release APKs
- [ ] Tighten storage permissions to scoped storage
- [ ] Optional: license decision (MeshSight precedent — decide later)
- [ ] Phase 6 from the old plan: enhanced capture-file detail view

## Provenance note

The firmware's architecture (frame_analyzer, pcap/hccapx serializers,
wsl_bypasser naming) descends from the open-source ESP32 Wi-Fi martial-arts
projects (ESP32Marauder / Spacehuhn lineage). GhostTap is a customized
fork-lineage build with its own app, OLED UI, snake game and AI animation
pipeline.
