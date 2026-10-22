package com.kore.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.HashMap;

/** Line icons of the fancy theme, stored as path commands (M, L, C, Z) on a 24 x 24 grid. */
final class IconPack {
    private static final HashMap<String, String> M = new HashMap<String, String>();
    private static final HashMap<String, Path> CACHE = new HashMap<String, Path>();

    static {
        M.put("radio", "M 12 22 L 12 13 M 14 11 C 14 12.1 13.1 13 12 13 C 10.9 13 10 12.1 10 11 C 10 9.9 10.9 9 12 9 C 13.1 9 14 9.9 14 11 Z M 8.5 7.5 C 6.5 9.5 6.5 12.5 8.5 14.5 M 15.5 7.5 C 17.5 9.5 17.5 12.5 15.5 14.5 M 5.5 4.5 C 2 8 2 14 5.5 17.5 M 18.5 4.5 C 22 8 22 14 18.5 17.5");
        M.put("previous", "M 19 20 L 9 12 L 19 4 Z M 5 4 L 5 20");
        M.put("play", "M 6 4 L 20 12 L 6 20 Z");
        M.put("next", "M 5 4 L 15 12 L 5 20 Z M 19 4 L 19 20");
        M.put("mute", "M 11 5 L 6 9 L 2 9 L 2 15 L 6 15 L 11 19 L 11 5 Z M 22 9 L 16 15 M 16 9 L 22 15");
        M.put("home", "M 3 9 L 12 2 L 21 9 L 21 20 C 21 21.1 20.1 22 19 22 L 5 22 C 3.9 22 3 21.1 3 20 Z M 9 22 L 9 12 L 15 12 L 15 22");
        M.put("settings", "M 15 12 C 15 13.66 13.66 15 12 15 C 10.34 15 9 13.66 9 12 C 9 10.34 10.34 9 12 9 C 13.66 9 15 10.34 15 12 Z M 19.4 15 C 19.13 15.62 19.26 16.34 19.73 16.82 L 19.79 16.88 C 20.57 17.66 20.57 18.93 19.79 19.71 C 19.01 20.49 17.74 20.49 16.96 19.71 L 16.9 19.65 C 16.42 19.18 15.7 19.05 15.08 19.32 C 14.48 19.58 14.08 20.17 14.08 20.83 L 14.08 21 C 14.08 22.1 13.18 23 12.08 23 C 10.98 23 10.08 22.1 10.08 21 L 10.08 20.91 C 10.06 20.23 9.64 19.63 9 19.4 C 8.38 19.13 7.66 19.26 7.18 19.73 L 7.12 19.79 C 6.61 20.3 5.88 20.49 5.19 20.31 C 4.5 20.12 3.96 19.58 3.77 18.89 C 3.59 18.2 3.78 17.47 4.29 16.96 L 4.35 16.9 C 4.82 16.42 4.95 15.7 4.68 15.08 C 4.42 14.48 3.83 14.08 3.17 14.08 L 3 14.08 C 1.9 14.08 1 13.18 1 12.08 C 1 10.98 1.9 10.08 3 10.08 L 3.09 10.08 C 3.77 10.06 4.37 9.64 4.6 9 C 4.87 8.38 4.74 7.66 4.27 7.18 L 4.21 7.12 C 3.43 6.34 3.43 5.07 4.21 4.29 C 4.99 3.51 6.26 3.51 7.04 4.29 L 7.1 4.35 C 7.58 4.82 8.3 4.95 8.92 4.68 L 9 4.68 C 9.6 4.42 10 3.83 10 3.17 L 10 3 C 10 1.9 10.9 1 12 1 C 13.1 1 14 1.9 14 3 L 14 3.09 C 14 3.75 14.4 4.34 15 4.6 C 15.62 4.87 16.34 4.74 16.82 4.27 L 16.88 4.21 C 17.39 3.7 18.12 3.51 18.81 3.69 C 19.5 3.88 20.04 4.42 20.23 5.11 C 20.41 5.8 20.22 6.53 19.71 7.04 L 19.65 7.1 C 19.18 7.58 19.05 8.3 19.32 8.92 L 19.32 9 C 19.58 9.6 20.17 10 20.83 10 L 21 10 C 22.1 10 23 10.9 23 12 C 23 13.1 22.1 14 21 14 L 20.91 14 C 20.25 14 19.66 14.4 19.4 15 Z");
        M.put("app-grid", "M 6.5 5 C 6.5 5.83 5.83 6.5 5 6.5 C 4.17 6.5 3.5 5.83 3.5 5 C 3.5 4.17 4.17 3.5 5 3.5 C 5.83 3.5 6.5 4.17 6.5 5 Z M 13.5 5 C 13.5 5.83 12.83 6.5 12 6.5 C 11.17 6.5 10.5 5.83 10.5 5 C 10.5 4.17 11.17 3.5 12 3.5 C 12.83 3.5 13.5 4.17 13.5 5 Z M 20.5 5 C 20.5 5.83 19.83 6.5 19 6.5 C 18.17 6.5 17.5 5.83 17.5 5 C 17.5 4.17 18.17 3.5 19 3.5 C 19.83 3.5 20.5 4.17 20.5 5 Z M 6.5 12 C 6.5 12.83 5.83 13.5 5 13.5 C 4.17 13.5 3.5 12.83 3.5 12 C 3.5 11.17 4.17 10.5 5 10.5 C 5.83 10.5 6.5 11.17 6.5 12 Z M 13.5 12 C 13.5 12.83 12.83 13.5 12 13.5 C 11.17 13.5 10.5 12.83 10.5 12 C 10.5 11.17 11.17 10.5 12 10.5 C 12.83 10.5 13.5 11.17 13.5 12 Z M 20.5 12 C 20.5 12.83 19.83 13.5 19 13.5 C 18.17 13.5 17.5 12.83 17.5 12 C 17.5 11.17 18.17 10.5 19 10.5 C 19.83 10.5 20.5 11.17 20.5 12 Z M 6.5 19 C 6.5 19.83 5.83 20.5 5 20.5 C 4.17 20.5 3.5 19.83 3.5 19 C 3.5 18.17 4.17 17.5 5 17.5 C 5.83 17.5 6.5 18.17 6.5 19 Z M 13.5 19 C 13.5 19.83 12.83 20.5 12 20.5 C 11.17 20.5 10.5 19.83 10.5 19 C 10.5 18.17 11.17 17.5 12 17.5 C 12.83 17.5 13.5 18.17 13.5 19 Z M 20.5 19 C 20.5 19.83 19.83 20.5 19 20.5 C 18.17 20.5 17.5 19.83 17.5 19 C 17.5 18.17 18.17 17.5 19 17.5 C 19.83 17.5 20.5 18.17 20.5 19 Z");
        M.put("map-pin", "M 21 10 C 21 17 12 23 12 23 C 12 23 3 17 3 10 C 3 5.03 7.03 1 12 1 C 16.97 1 21 5.03 21 10 Z M 15 10 C 15 11.66 13.66 13 12 13 C 10.34 13 9 11.66 9 10 C 9 8.34 10.34 7 12 7 C 13.66 7 15 8.34 15 10 Z");
        M.put("youtube", "M 22.54 6.42 C 22.28 5.47 21.54 4.72 20.59 4.46 C 18.88 4 12 4 12 4 C 12 4 5.12 4 3.41 4.46 C 2.46 4.72 1.72 5.47 1.46 6.42 C 1.13 8.26 0.97 10.13 1 12 C 0.97 13.87 1.13 15.74 1.46 17.58 C 1.72 18.53 2.46 19.28 3.41 19.54 C 5.12 20 12 20 12 20 C 12 20 18.88 20 20.59 19.54 C 21.54 19.28 22.28 18.53 22.54 17.58 C 22.87 15.74 23.03 13.87 23 12 C 23.03 10.13 22.87 8.26 22.54 6.42 Z M 10 15 L 15 12 L 10 9 Z");
        M.put("person", "M 20 21 L 20 19 C 20 16.79 18.21 15 16 15 L 8 15 C 5.79 15 4 16.79 4 19 L 4 21 M 16 7 C 16 9.21 14.21 11 12 11 C 9.79 11 8 9.21 8 7 C 8 4.79 9.79 3 12 3 C 14.21 3 16 4.79 16 7 Z");
        M.put("microphone", "M 12 2 L 12 2 C 13.66 2 15 3.34 15 5 L 15 11 C 15 12.66 13.66 14 12 14 L 12 14 C 10.34 14 9 12.66 9 11 L 9 5 C 9 3.34 10.34 2 12 2 Z M 19 10 L 19 11 C 19 14.87 15.87 18 12 18 C 8.13 18 5 14.87 5 11 L 5 10 M 12 19 L 12 22");
        M.put("steering-wheel", "M 20.5 12 C 20.5 16.69 16.69 20.5 12 20.5 C 7.31 20.5 3.5 16.69 3.5 12 C 3.5 7.31 7.31 3.5 12 3.5 C 16.69 3.5 20.5 7.31 20.5 12 Z M 14.1 12 C 14.1 13.16 13.16 14.1 12 14.1 C 10.84 14.1 9.9 13.16 9.9 12 C 9.9 10.84 10.84 9.9 12 9.9 C 13.16 9.9 14.1 10.84 14.1 12 Z M 3.8 10.5 L 9.9 12 M 20.2 10.5 L 14.1 12 M 12 14.1 L 12 20.4");
        M.put("back", "M 15 4 L 7 12 L 15 20 M 8 12 L 21 12");
        M.put("navigation", "M 12 2 L 20 21 L 12 17 L 4 21 Z");
        M.put("music", "M 9 18 L 9 5 L 20 3 L 20 16 M 9 18 C 9 19.66 7.66 21 6 21 C 4.34 21 3 19.66 3 18 C 3 16.34 4.34 15 6 15 C 7.66 15 9 16.34 9 18 Z M 20 16 C 20 17.66 18.66 19 17 19 C 15.34 19 14 17.66 14 16 C 14 14.34 15.34 13 17 13 C 18.66 13 20 14.34 20 16 Z");
        M.put("phone", "M 7 3.8 L 4.8 5.1 C 4 5.6 3.9 6.7 4.2 7.6 C 6.2 13.4 10.6 17.8 16.4 19.8 C 17.3 20.1 18.4 20 18.9 19.2 L 20.2 17 L 16.1 14.8 L 14.5 16.6 C 11.48 15.16 9.04 12.72 7.6 9.7 L 9.4 8.1 Z");
        M.put("360-car", "M 4 14 L 5.4 9.5 C 5.62 8.63 6.4 8.02 7.3 8 L 16.7 8 C 17.6 8.02 18.38 8.63 18.6 9.5 L 20 14 L 20 18 L 18 18 L 18 16 L 6 16 L 6 18 L 4 18 Z M 6 13 L 18 13 M 7 10 L 6 13 M 17 10 L 18 13 M 7 5 C 8.32 3.66 10.12 2.9 12 2.9 C 13.88 2.9 15.68 3.66 17 5 M 19 5 L 17 5 L 17 3 M 5 19 C 6.32 20.34 8.12 21.1 10 21.1 C 11.88 21.1 13.68 20.34 15 19 M 5 19 L 7 19 L 7 21");
    }

    private IconPack() {
    }

    static boolean has(String name) {
        return name != null && M.containsKey(name);
    }

    private static Path path(String name) {
        Path cached = CACHE.get(name);
        if (cached != null) return cached;
        Path p = new Path();
        String[] t = M.get(name).split(" ");
        int i = 0;
        while (i < t.length) {
            String c = t[i++];
            if (c.equals("M")) {
                p.moveTo(Float.parseFloat(t[i]), Float.parseFloat(t[i + 1]));
                i += 2;
            } else if (c.equals("L")) {
                p.lineTo(Float.parseFloat(t[i]), Float.parseFloat(t[i + 1]));
                i += 2;
            } else if (c.equals("C")) {
                p.cubicTo(Float.parseFloat(t[i]), Float.parseFloat(t[i + 1]),
                        Float.parseFloat(t[i + 2]), Float.parseFloat(t[i + 3]),
                        Float.parseFloat(t[i + 4]), Float.parseFloat(t[i + 5]));
                i += 6;
            } else if (c.equals("Z")) {
                p.close();
            }
        }
        CACHE.put(name, p);
        return p;
    }

    /** Draws the icon in a 100 x 100 box (the caller has already scaled the canvas). */
    static void draw(Canvas c, String name, Paint stroke) {
        c.save();
        c.scale(100f / 24f, 100f / 24f);
        stroke.setStrokeWidth(1.6f);
        c.drawPath(path(name), stroke);
        c.restore();
    }

    // ---- icons supplied by an imported theme (stroked paths on the same 24 x 24 grid)

    private static final HashMap<String, String> O = new HashMap<String, String>();

    static void clearOverrides() {
        O.clear();
    }

    static void setOverride(String name, String d) {
        if (name == null) return;
        if (d == null || d.trim().length() == 0) O.remove(name);
        else O.put(name, d.trim());
    }

    static boolean hasOverride(String name) {
        return name != null && O.containsKey(name);
    }

    /** Draws a themed icon in a 100 x 100 box (the caller has already scaled the canvas). */
    static void drawOverride(Canvas c, String name, Paint stroke) {
        String d = O.get(name);
        if (d == null) return;
        Path p = CACHE.get("d:" + d);
        if (p == null) {
            p = new Path();
            try {
                String[] t = d.split("\\s+");
                int i = 0;
                while (i < t.length) {
                    String cmd = t[i++];
                    if (cmd.equals("M")) {
                        p.moveTo(Float.parseFloat(t[i]), Float.parseFloat(t[i + 1]));
                        i += 2;
                    } else if (cmd.equals("L")) {
                        p.lineTo(Float.parseFloat(t[i]), Float.parseFloat(t[i + 1]));
                        i += 2;
                    } else if (cmd.equals("C")) {
                        p.cubicTo(Float.parseFloat(t[i]), Float.parseFloat(t[i + 1]),
                                Float.parseFloat(t[i + 2]), Float.parseFloat(t[i + 3]),
                                Float.parseFloat(t[i + 4]), Float.parseFloat(t[i + 5]));
                        i += 6;
                    } else if (cmd.equals("Z")) {
                        p.close();
                    }
                }
            } catch (Exception e) {
                p = new Path();
            }
            CACHE.put("d:" + d, p);
        }
        c.save();
        c.scale(100f / 24f, 100f / 24f);
        stroke.setStrokeWidth(1.6f);
        c.drawPath(p, stroke);
        c.restore();
    }
}
