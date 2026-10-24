"""Builds the Rehome boot logo and boot animation (1280x720).

usage: python3 -I make_boot.py OUTDIR [badge.png]
 - boot logo: the safety memo on the glow background (also rotated 720x1280 for units with a portrait panel)
 - boot animation: the memo dissolves into the middle, a badge shines for a moment, then the glow stays
 - badge.png (optional, square, transparent): your own badge. Without it a neutral ring-and-house mark is drawn.
"""
import sys, os, math, zipfile
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H, FPS, SECS = 1280, 720, 10, 6.0
out = sys.argv[1]
badge_path = sys.argv[2] if len(sys.argv) > 2 else None
os.makedirs(out + '/part0', exist_ok=True)

FONT = '/usr/share/fonts/opentype/inter/InterDisplay-Medium.otf'
FONT_B = '/usr/share/fonts/opentype/inter/InterDisplay-SemiBold.otf'
if not os.path.exists(FONT_B): FONT_B = FONT

yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
nx, ny = (xx - W / 2) / (W / 2), (yy - H / 2) / (H / 2)
r = np.sqrt(nx * nx + ny * ny)

def ease(x):
    x = min(1.0, max(0.0, x)); return x * x * (3 - 2 * x)

def background(glow):
    top = np.array([14, 20, 27], np.float32); bot = np.array([7, 10, 14], np.float32)
    k = (yy / H)[..., None]
    img = top * (1 - k) + bot * k
    g1 = np.clip(1 - r / (0.85 + 0.1 * glow), 0, 1) ** 1.6 * (0.55 + 0.40 * glow)
    g2 = np.clip(1 - r / (1.2 + 0.1 * glow), 0, 1) ** 1.8 * (0.35 + 0.35 * glow)
    img = img + g1[..., None] * np.array([200, 100, 26], np.float32) + g2[..., None] * np.array([45, 108, 200], np.float32)
    vig = np.clip(r / 1.45, 0, 1) ** 2.2
    img = img * (1 - 0.65 * vig[..., None])
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).convert('RGBA')

LINES = ["Eyes on the road.",
         "Check the doors and your passengers.",
         "Check the mirrors: who is behind you, a child or a truck?",
         "Mind your tyres and the kerb.",
         "Know the risk before you speed.",
         "Take your keys. Switch off when you leave."]

def memo_layer():
    lay = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    ft = ImageFont.truetype(FONT_B, 30); fl = ImageFont.truetype(FONT, 36)
    title = "BEFORE YOU DRIVE"
    tw = d.textlength(title, font=ft) + 6 * (len(title) - 1)
    y = 118
    x = (W - tw) / 2
    for ch in title:
        d.text((x, y), ch, font=ft, fill=(255, 190, 120, 235)); x += d.textlength(ch, font=ft) + 6
    d.line([(W / 2 - 60, y + 54), (W / 2 + 60, y + 54)], fill=(255, 255, 255, 90), width=2)
    y += 100
    for i, s in enumerate(LINES):
        w = d.textlength(s, font=fl)
        d.text(((W - w) / 2, y), s, font=fl, fill=(236, 242, 248, 245))
        y += 70
    return lay

def neutral_badge(size=300):
    S = 4; n = size * S
    im = Image.new('RGBA', (n, n), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    c = n // 2
    d.ellipse([n * .06, n * .06, n * .94, n * .94], outline=(232, 238, 245, 255), width=int(n * .035))
    pts = [(c - n * .20, c + n * .02), (c, c - n * .20), (c + n * .20, c + n * .02)]
    d.line(pts, fill=(232, 238, 245, 255), width=int(n * .04), joint='curve')
    d.rectangle([c - n * .14, c + n * .02, c + n * .14, c + n * .22], outline=(232, 238, 245, 255), width=int(n * .04))
    return im.resize((size, size), Image.LANCZOS)

def load_badge(size=300):
    if badge_path:
        b = Image.open(badge_path).convert('RGBA'); return b.resize((size, int(size * b.size[1] / b.size[0])), Image.LANCZOS)
    return neutral_badge(size)

BADGE = load_badge()
MEMO = memo_layer()

def paste_center(base, layer, scale=1.0, alpha=1.0, blur=0.0, dy=0):
    if alpha <= 0: return base
    w, h = layer.size
    L = layer.resize((max(1, int(w * scale)), max(1, int(h * scale))), Image.LANCZOS)
    if blur > 0.2: L = L.filter(ImageFilter.GaussianBlur(blur))
    a = L.split()[3].point(lambda v: int(v * alpha)); L.putalpha(a)
    base.alpha_composite(L, (int(W / 2 - L.size[0] / 2), int(H / 2 - L.size[1] / 2 + dy)))
    return base

def frame(t):
    glow = 0.5 + 0.5 * math.sin(t * 1.4)
    mid = 0.35 + 0.65 * ease((t - 3.8) / 1.6)       # glow grows once the badge has gone
    base = background(0.25 + 0.75 * glow * mid)
    q = ease((t - 1.2) / 1.5)                         # memo dissolves between 1.2 s and 2.7 s
    if q < 1:
        paste_center(base, MEMO, scale=1 - 0.6 * q ** 1.4, alpha=1 - q, blur=16 * q)
    # badge
    fin = ease((t - 2.5) / 1.0); fout = ease((t - 4.3) / 1.0)
    a = fin * (1 - fout)
    if a > 0:
        paste_center(base, BADGE, scale=0.88 + 0.12 * fin, alpha=a)
        # shine sweep
        s = (t - 3.0) / 0.9
        if 0 <= s <= 1:
            sh = Image.new('RGBA', (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(sh)
            cx = W / 2 - 220 + 440 * s
            d.polygon([(cx - 22, H / 2 + 170), (cx + 22, H / 2 + 170), (cx + 92, H / 2 - 170), (cx + 48, H / 2 - 170)], fill=(255, 255, 255, int(110 * math.sin(math.pi * s))))
            sh = sh.filter(ImageFilter.GaussianBlur(10))
            m = BADGE.split()[3]
            mask = Image.new('L', (W, H), 0); mask.paste(m, (int(W / 2 - BADGE.size[0] / 2), int(H / 2 - BADGE.size[1] / 2)))
            sh.putalpha(Image.fromarray((np.array(sh.split()[3], np.float32) * np.array(mask, np.float32) / 255 * a).astype(np.uint8)))
            base.alpha_composite(sh)
    return base.convert('RGB')

if __name__ == '__main__':
    n = int(FPS * SECS)
    for i in range(n):
        im = frame(i / FPS)
        im.quantize(256, method=Image.MEDIANCUT, dither=Image.FLOYDSTEINBERG).save('%s/part0/frame%04d.png' % (out, i), optimize=True)
    open(out + '/desc.txt', 'w', newline='\n').write('%d %d %d\np 1 0 part0\n' % (W, H, FPS))
    with zipfile.ZipFile(out + '/bootanimation.zip', 'w', zipfile.ZIP_STORED) as z:
        z.write(out + '/desc.txt', 'desc.txt')
        for i in range(n): z.write('%s/part0/frame%04d.png' % (out, i), 'part0/frame%04d.png' % i)
    logo = frame(0.0)
    logo.save(out + '/bootlogo_1280x720.png')
    rot = logo.rotate(-90, expand=True)
    rot.save(out + '/bootlogo_720x1280_rotated.png'); rot.save(out + '/bootlogo_720x1280_rotated.bmp')
    sheet = Image.new('RGB', (4 * 320, 2 * 180))
    for k, t in enumerate([0, 1.5, 2.2, 2.9, 3.4, 4.2, 5.0, 5.9]):
        sheet.paste(frame(t).resize((320, 180)), ((k % 4) * 320, (k // 4) * 180))
    sheet.save(out + '/preview_sheet.jpg', quality=88)
