package com.rehome.launcher;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Makes the boot logo and boot animation on the device: a safety reminder that dissolves into the middle,
 * a badge that shines for a moment, then a soft glow. Same design as boot/tools/make_boot.py in the repository.
 */
final class BootPack {
    static final int W = 1280, H = 720, FPS = 10;
    static final float SECS = 6f;
    static final String DEFAULT_LINES = "Eyes on the road.\nCheck the doors and your passengers.\n"
            + "Check the mirrors: who is behind you, a child or a truck?\nMind your tyres and the kerb.\n"
            + "Know the risk before you speed.\nTake your keys. Switch off when you leave.";

    interface Progress {
        void on(int done, int total);
    }

    private final Bitmap badge;
    private final Bitmap memo;

    BootPack(Bitmap badgeOrNull, String text) {
        badge = badgeOrNull != null ? fit(badgeOrNull, 300) : neutralBadge(300);
        memo = memoLayer(text);
    }

    // ------------------------------------------------------------------ drawing

    private static float ease(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3f - 2f * x);
    }

    private static Bitmap fit(Bitmap b, int box) {
        float s = Math.min((float) box / b.getWidth(), (float) box / b.getHeight());
        return Bitmap.createScaledBitmap(b, Math.max(1, Math.round(b.getWidth() * s)), Math.max(1, Math.round(b.getHeight() * s)), true);
    }

    static Bitmap neutralBadge(int size) {
        Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(0xFFE8EEF5);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        float n = size;
        p.setStrokeWidth(n * 0.035f);
        c.drawOval(new RectF(n * .06f, n * .06f, n * .94f, n * .94f), p);
        float cx = n / 2f;
        p.setStrokeWidth(n * 0.04f);
        Path roof = new Path();
        roof.moveTo(cx - n * .20f, cx + n * .02f);
        roof.lineTo(cx, cx - n * .20f);
        roof.lineTo(cx + n * .20f, cx + n * .02f);
        c.drawPath(roof, p);
        c.drawRect(cx - n * .14f, cx + n * .02f, cx + n * .14f, cx + n * .22f, p);
        return bm;
    }

    /** Makes the colour of the top-left corner transparent (with a soft edge), for badges saved on a plain background. */
    static Bitmap removeBackground(Bitmap src) {
        Bitmap b = src.copy(Bitmap.Config.ARGB_8888, true);
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        int bg = px[0];
        int br = Color.red(bg), bgc = Color.green(bg), bb = Color.blue(bg);
        for (int i = 0; i < px.length; i++) {
            int dr = Color.red(px[i]) - br, dg = Color.green(px[i]) - bgc, db = Color.blue(px[i]) - bb;
            double d = Math.sqrt(dr * dr + dg * dg + db * db);
            float a = (float) Math.max(0, Math.min(1, (d - 28) / 40.0));
            px[i] = (Math.round(Color.alpha(px[i]) * a) << 24) | (px[i] & 0x00FFFFFF);
        }
        b.setPixels(px, 0, w, 0, 0, w, h);
        return b;
    }

    private static Bitmap memoLayer(String text) {
        Bitmap bm = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        String[] lines = text.trim().length() == 0 ? DEFAULT_LINES.split("\n") : text.split("\n");
        int n = Math.min(8, lines.length);
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setTextAlign(Paint.Align.CENTER);
        t.setColor(0xFFFFBE78);
        t.setTextSize(30);
        t.setLetterSpacing(0.18f);
        float step = 70f;
        float total = 100f + n * step;
        float y = (H - total) / 2f + 30f;
        c.drawText("BEFORE YOU DRIVE", W / 2f, y, t);
        Paint ln = new Paint(Paint.ANTI_ALIAS_FLAG);
        ln.setColor(0x5AFFFFFF);
        ln.setStrokeWidth(2f);
        c.drawLine(W / 2f - 60, y + 28, W / 2f + 60, y + 28, ln);
        Paint l = new Paint(Paint.ANTI_ALIAS_FLAG);
        l.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        l.setTextAlign(Paint.Align.CENTER);
        l.setColor(0xF5ECF2F8);
        y += 100;
        for (int i = 0; i < n; i++) {
            float size = 36f;
            l.setTextSize(size);
            while (l.measureText(lines[i]) > W - 140 && size > 18) {
                size -= 2;
                l.setTextSize(size);
            }
            c.drawText(lines[i].trim(), W / 2f, y, l);
            y += step;
        }
        return bm;
    }

    private static int[] glowColors(int rgb, float amp, float power) {
        int steps = 8;
        int[] out = new int[steps];
        for (int i = 0; i < steps; i++) {
            float s = i / (float) (steps - 1);
            float a = (float) (amp * Math.pow(1f - s, power));
            out[i] = (Math.round(Math.max(0f, Math.min(1f, a)) * 255) << 24) | (rgb & 0xFFFFFF);
        }
        return out;
    }

    private static float[] stops() {
        float[] s = new float[8];
        for (int i = 0; i < 8; i++) s[i] = i / 7f;
        return s;
    }

    private static void drawBackground(Canvas c, float glow) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 0, H, 0xFF0E141B, 0xFF070A0E, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p);
        Matrix m = new Matrix();
        float r1 = 0.85f + 0.1f * glow, r2 = 1.2f + 0.1f * glow;
        RadialGradient g1 = new RadialGradient(0, 0, 1, glowColors(0xC8641A, 0.55f + 0.40f * glow, 1.6f), stops(), Shader.TileMode.CLAMP);
        m.setScale(W / 2f * r1, H / 2f * r1);
        m.postTranslate(W / 2f, H / 2f);
        g1.setLocalMatrix(m);
        p.setShader(g1);
        c.drawRect(0, 0, W, H, p);
        RadialGradient g2 = new RadialGradient(0, 0, 1, glowColors(0x2D6CC8, 0.35f + 0.35f * glow, 1.8f), stops(), Shader.TileMode.CLAMP);
        m = new Matrix();
        m.setScale(W / 2f * r2, H / 2f * r2);
        m.postTranslate(W / 2f, H / 2f);
        g2.setLocalMatrix(m);
        p.setShader(g2);
        c.drawRect(0, 0, W, H, p);
        // soft dark edge
        int[] vc = new int[8];
        for (int i = 0; i < 8; i++) {
            float s = i / 7f;
            vc[i] = (Math.round(255 * 0.65f * (float) Math.pow(s, 2.2)) << 24);
        }
        RadialGradient vg = new RadialGradient(0, 0, 1, vc, stops(), Shader.TileMode.CLAMP);
        m = new Matrix();
        m.setScale(W / 2f * 1.45f, H / 2f * 1.45f);
        m.postTranslate(W / 2f, H / 2f);
        vg.setLocalMatrix(m);
        p.setShader(vg);
        c.drawRect(0, 0, W, H, p);
    }

    private static void drawCentered(Canvas c, Bitmap b, float scale, float alpha, float blur) {
        if (alpha <= 0f) return;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setAlpha(Math.round(255 * Math.min(1f, alpha)));
        Bitmap src = b;
        if (blur > 0.5f) {
            float k = 1f + blur / 3f;
            Bitmap small = Bitmap.createScaledBitmap(b, Math.max(8, Math.round(b.getWidth() / k)), Math.max(8, Math.round(b.getHeight() / k)), true);
            src = small;
        }
        float w = b.getWidth() * scale, h = b.getHeight() * scale;
        c.drawBitmap(src, null, new RectF(W / 2f - w / 2f, H / 2f - h / 2f, W / 2f + w / 2f, H / 2f + h / 2f), p);
    }

    Bitmap frame(float t) {
        Bitmap out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        float glow = 0.5f + 0.5f * (float) Math.sin(t * 1.4);
        float mid = 0.35f + 0.65f * ease((t - 3.8f) / 1.6f);
        drawBackground(c, 0.25f + 0.75f * glow * mid);
        float q = ease((t - 1.2f) / 1.5f);
        if (q < 1f) drawCentered(c, memo, 1f - 0.6f * (float) Math.pow(q, 1.4), 1f - q, 16f * q);
        float fin = ease((t - 2.5f) / 1.0f), fout = ease((t - 4.3f) / 1.0f);
        float a = fin * (1f - fout);
        if (a > 0f) {
            float sc = 0.88f + 0.12f * fin;
            Bitmap layer = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            Canvas lc = new Canvas(layer);
            float bw = badge.getWidth(), bh = badge.getHeight();
            lc.drawBitmap(badge, W / 2f - bw / 2f, H / 2f - bh / 2f, null);
            float s = (t - 3.0f) / 0.9f;
            if (s >= 0f && s <= 1f) {
                Paint sh = new Paint(Paint.ANTI_ALIAS_FLAG);
                sh.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
                sh.setColor(Color.argb(Math.round(110 * (float) Math.sin(Math.PI * s)), 255, 255, 255));
                sh.setMaskFilter(new BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL));
                float cx = W / 2f - 220 + 440 * s;
                Path poly = new Path();
                poly.moveTo(cx - 22, H / 2f + 170);
                poly.lineTo(cx + 22, H / 2f + 170);
                poly.lineTo(cx + 92, H / 2f - 170);
                poly.lineTo(cx + 48, H / 2f - 170);
                poly.close();
                lc.drawPath(poly, sh);
            }
            Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            lp.setAlpha(Math.round(255 * a));
            c.save();
            c.scale(sc, sc, W / 2f, H / 2f);
            c.drawBitmap(layer, 0, 0, lp);
            c.restore();
        }
        return out;
    }

    // ------------------------------------------------------------------ files

    private static void chunk(ByteArrayOutputStream o, String type, byte[] data) throws IOException {
        byte[] tb = type.getBytes("US-ASCII");
        int len = data.length;
        o.write(new byte[]{(byte) (len >>> 24), (byte) (len >>> 16), (byte) (len >>> 8), (byte) len});
        o.write(tb);
        o.write(data);
        CRC32 crc = new CRC32();
        crc.update(tb);
        crc.update(data);
        long v = crc.getValue();
        o.write(new byte[]{(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v});
    }

    private static final int RL = 6, GL = 6, BL = 7;
    private static final int[] PAL = new int[RL * GL * BL];

    private static int level(int i, int n) {
        return Math.round(255f * (float) Math.pow(i / (float) (n - 1), 1.8));
    }

    static {
        int k = 0;
        for (int r = 0; r < RL; r++) for (int g = 0; g < GL; g++) for (int b = 0; b < BL; b++) {
            PAL[k++] = (level(r, RL) << 16) | (level(g, GL) << 8) | level(b, BL);
        }
    }

    private static int nearest(float v, int n) {
        int best = 0;
        float bd = 1e9f;
        for (int i = 0; i < n; i++) {
            float d = Math.abs(level(i, n) - v);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    /** 8-bit palette PNG with error-diffusion dithering: small files, fine for a dark glow. */
    static byte[] indexedPng(Bitmap bm) throws IOException {
        int w = bm.getWidth(), h = bm.getHeight();
        int[] px = new int[w * h];
        bm.getPixels(px, 0, w, 0, 0, w, h);
        float[][] cur = new float[3][w + 2], nxt = new float[3][w + 2];
        byte[] raw = new byte[(w + 1) * h];
        // lookup tables for the per-channel nearest level
        int[] lr = new int[256], lg = new int[256], lb = new int[256];
        for (int v = 0; v < 256; v++) {
            lr[v] = nearest(v, RL);
            lg[v] = nearest(v, GL);
            lb[v] = nearest(v, BL);
        }
        for (int y = 0; y < h; y++) {
            raw[y * (w + 1)] = 0;
            float[][] t = cur;
            cur = nxt;
            nxt = t;
            for (int ch = 0; ch < 3; ch++) java.util.Arrays.fill(nxt[ch], 0f);
            for (int x = 0; x < w; x++) {
                int p = px[y * w + x];
                float r = Math.max(0, Math.min(255, Color.red(p) + cur[0][x + 1]));
                float g = Math.max(0, Math.min(255, Color.green(p) + cur[1][x + 1]));
                float b = Math.max(0, Math.min(255, Color.blue(p) + cur[2][x + 1]));
                int ri = lr[Math.round(r)], gi = lg[Math.round(g)], bi = lb[Math.round(b)];
                raw[y * (w + 1) + 1 + x] = (byte) ((ri * GL + gi) * BL + bi);
                float er = r - level(ri, RL), eg = g - level(gi, GL), eb = b - level(bi, BL);
                float[] e = {er, eg, eb};
                for (int ch = 0; ch < 3; ch++) {
                    cur[ch][x + 2] += e[ch] * 7f / 16f;
                    nxt[ch][x] += e[ch] * 3f / 16f;
                    nxt[ch][x + 1] += e[ch] * 5f / 16f;
                    nxt[ch][x + 2] += e[ch] * 1f / 16f;
                }
            }
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        byte[] ihdr = new byte[13];
        ihdr[0] = (byte) (w >>> 24);
        ihdr[1] = (byte) (w >>> 16);
        ihdr[2] = (byte) (w >>> 8);
        ihdr[3] = (byte) w;
        ihdr[4] = (byte) (h >>> 24);
        ihdr[5] = (byte) (h >>> 16);
        ihdr[6] = (byte) (h >>> 8);
        ihdr[7] = (byte) h;
        ihdr[8] = 8;
        ihdr[9] = 3;
        chunk(o, "IHDR", ihdr);
        byte[] plte = new byte[PAL.length * 3];
        for (int i = 0; i < PAL.length; i++) {
            plte[i * 3] = (byte) (PAL[i] >> 16);
            plte[i * 3 + 1] = (byte) (PAL[i] >> 8);
            plte[i * 3 + 2] = (byte) PAL[i];
        }
        chunk(o, "PLTE", plte);
        Deflater d = new Deflater(9);
        d.setInput(raw);
        d.finish();
        ByteArrayOutputStream z = new ByteArrayOutputStream();
        byte[] buf = new byte[1 << 16];
        while (!d.finished()) z.write(buf, 0, d.deflate(buf));
        d.end();
        chunk(o, "IDAT", z.toByteArray());
        chunk(o, "IEND", new byte[0]);
        return o.toByteArray();
    }

    static byte[] bmp24(Bitmap b) {
        int w = b.getWidth(), h = b.getHeight();
        int rowSize = (w * 3 + 3) & ~3;
        int size = 54 + rowSize * h;
        byte[] out = new byte[size];
        out[0] = 'B';
        out[1] = 'M';
        putInt(out, 2, size);
        putInt(out, 10, 54);
        putInt(out, 14, 40);
        putInt(out, 18, w);
        putInt(out, 22, h);
        out[26] = 1;
        out[28] = 24;
        putInt(out, 34, rowSize * h);
        int[] px = new int[w];
        for (int y = 0; y < h; y++) {
            b.getPixels(px, 0, w, 0, h - 1 - y, w, 1);
            int o = 54 + y * rowSize;
            for (int x = 0; x < w; x++) {
                out[o++] = (byte) Color.blue(px[x]);
                out[o++] = (byte) Color.green(px[x]);
                out[o++] = (byte) Color.red(px[x]);
            }
        }
        return out;
    }

    private static void putInt(byte[] a, int at, int v) {
        a[at] = (byte) v;
        a[at + 1] = (byte) (v >>> 8);
        a[at + 2] = (byte) (v >>> 16);
        a[at + 3] = (byte) (v >>> 24);
    }

    private static void writeFile(File f, byte[] data) throws IOException {
        FileOutputStream os = new FileOutputStream(f);
        os.write(data);
        os.close();
    }

    /** Writes bootanimation.zip, desc.txt and the boot logo files into dir. */
    void write(File dir, Progress progress) throws IOException {
        dir.mkdirs();
        int n = Math.round(FPS * SECS);
        File zip = new File(dir, "bootanimation.zip");
        ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(zip));
        zo.setLevel(0);
        byte[] desc = (W + " " + H + " " + FPS + "\np 1 0 part0\n").getBytes("US-ASCII");
        putStored(zo, "desc.txt", desc);
        for (int i = 0; i < n; i++) {
            Bitmap f = frame(i / (float) FPS);
            putStored(zo, String.format(java.util.Locale.US, "part0/frame%04d.png", i), indexedPng(f));
            f.recycle();
            if (progress != null) progress.on(i + 1, n + 1);
        }
        zo.close();
        writeFile(new File(dir, "desc.txt"), desc);
        Bitmap logo = frame(0f);
        Matrix m = new Matrix();
        m.postRotate(90);
        Bitmap rot = Bitmap.createBitmap(logo, 0, 0, W, H, m, true);
        writeFile(new File(dir, "bootlogo_720x1280_rotated.bmp"), bmp24(rot));
        FileOutputStream po = new FileOutputStream(new File(dir, "bootlogo_720x1280_rotated.png"));
        rot.compress(Bitmap.CompressFormat.PNG, 100, po);
        po.close();
        po = new FileOutputStream(new File(dir, "bootlogo_1280x720.png"));
        logo.compress(Bitmap.CompressFormat.PNG, 100, po);
        po.close();
        if (progress != null) progress.on(n + 1, n + 1);
    }

    private static void putStored(ZipOutputStream zo, String name, byte[] data) throws IOException {
        ZipEntry e = new ZipEntry(name);
        e.setMethod(ZipEntry.STORED);
        e.setSize(data.length);
        e.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        e.setCrc(crc.getValue());
        zo.putNextEntry(e);
        zo.write(data);
        zo.closeEntry();
    }
}
