# Release notes

## 1.3 (compared with 1.1)

Note: the 1.1 release was built from a commit whose app version already said 1.2 inside, so the installed 1.1 APK shows "1.2" in Android's app info. This release is 1.3 (version code 8).

- Start-up: black screen first, then the interface, then a fade-in once the map has had time to draw (no white tiles). Map background no longer flashes white in the dark theme.
- Tesla-style speed glow: a wider, brighter light along the top and both sides of the map (accent colour normally, amber from 80 km/h, red from 120 km/h).
- The bottom bar and the contacts bar are each one rounded bar (no separate tiles, no tile gap).
- The play button in the YouTube widget is no longer hidden behind the vignette.
- New shield bar: a black bar at the bottom of other apps with the 360 and primary-contact buttons and an adjustable angle; it blocks touches. The frame, shield bar and floating buttons each have a NetGuard-style exclusion list (Settings > Per-app rules). Needs Usage access.

## 1.1 (compared with 1.0)

- Offline Mapsforge map only (online maps removed), new thin "line art" map style, stronger vignette, tile gaps and icon glow (all adjustable in the theme).
- Configurable bottom bar (radio, map/YouTube, navigation, music, dialer, car app, day/night, apps, settings).
- PIP off = simple full-width layout: no 360 window, no black dock; radio and map/YouTube switch move into the contacts bar.
- Faster start: the launcher shows its background first, then builds the screen and loads the map.
- Renamed KORE with a new app icon; holding the play button in the YouTube widget opens Brave; floating buttons start on the left.
- Floating second button can be Back (default; falls back to Home where the unit does not allow it) or Home.
- Per-app rules using Usage access (no accessibility service).
- Welcome guide: overlay permission, notification access, usage access.
- Navigation info in readable dark boxes; Navigate button; settings backup/restore; status bar looks; YouTube embed fixes (no more intent: errors); call hang-up; sea-level altitude.
- New defaults: 360 window 39.8 x 51.5, black bar 6.5, margin 8, corner 60, auto-360 speed 5-10 km/h, 360 retry 4 s, radio 1 s.

## 1.0 (compared with 0.2)

**Layout**
- New home screen: map / widget area on top, a 15% bar with the 360 camera and three contacts (right of the 360 window), and a black bottom dock (clock under the 360 window, icons: Radio, map/YouTube switch, theme sun/moon, app drawer, settings).
- The play button moved from the icon grid into the YouTube widget.
- The 360 camera window slot now shows a summary behind it: date, heading, altitude, GPS accuracy.
- Calibration screen with live guides: 360 window width and height, black bar height, bar height, frame margin, corner size.

**Screen frame and floating buttons**
- The black frame is now a real 4-corner overlay above every app (touches pass through).
- Bigger neon Mute and Home buttons. Default side follows the driver seat, optional lock, colours, size.
- Mute now has five methods; the default is "like the system mute key".

**Map**
- Engines: OpenStreetMap, offline Mapsforge, Google Maps web page, none.
- Map zoom, map text size, and a vignette with adjustable strength.
- Navigation info test (Settings > Map) shows the raw notification data for troubleshooting.

**YouTube widget**
- Choose notification mode (Open YouTube button) or embedded page. Thumbnail only, play/pause on the thumbnail, the thumbnail stays.

**360 camera**
- Opens automatically when you drive slowly (GPS speed), with time-gap or re-arm options and a touch pause. Optional second try after boot.

**Calling**
- Choose the account (Bluetooth or SIM) for calls.
- Steering-wheel voice button: pick "Vezel Call" in the unit's voice list to call the primary contact.

**Look**
- Radio-style look: gradient, soft colour blobs, vignette, dark translucent tiles, orange accent.
- Settings menus are now side-tab panels at the top right.
- Contacts show a phone icon with the letter; the primary is in the accent colour, the others grey.
- Everything is themable with one JSON file: export, edit, import (see THEMING.md).
- Settings > About with "Made with Claude".

**Start-up**
- The launcher is now direct-boot aware: at a cold start it shows a black screen straight away (instead of "Android is starting") and loads the full home screen as soon as the unit unlocks.

**Removed**: fancy look (replaced by the theme file), three-corner screen rounding.

Version is now 1.0 (versionCode 4).
