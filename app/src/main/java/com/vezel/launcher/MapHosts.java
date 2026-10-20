package com.vezel.launcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;

/** The three ways the top area can show a map. */
interface MapHost {
    /** A normal phone-Chrome identity so web pages do not treat the launcher as an embedded web view. */
    String CHROME_UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/124.0.0.0 Mobile Safari/537.36";

    View view();

    void onResume();

    void onPause();

    void onDestroy();

    /** Start showing the car position (OpenStreetMap does this by itself). */
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

    /** Dark, greyscale, slightly dimmed look. */
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

/** Online OpenStreetMap tiles (cached while you drive). */
final class OsmHost implements MapHost {
    private final Context ctx;
    private final MapView map;
    private MyLocationNewOverlay overlay;

    OsmHost(Context c, View.OnTouchListener touch, boolean night, double zoom, float textScale) {
        ctx = c;
        map = new MapView(c);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.getController().setZoom(zoom);
        // bigger map text: scale the tiles (looked up by name so a different osmdroid version cannot break the build)
        try {
            map.setTilesScaledToDpi(true);
            MapView.class.getMethod("setTilesScaleFactor", float.class).invoke(map, textScale);
        } catch (Throwable ignored) {
        }
        map.getController().setCenter(new GeoPoint(6.9271, 79.8612));
        if (night) map.getOverlayManager().getTilesOverlay().setColorFilter(MapStyle.dark());
        map.setOnTouchListener(touch);
    }

    @Override
    public View view() {
        return map;
    }

    @Override
    public void onResume() {
        map.onResume();
    }

    @Override
    public void onPause() {
        map.onPause();
    }

    @Override
    public void onDestroy() {
        map.onDetach();
    }

    @Override
    public void showCar() {
        if (overlay != null) return;
        overlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        overlay.enableMyLocation();
        map.getOverlays().add(overlay);
    }

    @Override
    public void setCar(double lat, double lon) {
        // the overlay follows the GPS by itself
    }

    @Override
    public void centerOn(double lat, double lon) {
        map.getController().animateTo(new GeoPoint(lat, lon));
    }

    @Override
    public double[] center() {
        GeoPoint c = (GeoPoint) map.getMapCenter();
        return new double[]{c.getLatitude(), c.getLongitude()};
    }
}

/** Fully offline vector map read from a .map file (no internet needed). */
final class MapsforgeHost implements MapHost {
    private final org.mapsforge.map.android.view.MapView mv;
    private Circle dot;
    private final boolean night;

    MapsforgeHost(Activity a, File mapFile, View.OnTouchListener touch, boolean night, int zoom, float textScale) {
        this.night = night;
        AndroidGraphicFactory.createInstance(a.getApplication());
        mv = new org.mapsforge.map.android.view.MapView(a);
        mv.setClickable(true);
        mv.getMapScaleBar().setVisible(false);
        mv.setBuiltInZoomControls(false);
        mv.getModel().mapViewPosition.setZoomLevel((byte) zoom);
        mv.getModel().mapViewPosition.setCenter(new LatLong(6.9271, 79.8612));

        TileCache cache = AndroidUtil.createTileCache(a, "vezelmap",
                mv.getModel().displayModel.getTileSize(), 1f,
                mv.getModel().frameBufferModel.getOverdrawFactor());
        MapDataStore store = new MapFile(mapFile);
        TileRendererLayer layer = new TileRendererLayer(cache, store,
                mv.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        layer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);
        // bigger place and road names (looked up by name so a different version cannot break the build)
        try {
            TileRendererLayer.class.getMethod("setTextScale", float.class).invoke(layer, textScale);
        } catch (Throwable ignored) {
        }
        mv.getLayerManager().getLayers().add(layer);

        if (night) {
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
            fill.setColor(night ? 0xFF000000 : 0xFF1A1A1A); // black becomes white after the dark filter
            fill.setStyle(Style.FILL);
            org.mapsforge.core.graphics.Paint edge = AndroidGraphicFactory.INSTANCE.createPaint();
            edge.setColor(night ? 0xFFFFFFFF : 0xFF000000);
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

/** No map inside the launcher: navigation is done in the Google Maps app. */
final class NoMapHost implements MapHost {
    private final FrameLayout box;

    NoMapHost(Context c, View.OnTouchListener touch) {
        box = new FrameLayout(c);
        box.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF16181B, 0xFF050607}));
        TextView t = new TextView(c);
        t.setText("Navigation runs in Google Maps\nTap the map icon below to open it");
        t.setTextColor(0xFF9BA1A8);
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        lp.leftMargin = 360;
        box.addView(t, lp);
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

/** A web page as the map (for example Google Maps in the browser engine). Needs internet. */
final class WebMapHost implements MapHost {
    private final WebView web;

    WebMapHost(Context c, View.OnTouchListener touch, String url) {
        web = new WebView(c);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setUserAgentString(CHROME_UA);
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                cb.invoke(origin, true, false);
            }
        });
        web.setOnTouchListener(touch);
        web.loadUrl(url == null || url.length() == 0 ? "https://www.google.com/maps" : url);
    }

    @Override
    public View view() {
        return web;
    }

    @Override
    public void onResume() {
        web.onResume();
    }

    @Override
    public void onPause() {
        web.onPause();
    }

    @Override
    public void onDestroy() {
        try {
            web.destroy();
        } catch (Exception ignored) {
        }
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
