# Release notes

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
