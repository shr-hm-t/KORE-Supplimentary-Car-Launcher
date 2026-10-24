# Boot logo and boot animation

A neutral set (no brand marks) made by `tools/make_boot.py`.

- **Boot logo:** a short safety reminder on a soft glowing background, shown while the unit starts. `bootlogo_720x1280_rotated.bmp` (and `.png`) is rotated 90 degrees clockwise for units with a portrait panel; `bootlogo_1280x720.png` is the same picture unrotated.
- **Boot animation:** the reminder dissolves into the middle, a badge shines for a moment, then a glow stays on screen. `bootanimation.zip` is stored (not compressed), 1280x720, 10 fps, 60 frames, plays once (`p 1 0 part0`) and holds the last frame until Android has finished starting. Install it as `/system/media/bootanimation.zip` (needs root; keep a copy of the original).
- `preview_sheet.jpg` shows eight frames.

To use your own badge: `python3 -I tools/make_boot.py outfolder badge.png` (square PNG with a transparent background). Edit the `LINES` list in the script to change the reminder text.

How long each stage lasts depends on the unit: the boot logo is held by its firmware, and the animation only plays while Android starts.
