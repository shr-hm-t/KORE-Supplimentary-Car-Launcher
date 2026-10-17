package com.vezel.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int BLUE = 0xFF1093E1;
    private static final int LINE = 0xFF1E4A8E;

    private SharedPreferences cfg;
    private MapView map;
    private MyLocationNewOverlay locOverlay;
    private LocationManager lm;
    private boolean locationStarted = false;

    private ImageView navArrow;
    private TextView navTitle, navText, cityText, subText, hintText;
    private View lightGlow, lightLine;
    private TextView[] contactInitial = new TextView[3];
    private TextView[] contactLabel = new TextView[3];

    private long lastGeocode = 0;

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        cfg = getSharedPreferences("cfg", MODE_PRIVATE);
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        buildUi();
        requestPerms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        NavListener.onChange = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocation();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
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

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        // ---------- top: live map with overlays
        FrameLayout top = new FrameLayout(this);
        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.getController().setZoom(17.0);
        map.getController().setCenter(new GeoPoint(6.9271, 79.8612)); // Colombo until the first GPS fix
        map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        top.addView(map, new FrameLayout.LayoutParams(-1, -1));

        View tint = new View(this);
        tint.setBackgroundColor(0x40102A55);
        tint.setClickable(false);
        top.addView(tint, new FrameLayout.LayoutParams(-1, -1));

        // left fade so the text stays readable
        View fade = new View(this);
        GradientDrawable fg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xF0000000, 0xB0000000, 0x00000000});
        fade.setBackground(fg);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(dp(420), -1);
        top.addView(fade, flp);

        // speed light from the top edge (yellow from 80 km/h, red from 120 km/h)
        lightGlow = new View(this);
        top.addView(lightGlow, new FrameLayout.LayoutParams(-1, dp(70), Gravity.TOP));
        lightLine = new View(this);
        top.addView(lightLine, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));
        setLight(0);

        // navigation + city text
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(18), dp(14), dp(10), dp(10));
        navArrow = new ImageView(this);
        info.addView(navArrow, new LinearLayout.LayoutParams(dp(64), dp(64)));
        navTitle = text(22, Color.WHITE, true);
        navText = text(14, 0xFF9AA8BC, false);
        info.addView(navTitle);
        info.addView(navText);
        View spacer = new View(this);
        info.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
        cityText = text(30, Color.WHITE, true);
        subText = text(16, 0xFF9AA8BC, false);
        info.addView(cityText);
        info.addView(subText);
        hintText = text(13, 0xFF1093E1, false);
        hintText.setText("Tap here to enable navigation info");
        hintText.setPadding(0, dp(8), 0, 0);
        hintText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        info.addView(hintText);
        top.addView(info, new FrameLayout.LayoutParams(dp(360), -1));

        root.addView(top, weight(54f, false));

        // ---------- bottom: 360 placeholder + 2x4 grid
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF06142B, 0xFF020813}));

        FrameLayout pip = cell();
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(56);
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        cp.bottomMargin = dp(30);
        pip.addView(clock, cp);
        TextView pipLabel = text(15, 0xFF9AA8BC, false);
        pipLabel.setText("360° PIP slot  (tap: open 360, hold: settings)");
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        pl.topMargin = dp(60);
        pip.addView(pipLabel, pl);
        pip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPref("pkg360");
            }
        });
        pip.setOnLongClickListener(settingsLongClick());
        bottom.addView(pip, weight(42.3f, true));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row1 = new LinearLayout(this);
        LinearLayout row2 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        row1.addView(iconCell(Glyph.RADIO, "Radio", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPref("pkgRadio");
            }
        }, settingsLongClick()), weight(1f, true));
        row1.addView(iconCell(Glyph.PREV, null, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
        }, null), weight(1f, true));
        row1.addView(iconCell(Glyph.PLAY, null, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
        }, null), weight(1f, true));
        row1.addView(iconCell(Glyph.NEXT, null, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            }
        }, null), weight(1f, true));

        row2.addView(iconCell(Glyph.R360, "Full", new View.OnClickListener() {
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
        bottom.addView(grid, weight(57.7f, true));

        root.addView(bottom, weight(46f, false));
        setContentView(root);
    }

    private TextView text(int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
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

    private FrameLayout iconCell(int glyph, String label, View.OnClickListener click, View.OnLongClickListener lc) {
        FrameLayout f = cell();
        Glyph g = new Glyph(this, glyph);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(78), dp(78), Gravity.CENTER);
        if (label != null) p.bottomMargin = dp(14);
        f.addView(g, p);
        if (label != null) {
            TextView t = text(14, 0xFFB8C4D6, false);
            t.setText(label);
            FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            tp.bottomMargin = dp(10);
            f.addView(t, tp);
        }
        f.setOnClickListener(click);
        if (lc != null) f.setOnLongClickListener(lc);
        return f;
    }

    private FrameLayout contactCell(final int i) {
        FrameLayout f = cell();
        TextView c = new TextView(this);
        c.setGravity(Gravity.CENTER);
        c.setTextColor(Color.WHITE);
        c.setTextSize(30);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        int[] cols = {0xFF2E8B63, 0xFF8A4A7C, 0xFF3A66B0};
        GradientDrawable oval = new GradientDrawable();
        oval.setShape(GradientDrawable.OVAL);
        oval.setColor(cols[i % 3]);
        c.setBackground(oval);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(66), dp(66), Gravity.CENTER);
        p.bottomMargin = dp(14);
        f.addView(c, p);
        TextView t = text(14, 0xFFB8C4D6, false);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        tp.bottomMargin = dp(10);
        f.addView(t, tp);
        contactInitial[i] = c;
        contactLabel[i] = t;
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
            locOverlay.enableMyLocation();
            locOverlay.enableFollowLocation();
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
            float kmh = loc.hasSpeed() ? loc.getSpeed() * 3.6f : 0f;
            setLight(kmh);
            long now = System.currentTimeMillis();
            if (now - lastGeocode > 30000) {
                lastGeocode = now;
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
        else col = 0xFF8FB4E8;
        int glowA = kmh >= 80f ? 0xB0 : 0x30;
        int c = (glowA << 24) | (col & 0x00FFFFFF);
        lightGlow.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{c, 0x00000000}));
        lightLine.setBackgroundColor(kmh >= 80f ? col : 0x408FB4E8);
    }

    private void reverseGeocode(final double lat, final double lon) {
        if (!Geocoder.isPresent()) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Address> a = new Geocoder(MainActivity.this, Locale.getDefault()).getFromLocation(lat, lon, 1);
                    if (a == null || a.isEmpty()) return;
                    final Address ad = a.get(0);
                    final String city = ad.getLocality() != null ? ad.getLocality() : ad.getSubAdminArea();
                    final String sub = ad.getSubLocality() != null ? ad.getSubLocality() : ad.getThoroughfare();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cityText.setText(city == null ? "" : city);
                            subText.setText(sub == null ? "" : sub);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    // ------------------------------------------------------------------ actions

    private void mediaKey(int code) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private void launchPref(String key) {
        String pkg = cfg.getString(key, "");
        if (pkg.length() == 0) {
            Toast.makeText(this, "Hold this tile to set the app package", Toast.LENGTH_LONG).show();
            editSettings();
            return;
        }
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            Toast.makeText(this, "App not found: " + pkg, Toast.LENGTH_LONG).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void refreshContacts() {
        String[] defName = {"Dad", "Amma", "Sis"};
        for (int i = 0; i < 3; i++) {
            String n = cfg.getString("name" + i, defName[i]);
            contactLabel[i].setText(n);
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
                Toast.makeText(this, "Set the macro action in settings (hold the 360 tile)", Toast.LENGTH_LONG).show();
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
        new AlertDialog.Builder(this)
                .setTitle("Contact " + (i + 1))
                .setView(l)
                .setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        cfg.edit().putString("name" + i, n.getText().toString().trim())
                                .putString("num" + i, p.getText().toString().trim()).apply();
                        refreshContacts();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void editSettings() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(10), dp(20), 0);
        final EditText radio = field(l, "Radio app package (e.g. com.microntek.radio)", cfg.getString("pkgRadio", ""), InputType.TYPE_CLASS_TEXT);
        final EditText cam = field(l, "360 app package", cfg.getString("pkg360", ""), InputType.TYPE_CLASS_TEXT);
        final EditText mode = field(l, "Call mode: 0 direct, 1 dial screen, 2 macro", String.valueOf(cfg.getInt("callMode", 0)), InputType.TYPE_CLASS_NUMBER);
        final EditText macro = field(l, "Macro broadcast action (mode 2)", cfg.getString("macroAction", ""), InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("Launcher settings")
                .setView(l)
                .setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        int m = 0;
                        try {
                            m = Integer.parseInt(mode.getText().toString().trim());
                        } catch (Exception ignored) {
                        }
                        cfg.edit().putString("pkgRadio", radio.getText().toString().trim())
                                .putString("pkg360", cam.getText().toString().trim())
                                .putInt("callMode", m)
                                .putString("macroAction", macro.getText().toString().trim()).apply();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------------ icons

    static class Glyph extends View {
        static final int RADIO = 0, PREV = 1, PLAY = 2, NEXT = 3, R360 = 4;
        private final int type;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        Glyph(android.content.Context c, int type) {
            super(c);
            this.type = type;
            fill.setColor(0xFFF4F7FA);
            fill.setStyle(Paint.Style.FILL);
            stroke.setColor(0xFFF4F7FA);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), u = Math.min(w, h) / 100f;
            c.save();
            c.translate((w - 100f * u) / 2f, (h - 100f * u) / 2f);
            c.scale(u, u);
            Path p = new Path();
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
                    stroke.setStrokeWidth(5);
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
                    stroke.setStrokeWidth(5);
                    c.drawCircle(50, 50, 40, stroke);
                    Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
                    t.setColor(0xFFF4F7FA);
                    t.setTextAlign(Paint.Align.CENTER);
                    t.setTypeface(Typeface.DEFAULT_BOLD);
                    t.setTextSize(26);
                    c.drawText("360°", 50, 60, t);
                    break;
                default:
                    break;
            }
            c.restore();
        }
    }
          }
