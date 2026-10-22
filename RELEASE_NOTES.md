# Release notes

## 1.5 (compared with 1.4)

Renamed to **Rehome** (the repository is rehome-launcher-for-car). The package is now com.rehome.launcher, so this installs as a new app next to the old one: set Rehome as the home app again and go through the welcome screen. Three icons and names: the app itself "Rehome Installer" (what the installer shows), the home screen "Rehome: for you", the call helper "Rehome: Call Plugin".

- Layout: the map runs under one rounded, glass contacts bar (no black box behind it); the bar stays inside the frame margin; the bottom dock is a plain black bar. The 360 window and the bars mirror when the driver sits on the left.
- Navigation and city info now sit on the side away from the 360 window, in glass boxes. Icons on the contacts bar sit on glowing red/blue glass; the dock has no glow.
- Simple layout bar order: radio, 360, map/media switch, three contacts. The switch shows a map pin while the map is shown; tap opens Google Maps (or Brave while the media widget is shown), hold changes the widget. The Navigate button is gone.
- The YouTube embed is gone; the widget shows what is playing (from the notification) with a play/pause button.
- Frame: one adjustable frame (margin, rounded corners) that blocks touches on its black parts; optional extended bottom bar with 360 and the primary contact (simple layout); optional split status-bar slabs; a setting to leave Android system apps uncovered.
- Floating buttons: choose which ones (Home by default; Back, Mute, Radio, 360 optional; Back and Mute may not work on every unit); they appear only in the apps you choose (default: the 360 app).
- Speed glow: top third of the screen only, yellow from 80 km/h and red from 120 km/h, nothing below that. No orange mist.
- Vignette up to 200. Map text 300% and UI size 130% by default. Dark theme by default.
- Start-up: radio and 360 start first; the interface, frame and floating buttons appear a few seconds later (setting). Black cover that fades out after the map has drawn.
- Welcome screen simplified (look, driver side, 360 window); then Android asks for home app, display over other apps and notification access.
- New Guide tab (first): how to open settings, Brave, map downloads, 3D map files. Settings tab list shows arrows when there are more tabs.
- Optional centre logo (your own PNG). Call settings reduced to the call account; advanced call and wheel-button settings appear after holding the About tab.
- Removed: Navigate button, navigation test, backup/restore, per-app exclusion lists, status bar looks, hold-to-open-Brave, YouTube embed.

## 1.4 (compared with 1.3)

- Package name is now com.kore.launcher (a new app: it does not update over the old one; set Rehome as the home app again and redo the permissions). The APK is called Rehome.apk.
- New app icon.
- The welcome screen asks whether your unit has a 360 window (PIP).
- Wider Tesla-style speed glow along the top and sides.
- Optional 3D map (MapLibre): choose it in Settings > Map and pick an offline .mbtiles vector file (OpenMapTiles layout). Tilted view with 3D buildings; no place names yet. Mapsforge flat map stays the default.

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
- Renamed Rehome with a new app icon; holding the play button in the YouTube widget opens Brave; floating buttons start on the left.
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
