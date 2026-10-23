# Boot animation and boot logo (preview)

Made from one 4x4 storyboard grid: the first picture is the boot logo, the other 15 are the animation frames (left to right, top to bottom).

- `bootlogo_720x1280_rotated.bmp` / `.png`: boot logo, rotated 90 degrees clockwise for the unit's portrait panel. `bootlogo_1280x720.png` is the same picture unrotated.
- `bootanimation.zip`: install as `/system/media/bootanimation.zip` (needs root/system access; keep a backup of the original). Stored (not compressed) zip.
- `desc.txt`: `1280 720 1` then `p 1 0 part0` (1 fps preview, 15 frames, plays once, then holds the last frame). Frames are `part0/frame0000.png` to `frame0014.png`.

A trailing newline after the last line of desc.txt is fine and is included. Frame names do not strictly need 4 digits, but equal-length zero-padded names keep the order correct.

This is a 1 fps preview. Later versions can add in-between frames for 15 or 24 fps.
