package com.kore.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;
import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.sources.GeoJsonSource;

import java.io.File;

/**
 * Optional 3D map: MapLibre Native reading an offline vector-tile file (.mbtiles, OpenMapTiles layout).
 * Roads and buildings follow the theme colours; buildings are extruded and the view can be tilted.
 * (No place names yet: that needs font files.)
 */
final class MapLibreHost implements MapHost {
    private final MapView mv;
    private MapLibreMap map;
    private Style style;
    private boolean started = false;
    private double carLat = Double.NaN, carLon = Double.NaN;
    private double wantLat = Double.NaN, wantLon = Double.NaN;
    private final double zoom;
    private final double tilt;

    private static String hex(int c) {
        return String.format("#%02X%02X%02X", Color.red(c), Color.green(c), Color.blue(c));
    }

    MapLibreHost(Activity a, File mbtiles, View.OnTouchListener touch, double zoom, double tilt,
                 int ground, int water, int road, int major, int building, int accent) throws Exception {
        this.zoom = zoom;
        this.tilt = tilt;
        MapLibre.getInstance(a);
        mv = new MapView(a);
        mv.setBackgroundColor(ground | 0xFF000000);
        mv.onCreate(null);
        mv.setOnTouchListener(touch);
        final String json = styleJson(mbtiles, ground, water, road, major, building, accent);
        mv.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapLibreMap m) {
                map = m;
                try {
                    m.getUiSettings().setAttributionEnabled(false);
                    m.getUiSettings().setLogoEnabled(false);
                    m.getUiSettings().setCompassEnabled(false);
                } catch (Throwable ignored) {
                }
                m.setStyle(new Style.Builder().fromJson(json), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(Style s) {
                        style = s;
                        double la = Double.isNaN(wantLat) ? 6.9271 : wantLat;
                        double lo = Double.isNaN(wantLon) ? 79.8612 : wantLon;
                        map.setCameraPosition(new CameraPosition.Builder()
                                .target(new LatLng(la, lo)).zoom(MapLibreHost.this.zoom)
                                .tilt(MapLibreHost.this.tilt).bearing(0).build());
                        pushCar();
                    }
                });
            }
        });
    }

    private static JSONArray arr(Object... v) throws Exception {
        JSONArray a = new JSONArray();
        for (Object o : v) a.put(o);
        return a;
    }

    private static String styleJson(File f, int ground, int water, int road, int major, int bld, int accent) throws Exception {
        JSONObject st = new JSONObject();
        st.put("version", 8);
        JSONObject sources = new JSONObject();
        sources.put("omt", new JSONObject().put("type", "vector").put("url", "mbtiles://" + f.getAbsolutePath()));
        sources.put("car", new JSONObject().put("type", "geojson")
                .put("data", new JSONObject().put("type", "FeatureCollection").put("features", new JSONArray())));
        st.put("sources", sources);
        JSONArray layers = new JSONArray();
        layers.put(new JSONObject().put("id", "bg").put("type", "background")
                .put("paint", new JSONObject().put("background-color", hex(ground))));
        layers.put(new JSONObject().put("id", "water").put("type", "fill").put("source", "omt").put("source-layer", "water")
                .put("paint", new JSONObject().put("fill-color", hex(water))));
        layers.put(new JSONObject().put("id", "roads").put("type", "line").put("source", "omt").put("source-layer", "transportation")
                .put("filter", arr("!in", "class", "motorway", "trunk", "primary", "secondary", "rail", "transit", "path", "pier"))
                .put("layout", new JSONObject().put("line-cap", "round").put("line-join", "round"))
                .put("paint", new JSONObject().put("line-color", hex(road))
                        .put("line-width", arr("interpolate", arr("exponential", 1.4), arr("zoom"), 10, 0.4, 16, 2.0, 19, 6.0))));
        layers.put(new JSONObject().put("id", "major").put("type", "line").put("source", "omt").put("source-layer", "transportation")
                .put("filter", arr("in", "class", "motorway", "trunk", "primary", "secondary"))
                .put("layout", new JSONObject().put("line-cap", "round").put("line-join", "round"))
                .put("paint", new JSONObject().put("line-color", hex(major))
                        .put("line-width", arr("interpolate", arr("exponential", 1.4), arr("zoom"), 8, 0.6, 16, 3.0, 19, 9.0))));
        layers.put(new JSONObject().put("id", "bld3d").put("type", "fill-extrusion").put("source", "omt").put("source-layer", "building")
                .put("minzoom", 14)
                .put("paint", new JSONObject().put("fill-extrusion-color", hex(bld))
                        .put("fill-extrusion-height", arr("coalesce", arr("get", "render_height"), 8))
                        .put("fill-extrusion-base", arr("coalesce", arr("get", "render_min_height"), 0))
                        .put("fill-extrusion-opacity", 0.92)));
        layers.put(new JSONObject().put("id", "car").put("type", "circle").put("source", "car")
                .put("paint", new JSONObject().put("circle-radius", 9).put("circle-color", hex(accent))
                        .put("circle-stroke-color", "#FFFFFF").put("circle-stroke-width", 2.5)));
        st.put("layers", layers);
        return st.toString();
    }

    private void pushCar() {
        if (style == null || Double.isNaN(carLat)) return;
        try {
            GeoJsonSource src = style.getSourceAs("car");
            if (src != null) {
                src.setGeoJson("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":["
                        + carLon + "," + carLat + "]}}");
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public View view() {
        return mv;
    }

    @Override
    public void onResume() {
        if (!started) {
            started = true;
            mv.onStart();
        }
        mv.onResume();
    }

    @Override
    public void onPause() {
        mv.onPause();
        if (started) {
            started = false;
            mv.onStop();
        }
    }

    @Override
    public void onDestroy() {
        try {
            mv.onDestroy();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void showCar() {
    }

    @Override
    public void setCar(double lat, double lon) {
        carLat = lat;
        carLon = lon;
        pushCar();
    }

    @Override
    public void centerOn(double lat, double lon) {
        wantLat = lat;
        wantLon = lon;
        if (map != null && style != null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
        }
    }

    @Override
    public double[] center() {
        if (map != null) {
            LatLng t = map.getCameraPosition().target;
            if (t != null) return new double[]{t.getLatitude(), t.getLongitude()};
        }
        return new double[]{Double.isNaN(wantLat) ? 6.9271 : wantLat, Double.isNaN(wantLon) ? 79.8612 : wantLon};
    }
}
