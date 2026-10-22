package com.rehome.launcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.File;

/** What the top area needs from a map. */
interface MapHost {
    /** A normal phone-Chrome identity so web pages do not treat the launcher as an embedded web view. */
    String CHROME_UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/124.0.0.0 Mobile Safari/537.36";

    View view();

    void onResume();

    void onPause();

    void onDestroy();

    /** Start showing the car position. */
    void showCar();

    /** Tell the map where the car is now. */
    void setCar(double lat, double lon);

    void centerOn(double lat, double lon);

    /** Current map centre as {lat, lon}, or null when there is no map. */
    double[] center();
}

final class MapStyle {
    private MapStyle() {
    }

    /** Dark, greyscale, slightly dimmed look (used by the "standard" map style at night). */
    static ColorMatrixColorFilter dark() {
        ColorMatrix cm = new ColorMatrix(new float[]{
                -1, 0, 0, 0, 255,
                0, -1, 0, 0, 255,
                0, 0, -1, 0, 255,
                0, 0, 0, 1, 0});
        ColorMatrix sat = new ColorMatrix();
        sat.setSaturation(0f);
        cm.postConcat(sat);
        ColorMatrix dim = new ColorMatrix(new float[]{
                0.75f, 0, 0, 0, -6,
                0, 0.75f, 0, 0, -6,
                0, 0, 0.75f, 0, -6,
                0, 0, 0, 1, 0});
        cm.postConcat(dim);
        return new ColorMatrixColorFilter(cm);
    }
}

/**
 * The "line art" map style: a plain ground colour, thin pale roads, outline-only buildings and quiet labels.
 * Written as a Mapsforge render theme so the colours can follow the launcher theme.
 */
final class LineArtTheme {
    private LineArtTheme() {
    }

    static String hex(int argb) {
        if ((argb >>> 24) == 0xFF) return String.format("#%06X", argb & 0xFFFFFF);
        return String.format("#%08X", argb);
    }

    static String xml(int ground, int water, int road, int major, int building, int text, int halo) {
        String g = hex(ground), w = hex(water), r = hex(road), m = hex(major), b = hex(building),
                t = hex(text), h = hex(halo);
        String minorRoads = "residential|unclassified|living_street|service|road|pedestrian|track";
        String midRoads = "tertiary|tertiary_link|secondary|secondary_link";
        String bigRoads = "primary|primary_link|trunk|trunk_link|motorway|motorway_link";
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        x.append("<rendertheme xmlns=\"http://mapsforge.org/renderTheme\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"http://mapsforge.org/renderTheme ")
                .append("https://raw.githubusercontent.com/mapsforge/mapsforge/master/resources/renderTheme.xsd\" ")
                .append("version=\"5\" map-background=\"").append(g).append("\" map-background-outside=\"").append(g).append("\">\n");

        // sea, lakes, land
        x.append("<rule e=\"way\" k=\"natural\" v=\"issea|sea\"><area fill=\"").append(w).append("\" /></rule>\n");
        x.append("<rule e=\"way\" k=\"natural\" v=\"nosea\"><area fill=\"").append(g).append("\" stroke=\"").append(g)
                .append("\" stroke-width=\"1.0\" /></rule>\n");
        x.append("<rule e=\"way\" k=\"natural|landuse\" v=\"water|reservoir|basin\"><area fill=\"").append(w).append("\" /></rule>\n");
        x.append("<rule e=\"way\" k=\"waterway\" v=\"riverbank\"><area fill=\"").append(w).append("\" /></rule>\n");
        x.append("<rule e=\"way\" k=\"waterway\" v=\"river|canal\" zoom-min=\"11\"><line stroke=\"").append(w)
                .append("\" stroke-width=\"1.6\" stroke-linecap=\"round\" /></rule>\n");
        x.append("<rule e=\"way\" k=\"waterway\" v=\"stream|drain\" zoom-min=\"15\"><line stroke=\"").append(w)
                .append("\" stroke-width=\"0.8\" stroke-linecap=\"round\" /></rule>\n");

        // buildings: outline only
        x.append("<rule e=\"way\" k=\"building\" v=\"*\" zoom-min=\"16\"><area fill=\"#00000000\" stroke=\"").append(b)
                .append("\" stroke-width=\"0.5\" /></rule>\n");

        // railways
        x.append("<rule e=\"way\" k=\"railway\" v=\"rail|light_rail|subway\" zoom-min=\"13\"><line stroke=\"").append(b)
                .append("\" stroke-width=\"0.7\" stroke-dasharray=\"6, 5\" stroke-linecap=\"butt\" /></rule>\n");

        // small paths, then minor, middle and big roads (drawn in this order, so big roads end up on top)
        x.append("<rule e=\"way\" k=\"highway\" v=\"footway|path|cycleway|steps|bridleway\" zoom-min=\"16\"><line stroke=\"").append(b)
                .append("\" stroke-width=\"0.4\" stroke-linecap=\"round\" /></rule>\n");
        x.append("<rule e=\"way\" k=\"highway\" v=\"").append(minorRoads).append("\" zoom-min=\"13\"><line stroke=\"").append(r)
                .append("\" stroke-width=\"0.7\" stroke-linecap=\"round\" stroke-linejoin=\"round\" /></rule>\n");
        x.append("<rule e=\"way\" k=\"highway\" v=\"").append(midRoads).append("\"><line stroke=\"").append(r)
                .append("\" stroke-width=\"1.1\" stroke-linecap=\"round\" stroke-linejoin=\"round\" /></rule>\n");
        x.append("<rule e=\"way\" k=\"highway\" v=\"").append(bigRoads).append("\"><line stroke=\"").append(m)
                .append("\" stroke-width=\"1.6\" stroke-linecap=\"round\" stroke-linejoin=\"round\" /></rule>\n");

        // street names
        x.append("<rule e=\"way\" k=\"highway\" v=\"").append(bigRoads).append("|").append(midRoads)
                .append("\" zoom-min=\"14\"><pathText fill=\"").append(t).append("\" font-size=\"11\" font-style=\"bold\" k=\"name\" stroke=\"")
                .append(h).append("\" stroke-width=\"2.0\" repeat-gap=\"300\" repeat-start=\"40\" /></rule>\n");
        x.append("<rule e=\"way\" k=\"highway\" v=\"residential|unclassified|living_street\" zoom-min=\"16\"><pathText fill=\"")
                .append(t).append("\" font-size=\"10\" font-style=\"normal\" k=\"name\" stroke=\"").append(h)
                .append("\" stroke-width=\"2.0\" repeat-gap=\"250\" repeat-start=\"30\" /></rule>\n");

        // places
        String[][] places = {
                {"city", "6", "11", "16", "90"},
                {"town", "9", "13", "14", "80"},
                {"suburb", "12", "17", "13", "60"},
                {"village", "11", "16", "12", "55"},
                {"hamlet|neighbourhood|quarter", "14", "18", "12", "40"}
        };
        for (String[] p : places) {
            x.append("<rule e=\"node\" k=\"place\" v=\"").append(p[0]).append("\" zoom-min=\"").append(p[1])
                    .append("\"><caption fill=\"").append(t).append("\" font-size=\"").append(p[3])
                    .append("\" font-style=\"bold\" k=\"name\" priority=\"").append(p[4]).append("\" stroke=\"").append(h)
                    .append("\" stroke-width=\"2.0\" /></rule>\n");
        }
        x.append("<rule e=\"any\" k=\"natural|landuse\" v=\"water|reservoir|bay\" zoom-min=\"13\"><caption fill=\"").append(m)
                .append("\" font-size=\"11\" font-style=\"italic\" k=\"name\" priority=\"20\" stroke=\"").append(h)
                .append("\" stroke-width=\"2.0\" /></rule>\n");
        x.append("</rendertheme>\n");
        return x.toString();
    }
}

/** Fully offline vector map read from a .map file (no internet needed). */
final class MapsforgeHost implements MapHost {
    private final org.mapsforge.map.android.view.MapView mv;
    private Circle dot;
    private final boolean night;
    private final boolean lineArt;
    private final int accent;

    /**
     * @param themeFile line-art render theme written by the launcher, or null for the standard Mapsforge style
     */
    MapsforgeHost(Activity a, File mapFile, View.OnTouchListener touch, boolean night, File themeFile,
                  int accent, int zoom, float textScale) {
        this.night = night;
        this.accent = accent;
        AndroidGraphicFactory.createInstance(a.getApplication());
        mv = new org.mapsforge.map.android.view.MapView(a);
        mv.setClickable(true);
        mv.setBackgroundColor(night ? 0xFF000000 : 0xFFE9EDF1);
        mv.getMapScaleBar().setVisible(false);
        mv.setBuiltInZoomControls(false);
        mv.getModel().mapViewPosition.setZoomLevel((byte) zoom);
        mv.getModel().mapViewPosition.setCenter(new LatLong(6.9271, 79.8612));

        TileCache cache = AndroidUtil.createTileCache(a, themeFile != null ? "vezelmap-line" : "vezelmap",
                mv.getModel().displayModel.getTileSize(), 1f,
                mv.getModel().frameBufferModel.getOverdrawFactor());
        MapDataStore store = new MapFile(mapFile);
        TileRendererLayer layer = new TileRendererLayer(cache, store,
                mv.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        boolean usedLineArt = false;
        if (themeFile != null) {
            try {
                layer.setXmlRenderTheme(new ExternalRenderTheme(themeFile));
                usedLineArt = true;
            } catch (Throwable t) {
                layer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);
            }
        } else {
            layer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);
        }
        this.lineArt = usedLineArt;
        // bigger place and road names (looked up by name so a different version cannot break the build)
        try {
            TileRendererLayer.class.getMethod("setTextScale", float.class).invoke(layer, textScale);
        } catch (Throwable ignored) {
        }
        mv.getLayerManager().getLayers().add(layer);

        if (night && !lineArt) {
            Paint filter = new Paint();
            filter.setColorFilter(MapStyle.dark());
            mv.setLayerType(View.LAYER_TYPE_HARDWARE, filter);
        }
        mv.setOnTouchListener(touch);
    }

    @Override
    public View view() {
        return mv;
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
        try {
            mv.destroyAll();
            AndroidGraphicFactory.clearResourceMemoryCache();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void showCar() {
    }

    @Override
    public void setCar(double lat, double lon) {
        LatLong ll = new LatLong(lat, lon);
        if (dot == null) {
            org.mapsforge.core.graphics.Paint fill = AndroidGraphicFactory.INSTANCE.createPaint();
            org.mapsforge.core.graphics.Paint edge = AndroidGraphicFactory.INSTANCE.createPaint();
            if (lineArt) {
                fill.setColor(accent);
                edge.setColor(0xFFFFFFFF);
            } else {
                fill.setColor(night ? 0xFF000000 : 0xFF1A1A1A); // black becomes white after the dark filter
                edge.setColor(night ? 0xFFFFFFFF : 0xFF000000);
            }
            fill.setStyle(Style.FILL);
            edge.setStyle(Style.STROKE);
            edge.setStrokeWidth(4);
            dot = new Circle(ll, 22f, fill, edge);
            mv.getLayerManager().getLayers().add(dot);
        } else {
            dot.setLatLong(ll);
            dot.requestRedraw();
        }
    }

    @Override
    public void centerOn(double lat, double lon) {
        mv.getModel().mapViewPosition.setCenter(new LatLong(lat, lon));
    }

    @Override
    public double[] center() {
        LatLong c = mv.getModel().mapViewPosition.getCenter();
        return new double[]{c.latitude, c.longitude};
    }
}

/** Shown until an offline map file has been chosen. */
final class NoMapHost implements MapHost {
    private final FrameLayout box;

    NoMapHost(Context c, View.OnTouchListener touch, int top, int bottom, int textColor, String message) {
        box = new FrameLayout(c);
        box.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom}));
        TextView t = new TextView(c);
        t.setText(message);
        t.setTextColor(textColor);
        t.setTextSize(18);
        t.setGravity(Gravity.CENTER);
        box.addView(t, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        box.setOnTouchListener(touch);
    }

    @Override
    public View view() {
        return box;
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void showCar() {
    }

    @Override
    public void setCar(double lat, double lon) {
    }

    @Override
    public void centerOn(double lat, double lon) {
    }

    @Override
    public double[] center() {
        return null;
    }
}
