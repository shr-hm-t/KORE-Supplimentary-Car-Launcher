package com.vezel.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    // design language: black, grey, cool-white chrome
    private static final int WHITE = 0xFFF2F5F8;
    private static final int GREY = 0xFF9BA1A8;
    private static final int LINE = 0xFF3A3F45;
    private static final int CHROME = 0xFFE2E8EC;

    // vertical layout weights (must add up to 100)
    private static final float TOP_W = 50f, MID_W = 38f, DOCK_W = 12f;
    private static final float PIP_FRAC = 0.423f;

    private static boolean bootHandled = false;

    private SharedPreferences cfg;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MapView map;
    private MyLocationNewOverlay locOverlay;
    private LocationManager lm;
    private boolean locationStarted = false;
    private Location lastLoc;
    private long lastTouch = 0;

    private ImageView navArrow;
    private TextView navTitle, navText, cityText, subText, hintText;
    private View lightGlow, lightLine;
    private TextView[] contactInitial = new TextView[3];
    private FrameLayout ytPane;
    private ImageView ytImage;
    private TextView ytTitle;
    private TextView[] tyre = new TextView[4];
    private Glyph radioGlyph;

    private long lastGeoTime = 0;
    private Location lastGeoLoc;

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        cfg = getSharedPreferences("cfg", MODE_PRIVATE);
        Configuration c = Configuration.getInstance();
        c.load(this, getSharedPreferences("osm", MODE_PRIVATE));
        c.setUserAgentValue(getPackageName());
        c.setTileFileSystemCacheMaxBytes(1500L * 1024 * 1024);
        c.setTileFileSystemCacheTrimBytes(1200L * 1024 * 1024);
        c.setExpirationOverrideDuration(1000L * 60 * 60 * 24 * 60); // keep tiles 60 days
        buildUi();
        requestPerms();

        if (!bootHandled) {
            bootHandled = true;
            final int secs = cfg.getInt("autoRadio", 0);
            if (secs > 0 && cfg.getString("pkgRadio", "").length() > 0) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        launchPref("pkgRadio");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent(MainActivity.this, MainActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivity(i);
                            }
                        }, 3500);
                    }
                }, secs * 1000L);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyStatusBar();
        map.onResume();
        NavListener.onChange = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshNav();
                    }
                });
            }
        };
        refreshNav();
        refreshContacts();
        startLocation();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        // back at home screen: re-centre straight away
        lastTouch = 0;
        recentre();

        if (getIntent() != null && getIntent().getBooleanExtra("autocall", false)) {
            getIntent().removeExtra("autocall");
            callContact(cfg.getInt("primary", 0));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        NavListener.onChange = null;
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocation();
    }

    /** The back button must not restart the home screen. */
    @Override
    public void onBackPressed() {
        // intentionally empty
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // steering wheel "voice" button
        if (keyCode == KeyEvent.KEYCODE_VOICE_ASSIST || keyCode == KeyEvent.KEYCODE_CALL) {
            callContact(cfg.getInt("primary", 0));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void applyStatusBar() {
        boolean show = cfg.getBoolean("showStatus", false);
        Window w = getWindow();
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (show) {
            w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            if (Build.VERSION.SDK_INT >= 21) w.setStatusBarColor(Color.BLACK);
        } else {
            w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        w.getDecorView().setSystemUiVisibility(flags);
    }

    // ------------------------------------------------------------------ periodic work

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            recentre();
            if (ytPane != null && ytPane.getVisibility() == View.VISIBLE) refreshYt();
            handler.postDelayed(this, 2000);
        }
    };

    /** Re-centre the map on the car when nobody has touched it for 10 seconds. */
    private void recentre() {
        if (lastLoc == null || map == null) return;
        if (System.currentTimeMillis() - lastTouch < 10000) return;
        GeoPoint gp = new GeoPoint(lastLoc.getLatitude(), lastLoc.getLongitude());
        GeoPoint c = (GeoPoint) map.getMapCenter();
        if (Math.abs(c.getLatitude() - gp.getLatitude()) > 0.00005
                || Math.abs(c.getLongitude() - gp.getLongitude()) > 0.00005) {
            map.getController().animateTo(gp);
        }
    }

    // ------------------------------------------------------------------ permissions

    private void requestPerms() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CALL_PHONE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        startLocation();
    }

    private boolean has(String perm) {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
    }

    // ------------------------------------------------------------------ UI

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams weight(float w, boolean horizontalParent) {
        if (horizontalParent) return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, w);
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, w);
    }

    private TextView text(int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setShadowLayer(6, 0, 1, 0xFF000000);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private void buildUi() {
        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        rootFrame.addView(root, new FrameLayout.LayoutParams(-1, -1));

        // ---------- top: live map with overlays
        FrameLayout top = new FrameLayout(this);
        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.getController().setZoom(16.0);
        map.getController().setCenter(new GeoPoint(6.9271, 79.8612)); // Colombo until the first GPS fix
        // dark + grey + slightly dimmed: calm, low-detail look, no blue/pink tint
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
        map.getOverlayManager().getTilesOverlay().setColorFilter(new ColorMatrixColorFilter(cm));
        map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                lastTouch = System.currentTimeMillis();
                return false;
            }
        });
        top.addView(map, new FrameLayout.LayoutParams(-1, -1));

        // YouTube thumbnail pane (replaces the map when switched on)
        ytPane = new FrameLayout(this);
        ytPane.setBackgroundColor(Color.BLACK);
        ytPane.setVisibility(View.GONE);
        ytImage = new ImageView(this);
        ytImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ytPane.addView(ytImage, new FrameLayout.LayoutParams(-1, -1));
        ytTitle = text(18, WHITE, true);
        ytTitle.setPadding(dp(16), dp(10), dp(16), dp(10));
        ytTitle.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xE0000000, 0x00000000}));
        ytTitle.setGravity(Gravity.BOTTOM);
        ytPane.addView(ytTitle, new FrameLayout.LayoutParams(-1, dp(90), Gravity.BOTTOM));
        ytPane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // tap the thumbnail to open YouTube
                Intent i = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            }
        });
        top.addView(ytPane, new FrameLayout.LayoutParams(-1, -1));

        // speed light from the top edge (yellow from 80 km/h, red from 120 km/h)
        lightGlow = new View(this);
        top.addView(lightGlow, new FrameLayout.LayoutParams(-1, dp(70), Gravity.TOP));
        lightLine = new View(this);
        top.addView(lightLine, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));
        setLight(0);

        // navigation + city panel (sharp, flush left)
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setBackgroundColor(0xB8000000);
        info.setPadding(dp(18), dp(14), dp(12), dp(10));
        navArrow = new ImageView(this);
        info.addView(navArrow, new LinearLayout.LayoutParams(dp(96), dp(96)));
        navTitle = text(30, Color.WHITE, true);
        navText = text(20, 0xFFC4CAD0, false);
        info.addView(navTitle);
        info.addView(navText);
        View spacer = new View(this);
        info.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
        cityText = text(30, Color.WHITE, true);
        subText = text(16, GREY, false);
        info.addView(cityText);
        info.addView(subText);
        hintText = text(13, 0xFFD0D6DC, false);
        hintText.setText("Tap here to enable navigation info");
        hintText.setPadding(0, dp(8), 0, 0);
        hintText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        info.addView(hintText);
        top.addView(info, new FrameLayout.LayoutParams(dp(340), -1));

        root.addView(top, weight(TOP_W, false));

        // ---------- middle: tyre-pressure slot + 2x4 grid
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.HORIZONTAL);
        mid.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF16181B, 0xFF050607}));

        FrameLayout pip = new FrameLayout(this);
        pip.setBackgroundColor(0xFF050607);
        LinearLayout tg = new LinearLayout(this);
        tg.setOrientation(LinearLayout.VERTICAL);
        tg.setGravity(Gravity.CENTER);
        String[] names = {"FL", "FR", "RL", "RR"};
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int k = 0; k < 2; k++) {
                int idx = r * 2 + k;
                TextView t = text(26, WHITE, true);
                t.setGravity(Gravity.CENTER);
                t.setText(names[idx] + "  --");
                tyre[idx] = t;
                row.addView(t, new LinearLayout.LayoutParams(dp(150), dp(56)));
            }
            tg.addView(row);
        }
        pip.addView(tg, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        pip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPref("pkg360");
            }
        });
        pip.setOnLongClickListener(settingsLongClick());
        mid.addView(pip, weight(PIP_FRAC * 100f, true));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row1 = new LinearLayout(this);
        LinearLayout row2 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        // radio: tap = mute / unmute, hold = open radio
        FrameLayout radioCell = iconCell(Glyph.RADIO, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMute();
            }
        }, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                launchPref("pkgRadio");
                return true;
            }
        });
        radioGlyph = (Glyph) radioCell.getChildAt(0);
        row1.addView(radioCell, weight(1f, true));
        row1.addView(iconCell(Glyph.PREV, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
        }, null), weight(1f, true));
        row1.addView(iconCell(Glyph.PLAY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unmuteIfMuted();
                mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
        }, null), weight(1f, true));
        row1.addView(iconCell(Glyph.NEXT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            }
        }, null), weight(1f, true));

        row2.addView(iconCell(Glyph.R360, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPref("pkg360");
            }
        }, settingsLongClick()), weight(1f, true));
        for (int i = 0; i < 3; i++) {
            row2.addView(contactCell(i), weight(1f, true));
        }

        grid.addView(row1, weight(1f, false));
        grid.addView(row2, weight(1f, false));
        mid.addView(grid, weight(100f - PIP_FRAC * 100f, true));

        root.addView(mid, weight(MID_W, false));

        // ---------- dock: clock under the slot, four icons under the grid
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setBackgroundColor(Color.BLACK);

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(WHITE);
        clock.setTextSize(30);
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        clock.setGravity(Gravity.CENTER);
        dock.addView(clock, weight(PIP_FRAC * 100f, true));

        LinearLayout dockIcons = new LinearLayout(this);
        dockIcons.setOrientation(LinearLayout.HORIZONTAL);
        dockIcons.addView(dockCell(Glyph.YT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleYt();
            }
        }, null), weight(1f, true));
        dockIcons.addView(dockCell(Glyph.DRAWER, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDrawer();
            }
        }, null), weight(1f, true));
        dockIcons.addView(dockCell(Glyph.MAP, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMapsApp();
            }
        }, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                searchDestination();
                return true;
            }
        }), weight(1f, true));
        dockIcons.addView(dockCell(Glyph.GEAR, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editSettings();
            }
        }, null), weight(1f, true));
        dock.addView(dockIcons, weight(100f - PIP_FRAC * 100f, true));
        root.addView(dock, weight(DOCK_W, false));

        // chrome frame + software rounded corners on top of everything
        rootFrame.addView(new Overlay(this), new FrameLayout.LayoutParams(-1, -1));
        setContentView(rootFrame);
    }

    private FrameLayout cell() {
        FrameLayout f = new FrameLayout(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x00000000);
        g.setStroke(1, LINE);
        f.setBackground(g);
        f.setClickable(true);
        return f;
    }

    private FrameLayout iconCell(int glyph, View.OnClickListener click, View.OnLongClickListener lc) {
        FrameLayout f = cell();
        Glyph g = new Glyph(this, glyph);
        f.addView(g, new FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER));
        f.setOnClickListener(click);
        if (lc != null) f.setOnLongClickListener(lc);
        return f;
    }

    private FrameLayout dockCell(int glyph, View.OnClickListener click, View.OnLongClickListener lc) {
        FrameLayout f = new FrameLayout(this);
        f.setClickable(true);
        f.addView(new Glyph(this, glyph), new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER));
        f.setOnClickListener(click);
        if (lc != null) f.setOnLongClickListener(lc);
        return f;
    }

    private FrameLayout contactCell(final int i) {
        FrameLayout f = cell();
        TextView c = new TextView(this);
        c.setGravity(Gravity.CENTER);
        c.setTextColor(WHITE);
        c.setTextSize(30);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(0xFF15171A);
        ring.setStroke(dp(2), CHROME);
        c.setBackground(ring);
        f.addView(c, new FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER));
        contactInitial[i] = c;
        f.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callContact(i);
            }
        });
        f.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editContact(i);
                return true;
            }
        });
        return f;
    }

    private View.OnLongClickListener settingsLongClick() {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editSettings();
                return true;
            }
        };
    }

    // ------------------------------------------------------------------ navigation info

    private void refreshNav() {
        CharSequence t = NavListener.title;
        CharSequence x = NavListener.text;
        navTitle.setText(t == null ? "" : t);
        navText.setText(x == null ? "" : x);
        if (NavListener.icon != null) {
            navArrow.setImageBitmap(NavListener.icon);
            navArrow.setVisibility(View.VISIBLE);
        } else {
            navArrow.setImageDrawable(null);
            navArrow.setVisibility(View.INVISIBLE);
        }
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean on = enabled != null && enabled.contains(getPackageName());
        hintText.setVisibility(on ? View.GONE : View.VISIBLE);
    }

    // ------------------------------------------------------------------ location / speed light

    private void startLocation() {
        if (locationStarted || !has(Manifest.permission.ACCESS_FINE_LOCATION)) return;
        try {
            lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locListener);
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, locListener);
            locOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            locOverlay.enableMyLocation(); // marker only, centring is handled by recentre()
            map.getOverlays().add(locOverlay);
            locationStarted = true;
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission needed", Toast.LENGTH_LONG).show();
        }
    }

    private void stopLocation() {
        try {
            if (lm != null) lm.removeUpdates(locListener);
        } catch (SecurityException ignored) {
        }
    }

    private final LocationListener locListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            if (loc == null) return;
            lastLoc = loc;
            float kmh = loc.hasSpeed() ? loc.getSpeed() * 3.6f : 0f;
            setLight(kmh);
            recentre();
            long now = System.currentTimeMillis();
            boolean moved = lastGeoLoc == null || loc.distanceTo(lastGeoLoc) > 250;
            if ((moved && now - lastGeoTime > 8000) || now - lastGeoTime > 120000) {
                lastGeoTime = now;
                lastGeoLoc = loc;
                reverseGeocode(loc.getLatitude(), loc.getLongitude());
            }
        }

        @Override
        public void onStatusChanged(String p, int s, Bundle e) {
        }

        @Override
        public void onProviderEnabled(String p) {
        }

        @Override
        public void onProviderDisabled(String p) {
        }
    };

    private void setLight(float kmh) {
        int col;
        if (kmh >= 120f) col = 0xFFFF3B30;
        else if (kmh >= 80f) col = 0xFFFFBE1E;
        else col = 0xFFE6ECF2;
        int glowA = kmh >= 80f ? 0xB0 : 0x28;
        int c = (glowA << 24) | (col & 0x00FFFFFF);
        lightGlow.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{c, 0x00000000}));
        lightLine.setBackgroundColor(kmh >= 80f ? col : 0x40E6ECF2);
    }

    // ------------------------------------------------------------------ OpenStreetMap (Nominatim)

    private static String http(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestProperty("User-Agent", "VezelLauncher/0.2 (personal car launcher)");
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }

    private void reverseGeocode(final double lat, final double lon) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String s = http("https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=16&lat="
                            + lat + "&lon=" + lon + "&accept-language=en");
                    JSONObject a = new JSONObject(s).getJSONObject("address");
                    String city = firstOf(a, "city", "town", "village", "municipality", "county");
                    String sub = firstOf(a, "suburb", "neighbourhood", "quarter", "road");
                    final String c = city;
                    final String sb = sub;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (c.length() > 0) cityText.setText(c);
                            subText.setText(sb);
                        }
                    });
                } catch (Exception ignored) {
                    // offline: keep the last name
                }
            }
        }).start();
    }

    private static String firstOf(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = o.optString(k, "");
            if (v.length() > 0) return v;
        }
        return "";
    }

    // ------------------------------------------------------------------ map button / destination search

    private void openMapsApp() {
        Intent i = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
        if (i == null) {
            Toast.makeText(this, "Google Maps not found", Toast.LENGTH_LONG).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void searchDestination() {
        final EditText e = new EditText(this);
        e.setHint("Where to?");
        e.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Navigate to")
                .setView(e)
                .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        runSearch(e.getText().toString().trim());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runSearch(final String q) {
        if (q.length() == 0) return;
        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=6&q="
                            + URLEncoder.encode(q, "UTF-8");
                    if (lastLoc != null) {
                        double la = lastLoc.getLatitude(), lo = lastLoc.getLongitude();
                        url += "&viewbox=" + (lo - 1) + "," + (la + 1) + "," + (lo + 1) + "," + (la - 1);
                    }
                    JSONArray arr = new JSONArray(http(url));
                    final String[] names = new String[arr.length()];
                    final double[] lats = new double[arr.length()];
                    final double[] lons = new double[arr.length()];
                    for (int k = 0; k < arr.length(); k++) {
                        JSONObject o = arr.getJSONObject(k);
                        names[k] = o.optString("display_name", "?");
                        lats[k] = Double.parseDouble(o.getString("lat"));
                        lons[k] = Double.parseDouble(o.getString("lon"));
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (names.length == 0) {
                                Toast.makeText(MainActivity.this, "Nothing found", Toast.LENGTH_LONG).show();
                                return;
                            }
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Start navigation to")
                                    .setItems(names, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int which) {
                                            navigateTo(lats[which], lons[which]);
                                        }
                                    })
                                    .show();
                        }
                    });
                } catch (Exception ex) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Search needs internet", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void navigateTo(double lat, double lon) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + lat + "," + lon));
        i.setPackage("com.google.android.apps.maps");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Google Maps not found", Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------ YouTube thumbnail

    private void toggleYt() {
        if (ytPane.getVisibility() == View.VISIBLE) {
            ytPane.setVisibility(View.GONE);
        } else {
            ytPane.setVisibility(View.VISIBLE);
            refreshYt();
        }
    }

    private void refreshYt() {
        try {
            MediaSessionManager msm = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> list = msm.getActiveSessions(new ComponentName(this, NavListener.class));
            for (MediaController mc : list) {
                String p = mc.getPackageName();
                if (p == null || !p.contains("youtube")) continue;
                MediaMetadata md = mc.getMetadata();
                if (md == null) continue;
                Bitmap b = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                if (b != null) ytImage.setImageBitmap(b);
                CharSequence t = md.getText(MediaMetadata.METADATA_KEY_TITLE);
                ytTitle.setText(t == null ? "" : t);
                return;
            }
            ytImage.setImageDrawable(null);
            ytTitle.setText("Nothing playing in YouTube");
        } catch (SecurityException e) {
            ytTitle.setText("Enable navigation info access first (tap the hint on the map)");
        } catch (Exception e) {
            ytTitle.setText("YouTube info unavailable");
        }
    }

    // ------------------------------------------------------------------ audio / actions

    private void mediaKey(int code) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private void toggleMute() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean muted = Build.VERSION.SDK_INT >= 23 && am.isStreamMute(AudioManager.STREAM_MUSIC);
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                muted ? AudioManager.ADJUST_UNMUTE : AudioManager.ADJUST_MUTE, 0);
        Toast.makeText(this, muted ? "Sound on" : "Muted", Toast.LENGTH_SHORT).show();
        if (radioGlyph != null) radioGlyph.setAlpha(muted ? 1f : 0.4f);
    }

    private void unmuteIfMuted() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= 23 && am.isStreamMute(AudioManager.STREAM_MUSIC)) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            if (radioGlyph != null) radioGlyph.setAlpha(1f);
        }
    }

    private void launchPref(String key) {
        String pkg = cfg.getString(key, "");
        if (pkg.length() == 0) {
            Toast.makeText(this, "Choose the app in settings (gear icon)", Toast.LENGTH_LONG).show();
            editSettings();
            return;
        }
        launchPackage(pkg);
    }

    private void launchPackage(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            Toast.makeText(this, "App not found: " + pkg, Toast.LENGTH_LONG).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    // ------------------------------------------------------------------ app drawer / app picker

    private interface Pick {
        void onPick(String pkg, String label);
    }

    private List<ResolveInfo> launchableApps() {
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        final PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());
            }
        });
        return apps;
    }

    private void chooseApp(String title, final Pick cb) {
        final List<ResolveInfo> apps = launchableApps();
        final PackageManager pm = getPackageManager();
        BaseAdapter ad = new BaseAdapter() {
            @Override
            public int getCount() {
                return apps.size();
            }

            @Override
            public Object getItem(int p) {
                return apps.get(p);
            }

            @Override
            public long getItemId(int p) {
                return p;
            }

            @Override
            public View getView(int p, View v, ViewGroup parent) {
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(14), dp(8), dp(14), dp(8));
                ImageView iv = new ImageView(MainActivity.this);
                ResolveInfo ri = apps.get(p);
                iv.setImageDrawable(ri.loadIcon(pm));
                row.addView(iv, new LinearLayout.LayoutParams(dp(44), dp(44)));
                TextView tv = new TextView(MainActivity.this);
                tv.setText(ri.loadLabel(pm));
                tv.setTextSize(18);
                tv.setPadding(dp(14), 0, 0, 0);
                row.addView(tv);
                return row;
            }
        };
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setAdapter(ad, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        ResolveInfo ri = apps.get(which);
                        cb.onPick(ri.activityInfo.packageName, ri.loadLabel(pm).toString());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDrawer() {
        chooseApp("Apps", new Pick() {
            @Override
            public void onPick(String pkg, String label) {
                launchPackage(pkg);
            }
        });
    }

    // ------------------------------------------------------------------ contacts / calls

    private void refreshContacts() {
        String[] defName = {"Dad", "Amma", "Sis"};
        for (int i = 0; i < 3; i++) {
            String n = cfg.getString("name" + i, defName[i]);
            contactInitial[i].setText(n.length() > 0 ? n.substring(0, 1).toUpperCase() : "?");
        }
    }

    private void callContact(int i) {
        String[] defName = {"Dad", "Amma", "Sis"};
        String name = cfg.getString("name" + i, defName[i]);
        String num = cfg.getString("num" + i, "");
        if (num.length() == 0) {
            editContact(i);
            return;
        }
        int mode = cfg.getInt("callMode", 0);
        if (mode == 2) {
            String action = cfg.getString("macroAction", "");
            if (action.length() == 0) {
                Toast.makeText(this, "Set the macro action in settings", Toast.LENGTH_LONG).show();
                return;
            }
            Intent bi = new Intent(action);
            bi.putExtra("name", name);
            bi.putExtra("number", num);
            sendBroadcast(bi);
            return;
        }
        Uri uri = Uri.parse("tel:" + Uri.encode(num));
        Intent call;
        if (mode == 0 && has(Manifest.permission.CALL_PHONE)) call = new Intent(Intent.ACTION_CALL, uri);
        else call = new Intent(Intent.ACTION_DIAL, uri);
        call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(call);
        } catch (Exception e) {
            Toast.makeText(this, "No phone app found for calls", Toast.LENGTH_LONG).show();
        }
    }

    private EditText field(LinearLayout parent, String hint, String value, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setInputType(inputType);
        e.setSingleLine(true);
        parent.addView(e);
        return e;
    }

    private void editContact(final int i) {
        String[] defName = {"Dad", "Amma", "Sis"};
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(10), dp(20), 0);
        final EditText n = field(l, "Name", cfg.getString("name" + i, defName[i]), InputType.TYPE_CLASS_TEXT);
        final EditText p = field(l, "Phone number", cfg.getString("num" + i, ""), InputType.TYPE_CLASS_PHONE);
        final CheckBox prim = new CheckBox(this);
        prim.setText("Primary contact (steering wheel voice button calls this)");
        prim.setChecked(cfg.getInt("primary", 0) == i);
        l.addView(prim);
        new AlertDialog.Builder(this)
                .setTitle("Contact " + (i + 1))
                .setView(l)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        SharedPreferences.Editor ed = cfg.edit();
                        ed.putString("name" + i, n.getText().toString().trim());
                        ed.putString("num" + i, p.getText().toString().trim());
                        if (prim.isChecked()) ed.putInt("primary", i);
                        ed.apply();
                        refreshContacts();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------------ settings

    private String appLabel(String pkg) {
        if (pkg == null || pkg.length() == 0) return "not set";
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private void editSettings() {
        final String[] radioPkg = {cfg.getString("pkgRadio", "")};
        final String[] camPkg = {cfg.getString("pkg360", "")};

        ScrollView sv = new ScrollView(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(10), dp(20), dp(10));
        sv.addView(l);

        final Button radioBtn = new Button(this);
        radioBtn.setText("Radio app: " + appLabel(radioPkg[0]));
        radioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseApp("Radio app", new Pick() {
                    @Override
                    public void onPick(String pkg, String label) {
                        radioPkg[0] = pkg;
                        radioBtn.setText("Radio app: " + label);
                    }
                });
            }
        });
        l.addView(radioBtn);

        final Button camBtn = new Button(this);
        camBtn.setText("360 camera app: " + appLabel(camPkg[0]));
        camBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseApp("360 camera app", new Pick() {
                    @Override
                    public void onPick(String pkg, String label) {
                        camPkg[0] = pkg;
                        camBtn.setText("360 camera app: " + label);
                    }
                });
            }
        });
        l.addView(camBtn);

        TextView mt = new TextView(this);
        mt.setText("\nCalling");
        l.addView(mt);
        final RadioGroup rg = new RadioGroup(this);
        String[] modes = {"Direct call (Bluetooth / SIM)", "Open dial screen", "Macro broadcast"};
        for (int k = 0; k < 3; k++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(modes[k]);
            rb.setId(1000 + k);
            rg.addView(rb);
        }
        rg.check(1000 + cfg.getInt("callMode", 0));
        l.addView(rg);
        final EditText macro = field(l, "Macro broadcast action (only for macro mode)", cfg.getString("macroAction", ""), InputType.TYPE_CLASS_TEXT);

        final CheckBox status = new CheckBox(this);
        status.setText("Show status bar (with black background)");
        status.setChecked(cfg.getBoolean("showStatus", false));
        l.addView(status);

        final EditText corner = field(l, "Screen corner roundness in dp (0 = square)", String.valueOf(cfg.getInt("corner", 30)), InputType.TYPE_CLASS_NUMBER);
        final EditText auto = field(l, "Open radio this many seconds after boot (0 = off)", String.valueOf(cfg.getInt("autoRadio", 0)), InputType.TYPE_CLASS_NUMBER);

        Button dl = new Button(this);
        dl.setText("Download offline map around here");
        dl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadOffline();
            }
        });
        l.addView(dl);

        new AlertDialog.Builder(this)
                .setTitle("Launcher settings")
                .setView(sv)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        int m = rg.getCheckedRadioButtonId() - 1000;
                        if (m < 0 || m > 2) m = 0;
                        int cr = 30, ar = 0;
                        try {
                            cr = Integer.parseInt(corner.getText().toString().trim());
                        } catch (Exception ignored) {
                        }
                        try {
                            ar = Integer.parseInt(auto.getText().toString().trim());
                        } catch (Exception ignored) {
                        }
                        cfg.edit().putString("pkgRadio", radioPkg[0])
                                .putString("pkg360", camPkg[0])
                                .putInt("callMode", m)
                                .putString("macroAction", macro.getText().toString().trim())
                                .putBoolean("showStatus", status.isChecked())
                                .putInt("corner", cr)
                                .putInt("autoRadio", ar).apply();
                        applyStatusBar();
                        map.getRootView().invalidate();
                        View rv = findViewById(android.R.id.content);
                        if (rv != null) rv.invalidate();
                        recreate();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void downloadOffline() {
        if (lastLoc == null) {
            Toast.makeText(this, "Wait for a GPS fix first", Toast.LENGTH_LONG).show();
            return;
        }
        double la = lastLoc.getLatitude(), lo = lastLoc.getLongitude();
        BoundingBox bb = new BoundingBox(la + 0.12, lo + 0.12, la - 0.12, lo - 0.12);
        try {
            CacheManager mgr = new CacheManager(map);
            mgr.downloadAreaAsync(this, bb, 10, 16, new CacheManager.CacheManagerCallback() {
                @Override
                public void onTaskComplete() {
                    Toast.makeText(MainActivity.this, "Offline map ready", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onTaskFailed(int errors) {
                    Toast.makeText(MainActivity.this, "Some tiles failed: " + errors, Toast.LENGTH_LONG).show();
                }

                @Override
                public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {
                }

                @Override
                public void downloadStarted() {
                }

                @Override
                public void setPossibleTilesInArea(int total) {
                }
            });
            Toast.makeText(this, "Downloading map in the background...", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Download failed", Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------ overlay: chrome lines + rounded screen corners

    private class Overlay extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path corner = new Path();

        Overlay(Context c) {
            super(c);
            setClickable(false);
            setFocusable(false);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float x1 = w * PIP_FRAC;
            float y0 = h * TOP_W / 100f;
            float y1 = h * (TOP_W + MID_W) / 100f;
            float g = dp(22);

            // glow around the top and right edge of the slot (soft, restrained)
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, y0 - g, 0, y0, 0x00FFFFFF, 0x38FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(0, y0 - g, x1, y0, p);
            p.setShader(new LinearGradient(0, y0, 0, y0 + g, 0x38FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(0, y0, x1, y0 + g, p);
            p.setShader(new LinearGradient(x1 - g, 0, x1, 0, 0x00FFFFFF, 0x38FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(x1 - g, y0, x1, y1, p);
            p.setShader(new LinearGradient(x1, 0, x1 + g, 0, 0x38FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(x1, y0, x1 + g, y1, p);
            p.setShader(null);

            // thin chrome line with dark hairlines either side
            float t = Math.max(2f, dp(1.5f));
            p.setColor(0x99000000);
            c.drawRect(0, y0 - t * 1.5f, x1 + t, y0 - t * 0.5f, p);
            c.drawRect(0, y0 + t * 0.5f, x1 + t, y0 + t * 1.5f, p);
            p.setColor(CHROME);
            c.drawRect(0, y0 - t * 0.5f, x1 + t * 0.5f, y0 + t * 0.5f, p);
            p.setColor(0x99000000);
            c.drawRect(x1 - t * 1.5f, y0, x1 - t * 0.5f, y1, p);
            c.drawRect(x1 + t * 0.5f, y0, x1 + t * 1.5f, y1, p);
            p.setColor(CHROME);
            c.drawRect(x1 - t * 0.5f, y0 - t * 0.5f, x1 + t * 0.5f, y1, p);

            // software rounded corners
            float r = dp(cfg.getInt("corner", 30));
            if (r < 1f) return;
            corner.reset();
            corner.moveTo(0, 0);
            corner.lineTo(r, 0);
            corner.arcTo(new RectF(0, 0, 2 * r, 2 * r), 270, -90);
            corner.close();
            p.setColor(Color.BLACK);
            c.drawPath(corner, p);
            c.save();
            c.translate(w, 0);
            c.rotate(90);
            c.drawPath(corner, p);
            c.restore();
            c.save();
            c.translate(w, h);
            c.rotate(180);
            c.drawPath(corner, p);
            c.restore();
            c.save();
            c.translate(0, h);
            c.rotate(270);
            c.drawPath(corner, p);
            c.restore();
        }
    }

    // ------------------------------------------------------------------ icons

    static class Glyph extends View {
        static final int RADIO = 0, PREV = 1, PLAY = 2, NEXT = 3, R360 = 4,
                YT = 5, DRAWER = 6, MAP = 7, GEAR = 8;
        private final int type;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);

        Glyph(Context c, int type) {
            super(c);
            this.type = type;
            fill.setColor(0xFFF2F5F8);
            fill.setStyle(Paint.Style.FILL);
            stroke.setColor(0xFFF2F5F8);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            hole.setColor(Color.BLACK);
            hole.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), u = Math.min(w, h) / 100f;
            c.save();
            c.translate((w - 100f * u) / 2f, (h - 100f * u) / 2f);
            c.scale(u, u);
            Path p = new Path();
            stroke.setStrokeWidth(5);
            switch (type) {
                case PLAY:
                    p.moveTo(30, 14);
                    p.lineTo(30, 86);
                    p.lineTo(84, 50);
                    p.close();
                    c.drawPath(p, fill);
                    break;
                case PREV:
                    c.drawRect(16, 22, 24, 78, fill);
                    p.moveTo(82, 20);
                    p.lineTo(82, 80);
                    p.lineTo(32, 50);
                    p.close();
                    c.drawPath(p, fill);
                    break;
                case NEXT:
                    c.drawRect(76, 22, 84, 78, fill);
                    p.moveTo(18, 20);
                    p.lineTo(18, 80);
                    p.lineTo(68, 50);
                    p.close();
                    c.drawPath(p, fill);
                    break;
                case RADIO:
                    c.drawLine(50, 40, 28, 90, stroke);
                    c.drawLine(50, 40, 72, 90, stroke);
                    c.drawLine(36, 68, 64, 68, stroke);
                    c.drawLine(42, 54, 58, 54, stroke);
                    c.drawCircle(50, 32, 5, fill);
                    RectF a1 = new RectF(36, 18, 64, 46);
                    RectF a2 = new RectF(24, 6, 76, 58);
                    c.drawArc(a1, 140, 80, false, stroke);
                    c.drawArc(a1, 320, 80, false, stroke);
                    c.drawArc(a2, 140, 80, false, stroke);
                    c.drawArc(a2, 320, 80, false, stroke);
                    break;
                case R360:
                    // car from above inside two circling arrows
                    c.drawRoundRect(new RectF(38, 26, 62, 74), 10, 10, stroke);
                    c.drawLine(40, 40, 60, 40, stroke);
                    c.drawLine(40, 62, 60, 62, stroke);
                    RectF ov = new RectF(10, 18, 90, 82);
                    c.drawArc(ov, 200, 120, false, stroke);
                    c.drawArc(ov, 20, 120, false, stroke);
                    p.moveTo(86, 22);
                    p.lineTo(80, 42);
                    p.lineTo(94, 38);
                    p.close();
                    c.drawPath(p, fill);
                    p.reset();
                    p.moveTo(14, 78);
                    p.lineTo(20, 58);
                    p.lineTo(6, 62);
                    p.close();
                    c.drawPath(p, fill);
                    break;
                case YT:
                    c.drawRoundRect(new RectF(8, 22, 92, 78), 14, 14, stroke);
                    p.moveTo(42, 38);
                    p.lineTo(42, 62);
                    p.lineTo(64, 50);
                    p.close();
                    c.drawPath(p, fill);
                    break;
                case DRAWER:
                    for (int a = 0; a < 3; a++) {
                        for (int b = 0; b < 3; b++) {
                            c.drawCircle(24 + a * 26, 24 + b * 26, 7, fill);
                        }
                    }
                    break;
                case MAP:
                    c.drawCircle(50, 38, 28, fill);
                    p.moveTo(26, 52);
                    p.lineTo(74, 52);
                    p.lineTo(50, 92);
                    p.close();
                    c.drawPath(p, fill);
                    c.drawCircle(50, 38, 11, hole);
                    break;
                case GEAR:
                    stroke.setStrokeWidth(9);
                    c.drawCircle(50, 50, 22, stroke);
                    for (int k = 0; k < 8; k++) {
                        double ang = Math.toRadians(k * 45);
                        float cx = (float) Math.cos(ang), cy = (float) Math.sin(ang);
                        c.drawLine(50 + cx * 30, 50 + cy * 30, 50 + cx * 40, 50 + cy * 40, stroke);
                    }
                    break;
                default:
                    break;
            }
            c.restore();
        }
    }
  }
