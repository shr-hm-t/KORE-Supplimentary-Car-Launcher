package com.vezel.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.HashMap;

/** Line icons of the fancy theme, stored as path commands (M, L, C, Z) on a 24 x 24 grid. */
final class IconPack {
    private static final HashMap<String, String> M = new HashMap<String, String>();
    private static final HashMap<String, Path> CACHE = new HashMap<String, Path>();

    static {
        M.put("radio", "M 14.2 12 C 14.2 13.22 13.22 14.2 12 14.2 C 10.78 14.2 9.8 13.22 9.8 12 C 9.8 10.78 10.78 9.8 12 9.8 C 13.22 9.8 14.2 10.78 14.2 12 Z M 8.1 8.1 C 7.06 9.13 6.48 10.54 6.48 12 C 6.48 13.46 7.06 14.87 8.1 15.9 M 15.9 8.1 C 16.94 9.13 17.52 10.54 17.52 12 C 17.52 13.46 16.94 14.87 15.9 15.9 M 5.2 5.2 C 3.39 7 2.38 9.45 2.38 12 C 2.38 14.55 3.39 17 5.2 18.8 M 18.8 5.2 C 20.61 7 21.62 9.45 21.62 12 C 21.62 14.55 20.61 17 18.8 18.8");
        M.put("previous", "M 5 5 L 5 19 M 19 5 L 8 12 L 19 19 Z");
        M.put("next", "M 19 5 L 19 19 M 5 5 L 16 12 L 5 19 Z");
        M.put("play", "M 7 4.5 L 7 19.5 L 19 12 Z");
        M.put("pause", "M 8 5 L 8 19 M 16 5 L 16 19");
        M.put("360-car", "M 4 14 L 5.4 9.5 C 5.62 8.63 6.4 8.02 7.3 8 L 16.7 8 C 17.6 8.02 18.38 8.63 18.6 9.5 L 20 14 L 20 18 L 18 18 L 18 16 L 6 16 L 6 18 L 4 18 Z M 6 13 L 18 13 M 7 10 L 6 13 M 17 10 L 18 13 M 7 5 C 8.32 3.66 10.12 2.9 12 2.9 C 13.88 2.9 15.68 3.66 17 5 M 19 5 L 17 5 L 17 3 M 5 19 C 6.32 20.34 8.12 21.1 10 21.1 C 11.88 21.1 13.68 20.34 15 19 M 5 19 L 7 19 L 7 21");
        M.put("youtube", "M 5 5 L 19 5 C 20.1 5 21 5.9 21 7 L 21 17 C 21 18.1 20.1 19 19 19 L 5 19 C 3.9 19 3 18.1 3 17 L 3 7 C 3 5.9 3.9 5 5 5 Z M 10 9 L 15 12 L 10 15 Z");
        M.put("app-grid", "M 4 4 L 8 4 L 8 8 L 4 8 Z M 10 4 L 14 4 L 14 8 L 10 8 Z M 16 4 L 20 4 L 20 8 L 16 8 Z M 4 10 L 8 10 L 8 14 L 4 14 Z M 10 10 L 14 10 L 14 14 L 10 14 Z M 16 10 L 20 10 L 20 14 L 16 14 Z M 4 16 L 8 16 L 8 20 L 4 20 Z M 10 16 L 14 16 L 14 20 L 10 20 Z M 16 16 L 20 16 L 20 20 L 16 20 Z");
        M.put("map-pin", "M 20 10 C 20 15 12 21 12 21 C 12 21 4 15 4 10 C 4 5.58 7.58 2 12 2 C 16.42 2 20 5.58 20 10 Z M 14.5 10 C 14.5 11.38 13.38 12.5 12 12.5 C 10.62 12.5 9.5 11.38 9.5 10 C 9.5 8.62 10.62 7.5 12 7.5 C 13.38 7.5 14.5 8.62 14.5 10 Z");
        M.put("settings", "M 15 12 C 15 13.66 13.66 15 12 15 C 10.34 15 9 13.66 9 12 C 9 10.34 10.34 9 12 9 C 13.66 9 15 10.34 15 12 Z M 19.4 15 C 19.1 15.63 19.22 16.39 19.7 16.9 L 19.8 17 L 18 18.8 L 17.9 18.7 C 17.39 18.22 16.63 18.1 16 18.4 C 15.41 18.67 15.02 19.25 15 19.9 L 15 20.1 L 12.4 20.1 L 12.4 19.9 C 12.38 19.25 11.99 18.67 11.4 18.4 C 10.77 18.1 10.01 18.22 9.5 18.7 L 9.4 18.8 L 7.6 17 L 7.7 16.9 C 8.18 16.39 8.3 15.63 8 15 C 7.73 14.41 7.15 14.02 6.5 14 L 6.3 14 L 6.3 11.4 L 6.5 11.4 C 7.15 11.38 7.73 10.99 8 10.4 C 8.3 9.77 8.18 9.01 7.7 8.5 L 7.6 8.4 L 9.4 6.6 L 9.5 6.7 C 10.01 7.18 10.77 7.3 11.4 7 C 11.99 6.73 12.38 6.15 12.4 5.5 L 12.4 5.3 L 15 5.3 L 15 5.5 C 15.02 6.15 15.41 6.73 16 7 C 16.63 7.3 17.39 7.18 17.9 6.7 L 18 6.6 L 19.8 8.4 L 19.7 8.5 C 19.22 9.01 19.1 9.77 19.4 10.4 C 19.67 10.99 20.25 11.38 20.9 11.4 L 21.1 11.4 L 21.1 14 L 20.9 14 C 20.25 14.02 19.67 14.41 19.4 15 Z");
        M.put("mute", "M 4 9 L 4 15 L 8 15 L 13 19 L 13 5 L 8 9 Z M 17 9 L 22 15 M 22 9 L 17 15");
        M.put("unmute", "M 4 9 L 4 15 L 8 15 L 13 19 L 13 5 L 8 9 Z M 16 9 C 16.86 9.76 17.35 10.85 17.35 12 C 17.35 13.15 16.86 14.24 16 15 M 18 6 C 19.72 7.52 20.71 9.7 20.71 12 C 20.71 14.3 19.72 16.48 18 18");
        M.put("home", "M 3 11.5 L 12 4 L 21 11.5 M 5.5 10 L 5.5 20 L 18.5 20 L 18.5 10 M 9.5 20 L 9.5 14 L 14.5 14 L 14.5 20");
        M.put("mic", "M 12 3 L 12 3 C 13.66 3 15 4.34 15 6 L 15 12 C 15 13.66 13.66 15 12 15 L 12 15 C 10.34 15 9 13.66 9 12 L 9 6 C 9 4.34 10.34 3 12 3 Z M 5.5 11 C 5.5 14.59 8.41 17.5 12 17.5 C 15.59 17.5 18.5 14.59 18.5 11 M 12 17.5 L 12 21 M 9 21 L 15 21");
        M.put("person", "M 15.2 7.5 C 15.2 9.27 13.77 10.7 12 10.7 C 10.23 10.7 8.8 9.27 8.8 7.5 C 8.8 5.73 10.23 4.3 12 4.3 C 13.77 4.3 15.2 5.73 15.2 7.5 Z M 5 21 L 5 19 C 5 15.13 8.13 12 12 12 C 15.87 12 19 15.13 19 19 L 19 21 Z");
        M.put("phone", "M 7 3 L 4.5 3 C 3.67 3 3 3.67 3 4.5 C 3 13.6 10.4 21 19.5 21 C 20.33 21 21 20.33 21 19.5 L 21 17 L 16 15 L 14.5 17 C 11.39 15.6 8.9 13.11 7.5 10 L 9.5 8 Z");
        M.put("tyre", "M 20.5 12 C 20.5 16.69 16.69 20.5 12 20.5 C 7.31 20.5 3.5 16.69 3.5 12 C 3.5 7.31 7.31 3.5 12 3.5 C 16.69 3.5 20.5 7.31 20.5 12 Z M 16.4 12 C 16.4 14.43 14.43 16.4 12 16.4 C 9.57 16.4 7.6 14.43 7.6 12 C 7.6 9.57 9.57 7.6 12 7.6 C 14.43 7.6 16.4 9.57 16.4 12 Z M 12 3.5 L 12 7.5 M 12 16.5 L 12 20.5 M 3.5 12 L 7.5 12 M 16.5 12 L 20.5 12 M 6 6 L 8.8 8.8 M 15.2 15.2 L 18 18 M 18 6 L 15.2 8.8 M 8.8 15.2 L 6 18");
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
        stroke.setStrokeWidth(1.7f);
        c.drawPath(path(name), stroke);
        c.restore();
    }
}
