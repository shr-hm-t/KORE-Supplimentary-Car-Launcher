package com.rehome.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.RadialGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {

    // design language: black, grey, cool-white chrome
    private static final int WHITE = 0xFFF2F5F8;
    private static final int GREY = 0xFF9BA1A8;
    private static final int LINE = 0xFF3A3F45;
    private static final int CHROME = 0xFFE2E8EC;

    static volatile boolean resumed = false;
    static volatile boolean videoPlaying = false;
    private static Bitmap lastYtBmp;
    private static String lastYtTitle = "";
    private static boolean bootHandled = false;

    private SharedPreferences cfg;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MapHost host;
    private boolean night = true;
    private int cText = Color.WHITE, cSub = 0xFF9BA1A8, cGlyph = 0xFFF2F5F8, cLine = 0xFF3A3F45;
    private int cPipBg = 0xFF050607, cPanel = 0xFF0B0C0D, cMidTop = 0xFF16181B, cMidBot = 0xFF050607;
    private int cRing = 0xFFE2E8EC, cRingFill = 0xFF15171A;
    private int cAccent = 0xFFFF8A1E, cTileFill = 0x59000000, cTileStroke = 0x66FFFFFF, cVig = 0xFF000000;
    private int cMenuTop = 0xFF212B35, cMenuBot = 0xFF0A0E12, cMenuStroke = 0xFF5A6068;
    private int tileRadius = 60, vigStrength = 200, tileGap = 6, iconGlow = 0;
    private int vigStyle = 1, vigShape = 0, vigSpread = 100, ytVig = 120;
    private org.json.JSONObject mapPal;
    private FrameLayout leftPanel;
    private double mslAlt = Double.NaN;
    private long mslTime = 0;
    private Object nmeaListener;
    private org.json.JSONObject themeJ;
    private long lastThemeCheck = 0, lastLocSave = 0;
    private LocationManager lm;
    private boolean locationStarted = false;
    private float scale = 1f;
    private boolean welcomeShown = false;
    private Location lastLoc;
    private long lastTouch = 0;

    // auto 360
    private long resumedAt = 0, lastAuto360 = 0, lastTouchAny = 0, lastSpeedTime = 0;
    private float curKmh = 0f;
    private boolean armed360 = true;

    private Body body;
    private Overlay overlay;
    private boolean showGuides = false;

    private ImageView navArrow;
    private TextView navTitle, navText, navSub, cityText, subText, hintText;
    private LinearLayout infoPanel;
    private SpeedGlow lightGlow;
    private View lightLine;
    private Badge[] contactBadge = new Badge[3];
    private TextView slotDate, slotHeading, slotAlt, slotGps;
    private Glyph themeGlyph;

    // YouTube widget
    private FrameLayout ytPane;
    private FrameLayout ytShade;
    private ImageView ytImage;
    private TextView ytTitle;
    private FrameLayout ytPlayBtn;
    private Glyph ytPlayGlyph;
    private boolean ytPlayingNow = false;

    private long lastGeoTime = 0;
    private Location lastGeoLoc;

    /** One-time clean-up when updating from an older version. */
    private void migrateSettings() {
        if (cfg.getBoolean("migrated11", false)) return;
        // older versions saved the vignette and tile roundness for good, which hid the theme file's values
        cfg.edit().remove("vignette").remove("tileRadius").remove("mapEngine").remove("webMapUrl")
                .remove("orient").putBoolean("migrated11", true).apply();
    }

    /** One-time clean-up for 1.6: new defaults for size, vignette, logo, dock order and the floating button. */
    private void migrate16() {
        if (cfg.getBoolean("migrated16", false)) return;
        cfg.edit().remove("vignette").remove("tileRadius").remove("uiScale").remove("logoSize")
                .remove("dockItems").remove("floatBtns").remove("floatIn").remove("mapStyle").remove("iconGlow")
                .remove("floatX").remove("floatY").putBoolean("migrated16", true).apply();
    }

    private boolean mirror() {
        return cfg.getInt("seat", 0) == 1;
    }

    /** Extended frame: the bottom of the frame grows into a bar with the 360 and primary-contact buttons (simple layout only). */
    private boolean extOn() {
        return !pipOn() && cfg.getBoolean("frame", true) && cfg.getBoolean("frameExt", false);
    }

    private float shieldH() {
        return Math.max(8, Math.min(30, cfg.getInt("shieldH", 18)));
    }

    private boolean pipOn() {
        return cfg.getBoolean("pipOn", false);
    }

    /** With the 360 window on, the map / YouTube area keeps clear of it so none of its buttons are covered. */
    private boolean avoidPip() {
        return pipOn() && cfg.getBoolean("avoidPip", true);
    }

    /** Colour for an icon: the theme's iconColors entry, or the default. */
    private int iconColor(String id, int def) {
        org.json.JSONObject o = themeJ == null ? null : themeJ.optJSONObject("iconColors");
        return ThemeFile.col(o, id, def);
    }

    // ------------------------------------------------------------------ layout fractions (calibration)

    private static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private float pipW() {
        return clampF(cfg.getFloat("pipW", 0.398f), 0.25f, 0.65f);
    }

    private float pipH() {
        return clampF(cfg.getFloat("pipH", 0.515f), 0.20f, 0.85f);
    }

    private float dockH() {
        return clampF(cfg.getFloat("dockH", 0.065f), 0.03f, 0.25f);
    }

    private float barH() {
        return clampF(cfg.getFloat("barH", 0.15f), 0.08f, 0.30f);
    }

    // ------------------------------------------------------------------ lifecycle

    private boolean lockedBoot = false;
    private android.content.BroadcastReceiver unlockRx;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // Direct boot: at a cold start the phone storage can still be locked. Show a plain black screen
        // (so Android has a home screen right away and skips "Android is starting") and build the real screen once unlocked.
        if (Build.VERSION.SDK_INT >= 24) {
            android.os.UserManager um = (android.os.UserManager) getSystemService(Context.USER_SERVICE);
            if (um != null && !um.isUserUnlocked()) {
                lockedBoot = true;
                View black = new View(this);
                black.setBackgroundColor(Color.BLACK);
                setContentView(black);
                unlockRx = new android.content.BroadcastReceiver() {
                    @Override
                    public void onReceive(Context c, Intent i) {
                        recreate();
                    }
                };
                registerReceiver(unlockRx, new android.content.IntentFilter(Intent.ACTION_USER_UNLOCKED));
                return;
            }
        }
        cfg = getSharedPreferences("cfg", MODE_PRIVATE);
        if (b == null && fromAppList(getIntent())) openSettingsPending = true;
        scale = Math.max(0.6f, Math.min(1.6f, cfg.getInt("uiScale", 100) / 100f));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        migrateSettings();
        migrate16();
        night = computeNight();
        initTheme();
        // First frame: just the background, so Android sees the launcher straight away.
        // The map, widgets and buttons are built right afterwards.
        View first = new View(this);
        first.setBackgroundColor(Color.BLACK);
        setContentView(first);
        applyStatusBar();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                // 1) only the services: radio and 360 start first
                boolean cold = !bootHandled;
                bootTasks();
                // 2) the interface, touch handling, frame and floating buttons follow once the 360 is up
                long wait = 0;
                if (cold && cfg.getString("pkg360", "").length() > 0 && cfg.contains("seat")) {
                    wait = (cfg.getInt("boot360Retry", 5) + cfg.getInt("floatDelay", 5)) * 1000L;
                }
                if (wait <= 0) {
                    buildNow();
                } else {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            buildNow();
                        }
                    }, wait);
                }
            }
        });
    }

    private boolean uiReady = false, pendingResume = false, activityResumed = false;
    private final long createdAt = System.currentTimeMillis();

    private void buildNow() {
        if (uiReady || isFinishing() || isDestroyed() || lockedBoot) return;
        buildUi();
        uiReady = true;
        requestPerms();
        if (pendingResume && activityResumed) resumeWork();
        pendingResume = false;
        if (openSettingsPending) {
            openSettingsPending = false;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    editSettings();
                }
            }, 500);
        }
    }

    private void bootTasks() {
        if (!bootHandled) {
            bootHandled = true;
            final int secs = cfg.getInt("autoRadio", 1);
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
            // optional second try: if 360 is not on screen some seconds after boot, start it
            final int retry = cfg.getInt("boot360Retry", 5);
            if (retry > 0 && cfg.getString("pkg360", "").length() > 0) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (activityResumed) launchPref("pkg360");
                    }
                }, retry * 1000L);
            }
        }
    }

    private boolean openSettingsPending = false;

    /** Opened from the app list (not by the Home button): the emergency way into the settings. */
    private static boolean fromAppList(Intent i) {
        return i != null && i.hasCategory(Intent.CATEGORY_LAUNCHER) && !i.hasCategory(Intent.CATEGORY_HOME);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (fromAppList(intent)) {
            if (uiReady) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        editSettings();
                    }
                }, 400);
            } else {
                openSettingsPending = true;
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        lastTouchAny = System.currentTimeMillis();
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lockedBoot) return;
        applyStatusBar();
        activityResumed = true;
        if (!uiReady) {
            pendingResume = true;
            // back at the home screen after the 360 had its chance: do not keep waiting
            if (System.currentTimeMillis() - createdAt > cfg.getInt("boot360Retry", 5) * 1000L + 1500L) buildNow();
            return;
        }
        resumeWork();
    }

    private void resumeWork() {
        resumed = true;
        if (cfg.getInt("guide", 9) < 9) handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (resumed) guideNext();
            }
        }, 600);
        resumedAt = System.currentTimeMillis();
        host.onResume();
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
        updateSlot();

        if (getIntent() != null && getIntent().getBooleanExtra("autocall", false)) {
            getIntent().removeExtra("autocall");
            wheelAction();
        }
        updateFloating();
        if (!cfg.contains("seat") && !welcomeShown) {
            welcomeShown = true;
            showWelcome();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (lockedBoot) return;
        resumed = false;
        activityResumed = false;
        if (host != null) host.onPause();
        NavListener.onChange = null;
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unlockRx != null) {
            try {
                unregisterReceiver(unlockRx);
            } catch (Exception ignored) {
            }
            unlockRx = null;
        }
        if (lockedBoot) return;
        stopLocation();
        if (host != null) host.onDestroy();
    }

    /** The back button must not restart the home screen. */
    @Override
    public void onBackPressed() {
        // intentionally empty
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (lockedBoot) return true;
        // steering wheel "voice" button
        if (keyCode == KeyEvent.KEYCODE_VOICE_ASSIST || keyCode == KeyEvent.KEYCODE_CALL) {
            wheelAction();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ------------------------------------------------------------------ day / night

    /** True when the dark look should be used. Light mode only ever follows the clock or the sun, never forced. */
    private boolean computeNight() {
        int mode = cfg.getInt("theme", 2);
        if (mode == 2) return true;
        long ms = System.currentTimeMillis();
        if (mode == 0 && cfg.contains("lat")) {
            double[] sun = sunTimes(cfg.getFloat("lat", 0f), cfg.getFloat("lon", 0f));
            if (sun == null) return !sunUp; // polar day or polar night
            double nowMin = (ms / 60000L) % 1440;
            double a = ((sun[0] % 1440) + 1440) % 1440;
            double b = ((sun[1] % 1440) + 1440) % 1440;
            boolean day = a < b ? (nowMin >= a && nowMin < b) : (nowMin >= a || nowMin < b);
            return !day;
        }
        // fixed hours (also the fallback for automatic mode before the first GPS fix)
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int dayStart = cfg.getInt("dayStart", 6);
        int nightStart = cfg.getInt("nightStart", 18);
        boolean day = dayStart < nightStart ? (hour >= dayStart && hour < nightStart)
                : (hour >= dayStart || hour < nightStart);
        return !day;
    }

    private boolean sunUp = false;

    /** Sunrise and sunset as minutes after midnight UTC, or null near the poles. */
    private double[] sunTimes(double lat, double lon) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int n = cal.get(Calendar.DAY_OF_YEAR);
        double g = 2 * Math.PI / 365.0 * (n - 1);
        double eqt = 229.18 * (0.000075 + 0.001868 * Math.cos(g) - 0.032077 * Math.sin(g)
                - 0.014615 * Math.cos(2 * g) - 0.040849 * Math.sin(2 * g));
        double decl = 0.006918 - 0.399912 * Math.cos(g) + 0.070257 * Math.sin(g)
                - 0.006758 * Math.cos(2 * g) + 0.000907 * Math.sin(2 * g)
                - 0.002697 * Math.cos(3 * g) + 0.00148 * Math.sin(3 * g);
        double latR = Math.toRadians(lat);
        double cosH = Math.cos(Math.toRadians(90.833)) / (Math.cos(latR) * Math.cos(decl))
                - Math.tan(latR) * Math.tan(decl);
        if (cosH > 1) {
            sunUp = false;
            return null;
        }
        if (cosH < -1) {
            sunUp = true;
            return null;
        }
        double ha = Math.toDegrees(Math.acos(cosH));
        return new double[]{720 - 4 * (lon + ha) - eqt, 720 - 4 * (lon - ha) - eqt};
    }

    private void initTheme() {
        themeJ = ThemeFile.merged(cfg);
        org.json.JSONObject pal = themeJ.optJSONObject(night ? "dark" : "light");
        cText = ThemeFile.col(pal, "text", night ? 0xFFF2F5F8 : 0xFF14171A);
        cSub = ThemeFile.col(pal, "sub", night ? 0xFF9BA1A8 : 0xFF4A5057);
        cGlyph = ThemeFile.col(pal, "glyph", cText);
        cMidTop = ThemeFile.col(pal, "bgTop", night ? 0xFF1B2733 : 0xFFEEF2F6);
        cMidBot = ThemeFile.col(pal, "bgBottom", night ? 0xFF0A0F14 : 0xFFCBD3DA);
        cAccent = ThemeFile.col(pal, "accent", 0xFFFF8A1E);
        cTileFill = ThemeFile.col(pal, "tileFill", night ? 0x59000000 : 0x4DFFFFFF);
        cTileStroke = ThemeFile.col(pal, "tileStroke", night ? 0x66FFFFFF : 0x66000000);
        cVig = ThemeFile.col(pal, "vignetteColor", night ? 0xFF000000 : 0xFF3A4856);
        cLine = cTileStroke;
        cPipBg = cMidBot;
        org.json.JSONObject menu = themeJ.optJSONObject("menu");
        cMenuTop = ThemeFile.col(menu, "bgTop", 0xFF212B35);
        cMenuBot = ThemeFile.col(menu, "bgBottom", 0xFF0A0E12);
        cMenuStroke = ThemeFile.col(menu, "stroke", 0xFF5A6068);
        vigStrength = Math.max(0, Math.min(200, cfg.getInt("vignette", themeJ.optInt("vignette", 200))));
        vigShape = Math.max(0, Math.min(2, cfg.getInt("vigShape", 0)));
        vigSpread = Math.max(50, Math.min(150, cfg.getInt("vigSpread", 100)));
        ytVig = Math.max(0, Math.min(200, cfg.getInt("ytVignette", 120)));
        int vs = cfg.getInt("vigStyle", 0);   // 0 = follow the appearance, 1 = surround, 2 = fade
        vigStyle = vs == 0 ? (cfg.getInt("theme", 2) == 2 ? 1 : 2) : vs;
        tileRadius = Math.max(0, cfg.getInt("tileRadius", themeJ.optInt("tileRadius", 60)));
        tileGap = Math.max(0, Math.min(30, cfg.getInt("tileGap", themeJ.optInt("tileGap", 0))));
        iconGlow = 0;
        mapPal = pal == null ? null : pal.optJSONObject("map");
        IconPack.clearOverrides();
        org.json.JSONObject icons = themeJ.optJSONObject("icons");
        if (icons != null) {
            java.util.Iterator<String> it = icons.keys();
            while (it.hasNext()) {
                String k = it.next();
                IconPack.setOverride(k, icons.optString(k, ""));
            }
        }
    }

    private void toggleTheme() {
        int cur = cfg.getInt("theme", 2);
        if (cur == 2) {
            cfg.edit().putInt("theme", cfg.getInt("themePrev", 0)).apply();
            Toast.makeText(this, "Appearance: automatic", Toast.LENGTH_SHORT).show();
        } else {
            cfg.edit().putInt("themePrev", cur).putInt("theme", 2).apply();
            Toast.makeText(this, "Appearance: always dark", Toast.LENGTH_SHORT).show();
        }
        recreate();
    }

    private void applyStatusBar() {
        boolean show = cfg.getBoolean("showStatus", false);
        Window w = getWindow();
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (show) {
            // the system bar floats over the map; the frame draws the two dark slabs behind its icons
            w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            if (Build.VERSION.SDK_INT >= 21) w.setStatusBarColor(Color.TRANSPARENT);
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
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
            long nowT = System.currentTimeMillis();
            if (nowT - lastThemeCheck > 30000) {
                lastThemeCheck = nowT;
                if (computeNight() != night) {
                    recreate();
                    return;
                }
            }
            refreshYt();
            updateSlot();
            checkAuto360();
            handler.postDelayed(this, 2000);
        }
    };

    /** Re-centre the map on the car when nobody has touched it for 10 seconds. */
    private void recentre() {
        if (lastLoc == null || host == null) return;
        if (System.currentTimeMillis() - lastTouch < 10000) return;
        double[] c = host.center();
        if (c == null) return;
        double la = lastLoc.getLatitude(), lo = lastLoc.getLongitude();
        if (Math.abs(c[0] - la) > 0.00005 || Math.abs(c[1] - lo) > 0.00005) {
            host.centerOn(la, lo);
        }
    }

    /**
     * Show the 360 camera full screen when the car creeps along slowly while the launcher is the
     * screen underneath (for example after the 360 shrank to its small window).
     */
    private void checkAuto360() {
        if (!cfg.getBoolean("auto360", true) || !resumed) return;
        String pkg = cfg.getString("pkg360", "");
        if (pkg.length() == 0) return;
        long now = System.currentTimeMillis();
        if (now - lastSpeedTime > 6000) return;               // no fresh GPS speed
        if (now - resumedAt < 4000) return;                   // launcher just appeared
        if (now - lastTouchAny < cfg.getInt("auto360Pause", 20) * 1000L) return; // someone is using the screen
        if (cfg.getBoolean("auto360PauseVideo", false) && videoPlaying) return;
        float lo = cfg.getInt("auto360Min", 5);
        float hi = cfg.getInt("auto360Max", 10);
        if (curKmh > 15f) armed360 = true;
        if (!(curKmh >= lo && curKmh < hi)) return;
        int mode = cfg.getInt("auto360Mode", 0);
        if (mode == 1 && !armed360) return;
        if (mode == 0 && now - lastAuto360 < cfg.getInt("auto360Gap", 60) * 1000L) return;
        lastAuto360 = now;
        armed360 = false;
        launchPackage(pkg);
    }

    // ------------------------------------------------------------------ permissions

    private void requestPerms() {
        if (Build.VERSION.SDK_INT >= 26) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE,
                    "android.permission.ANSWER_PHONE_CALLS",
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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

    // ------------------------------------------------------------------ small helpers

    private int dp(float v) {
        return (int) (v * scale * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** Like dp() but ignores the UI size setting (used by dialogs). */
    private int rdp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(sp * scale);
        t.setTextColor(color);
        t.setShadowLayer(6, 0, 1, night ? 0xFF000000 : 0xFFFFFFFF);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }


    // ------------------------------------------------------------------ panels (menus open at the top right, clear of the 360 window)

    private class Capped extends LinearLayout {
        private final int maxH;

        Capped(Context c, int maxH) {
            super(c);
            this.maxH = maxH;
        }

        @Override
        protected void onMeasure(int ws, int hs) {
            int size = MeasureSpec.getSize(hs);
            int cap = Math.min(size > 0 ? size : maxH, maxH);
            super.onMeasure(ws, MeasureSpec.makeMeasureSpec(cap, MeasureSpec.AT_MOST));
        }
    }

    private Button flat(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(WHITE);
        b.setTextSize(16);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF15171A);
        g.setStroke(Math.max(1, rdp(1)), 0xFF5A6068);
        b.setBackground(g);
        b.setPadding(rdp(14), rdp(8), rdp(14), rdp(8));
        return b;
    }

    private class Panel {
        final Dialog d;
        final LinearLayout buttons;

        Panel(String title, View bodyView, boolean scroll) {
            this(title, bodyView, scroll, false);
        }

        Panel(String title, View bodyView, boolean scroll, boolean wide) {
            d = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            Capped box = new Capped(MainActivity.this, (int) (dm.heightPixels * (wide ? 0.84f : 0.70f)));
            box.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{cMenuTop, cMenuBot});
            bg.setStroke(Math.max(1, rdp(1)), cMenuStroke);
            box.setBackground(bg);
            TextView t = new TextView(MainActivity.this);
            t.setText(title);
            t.setTextColor(WHITE);
            t.setTextSize(20);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setPadding(rdp(18), rdp(14), rdp(18), rdp(10));
            box.addView(t);
            View line = new View(MainActivity.this);
            line.setBackgroundColor(0xFF3A3F45);
            box.addView(line, new LinearLayout.LayoutParams(-1, 1));
            if (scroll) {
                ScrollView sv = new ScrollView(MainActivity.this);
                sv.addView(bodyView);
                box.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
            } else {
                box.addView(bodyView, new LinearLayout.LayoutParams(-1, 0, 1f));
            }
            buttons = new LinearLayout(MainActivity.this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setGravity(Gravity.END);
            buttons.setPadding(rdp(12), rdp(8), rdp(12), rdp(12));
            box.addView(buttons, new LinearLayout.LayoutParams(-1, -2));
            d.setContentView(box);
            d.setCanceledOnTouchOutside(true);

            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
                boolean portrait = dm.heightPixels > dm.widthPixels;
                w.setLayout((int) (dm.widthPixels * (portrait ? 0.95f : (wide ? 0.74f : 0.46f))), WindowManager.LayoutParams.WRAP_CONTENT);
                w.setGravity(Gravity.TOP | Gravity.END);
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.x = rdp(10);
                lp.y = rdp(10);
                lp.dimAmount = 0.35f;
                w.setAttributes(lp);
                w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility());
            }
        }

        Panel button(String label, final Runnable r) {
            Button b = flat(label);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    d.dismiss();
                    if (r != null) r.run();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.leftMargin = rdp(8);
            buttons.addView(b, lp);
            return this;
        }

        Panel onClose(final Runnable r) {
            d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(android.content.DialogInterface di) {
                    r.run();
                }
            });
            return this;
        }

        void show() {
            try {
                d.show();
            } catch (Exception ignored) {
            }
        }
    }

    private android.graphics.drawable.Drawable rowBg() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x33000000);
        g.setStroke(Math.max(1, rdp(1)), 0x55FFFFFF);
        return g;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(rdp(18), rdp(8), rdp(18), rdp(8));
        return l;
    }

    private TextView heading(LinearLayout l, String t) {
        TextView h = new TextView(this);
        h.setText(t);
        h.setTextColor(WHITE);
        h.setTextSize(17);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setPadding(0, rdp(18), 0, rdp(4));
        l.addView(h);
        View line = new View(this);
        line.setBackgroundColor(0xFF3A3F45);
        l.addView(line, new LinearLayout.LayoutParams(-1, 1));
        return h;
    }

    private TextView label(LinearLayout l, String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(GREY);
        t.setTextSize(14);
        t.setPadding(0, rdp(8), 0, 0);
        l.addView(t);
        return t;
    }

    private void addFull(LinearLayout l, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = rdp(6);
        l.addView(v, lp);
    }

    private RadioGroup radios(LinearLayout parent, String[] names, int base, int checked) {
        RadioGroup g = new RadioGroup(this);
        for (int k = 0; k < names.length; k++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(names[k]);
            rb.setTextColor(WHITE);
            rb.setTextSize(16);
            rb.setId(base + k);
            rb.setBackground(rowBg());
            rb.setPadding(rdp(10), rdp(8), rdp(10), rdp(8));
            RadioGroup.LayoutParams rlp = new RadioGroup.LayoutParams(-1, -2);
            rlp.topMargin = rdp(4);
            g.addView(rb, rlp);
        }
        g.check(base + Math.max(0, Math.min(names.length - 1, checked)));
        parent.addView(g);
        return g;
    }

    private int checkedIdx(RadioGroup g, int base, int n, int def) {
        int v = g.getCheckedRadioButtonId() - base;
        return (v < 0 || v >= n) ? def : v;
    }

    private CheckBox check(LinearLayout parent, String t, boolean v) {
        CheckBox c = new CheckBox(this);
        c.setText(t);
        c.setTextColor(WHITE);
        c.setTextSize(16);
        c.setChecked(v);
        c.setBackground(rowBg());
        c.setPadding(rdp(10), rdp(8), rdp(10), rdp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = rdp(4);
        parent.addView(c, lp);
        return c;
    }

    private EditText field(LinearLayout parent, String hint, String value, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0xFF6E757C);
        e.setTextColor(WHITE);
        e.setTextSize(16);
        e.setText(value);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setBackground(rowBg());
        e.setPadding(rdp(10), rdp(8), rdp(10), rdp(8));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, -2);
        flp.topMargin = rdp(4);
        parent.addView(e, flp);
        return e;
    }

    private EditText numField(LinearLayout parent, String lab, int value) {
        label(parent, lab);
        return field(parent, "", String.valueOf(value), InputType.TYPE_CLASS_NUMBER);
    }

    private int intOf(EditText e, int def) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (Exception ex) {
            return def;
        }
    }

    private interface IdxCb {
        void on(int index);
    }

    private void listPanel(String title, final String[] names, final IdxCb cb) {
        ListView lv = new ListView(this);
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names) {
            @Override
            public View getView(int p, View v, ViewGroup parent) {
                TextView tv = (TextView) super.getView(p, v, parent);
                tv.setTextColor(WHITE);
                tv.setTextSize(17);
                return tv;
            }
        });
        final Panel pn = new Panel(title, lv, false);
        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> a, View v, int pos, long id) {
                pn.d.dismiss();
                cb.on(pos);
            }
        });
        pn.button("Cancel", null).show();
    }

    // ------------------------------------------------------------------ main layout container

    /**
     * Places the main areas by fractions of the screen so calibration can move them live.
     * Children: 0 map/widget, 1 slot behind the 360 window, 2 bar (360 + contacts), 3 clock, 4 dock icons,
     * 5 left panel (navigation info above the 360 window).
     */
    private class Body extends ViewGroup {
        Body(Context c) {
            super(c);
        }

        private void measureExact(int i, int w, int h) {
            View v = getChildAt(i);
            if (v == null) return;
            v.measure(MeasureSpec.makeMeasureSpec(Math.max(0, w), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Math.max(0, h), MeasureSpec.EXACTLY));
        }

        private void place(int i, int x0, int y0, int x1, int y1) {
            View v = getChildAt(i);
            if (v != null) v.layout(x0, y0, x1, y1);
        }

        @Override
        protected void onMeasure(int ws, int hs) {
            int W = MeasureSpec.getSize(ws), H = MeasureSpec.getSize(hs);
            setMeasuredDimension(W, H);
            boolean pip = pipOn(), avoid = avoidPip();
            int dock = pip ? Math.round(H * dockH()) : 0, bar = Math.round(H * barH());
            int pw = Math.round(W * pipW()), ph = Math.round(H * pipH());
            int clockW = pip ? pw : Math.round(W * 0.2f);
            measureExact(0, avoid ? W - pw : W, H);                // map / widget, also under the bars
            measureExact(1, pip ? pw : 0, pip ? ph - dock : 0);    // slot behind the 360 window
            measureExact(2, pip ? W - pw : W, bar);                // contacts bar
            measureExact(3, clockW, dock);                         // clock
            measureExact(4, W - clockW, dock);                     // dock icons
            measureExact(5, avoid ? pw : 0, avoid ? H - ph : 0);   // plain panel above the 360 window
        }

        private void placeX(int i, int x0, int y0, int x1, int y1, int W) {
            if (mirror()) place(i, W - x1, y0, W - x0, y1);
            else place(i, x0, y0, x1, y1);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int W = r - l, H = b - t;
            boolean pip = pipOn(), avoid = avoidPip();
            int dock = pip ? Math.round(H * dockH()) : 0, bar = Math.round(H * barH());
            int ext = extOn() ? Math.round(H * shieldH() / 100f) : 0;
            int pw = Math.round(W * pipW()), ph = Math.round(H * pipH());
            int clockW = pip ? pw : Math.round(W * 0.2f);
            placeX(0, avoid ? pw : 0, 0, W, H, W);
            if (pip) placeX(1, 0, H - ph, pw, H - dock, W);
            else place(1, 0, 0, 0, 0);
            placeX(2, pip ? pw : 0, H - dock - ext - bar, W, H - dock - ext, W);
            placeX(3, 0, H - dock, clockW, H, W);
            placeX(4, clockW, H - dock, W, H, W);
            if (avoid) placeX(5, 0, 0, pw, H - ph, W);
            else place(5, 0, 0, 0, 0);
        }
    }

    /** Text only ("360°") or a contact: phone icon + initial, bright accent when it is the primary contact. */
    private class Badge extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ic = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String label = "";
        private final int kind; // 0 = text only, 1 = contact
        private boolean primary = false;

        Badge(Context c, int kind) {
            super(c);
            this.kind = kind;
            ic.setStyle(Paint.Style.STROKE);
            ic.setStrokeCap(Paint.Cap.ROUND);
            ic.setStrokeJoin(Paint.Join.ROUND);
        }

        void setLabel(String s) {
            label = s;
            invalidate();
        }

        void setPrimary(boolean b) {
            primary = b;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float cy = h / 2f;
            int tint = primary ? 0xFFFF5A68 : WHITE;
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setStyle(Paint.Style.FILL);
            if (kind == 0) {
                p.setTextAlign(Paint.Align.CENTER);
                p.setColor(WHITE);
                p.setTextSize(Math.min(h * 0.40f, w * 0.30f));
                c.drawText(label, w / 2f, cy - (p.descent() + p.ascent()) / 2f, p);
                return;
            }
            float size = Math.min(h * 0.46f, w * 0.26f);
            p.setTextAlign(Paint.Align.LEFT);
            p.setColor(tint);
            p.setTextSize(size * 1.05f);
            float tw = p.measureText(label);
            float gap = size * 0.22f;
            float x0 = (w - (size + gap + tw)) / 2f;
            ic.setColor(tint);
            c.save();
            c.translate(x0, cy - size / 2f);
            c.scale(size / 100f, size / 100f);
            if (IconPack.hasOverride("phone")) IconPack.drawOverride(c, "phone", ic);
            else IconPack.draw(c, "phone", ic);
            c.restore();
            c.drawText(label, x0 + size + gap, cy - (p.descent() + p.ascent()) / 2f, p);
        }
    }

    static final int BLUE = 0xFF2D7BFF, RED = 0xFFFF3B4A;
    private final Paint glassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Liquid glass behind one icon: coloured glow, tinted dark body, light rim and a soft highlight. */
    private void drawGlass(Canvas c, RectF r, int color, float radius) {
        Paint p = glassPaint;
        int col = color & 0x00FFFFFF;
        float amt = iconGlow / 100f;
        if (amt > 0f) {
            float gr = Math.max(r.width(), r.height()) * 0.62f;
            p.setShader(new RadialGradient(r.centerX(), r.centerY(), gr,
                    ((int) (Math.min(1f, amt) * 0.7f * 255f) << 24) | col, 0x00000000, Shader.TileMode.CLAMP));
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(r.centerX(), r.centerY(), gr, p);
        }
        p.setShader(new LinearGradient(0, r.top, 0, r.bottom, new int[]{0x88000000 | col, 0xDD0C1218}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, radius, radius, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, dp(1)));
        p.setColor(0x66FFFFFF);
        c.drawRoundRect(r, radius, radius, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x16FFFFFF);
        RectF hi = new RectF(r.left + r.width() * 0.08f, r.top + r.height() * 0.07f, r.right - r.width() * 0.08f, r.centerY());
        c.drawRoundRect(hi, radius * 0.8f, radius * 0.8f, p);
    }

    /** The glass disc behind a bar icon. */
    private class GlassDisc extends View {
        private final int color;

        GlassDisc(Context c, int color) {
            super(c);
            this.color = color;
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), d = Math.min(w, h) * 0.82f;
            RectF r = new RectF((w - d) / 2f, (h - d) / 2f, (w + d) / 2f, (h + d) / 2f);
            drawGlass(c, r, color, d / 2f);
        }
    }

    // ---- soft coloured background with vignette (the look of the unit's own radio screen)

    private static int[] softColors(int clear, int a) {
        // eased ramp so the dark edge fades in without a visible line
        float[] k = {0f, 0.04f, 0.15f, 0.33f, 0.56f, 0.8f, 1f};
        int[] out = new int[k.length];
        for (int i = 0; i < k.length; i++) out[i] = (Math.round(a * k[i]) << 24) | clear;
        return out;
    }

    private static final float[] SOFT_POS = {0f, 0.2f, 0.4f, 0.58f, 0.74f, 0.88f, 1f};

    private void drawVignette(Canvas c, Paint p, float w, float h, int strength, int color) {
        if (strength <= 0) return;
        int a = (int) (Math.min(100, strength) / 100f * 0.95f * 255f);
        int clear = color & 0x00FFFFFF;
        float extra = Math.max(0, Math.min(200, strength) - 100) / 100f;   // above 100 the dark edge reaches further in
        p.setStyle(Paint.Style.FILL);
        if (vigStyle == 2) {
            // fade: only a soft darkening along the bottom (and a little at the top)
            float fb = h * (0.38f + 0.22f * extra) * (100f / vigSpread);
            float ft = h * (0.16f + 0.10f * extra) * (100f / vigSpread);
            p.setShader(new LinearGradient(0, h, 0, h - fb, softColors(clear, a), SOFT_POS, Shader.TileMode.CLAMP));
            c.drawRect(0, h - fb, w, h, p);
            p.setShader(new LinearGradient(0, 0, 0, ft, softColors(clear, (int) (a * 0.7f)), SOFT_POS, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, ft, p);
            p.setShader(null);
            return;
        }
        float inner = Math.max(0.12f, Math.min(0.85f, 0.42f * vigSpread / 100f));
        float s = 0.58f - 0.16f * extra;
        if (vigShape == 2) {
            // rounded rectangle: four soft edges, corners are darker where they overlap
            float d = Math.min(w, h) * (0.55f - 0.18f * extra) * (100f / vigSpread);
            p.setShader(new LinearGradient(0, 0, d, 0, softColors(clear, a), SOFT_POS, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, d, h, p);
            p.setShader(new LinearGradient(w, 0, w - d, 0, softColors(clear, a), SOFT_POS, Shader.TileMode.CLAMP));
            c.drawRect(w - d, 0, w, h, p);
            p.setShader(new LinearGradient(0, 0, 0, d, softColors(clear, a), SOFT_POS, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, d, p);
            p.setShader(new LinearGradient(0, h, 0, h - d, softColors(clear, a), SOFT_POS, Shader.TileMode.CLAMP));
            c.drawRect(0, h - d, w, h, p);
            p.setShader(null);
            return;
        }
        int[] cols = softColors(clear, a);
        float[] pos = new float[SOFT_POS.length];
        for (int i = 0; i < pos.length; i++) pos[i] = inner + (1f - inner) * SOFT_POS[i];
        // pos[0] = inner: everything inside is clear
        int[] cl2 = new int[cols.length + 1];
        float[] ps2 = new float[pos.length + 1];
        cl2[0] = clear;
        ps2[0] = 0f;
        for (int i = 0; i < cols.length; i++) {
            cl2[i + 1] = cols[i];
            ps2[i + 1] = Math.max(pos[i], 0.001f * (i + 1));
        }
        RadialGradient g = new RadialGradient(0f, 0f, 1f, cl2, ps2, Shader.TileMode.CLAMP);
        Matrix mx = new Matrix();
        if (vigShape == 1) {
            float r = Math.max(w, h) * (s + 0.05f);        // a true circle
            mx.setScale(r, r);
        } else {
            mx.setScale(w * s, h * s);                      // oval fitted to the screen
        }
        mx.postTranslate(w / 2f, h / 2f);
        g.setLocalMatrix(mx);
        p.setShader(g);
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
    }

    private class Backdrop extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Backdrop(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 0, 0, h, cMidTop, cMidBot, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            float r = Math.max(w, h) * 0.65f;
            p.setShader(new RadialGradient(w * 0.15f, h * 0.1f, r, (cAccent & 0x00FFFFFF) | 0x22000000,
                    0x00000000, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(new RadialGradient(w * 0.9f, h * 0.95f, r, (cTileStroke & 0x00FFFFFF) | 0x1C000000,
                    0x00000000, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);
            drawVignette(c, p, w, h, vigStrength, cVig);
        }
    }

    private class VignetteView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final int strength;

        VignetteView(Context c, int strength) {
            super(c);
            this.strength = strength;
        }

        @Override
        protected void onDraw(Canvas c) {
            drawVignette(c, p, getWidth(), getHeight(), strength, cVig);
        }
    }

    // ------------------------------------------------------------------ UI

    private void buildUi() {
        final FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(Color.BLACK);
        body = new Body(this);
        rootFrame.addView(body, new FrameLayout.LayoutParams(-1, -1));
        DisplayMetrics dm = getResources().getDisplayMetrics();

        // ---------- map / widget area with overlays
        FrameLayout top = new FrameLayout(this);
        View.OnTouchListener touch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                lastTouch = System.currentTimeMillis();
                return false;
            }
        };
        mapTouch = touch;
        mapSlot = new FrameLayout(this);
        host = new NoMapHost(this, touch, cMidTop, cMidBot, cText, "");   // replaced right after the first frame
        mapSlot.addView(host.view(), new FrameLayout.LayoutParams(-1, -1));
        top.addView(mapSlot, new FrameLayout.LayoutParams(-1, -1));
        handler.post(new Runnable() {
            @Override
            public void run() {
                loadMap();
            }
        });

        if (vigStrength > 0) top.addView(new VignetteView(this, vigStrength), new FrameLayout.LayoutParams(-1, -1));
        buildYtPane(top);

        // speed light from the top edge (yellow from 80 km/h, red from 120 km/h)
        lightGlow = new SpeedGlow(this);
        top.addView(lightGlow, new FrameLayout.LayoutParams(-1, -1));
        lightLine = new View(this);
        top.addView(lightLine, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));
        setLight(0);

        // navigation + city text (no box behind it)
        infoPanel = new LinearLayout(this);
        infoPanel.setOrientation(LinearLayout.VERTICAL);
        int clearBottom = Math.round(dm.heightPixels * (barH() + (pipOn() ? dockH() : 0f)
                + (extOn() ? shieldH() / 100f : 0f))) + dp(14);
        infoPanel.setPadding(dp(12), dp(14), dp(18), clearBottom);
        navBox = infoBox();
        navArrow = new ImageView(this);
        navBox.addView(navArrow, new LinearLayout.LayoutParams(dp(120), dp(120), Gravity.END));
        navTitle = text(34, WHITE, true);
        navText = text(22, 0xFFD5DADF, false);
        navSub = text(18, 0xFFB8C0C8, false);
        for (TextView t : new TextView[]{navTitle, navText, navSub}) {
            t.setGravity(Gravity.END);
            t.setShadowLayer(8, 0, 1, 0xFF000000);
        }
        navBox.addView(navTitle);
        navBox.addView(navText);
        navBox.addView(navSub);
        infoPanel.addView(navBox, boxLp());
        View spacer = new View(this);
        infoPanel.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
        cityBox = infoBox();
        cityText = text(32, WHITE, true);
        subText = text(18, 0xFFD5DADF, false);
        cityText.setGravity(Gravity.END);
        subText.setGravity(Gravity.END);
        cityText.setShadowLayer(8, 0, 1, 0xFF000000);
        subText.setShadowLayer(8, 0, 1, 0xFF000000);
        cityBox.addView(cityText);
        cityBox.addView(subText);
        infoPanel.addView(cityBox, boxLp());
        hintText = text(15, WHITE, true);
        hintText.setText("Tap here to enable navigation info");
        hintText.setGravity(Gravity.CENTER);
        hintText.setPadding(dp(12), dp(12), dp(12), dp(12));
        hintText.setBackground(glass(RED, false));
        hintText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        infoPanel.addView(hintText, boxLp());
        updateBoxes();
        leftPanel = new FrameLayout(this);
        leftPanel.addView(new Backdrop(this), new FrameLayout.LayoutParams(-1, -1));
        // navigation and city info on the side away from the 360 window
        top.addView(infoPanel, new FrameLayout.LayoutParams(dp(360), -1, mirror() ? Gravity.START : Gravity.END));
        String logoPath = cfg.getString("logoFile", "");
        if (logoPath.length() > 0 && new File(logoPath).exists()) {
            Bitmap lb = BitmapFactory.decodeFile(logoPath);
            if (lb != null) {
                ImageView logo = new ImageView(this);
                logo.setImageBitmap(lb);
                logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                logo.setAlpha(0.9f);
                int ls = dp(Math.max(24, Math.min(240, cfg.getInt("logoSize", 160))));
                FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(ls, ls, Gravity.TOP | (mirror() ? Gravity.END : Gravity.START));
                llp.topMargin = dp(12);
                llp.leftMargin = dp(16);
                llp.rightMargin = dp(16);
                logo.setOnLongClickListener(settingsLongClick());
                top.addView(logo, llp);
            }
        }
        infoPanel.setVisibility(cfg.getBoolean("showInfo", true) ? View.VISIBLE : View.GONE);

        body.addView(top);                       // child 0

        // ---------- slot behind the 360 window: date, heading, altitude
        FrameLayout slot = new FrameLayout(this);
        slot.addView(new Backdrop(this), new FrameLayout.LayoutParams(-1, -1));
        LinearLayout sg = new LinearLayout(this);
        sg.setOrientation(LinearLayout.VERTICAL);
        sg.setGravity(Gravity.CENTER);
        slotDate = text(28, cText, true);
        slotHeading = text(22, cSub, false);
        slotAlt = text(22, cSub, false);
        slotGps = text(16, cSub, false);
        for (TextView t : new TextView[]{slotDate, slotHeading, slotAlt, slotGps}) {
            t.setGravity(Gravity.CENTER);
            sg.addView(t, new LinearLayout.LayoutParams(-2, -2));
        }
        slot.addView(sg, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        slot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPref("pkg360");
            }
        });
        slot.setOnLongClickListener(settingsLongClick());
        slot.setVisibility(pipOn() ? View.VISIBLE : View.GONE);
        body.addView(slot);                      // child 1

        // ---------- contacts bar (one rounded bar, glass icons)
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        if (mirror()) bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        FrameLayout cam = cell();
        Badge camBadge = new Badge(this, 0);
        camBadge.setLabel("360°");
        cam.addView(camBadge, new FrameLayout.LayoutParams(-1, -1));
        cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPref("pkg360");
            }
        });
        cam.setOnLongClickListener(settingsLongClick());
        int prim = cfg.getInt("primary", 0);
        if (pipOn()) {
            bar.addView(cam, barLp());
            for (int i = 0; i < 3; i++) bar.addView(contactCell(i), barLp());
        } else if (extOn()) {
            // the frame's own bar holds 360 and the primary contact, so this bar gets the other things
            bar.addView(barIcon(Glyph.RADIO, "radio", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchPref("pkgRadio");
                }
            }, null), barLp());
            FrameLayout th = barIcon(Glyph.THEME, "theme", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleTheme();
                }
            }, null);
            themeGlyph = (Glyph) th.getChildAt(1);
            themeGlyph.setState(cfg.getInt("theme", 2) == 2 ? 1 : 0);
            bar.addView(th, barLp());
            bar.addView(barIcon(Glyph.DRAWER, "apps", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDrawer();
                }
            }, null), barLp());
            for (int i = 0; i < 3; i++) {
                if (i != prim) bar.addView(contactCell(i), barLp());
            }
        } else {
            // simple layout: radio, 360, map/media switch, three contacts
            bar.addView(barIcon(Glyph.RADIO, "radio", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchPref("pkgRadio");
                }
            }, new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showDrawer();
                    return true;
                }
            }), barLp());
            bar.addView(cam, barLp());
            bar.addView(switchCell(true), barLp());
            for (int i = 0; i < 3; i++) bar.addView(contactCell(i), barLp());
        }
        FrameLayout barWrap = new FrameLayout(this);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor((cTileFill & 0x00FFFFFF) | 0xB8000000);
        pill.setStroke(Math.max(1, rdp(1)), cTileStroke);
        pill.setCornerRadius(rdp(tileRadius));
        bar.setBackground(pill);
        GradientDrawable sep = new GradientDrawable();
        sep.setColor(0x66FFFFFF);
        sep.setSize(Math.max(1, rdp(1)), 1);
        bar.setDividerDrawable(sep);
        bar.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        bar.setDividerPadding(rdp(14));
        bar.setOnLongClickListener(settingsLongClick());
        // keep the bar inside the frame's margin and rounded corners
        int fm = cfg.getBoolean("frame", true) ? rdp(cfg.getInt("margin", 8)) : 0;
        FrameLayout.LayoutParams pillLp = new FrameLayout.LayoutParams(-1, -1);
        pillLp.setMargins(fm + rdp(8), rdp(6), fm + rdp(8), rdp(6) + (pipOn() || extOn() ? 0 : fm));
        barWrap.addView(bar, pillLp);
        body.addView(barWrap);                   // child 2

        // ---------- dock: clock under the 360 window, icons under the bar
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(WHITE);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, dm.heightPixels * dockH() * 0.45f);
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        clock.setGravity(Gravity.CENTER);
        clock.setBackgroundColor(Color.BLACK);
        clock.setOnLongClickListener(settingsLongClick());
        clock.setVisibility(pipOn() ? View.VISIBLE : View.GONE);
        body.addView(clock);                     // child 3

        LinearLayout dockIcons = new LinearLayout(this);
        dockIcons.setOrientation(LinearLayout.HORIZONTAL);
        dockIcons.setBackgroundColor(Color.BLACK);   // plain black bar, square corners
        for (String did : dockOrder()) {
            dockIcons.addView(dockItem(did), new LinearLayout.LayoutParams(0, -1, 1f));
        }
        if (mirror()) dockIcons.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        dockIcons.setVisibility(pipOn() ? View.VISIBLE : View.GONE);
        body.addView(dockIcons);                 // child 4
        body.addView(leftPanel);                 // child 5

        // chrome lines + (if the frame service is not running) the margin and rounded corners
        overlay = new Overlay(this);
        rootFrame.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        cover = new View(this);          // black until the map has had time to draw (no white tiles)
        cover.setBackgroundColor(Color.BLACK);
        rootFrame.addView(cover, new FrameLayout.LayoutParams(-1, -1));
        setContentView(rootFrame);
    }

    private View cover;

    private void fadeCover(long delay) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final View c = cover;
                if (c == null) return;
                cover = null;
                c.animate().alpha(0f).setDuration(450).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (c.getParent() instanceof ViewGroup) ((ViewGroup) c.getParent()).removeView(c);
                    }
                }).start();
            }
        }, delay);
    }

    /** Liquid-glass look: a dark translucent body tinted with the colour, a soft coloured glow and a light rim. */
    private Drawable glass(int glow, boolean oval) {
        int g = glow & 0x00FFFFFF;
        GradientDrawable halo = new GradientDrawable();
        GradientDrawable body = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x70000000 | g, 0xCC0C1218});
        halo.setColor(0x38000000 | g);
        if (oval) {
            halo.setShape(GradientDrawable.OVAL);
            body.setShape(GradientDrawable.OVAL);
        } else {
            halo.setCornerRadius(dp(22));
            body.setCornerRadius(dp(18));
        }
        body.setStroke(Math.max(1, dp(1)), 0x66FFFFFF);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{halo, body});
        int in = dp(3);
        ld.setLayerInset(1, in, in, in, in);
        return ld;
    }

    private FrameLayout cell() {
        FrameLayout f = new FrameLayout(this);
        f.setClickable(true);   // one rounded bar holds all cells (see barWrap), so no tile of its own
        return f;
    }

    private LinearLayout.LayoutParams barLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        return lp;
    }

    private static final String[] DOCK_IDS = {"radio", "mapyt", "nav", "web", "music", "phone", "car", "theme", "apps", "settings"};
    private static final String[] DOCK_NAMES = {"Radio", "Map / YouTube switch", "Open the navigation app",
            "Open YouTube in the browser", "Music app", "Phone dialer", "Car or vehicle app", "Day / night button", "All apps", "Settings"};
    private static final String DOCK_DEFAULT = "radio,mapyt,nav,web,music,phone,car,theme,apps,settings";

    /** The dock order the user chose; items missing from the saved list are added at the end. */
    private ArrayList<String> dockOrder() {
        ArrayList<String> out = new ArrayList<String>();
        for (String s : cfg.getString("dockItems", DOCK_DEFAULT).split(",")) {
            for (String id : DOCK_IDS) if (id.equals(s) && !out.contains(id)) out.add(id);
        }
        for (String id : DOCK_IDS) if (!out.contains(id)) out.add(id);
        return out;
    }

    private FrameLayout dockItem(String id) {
        if (id.equals("radio")) {
            return dockCell(Glyph.RADIO, "radio", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchPref("pkgRadio");
                }
            }, null);
        } else if (id.equals("mapyt")) {
            return switchCell(false);
        } else if (id.equals("nav")) {
            return dockCell(Glyph.NAVI, "navigation", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openNav();
                }
            }, null);
        } else if (id.equals("web")) {
            return dockCell(Glyph.YT, "web", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openYoutube();
                }
            }, null);
        } else if (id.equals("music")) {
            return dockCell(Glyph.MUSIC, "music", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchPref("pkgMusic");
                }
            }, null);
        } else if (id.equals("phone")) {
            return dockCell(Glyph.DIAL, "phone", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent d = new Intent(Intent.ACTION_DIAL);
                        d.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(d);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "No phone app found", Toast.LENGTH_LONG).show();
                    }
                }
            }, null);
        } else if (id.equals("car")) {
            return dockCell(Glyph.CAR, "car", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchPref("pkgCar");
                }
            }, null);
        } else if (id.equals("theme")) {
            FrameLayout themeCell = dockCell(Glyph.THEME, "theme", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleTheme();
                }
            }, null);
            themeGlyph = (Glyph) themeCell.getChildAt(0);
            themeGlyph.setState(cfg.getInt("theme", 2) == 2 ? 1 : 0);
            return themeCell;
        } else if (id.equals("apps")) {
            return dockCell(Glyph.DRAWER, "apps", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDrawer();
                }
            }, null);
        }
        return dockCell(Glyph.GEAR, "settings", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editSettings();
            }
        }, null);
    }

    private FrameLayout dockCell(int glyph, String id, View.OnClickListener click, View.OnLongClickListener lc) {
        FrameLayout f = new FrameLayout(this);
        f.setClickable(true);
        Glyph g = new Glyph(this, glyph);
        g.setFrac(0.62f);
        int ic = iconColor(id, 0xFFF2F5F8); // the bottom bar is black in every theme
        g.setColor(ic);
        f.addView(g, new FrameLayout.LayoutParams(-1, -1));
        f.setOnClickListener(click);
        f.setOnLongClickListener(lc != null ? lc : settingsLongClick());
        return f;
    }

    /** A bar icon on a glass disc (glyph is child 1 so callers can reach it). */
    private FrameLayout barIcon(int glyphType, String id, View.OnClickListener click, View.OnLongClickListener lc) {
        FrameLayout f = cell();
        f.addView(new View(this), new FrameLayout.LayoutParams(-1, -1));   // keeps the glyph at child 1
        Glyph g = new Glyph(this, glyphType);
        g.setFrac(0.50f);
        g.setColor(iconColor(id, 0xFFF2F5F8));
        f.addView(g, new FrameLayout.LayoutParams(-1, -1));
        f.setOnClickListener(click);
        f.setOnLongClickListener(lc != null ? lc : settingsLongClick());   // hold = settings (emergency way in)
        return f;
    }

    /** Map / media switch: tap opens Google Maps or the browser (whichever the widget shows), hold changes the widget. */
    private FrameLayout switchCell(boolean glassy) {
        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pipOn()) toggleYt();   // 360 window layout: tap switches, the two open buttons are separate dock icons
                else switchClick();
            }
        };
        View.OnLongClickListener hold = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleYt();
                return true;
            }
        };
        FrameLayout f;
        Glyph g;
        if (glassy) {
            f = barIcon(Glyph.MAP, "switch", click, hold);
            g = (Glyph) f.getChildAt(1);
        } else {
            f = dockCell(Glyph.MAP, "switch", click, hold);
            g = (Glyph) f.getChildAt(0);
        }
        switchGlyphs.add(g);
        return f;
    }

    private FrameLayout contactCell(final int i) {
        FrameLayout f = cell();
        Badge c = new Badge(this, 1);
        f.addView(c, new FrameLayout.LayoutParams(-1, -1));
        contactBadge[i] = c;
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

    // ------------------------------------------------------------------ summary slot

    private static final String[] DIRS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private void updateSlot() {
        if (slotDate == null) return;
        slotDate.setText(new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date()));
        long now = System.currentTimeMillis();
        boolean fresh = lastLoc != null && now - lastSpeedTime < 15000;
        if (fresh && lastLoc.hasBearing() && curKmh > 3f) {
            int b = Math.round(lastLoc.getBearing());
            slotHeading.setText("Heading " + DIRS[Math.round(b / 45f) % 8] + " " + b + "°");
        } else {
            slotHeading.setText("Heading --");
        }
        if (!Double.isNaN(mslAlt) && now - mslTime < 15000) {
            slotAlt.setText("Altitude " + Math.round(mslAlt) + " m");
        } else {
            slotAlt.setText(fresh && lastLoc.hasAltitude() ? "Altitude " + Math.round(lastLoc.getAltitude()) + " m" : "Altitude --");
        }
        slotGps.setText(fresh && lastLoc.hasAccuracy() ? "GPS ±" + Math.round(lastLoc.getAccuracy()) + " m" : "No GPS fix");
    }

    // ------------------------------------------------------------------ navigation info

    private LinearLayout navBox, cityBox;

    /** Dark rounded box so the white text is readable on the map and in the light theme. */
    private LinearLayout infoBox() {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.END);
        b.setPadding(dp(6), dp(6), dp(6), dp(6));
        return b;
    }

    private LinearLayout.LayoutParams boxLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(6);
        return lp;
    }

    private void updateBoxes() {
        boolean nav = NavListener.icon != null || navTitle.getText().length() > 0 || navText.getText().length() > 0 || navSub.getText().length() > 0;
        navBox.setVisibility(nav ? View.VISIBLE : View.GONE);
        cityBox.setVisibility(cityText.getText().length() > 0 || subText.getText().length() > 0 ? View.VISIBLE : View.GONE);
    }

    private void refreshNav() {
        CharSequence t = NavListener.title;
        CharSequence x = NavListener.text;
        navTitle.setText(t == null ? "" : t);
        navText.setText(x == null ? "" : x);
        CharSequence sb = NavListener.sub;
        navSub.setText(sb == null ? "" : sb);
        if (NavListener.icon != null) {
            navArrow.setImageBitmap(NavListener.icon);
            navArrow.setVisibility(View.VISIBLE);
        } else {
            navArrow.setImageDrawable(null);
            navArrow.setVisibility(View.GONE);
        }
        updateBoxes();
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
            host.showCar(); // marker only, centring is handled by recentre()
            startNmea();
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
        stopNmea();
    }

    /** Height above sea level from the GPS sentences (Location.getAltitude() is measured from a model of the earth and can be 100 m off). */
    private void startNmea() {
        if (Build.VERSION.SDK_INT < 24 || lm == null) return;
        try {
            android.location.OnNmeaMessageListener l = new android.location.OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String m, long t) {
                    if (m == null || m.length() < 10 || !m.startsWith("$") || !m.substring(3, 6).equals("GGA")) return;
                    String[] f = m.split(",");
                    if (f.length > 9 && f[6].length() > 0 && !f[6].equals("0") && f[9].length() > 0) {
                        try {
                            mslAlt = Double.parseDouble(f[9]);
                            mslTime = System.currentTimeMillis();
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            };
            lm.addNmeaListener(l);
            nmeaListener = l;
        } catch (Throwable ignored) {
        }
    }

    private void stopNmea() {
        if (Build.VERSION.SDK_INT < 24 || lm == null || nmeaListener == null) return;
        try {
            lm.removeNmeaListener((android.location.OnNmeaMessageListener) nmeaListener);
        } catch (Throwable ignored) {
        }
        nmeaListener = null;
    }

    private final LocationListener locListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            if (loc == null) return;
            lastLoc = loc;
            long tNow = System.currentTimeMillis();
            if (tNow - lastLocSave > 600000) {
                lastLocSave = tNow;
                cfg.edit().putFloat("lat", (float) loc.getLatitude()).putFloat("lon", (float) loc.getLongitude()).apply();
            }
            host.setCar(loc.getLatitude(), loc.getLongitude());
            // the network provider has no speed, so only GPS fixes update the speed
            if (LocationManager.GPS_PROVIDER.equals(loc.getProvider())) {
                curKmh = loc.hasSpeed() ? loc.getSpeed() * 3.6f : 0f;
                lastSpeedTime = tNow;
            }
            setLight(curKmh);
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

    /** Soft coloured light along the top and both sides of the map (calm below 80 km/h, amber from 80, red from 120). */
    private class SpeedGlow extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int color = 0;
        private int alpha = 0;

        SpeedGlow(Context c) {
            super(c);
        }

        void set(int col, int a) {
            color = col;
            alpha = a;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            if (w <= 0 || alpha <= 0) return;
            int strong = (alpha << 24) | (color & 0x00FFFFFF);
            int mid = ((alpha / 3) << 24) | (color & 0x00FFFFFF);
            int none = color & 0x00FFFFFF;
            float th = h / 3f;   // the top third only
            p.setShader(new LinearGradient(0, 0, 0, th, new int[]{strong, mid, none}, new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, th, p);
            p.setShader(null);
        }
    }

    private void setLight(float kmh) {
        int col, a;
        if (kmh >= 120f) {
            col = 0xFFFF3B30;
            a = 0xE0;
        } else if (kmh >= 80f) {
            col = 0xFFFFBE1E;
            a = 0xC8;
        } else {
            col = 0;
            a = 0;
        }
        lightGlow.set(col, a);
        lightLine.setBackgroundColor(a == 0 ? 0 : ((0xFF << 24) | (col & 0x00FFFFFF)));
    }

    // ------------------------------------------------------------------ navigation app + map style

    private FrameLayout mapSlot;
    private View.OnTouchListener mapTouch;

    private void swapHost(MapHost h) {
        MapHost old = host;
        mapSlot.removeAllViews();
        mapSlot.addView(h.view(), new FrameLayout.LayoutParams(-1, -1));
        host = h;
        try {
            if (old != null) old.onDestroy();
        } catch (Throwable ignored) {
        }
        if (resumed) h.onResume();
        h.showCar();
        if (lastLoc != null) {
            h.setCar(lastLoc.getLatitude(), lastLoc.getLongitude());
            h.centerOn(lastLoc.getLatitude(), lastLoc.getLongitude());
        } else if (cfg.contains("lat") && cfg.contains("lon")) {
            h.centerOn(cfg.getFloat("lat", 6.9271f), cfg.getFloat("lon", 79.8612f));
        }
    }

    /** Loads the offline map after the first frame (it is the slowest part of start-up). */
    private void loadMap() {
        if (isFinishing() || isDestroyed() || mapSlot == null) return;
        float textScale = Math.max(100, Math.min(300, cfg.getInt("mapText", 300))) / 100f;
        double zoom = Math.max(12, Math.min(19, cfg.getInt("mapZoom", 18)));
        String mf = cfg.getString("mapFile", "");
        File mfile = new File(mf);
        MapHost h = null;
        String mb = cfg.getString("mbFile", "");
        if (cfg.getInt("mapEngine", 0) == 1 && mb.length() > 0 && new File(mb).exists()) {
            try {
                h = new MapLibreHost(this, new File(mb), mapTouch, zoom - 1, cfg.getInt("mapTilt", 55),
                        ThemeFile.col(mapPal, "ground", night ? 0xFF0A1220 : 0xFFE9EEF3),
                        ThemeFile.col(mapPal, "water", night ? 0xFF0E2238 : 0xFFC6D8E8),
                        ThemeFile.col(mapPal, "road", night ? 0xFF4F6A86 : 0xFF7B8EA3),
                        ThemeFile.col(mapPal, "major", night ? 0xFF9FB8D4 : 0xFF33495F),
                        ThemeFile.col(mapPal, "building", night ? 0xFF26384D : 0xFFB7C3CF), cAccent);
            } catch (Throwable t) {
                h = null;
                Toast.makeText(this, "The 3D map could not start, using the normal map", Toast.LENGTH_LONG).show();
            }
        }
        if (h == null && mf.length() > 0 && mfile.exists()) {
            File themeFile = cfg.getInt("mapStyle", 0) == 0 ? writeMapTheme() : null;
            try {
                h = new MapsforgeHost(this, mfile, mapTouch, night, themeFile, cAccent, (int) zoom - 1, textScale);
            } catch (Throwable t) {
                Toast.makeText(this, "The offline map file could not be opened", Toast.LENGTH_LONG).show();
            }
        }
        if (h == null) {
            h = new NoMapHost(this, mapTouch, cMidTop, cMidBot, cText,
                    "No offline map yet\nOpen Settings > Map and choose a .map file");
        }
        swapHost(h);
        fadeCover(1100);
    }

    /** Opens the navigation app (Google Maps unless another one was chosen in settings). */
    private void openNav() {
        String pkg = cfg.getString("pkgNav", "com.google.android.apps.maps");
        if (pkg.length() == 0) pkg = "com.google.android.apps.maps";
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            Toast.makeText(this, "Navigation app not found. Choose one in settings.", Toast.LENGTH_LONG).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open the navigation app", Toast.LENGTH_LONG).show();
        }
    }

    /** Writes the line-art map style (colours from the theme) and returns the file, or null if that failed. */
    private File writeMapTheme() {
        try {
            int ground = ThemeFile.col(mapPal, "ground", night ? 0xFF0A1220 : 0xFFE9EEF3);
            int water = ThemeFile.col(mapPal, "water", night ? 0xFF0E2238 : 0xFFC6D8E8);
            int road = ThemeFile.col(mapPal, "road", night ? 0xFF4F6A86 : 0xFF7B8EA3);
            int major = ThemeFile.col(mapPal, "major", night ? 0xFF9FB8D4 : 0xFF33495F);
            int bld = ThemeFile.col(mapPal, "building", night ? 0xFF26384D : 0xFFB7C3CF);
            int text = ThemeFile.col(mapPal, "text", night ? 0xFFDCE8F5 : 0xFF1B2733);
            int halo = ThemeFile.col(mapPal, "halo", night ? 0xCC0A1220 : 0xCCE9EEF3);
            String xml = LineArtTheme.xml(ground, water, road, major, bld, text, halo);
            File f = new File(getFilesDir(), "rehome-lineart-" + Integer.toHexString(xml.hashCode()) + ".xml");
            if (!f.exists()) {
                File[] old = getFilesDir().listFiles();
                if (old != null) {
                    for (File o : old) {
                        if (o.getName().startsWith("rehome-lineart-")) o.delete();
                    }
                }
                FileOutputStream os = new FileOutputStream(f);
                os.write(xml.getBytes("UTF-8"));
                os.close();
            }
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ media widget (what is playing, from the notification)

    private Glyph ytPauseGlyph;
    private final ArrayList<Glyph> switchGlyphs = new ArrayList<Glyph>();

    private void buildYtPane(FrameLayout top) {
        ytPane = new FrameLayout(this);
        ytPane.setBackgroundColor(Color.BLACK);
        ytPane.setVisibility(View.GONE);

        ytShade = new FrameLayout(this);
        ytShade.setBackgroundColor(Color.BLACK);
        ytImage = new ImageView(this);
        ytImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ytShade.addView(ytImage, new FrameLayout.LayoutParams(-1, -1));
        ytTitle = text(18, WHITE, true);
        ytTitle.setShadowLayer(6, 0, 1, 0xFF000000);
        ytTitle.setPadding(dp(16), dp(10), dp(16), dp(10));
        ytTitle.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xE0000000, 0x00000000}));
        ytTitle.setGravity(Gravity.BOTTOM);
        ytTitle.setText(lastYtTitle);
        if (lastYtBmp != null) ytImage.setImageBitmap(lastYtBmp);
        ytShade.addView(ytTitle, new FrameLayout.LayoutParams(-1, dp(110), Gravity.BOTTOM));
        ytPane.addView(ytShade, new FrameLayout.LayoutParams(-1, -1));
        if (ytVig > 0) ytPane.addView(new VignetteView(this, ytVig), new FrameLayout.LayoutParams(-1, -1));

        // play / pause button
        ytPlayBtn = new FrameLayout(this);
        ytPlayBtn.setBackground(glass(0xFF2D7BFF, true));
        ytPlayGlyph = new Glyph(this, Glyph.PLAY);
        ytPlayGlyph.setColor(WHITE);
        ytPlayGlyph.setFrac(0.55f);
        ytPauseGlyph = new Glyph(this, Glyph.PAUSE);
        ytPauseGlyph.setColor(WHITE);
        ytPauseGlyph.setFrac(0.55f);
        ytPlayBtn.addView(ytPlayGlyph, new FrameLayout.LayoutParams(-1, -1));
        ytPlayBtn.addView(ytPauseGlyph, new FrameLayout.LayoutParams(-1, -1));
        ytPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ytPlayPause();
            }
        });
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(dp(92), dp(92), Gravity.BOTTOM | Gravity.END);
        DisplayMetrics pdm = getResources().getDisplayMetrics();
        plp.bottomMargin = Math.round(pdm.heightPixels * (barH() + (pipOn() ? dockH() : 0f)
                + (extOn() ? shieldH() / 100f : 0f))) + dp(24);
        plp.rightMargin = dp(40);
        ytPane.addView(ytPlayBtn, plp);
        top.addView(ytPane, new FrameLayout.LayoutParams(-1, -1));
        updateYtUi();
    }

    private void openYoutube() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com"));
        String bp = cfg.getString("pkgBrowser", "");
        if (bp.length() == 0) {
            try {
                getPackageManager().getPackageInfo("com.brave.browser", 0);
                bp = "com.brave.browser";
            } catch (Exception ignored) {
            }
        }
        if (bp.length() > 0) i.setPackage(bp);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "No browser found. Install Brave (see the Guide tab) or choose one in settings.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean mediaShown() {
        return ytPane != null && ytPane.getVisibility() == View.VISIBLE;
    }

    /** Click on the switch button: open the app that belongs to what the widget shows. */
    private void switchClick() {
        if (mediaShown()) openYoutube();
        else openNav();
    }

    /** Hold on the switch button: change the widget between the map and what is playing. */
    private void toggleYt() {
        boolean show = !mediaShown();
        ytPane.setVisibility(show ? View.VISIBLE : View.GONE);
        infoPanel.setVisibility(show ? View.GONE : (cfg.getBoolean("showInfo", true) ? View.VISIBLE : View.GONE));
        updateYtUi();
        refreshYt();
    }

    private void updateYtUi() {
        if (ytShade == null) return;
        ytPlayGlyph.setVisibility(ytPlayingNow ? View.GONE : View.VISIBLE);
        ytPauseGlyph.setVisibility(ytPlayingNow ? View.VISIBLE : View.GONE);
        // the switch button shows a map pin while the map is the widget, and the video icon while media is
        for (Glyph g : switchGlyphs) g.setType(mediaShown() ? Glyph.YT : Glyph.MAP);
    }

    private MediaController pickSession() {
        MediaSessionManager msm = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        List<MediaController> list = msm.getActiveSessions(new ComponentName(this, NavListener.class));
        String radio = cfg.getString("pkgRadio", "");
        MediaController first = null;
        for (MediaController mc : list) {
            String p = mc.getPackageName();
            if (p == null || p.equals(radio) || p.equals(getPackageName())) continue;
            PlaybackState ps = mc.getPlaybackState();
            if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) return mc;
            if (first == null && mc.getMetadata() != null) first = mc;
        }
        return first;
    }

    private void refreshYt() {
        if (ytPane == null) return;
        try {
            MediaController mc = pickSession();
            if (mc == null) {
                videoPlaying = false;
                ytPlayingNow = false;
            } else {
                MediaMetadata md = mc.getMetadata();
                if (md != null) {
                    Bitmap b = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
                    if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                    if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                    if (b != null) lastYtBmp = b;
                    CharSequence t = md.getText(MediaMetadata.METADATA_KEY_TITLE);
                    if (t != null && t.length() > 0) lastYtTitle = t.toString();
                }
                PlaybackState ps = mc.getPlaybackState();
                ytPlayingNow = ps != null && ps.getState() == PlaybackState.STATE_PLAYING;
                videoPlaying = ytPlayingNow;
            }
            if (lastYtBmp != null) ytImage.setImageBitmap(lastYtBmp);
            ytTitle.setText(lastYtTitle.length() > 0 ? lastYtTitle
                    : "Nothing playing yet. Tap the video button below to open YouTube.");
        } catch (SecurityException e) {
            ytTitle.setText("Allow notification access first (see the Guide tab)");
        } catch (Exception e) {
            ytTitle.setText("Media info unavailable");
        }
        updateYtUi();
    }

    private void ytPlayPause() {
        boolean done = false;
        try {
            MediaController mc = pickSession();
            if (mc != null) {
                if (ytPlayingNow) mc.getTransportControls().pause();
                else mc.getTransportControls().play();
                done = true;
            }
        } catch (Exception ignored) {
        }
        if (!done) mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshYt();
            }
        }, 700);
    }

    // ------------------------------------------------------------------ audio / actions

    private void mediaKey(int code) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
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
        if (pkg.equals(cfg.getString("pkg360", ""))) FloatingService.cover();
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

    private boolean inList(String key, String pkg) {
        return ("," + cfg.getString(key, "") + ",").contains("," + pkg + ",");
    }

    private void setInList(String key, String pkg, boolean on) {
        String[] parts = cfg.getString(key, "").split(",");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.length() == 0 || p.equals(pkg)) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(p);
        }
        if (on) {
            if (sb.length() > 0) sb.append(',');
            sb.append(pkg);
        }
        cfg.edit().putString(key, sb.toString()).apply();
    }

    private String pickedButtons(CheckBox[] cb) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < cb.length; k++) {
            if (cb[k].isChecked()) {
                if (sb.length() > 0) sb.append(',');
                sb.append(FloatingService.BTN_IDS[k]);
            }
        }
        return sb.toString();
    }

    /** A button that opens a tick-list of apps and stores the chosen packages (comma separated) under key. */
    private Button appsButton(final String title, final String key) {
        final Button b = flat("");
        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                String v = cfg.getString(key, "");
                int n = v.length() == 0 ? 0 : v.split(",").length;
                b.setText(title + (n == 0 ? " (default: the 360 app)" : " (" + n + " chosen)"));
            }
        };
        refresh.run();
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<ResolveInfo> apps = launchableApps();
                final PackageManager pm = getPackageManager();
                String cur = "," + cfg.getString(key, cfg.getString("pkg360", "")) + ",";
                CharSequence[] names = new CharSequence[apps.size()];
                final boolean[] on = new boolean[apps.size()];
                for (int i = 0; i < names.length; i++) {
                    names[i] = apps.get(i).loadLabel(pm);
                    on[i] = cur.contains("," + apps.get(i).activityInfo.packageName + ",");
                }
                new android.app.AlertDialog.Builder(MainActivity.this, android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                        .setTitle(title)
                        .setMultiChoiceItems(names, on, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which, boolean checked) {
                                on[which] = checked;
                            }
                        })
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < on.length; i++) {
                                    if (on[i]) {
                                        if (sb.length() > 0) sb.append(',');
                                        sb.append(apps.get(i).activityInfo.packageName);
                                    }
                                }
                                cfg.edit().putString(key, sb.toString()).apply();
                                refresh.run();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        return b;
    }

    private void linkButton(LinearLayout l, String text, final String url) {
        Button b = flat(text);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No browser found", Toast.LENGTH_LONG).show();
                }
            }
        });
        addFull(l, b);
    }

    private void chooseApp(String title, final Pick cb) {
        final List<ResolveInfo> apps = launchableApps();
        final PackageManager pm = getPackageManager();
        ListView lv = new ListView(this);
        lv.setAdapter(new BaseAdapter() {
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
                row.setPadding(rdp(14), rdp(8), rdp(14), rdp(8));
                ImageView iv = new ImageView(MainActivity.this);
                ResolveInfo ri = apps.get(p);
                iv.setImageDrawable(ri.loadIcon(pm));
                row.addView(iv, new LinearLayout.LayoutParams(rdp(44), rdp(44)));
                TextView tv = new TextView(MainActivity.this);
                tv.setText(ri.loadLabel(pm));
                tv.setTextColor(WHITE);
                tv.setTextSize(18);
                tv.setPadding(rdp(14), 0, 0, 0);
                row.addView(tv);
                return row;
            }
        });
        final Panel pn = new Panel(title, lv, false);
        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> a, View v, int which, long id) {
                pn.d.dismiss();
                ResolveInfo ri = apps.get(which);
                cb.onPick(ri.activityInfo.packageName, ri.loadLabel(pm).toString());
            }
        });
        pn.button("Cancel", null).show();
    }

    private void showDrawer() {
        final List<ResolveInfo> apps = launchableApps();
        final PackageManager pm = getPackageManager();
        android.widget.GridView gv = new android.widget.GridView(this);
        gv.setNumColumns(6);
        gv.setVerticalSpacing(rdp(14));
        gv.setHorizontalSpacing(rdp(6));
        gv.setPadding(rdp(12), rdp(14), rdp(12), rdp(6));
        gv.setSelector(new android.graphics.drawable.ColorDrawable(0));
        gv.setAdapter(new BaseAdapter() {
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
                LinearLayout cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                ImageView iv = new ImageView(MainActivity.this);
                ResolveInfo ri = apps.get(p);
                iv.setImageDrawable(ri.loadIcon(pm));
                cell.addView(iv, new LinearLayout.LayoutParams(rdp(64), rdp(64)));
                TextView tv = new TextView(MainActivity.this);
                tv.setText(ri.loadLabel(pm));
                tv.setTextColor(WHITE);
                tv.setTextSize(13);
                tv.setGravity(Gravity.CENTER);
                tv.setMaxLines(2);
                tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tv.setPadding(0, rdp(5), 0, 0);
                cell.addView(tv, new LinearLayout.LayoutParams(-1, -2));
                return cell;
            }
        });
        final Panel pn = new Panel("Apps", gv, false, true);
        gv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> a, View v, int which, long id) {
                pn.d.dismiss();
                launchPackage(apps.get(which).activityInfo.packageName);
            }
        });
        pn.button("Close", null).show();
    }

    // ------------------------------------------------------------------ contacts / calls

    private void refreshContacts() {
        String[] defName = {"Dad", "Amma", "Sis"};
        for (int i = 0; i < 3; i++) {
            String n = cfg.getString("name" + i, defName[i]);
            if (contactBadge[i] == null) continue;
            contactBadge[i].setLabel(n.length() > 0 ? n.substring(0, 1).toUpperCase() : "?");
            contactBadge[i].setPrimary(i == cfg.getInt("primary", 0));
        }
    }

    private PhoneAccountHandle findAccount(String key) {
        if (key == null || key.length() == 0) return null;
        try {
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            for (PhoneAccountHandle h : tm.getCallCapablePhoneAccounts()) {
                if ((h.getComponentName().flattenToString() + "|" + h.getId()).equals(key)) return h;
            }
        } catch (Throwable ignored) {
        }
        return null;
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
        if (mode == 0 && has(Manifest.permission.CALL_PHONE)) {
            call = new Intent(Intent.ACTION_CALL, uri);
            PhoneAccountHandle h = findAccount(cfg.getString("callAccount", ""));
            if (h != null) call.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, h);
        } else {
            call = new Intent(Intent.ACTION_DIAL, uri);
        }
        call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(call);
        } catch (Exception e) {
            Toast.makeText(this, "No phone app found for calls", Toast.LENGTH_LONG).show();
        }
    }

    private void editContact(final int i) {
        String[] defName = {"Dad", "Amma", "Sis"};
        LinearLayout l = column();
        final EditText n = field(l, "Name", cfg.getString("name" + i, defName[i]), InputType.TYPE_CLASS_TEXT);
        final EditText p = field(l, "Phone number", cfg.getString("num" + i, ""), InputType.TYPE_CLASS_PHONE);
        final CheckBox prim = check(l, "Primary contact (steering wheel voice button calls this)", cfg.getInt("primary", 0) == i);
        new Panel("Contact " + (i + 1), l, true)
                .button("Cancel", null)
                .button("Save", new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences.Editor ed = cfg.edit();
                        ed.putString("name" + i, n.getText().toString().trim());
                        ed.putString("num" + i, p.getText().toString().trim());
                        if (prim.isChecked()) ed.putInt("primary", i);
                        ed.apply();
                        refreshContacts();
                    }
                }).show();
    }

    private String appLabel(String pkg) {
        if (pkg == null || pkg.length() == 0) return "not set";
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    /** True when a call is ringing, dialling or active (and then it is ended / declined). */
    private boolean endCallIfActive() {
        try {
            android.telephony.TelephonyManager tel =
                    (android.telephony.TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tel == null || !has(Manifest.permission.READ_PHONE_STATE)) return false;
            if (tel.getCallState() == android.telephony.TelephonyManager.CALL_STATE_IDLE) return false;
        } catch (Throwable t) {
            return false;
        }
        boolean done = false;
        if (Build.VERSION.SDK_INT >= 28 && has("android.permission.ANSWER_PHONE_CALLS")) {
            try {
                TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
                done = tm.endCall();
            } catch (Throwable ignored) {
            }
        }
        Toast.makeText(this, done ? "Call ended" : "Could not end the call from here. Allow the Phone permission in the app settings.",
                Toast.LENGTH_LONG).show();
        return true;
    }

    private void wheelAction() {
        if (endCallIfActive()) return;
        int vm = cfg.getInt("voiceMode", 0);
        String voice = cfg.getString("pkgVoice", "");
        if (vm == 1 && voice.length() > 0) {
            launchPackage(voice);
        } else if (vm == 2) {
            String action = cfg.getString("wheelMacro", "");
            if (action.length() == 0) {
                Toast.makeText(this, "Set the macro action for the wheel button in settings", Toast.LENGTH_LONG).show();
            } else {
                sendBroadcast(new Intent(action));
            }
        } else {
            callContact(cfg.getInt("primary", 0));
        }
    }

    // ------------------------------------------------------------------ floating buttons + frame service

    private void updateFloating() {
        boolean want = cfg.getBoolean("floating", true) || cfg.getBoolean("frame", true);
        boolean can = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
        Intent svc = new Intent(this, FloatingService.class);
        try {
            if (want && can) startService(svc);
            else stopService(svc);
        } catch (Exception ignored) {
        }
    }

    /** Welcome guide, one Android screen per return to the launcher: home app, overlay permission, notification access. */
    private void guideNext() {
        int g = cfg.getInt("guide", 9);
        if (g == 1) {
            cfg.edit().putInt("guide", 2).apply();
            if (!isDefaultHome()) {
                Toast.makeText(this, "Choose Rehome: Scroll as the home app and tap Always", Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                    return;
                } catch (Exception e) {
                    // older Android: pressing Home asks which launcher to use
                    Intent h = new Intent(Intent.ACTION_MAIN);
                    h.addCategory(Intent.CATEGORY_HOME);
                    h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(h);
                        return;
                    } catch (Exception ignored) {
                    }
                }
            }
            g = 2;
        }
        if (g == 2) {
            cfg.edit().putInt("guide", 3).apply();
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                askOverlayPermission();
                return;
            }
            g = 3;
        }
        if (g == 3) {
            cfg.edit().putInt("guide", 9).apply();
            String en = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
            if (en == null || !en.contains(getPackageName())) {
                Toast.makeText(this, "Switch Rehome ON (turn-by-turn info), then press Back", Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isDefaultHome() {
        Intent h = new Intent(Intent.ACTION_MAIN);
        h.addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo ri = getPackageManager().resolveActivity(h, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null && getPackageName().equals(ri.activityInfo.packageName);
    }

    private void recreateSoon() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) recreate();
            }
        }, 400);
    }

    private void startGuide() {
        cfg.edit().putInt("guide", 1).apply();
        guideNext();
    }

    private void askOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow \"Display over other apps\" for the frame and floating buttons", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
            }
        }
    }

    private void showWelcome() {
        LinearLayout l = column();
        TextView m = new TextView(this);
        m.setText("Three quick choices. You can change all of them later.");
        m.setTextColor(WHITE);
        m.setTextSize(16);
        m.setPadding(0, rdp(6), 0, rdp(6));
        l.addView(m);
        label(l, "Look");
        final RadioGroup thG = radios(l, new String[]{"Dark (recommended)", "Automatic: dark at night, light by day"}, 7100, 0);
        label(l, "Where does the driver sit?");
        final RadioGroup seatG = radios(l, new String[]{"Right side", "Left side"}, 7200, 0);
        final CheckBox pipChk = check(l, "My unit shows a small 360 camera window over the launcher", false);
        label(l, "How the map / video widget works: the round button with a map pin or a video icon changes what the top area shows (the map, or what is playing in YouTube/Brave with play and pause). "
                + "In the simple layout, tap opens Google Maps or the browser and hold switches. With the 360 window layout, tap switches and two other bottom-bar icons open Maps and YouTube.");
        label(l, "After that Android will ask you for a few switches (home app, display over other apps, notification access). Say yes to each.");
        label(l, "To open the settings later: hold the 360° button or the clock, tap the gear icon, hold any bar icon, or open Rehome from the app list.");
        Panel p = new Panel("Welcome to Rehome", l, true);
        p.d.setCancelable(false);
        p.d.setCanceledOnTouchOutside(false);
        p.button("Start", new Runnable() {
            @Override
            public void run() {
                cfg.edit().putInt("theme", checkedIdx(thG, 7100, 2, 0) == 0 ? 2 : 0)
                        .putInt("seat", checkedIdx(seatG, 7200, 2, 0))
                        .putBoolean("pipOn", pipChk.isChecked())
                        .remove("floatX").remove("floatY").apply();
                startGuide();
                recreateSoon();
            }
        }).show();
    }

    // ------------------------------------------------------------------ OpenStreetMap (Nominatim)

    private static String http(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestProperty("User-Agent", "RehomeLauncher/0.2 (personal car launcher)");
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
                            updateBoxes();
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

    // ------------------------------------------------------------------ settings

    private void appButton(LinearLayout parent, final String title, final String[] store) {
        final Button b = flat(title + ": " + appLabel(store[0]));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseApp(title, new Pick() {
                    @Override
                    public void onPick(String pkg, String label) {
                        store[0] = pkg;
                        b.setText(title + ": " + label);
                    }
                });
            }
        });
        addFull(parent, b);
    }

    private String accountLabel(String key) {
        PhoneAccountHandle h = findAccount(key);
        if (h == null) return "System default";
        try {
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            PhoneAccount pa = tm.getPhoneAccount(h);
            if (pa != null && pa.getLabel() != null) return pa.getLabel().toString();
        } catch (Throwable ignored) {
        }
        return h.getId();
    }

    private void chooseAccount(final Button b, final String[] store) {
        try {
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            final List<PhoneAccountHandle> hs = tm.getCallCapablePhoneAccounts();
            final String[] names = new String[hs.size() + 1];
            final String[] keys = new String[hs.size() + 1];
            names[0] = "System default";
            keys[0] = "";
            for (int k = 0; k < hs.size(); k++) {
                PhoneAccountHandle h = hs.get(k);
                PhoneAccount pa = tm.getPhoneAccount(h);
                names[k + 1] = pa != null && pa.getLabel() != null ? pa.getLabel().toString() : h.getId();
                keys[k + 1] = h.getComponentName().flattenToString() + "|" + h.getId();
            }
            listPanel("Call through", names, new IdxCb() {
                @Override
                public void on(int i) {
                    store[0] = keys[i];
                    b.setText("Call through: " + names[i]);
                }
            });
        } catch (Throwable t) {
            Toast.makeText(this, "Allow the phone permission first (it is asked at start)", Toast.LENGTH_LONG).show();
        }
    }

    /** The orange starburst used as the "made with" mark. */
    static class ClaudeMark extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        ClaudeMark(Context c) {
            super(c);
            p.setColor(0xFFD97757);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, r = Math.min(w, h) / 2f;
            p.setStrokeWidth(r * 0.22f);
            int n = 12;
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n;
                float len = (i % 2 == 0) ? r * 0.96f : r * 0.7f;
                c.drawLine(cx + (float) Math.cos(a) * r * 0.2f, cy + (float) Math.sin(a) * r * 0.2f,
                        cx + (float) Math.cos(a) * len, cy + (float) Math.sin(a) * len, p);
            }
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    private LinearLayout newSec(ArrayList<String> names, ArrayList<LinearLayout> views, String name) {
        LinearLayout sec = column();
        names.add(name);
        views.add(sec);
        return sec;
    }

    /** Settings body in the style of the unit's own settings: categories on the left, rows on the right. */
    private View settingsBody(final ArrayList<String> names, final ArrayList<LinearLayout> views) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        final LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.VERTICAL);
        final ScrollView content = new ScrollView(this);
        final TextView[] tv = new TextView[names.size()];
        final IdxCb select = new IdxCb() {
            @Override
            public void on(int idx) {
                content.removeAllViews();
                LinearLayout sec = views.get(idx);
                if (sec.getParent() instanceof ViewGroup) ((ViewGroup) sec.getParent()).removeView(sec);
                content.addView(sec);
                content.scrollTo(0, 0);
                for (int k = 0; k < tv.length; k++) {
                    boolean on = k == idx;
                    tv[k].setTextColor(on ? WHITE : GREY);
                    tv[k].setBackgroundColor(on ? ((cAccent & 0x00FFFFFF) | 0x66000000) : 0x00000000);
                }
            }
        };
        for (int k = 0; k < names.size(); k++) {
            final int idx = k;
            TextView t = new TextView(this);
            t.setText(names.get(k));
            t.setTextSize(16);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setPadding(rdp(14), rdp(14), rdp(10), rdp(14));
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select.on(idx);
                }
            });
            if (names.get(k).equals("About")) {
                t.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        boolean now = !cfg.getBoolean("advanced", false);
                        cfg.edit().putBoolean("advanced", now).apply();
                        Toast.makeText(MainActivity.this, now ? "Advanced call settings shown. Close and reopen settings."
                                : "Advanced call settings hidden. Close and reopen settings.", Toast.LENGTH_LONG).show();
                        return true;
                    }
                });
            }
            tv[k] = t;
            tabs.addView(t, new LinearLayout.LayoutParams(-1, -2));
        }
        final ScrollView tabScroll = new ScrollView(this);
        tabScroll.setBackgroundColor(0x66000000);
        tabScroll.addView(tabs);
        final TextView down = new TextView(this);
        down.setText("▾");
        down.setTextColor(WHITE);
        down.setTextSize(22);
        down.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        down.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0xEE000000}));
        down.setPadding(0, rdp(22), 0, rdp(2));
        FrameLayout tabBox = new FrameLayout(this);
        tabBox.addView(tabScroll, new FrameLayout.LayoutParams(-1, -1));
        tabBox.addView(down, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        final Runnable hints = new Runnable() {
            @Override
            public void run() {
                down.setVisibility(tabScroll.canScrollVertically(1) ? View.VISIBLE : View.GONE);
            }
        };
        tabScroll.getViewTreeObserver().addOnScrollChangedListener(new android.view.ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                hints.run();
            }
        });
        tabScroll.post(hints);
        h.addView(tabBox, new LinearLayout.LayoutParams(rdp(190), -1));
        h.addView(content, new LinearLayout.LayoutParams(0, -1, 1f));
        if (names.size() > 0) select.on(0);
        return h;
    }

    private void showNavDebug() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean on = enabled != null && enabled.contains(getPackageName());
        LinearLayout l = column();
        TextView t = new TextView(this);
        t.setTextColor(WHITE);
        t.setTextSize(15);
        t.setText("Notification access: " + (on ? "ON" : "OFF (turn it on with the button below)")
                + "\n\nLast navigation notification:\n" + NavListener.debug
                + "\n\nShown on the map: title = " + NavListener.title + ", text = " + NavListener.text);
        l.addView(t);
        Button open = flat("Open notification access settings");
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                } catch (Exception ignored) {
                }
            }
        });
        addFull(l, open);
        new Panel("Navigation info test", l, true).button("Close", null).show();
    }

    private static void keepOrRemove(SharedPreferences.Editor ed, String key, int value, int themeValue) {
        if (value == themeValue) ed.remove(key);
        else ed.putInt(key, value);
    }

    // ------------------------------------------------------------------ settings backup / restore

    private static final int REQ_SETTINGS = 79;

    private void exportSettings() {
        try {
            org.json.JSONObject vals = new org.json.JSONObject();
            for (java.util.Map.Entry<String, ?> e : cfg.getAll().entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                if (k.equals("lat") || k.equals("lon") || v == null) continue;
                org.json.JSONObject x = new org.json.JSONObject();
                if (v instanceof Boolean) x.put("t", "b");
                else if (v instanceof Integer) x.put("t", "i");
                else if (v instanceof Long) x.put("t", "l");
                else if (v instanceof Float) x.put("t", "f");
                else x.put("t", "s");
                x.put("v", v instanceof Float ? (Object) Double.valueOf(((Float) v).doubleValue()) : v);
                vals.put(k, x);
            }
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("app", "RehomeLauncher");
            o.put("version", versionName());
            o.put("values", vals);
            File dir = getExternalFilesDir("settings");
            if (dir == null) dir = new File(getFilesDir(), "settings");
            dir.mkdirs();
            File out = new File(dir, "rehome-settings.json");
            FileOutputStream os = new FileOutputStream(out);
            os.write(o.toString(2).getBytes("UTF-8"));
            os.close();
            Toast.makeText(this, "Settings saved to " + out.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not save the settings file", Toast.LENGTH_LONG).show();
        }
    }

    private void pickSettingsFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_SETTINGS);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker on this device", Toast.LENGTH_LONG).show();
        }
    }

    private void importSettings(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[1 << 14];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            org.json.JSONObject o = new org.json.JSONObject(new String(bo.toByteArray(), "UTF-8"));
            if (!"RehomeLauncher".equals(o.optString("app"))) throw new Exception("not a settings file");
            org.json.JSONObject vals = o.getJSONObject("values");
            SharedPreferences.Editor ed = cfg.edit();
            java.util.Iterator<String> it = vals.keys();
            while (it.hasNext()) {
                String k = it.next();
                org.json.JSONObject x = vals.getJSONObject(k);
                String t = x.optString("t", "s");
                if (t.equals("b")) ed.putBoolean(k, x.getBoolean("v"));
                else if (t.equals("i")) ed.putInt(k, x.getInt("v"));
                else if (t.equals("l")) ed.putLong(k, x.getLong("v"));
                else if (t.equals("f")) ed.putFloat(k, (float) x.getDouble("v"));
                else ed.putString(k, x.getString("v"));
            }
            ed.apply();
            Toast.makeText(this, "Settings loaded", Toast.LENGTH_LONG).show();
            stopService(new Intent(this, FloatingService.class));
            recreate();
        } catch (Exception e) {
            Toast.makeText(this, "That file is not a Rehome settings file", Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------ theme file export / import

    private static final int REQ_THEME = 78;

    private void exportTheme() {
        try {
            org.json.JSONObject t = ThemeFile.merged(cfg);
            t.put("vignette", vigStrength);
            t.put("tileRadius", tileRadius);
            t.put("tileGap", tileGap);
            t.put("iconGlow", iconGlow);
            org.json.JSONObject lay = new org.json.JSONObject();
            lay.put("pipW", Math.round(pipW() * 1000f) / 1000.0);
            lay.put("pipH", Math.round(pipH() * 1000f) / 1000.0);
            lay.put("barH", Math.round(barH() * 1000f) / 1000.0);
            lay.put("dockH", Math.round(dockH() * 1000f) / 1000.0);
            t.put("layout", lay);
            org.json.JSONObject fr = new org.json.JSONObject();
            fr.put("margin", cfg.getInt("margin", 8));
            fr.put("corner", cfg.getInt("corner", 60));
            t.put("frame", fr);
            File dir = getExternalFilesDir("theme");
            if (dir == null) dir = new File(getFilesDir(), "theme");
            dir.mkdirs();
            File out = new File(dir, "rehome-theme.json");
            FileOutputStream os = new FileOutputStream(out);
            os.write(t.toString(2).getBytes("UTF-8"));
            os.close();
            Toast.makeText(this, "Theme saved to " + out.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not save the theme file", Toast.LENGTH_LONG).show();
        }
    }

    private void pickThemeFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_THEME);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker on this device", Toast.LENGTH_LONG).show();
        }
    }

    private void importTheme(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[1 << 14];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            org.json.JSONObject o = new org.json.JSONObject(new String(bo.toByteArray(), "UTF-8"));
            if (!o.has("dark") && !o.has("light") && !o.has("layout") && !o.has("icons")
                    && !o.has("menu") && !o.has("frame")) {
                throw new Exception("not a theme");
            }
            SharedPreferences.Editor ed = cfg.edit();
            ed.putString("themeJson", o.toString());
            org.json.JSONObject lay = o.optJSONObject("layout");
            if (lay != null) {
                if (lay.has("pipW")) ed.putFloat("pipW", (float) lay.optDouble("pipW", 0.398));
                if (lay.has("pipH")) ed.putFloat("pipH", (float) lay.optDouble("pipH", 0.515));
                if (lay.has("barH")) ed.putFloat("barH", (float) lay.optDouble("barH", 0.15));
                if (lay.has("dockH")) ed.putFloat("dockH", (float) lay.optDouble("dockH", 0.065));
            }
            org.json.JSONObject fr = o.optJSONObject("frame");
            if (fr != null) {
                if (fr.has("margin")) ed.putInt("margin", fr.optInt("margin", 8));
                if (fr.has("corner")) ed.putInt("corner", fr.optInt("corner", 60));
            }
            // the theme file is in charge of these again
            ed.remove("vignette").remove("tileRadius").remove("tileGap").remove("iconGlow");
            ed.apply();
            Toast.makeText(this, "Theme imported", Toast.LENGTH_LONG).show();
            stopService(new Intent(this, FloatingService.class));
            recreate();
        } catch (Exception e) {
            Toast.makeText(this, "That file is not a valid theme", Toast.LENGTH_LONG).show();
        }
    }

    private void resetTheme() {
        cfg.edit().remove("themeJson").remove("vignette").remove("tileRadius").remove("tileGap").remove("iconGlow").apply();
        Toast.makeText(this, "Theme reset", Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void editSettings() {
        final String[] radioPkg = {cfg.getString("pkgRadio", "")};
        final String[] camPkg = {cfg.getString("pkg360", "")};
        final String[] voicePkg = {cfg.getString("pkgVoice", "")};
        final String[] browserPkg = {cfg.getString("pkgBrowser", "")};
        final String[] musicPkg = {cfg.getString("pkgMusic", "")};
        final String[] navPkg = {cfg.getString("pkgNav", "com.google.android.apps.maps")};
        final String[] carPkg = {cfg.getString("pkgCar", "")};
        final String[] account = {cfg.getString("callAccount", "")};

        final ArrayList<String> secNames = new ArrayList<String>();
        final ArrayList<LinearLayout> secViews = new ArrayList<LinearLayout>();
        LinearLayout l = null;
        final boolean adv = cfg.getBoolean("advanced", false);

        l = newSec(secNames, secViews, "Guide");
        label(l, "Opening settings: hold the 360° button, the clock or any bar icon, tap the gear icon, or open Rehome from the app list (that always opens the settings). More tabs are listed on the left: scroll the list (an arrow shows when there are more).");
        label(l, "YouTube with the screen off or in the background: use the Brave browser and tap the video button (it opens YouTube in Brave).");
        linkButton(l, "Download Brave (official page)", "https://brave.com/download/");
        linkButton(l, "Brave on APKPure (for units without a store)", "https://apkpure.com/search?q=brave%20browser");
        label(l, "Brave opens many tabs? In Brave: menu (three dots) > Settings > Brave shields & privacy, and look for \"Close tabs on exit\" (the name and place differ between versions and it is missing in some). "
                + "Otherwise open the tab switcher, tap the menu and choose Close all tabs now and then.");
        label(l, "Flat offline map (default): a Mapsforge .map file for your country. Download it on a phone or computer, copy it to the unit (USB or the Download folder), then Settings > Map > Choose offline map file.");
        linkButton(l, "Mapsforge maps (download.mapsforge.org)", "https://download.mapsforge.org/");
        linkButton(l, "OpenAndroMaps (more map styles)", "https://www.openandromaps.org/en/downloads");
        label(l, "3D map (optional): needs a vector .mbtiles file in the OpenMapTiles layout. Rehome cannot download it by itself, so get the file on a phone or computer, copy it to the unit, then Settings > Map > Map engine MapLibre 3D > Choose 3D map file. Files are large (a whole country can be several hundred MB).");
        linkButton(l, "OpenMapTiles downloads", "https://openmaptiles.org/downloads/");
        label(l, "Turn-by-turn info on the map comes from Google Maps notifications: switch Rehome ON under Android's Notification access.");
        Button notifB = flat("Open Notification access");
        notifB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                } catch (Exception ignored) {
                }
            }
        });
        addFull(l, notifB);
        Button homeB = flat("Set Rehome as the home app");
        homeB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Press the Home button and choose Rehome: Scroll, then Always", Toast.LENGTH_LONG).show();
                }
            }
        });
        addFull(l, homeB);

        l = newSec(secNames, secViews, "Apps");
        appButton(l, "Radio app", radioPkg);
        appButton(l, "360 camera app", camPkg);
        appButton(l, "Browser for YouTube", browserPkg);
        appButton(l, "Navigation app (Google Maps by default)", navPkg);
        appButton(l, "Music app (for the music button in the bottom bar)", musicPkg);
        appButton(l, "Car or vehicle app (for the car button in the bottom bar)", carPkg);

        l = newSec(secNames, secViews, "Look");
        final RadioGroup thG = radios(l, new String[]{"Automatic: dark from sunset to sunrise",
                "Fixed hours (set below)", "Always dark"}, 7000, cfg.getInt("theme", 2));
        final EditText dayStart = numField(l, "Light mode starts at hour (0-23)", cfg.getInt("dayStart", 6));
        final EditText nightStart = numField(l, "Dark mode starts at hour (0-23)", cfg.getInt("nightStart", 18));
        label(l, "The sun/moon button in the bottom bar switches between your choice and always dark.");
        final EditText vignette = numField(l, "Vignette strength in percent (0 = off, 100 = strong, up to 200 for a very dark edge)", vigStrength);
        label(l, "Vignette style");
        final RadioGroup vigG = radios(l, new String[]{"Automatic: surround when always dark, soft bottom fade otherwise",
                "Surround (dark all around the edge)", "Fade (soft darkening at the bottom and top)"}, 4000, cfg.getInt("vigStyle", 0));
        label(l, "Surround shape");
        final RadioGroup shapeG = radios(l, new String[]{"Oval, fitted to the screen", "Circle", "Rounded rectangle"}, 4100, vigShape);
        final EditText spread = numField(l, "Vignette spread in percent (50 = reaches far in, 150 = only the edge)", vigSpread);
        final EditText ytV = numField(l, "Vignette strength over the YouTube widget (0 = off)", ytVig);
        final EditText tileRad = numField(l, "Bar corner roundness in dp (the rounded contacts bar; 0 = sharp)", tileRadius);
        label(l, "Logo (a PNG shown in the top corner of the map, for example your car's badge; on the right when the driver sits on the left). Nothing is shown unless you choose one.");
        Button logoB = flat("Choose logo (PNG)");
        logoB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFile(REQ_LOGO);
            }
        });
        addFull(l, logoB);
        Button logoOff = flat("Remove logo");
        logoOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg.edit().remove("logoFile").apply();
                recreate();
            }
        });
        addFull(l, logoOff);
        final EditText logoSize = numField(l, "Logo size in dp (default 160)", cfg.getInt("logoSize", 160));
        label(l, "Theme file: colours, icons, menus and layout in one file. Export a template to edit it or give it to a designer, then import it.");
        Button expT = flat("Export theme template");
        expT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportTheme();
            }
        });
        addFull(l, expT);
        Button impT = flat("Import theme file");
        impT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickThemeFile();
            }
        });
        addFull(l, impT);
        Button resT = flat("Reset theme to default");
        resT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTheme();
            }
        });
        addFull(l, resT);

        l = newSec(secNames, secViews, "Screen and layout");
        Button cal = flat("Calibrate layout and frame (save other changes first)");
        cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrate();
            }
        });
        addFull(l, cal);
        final CheckBox pip = check(l, "Show the 360 window area (leave off for the simple full-width layout)", cfg.getBoolean("pipOn", false));
        final CheckBox avoid = check(l, "Keep the map and YouTube clear of the 360 window (turn info moves above it)", cfg.getBoolean("avoidPip", true));
        final CheckBox status = check(l, "Show the status bar (date and the three buttons)", cfg.getBoolean("showStatus", false));
        final CheckBox statusFrame = check(l, "Draw glass frames behind the status bar icons (only with the status bar switched on)", cfg.getBoolean("statusFrame", true));
        final EditText statusH = numField(l, "Status bar frame height in dp (default 44)", cfg.getInt("statusH", 44));
        final CheckBox statusHome = check(l, "Also draw the status bar frame on the home screen", cfg.getBoolean("statusHome", true));
        label(l, "Driver's seat");
        final RadioGroup seatG = radios(l, new String[]{"Driver on the right", "Driver on the left"}, 8000, cfg.getInt("seat", 0));
        final CheckBox frame = check(l, "Draw the black margin and rounded corners over every app", cfg.getBoolean("frame", true));
        LinearLayout extL = adv ? l : column();   // extended frame settings appear after holding the About tab
        final CheckBox frameExt = check(extL, "Extended frame (simple layout only): the bottom of the frame grows into a bar with 360 and your primary contact", cfg.getBoolean("frameExt", false));
        final EditText shH = numField(extL, "Extended bar height in percent of the screen (8 to 30)", cfg.getInt("shieldH", 18));
        final EditText shA = numField(extL, "Extended bar angle in degrees (0 = square)", cfg.getInt("shieldAngle", 15));
        final CheckBox noSys = check(l, "Do not draw the frame or bar over Android system apps (the 360 app is always covered)", cfg.getBoolean("noSystem", true));
        label(l, "The frame and the extended bar do not let touches through. The margin and corner size are set in Calibrate layout and frame (above).");
        final EditText ui = numField(l, "UI size in percent (100 = default, up to 160)", cfg.getInt("uiScale", 100));
        label(l, "Order of the buttons in the bottom bar (with the 360 window layout). Use the arrows to move one.");
        final ArrayList<String> order = dockOrder();
        final LinearLayout dockBox = new LinearLayout(this);
        dockBox.setOrientation(LinearLayout.VERTICAL);
        final Runnable[] fill = new Runnable[1];
        fill[0] = new Runnable() {
            @Override
            public void run() {
                dockBox.removeAllViews();
                for (int k = 0; k < order.size(); k++) {
                    final int at = k;
                    String nm = order.get(k);
                    for (int q = 0; q < DOCK_IDS.length; q++) if (DOCK_IDS[q].equals(nm)) nm = DOCK_NAMES[q];
                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    TextView tn = new TextView(MainActivity.this);
                    tn.setText((k + 1) + "  " + nm);
                    tn.setTextColor(WHITE);
                    tn.setTextSize(15);
                    row.addView(tn, new LinearLayout.LayoutParams(0, -2, 1f));
                    Button up = flat("▲");
                    up.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (at > 0) {
                                Collections.swap(order, at, at - 1);
                                fill[0].run();
                            }
                        }
                    });
                    Button dn = flat("▼");
                    dn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (at < order.size() - 1) {
                                Collections.swap(order, at, at + 1);
                                fill[0].run();
                            }
                        }
                    });
                    row.addView(up, new LinearLayout.LayoutParams(-2, -2));
                    row.addView(dn, new LinearLayout.LayoutParams(-2, -2));
                    dockBox.addView(row);
                }
            }
        };
        fill[0].run();
        addFull(l, dockBox);

        l = newSec(secNames, secViews, "Map");
        label(l, "Map engine");
        final RadioGroup engG = radios(l, new String[]{"Mapsforge, flat (default, uses a .map file)",
                "MapLibre 3D (uses a .mbtiles vector file, tilted view with 3D buildings)"}, 5100, cfg.getInt("mapEngine", 0));
        final EditText tilt = numField(l, "3D tilt in degrees (0 = flat, 55 = normal)", cfg.getInt("mapTilt", 55));
        TextView curMb = new TextView(this);
        String mbf = cfg.getString("mbFile", "");
        curMb.setText("3D map file: " + (mbf.length() == 0 ? "none" : new File(mbf).getName()));
        curMb.setTextColor(GREY);
        l.addView(curMb);
        Button pickMb = flat("Choose 3D map file (.mbtiles)");
        pickMb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFile(REQ_MB);
            }
        });
        addFull(l, pickMb);
        final EditText zoom = numField(l, "Map zoom (12 = far, 19 = very close; place names show less when very close)", cfg.getInt("mapZoom", 18));
        final EditText mapText = numField(l, "Map text size in percent (100 = normal, 150 to 200 reads easier, 300 is the most)", cfg.getInt("mapText", 300));
        final CheckBox showInfo = check(l, "Show turn info and city name on the map", cfg.getBoolean("showInfo", true));
        TextView cur = new TextView(this);
        String mf = cfg.getString("mapFile", "");
        cur.setText("Offline map file: " + (mf.length() == 0 ? "none" : new File(mf).getName()));
        cur.setTextColor(GREY);
        cur.setPadding(0, rdp(8), 0, 0);
        l.addView(cur);
        Button pick = flat("Choose offline map file (.map)");
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickMapFile();
            }
        });
        addFull(l, pick);
        Button find = flat("Find .map files on this device");
        find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findMapFiles();
            }
        });
        addFull(l, find);

        l = newSec(secNames, secViews, "360 camera and start-up");
        final CheckBox auto360 = check(l, "Show the 360 full screen when driving slowly (EXPERIMENTAL: test while parked first)", cfg.getBoolean("auto360", true));
        final EditText a360min = numField(l, "...faster than (km/h)", cfg.getInt("auto360Min", 5));
        final EditText a360max = numField(l, "...and slower than (km/h)", cfg.getInt("auto360Max", 10));
        label(l, "Repeat");
        final RadioGroup a360mode = radios(l, new String[]{"After a time gap (set below)",
                "Only after going faster than 15 km/h again"}, 9100, cfg.getInt("auto360Mode", 0));
        final EditText a360gap = numField(l, "Time gap in seconds (match your 360's small-window delay)", cfg.getInt("auto360Gap", 60));
        final EditText a360pause = numField(l, "Do nothing for this many seconds after a touch", cfg.getInt("auto360Pause", 20));
        final CheckBox a360video = check(l, "Also wait while a video is playing", cfg.getBoolean("auto360PauseVideo", false));
        final EditText retry = numField(l, "Start 360 this many seconds after boot if it is not showing (0 = off)", cfg.getInt("boot360Retry", 5));
        final EditText auto = numField(l, "Open radio this many seconds after boot (0 = off)", cfg.getInt("autoRadio", 1));
        final CheckBox coverOn = check(l, "Cover the screen with black while the 360 app starts, then fade it out (hides its start-up screens; needs display over other apps)", cfg.getBoolean("coverOn", true));
        final EditText coverMs = numField(l, "Cover time in seconds (1 to 15)", cfg.getInt("coverSec", 4));

        l = newSec(secNames, secViews, "Floating buttons");
        final CheckBox floating = check(l, "Show the floating Home button (only in the 360 app, and not while the extended frame is on)", cfg.getBoolean("floating", true));
        label(l, "It appears only over the 360 app, because other apps have their own navigation buttons.");
        final EditText fdelay = numField(l, "Show the interface and floating button this many extra seconds after the 360 start check (0 to 59)", cfg.getInt("floatDelay", 5));
        final CheckBox lock = check(l, "Lock floating button position", cfg.getBoolean("floatLock", false));
        final EditText fsize = numField(l, "Button size in dp", cfg.getInt("floatSize", 84));
        label(l, "Colour");
        String[] colNames = new String[4];
        for (int k = 0; k < 4; k++) colNames[k] = FloatingService.COLOR_NAMES[k];
        final RadioGroup colG = radios(l, colNames, 6000, cfg.getInt("floatColor", 0));

        // the call tab normally shows only how to call; the rest appears after holding the About tab
        l = newSec(secNames, secViews, "Calling");
        label(l, "Call through Bluetooth (the phone connected to the unit) or through the unit's own SIM (a phone account from Google).");
        final Button accBtn = flat("Call through: " + accountLabel(account[0]));
        accBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseAccount(accBtn, account);
            }
        });
        addFull(l, accBtn);
        LinearLayout callAdv = adv ? l : column();
        final RadioGroup rg = radios(callAdv, new String[]{"Call straight away", "Open the dial screen first",
                "Macro broadcast"}, 1000, cfg.getInt("callMode", 0));
        final EditText macro = field(callAdv, "Macro broadcast action (only for macro mode)", cfg.getString("macroAction", ""), InputType.TYPE_CLASS_TEXT);

        l = adv ? newSec(secNames, secViews, "Steering-wheel voice button") : column();
        label(l, "Easiest: in your unit's voice or Speech app list (for example TW Radio Select, Speech) pick \"Rehome: Call Plugin\". Pressing the wheel button then calls the primary contact.");
        label(l, "If the unit sends a key to the launcher itself:");
        final RadioGroup voiceG = radios(l, new String[]{"Call the primary contact", "Open the voice assistant app",
                "Send a macro broadcast"}, 2000, cfg.getInt("voiceMode", 0));
        final EditText wheelMacro = field(l, "Macro action for the wheel button (only for macro mode)", cfg.getString("wheelMacro", ""), InputType.TYPE_CLASS_TEXT);
        appButton(l, "Voice assistant app", voicePkg);

        l = newSec(secNames, secViews, "About");
        TextView ab = label(l, "Rehome  v" + versionName());
        ab.setTextColor(cText);
        ab.setTextSize(18);
        label(l, "A home screen for car head units: map, 360 camera, radio, YouTube and calls on one screen.");
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.HORIZONTAL);
        made.setGravity(Gravity.CENTER_VERTICAL);
        TextView m1 = new TextView(this);
        m1.setText("Made with ");
        m1.setTextColor(cText);
        m1.setTextSize(16);
        made.addView(m1);
        made.addView(new ClaudeMark(this), new LinearLayout.LayoutParams(rdp(22), rdp(22)));
        TextView m2 = new TextView(this);
        m2.setText(" Claude");
        m2.setTextColor(cText);
        m2.setTextSize(16);
        made.addView(m2);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-2, -2);
        mlp.topMargin = rdp(14);
        l.addView(made, mlp);
        label(l, "Credits: Claude (Anthropic), lead builder and designer. Google AI, images and media. Minor helpers: ChatGPT (OpenAI) and Grok (xAI). Map data: OpenStreetMap contributors. Map engine: Mapsforge.");

        new Panel("Launcher settings", settingsBody(secNames, secViews), false, true)
                .button("Cancel", null)
                .button("Save", new Runnable() {
                    @Override
                    public void run() {
                        int seat = checkedIdx(seatG, 8000, 2, 0);
                        boolean seatChanged = seat != cfg.getInt("seat", 0);
                        int sc = Math.max(60, Math.min(160, intOf(ui, 100)));
                        SharedPreferences.Editor ed = cfg.edit();
                        ed.putString("pkgRadio", radioPkg[0])
                                .putString("pkg360", camPkg[0])
                                .putString("pkgVoice", voicePkg[0])
                                .putString("pkgBrowser", browserPkg[0])
                                .putString("callAccount", account[0])
                                .putInt("voiceMode", checkedIdx(voiceG, 2000, 3, 0))
                                .putString("wheelMacro", wheelMacro.getText().toString().trim())
                                .putString("pkgMusic", musicPkg[0])
                                .putString("pkgNav", navPkg[0])
                                .putString("pkgCar", carPkg[0])
                                .putInt("mapStyle", 0)
                                .putInt("mapZoom", Math.max(12, Math.min(19, intOf(zoom, 18))))
                                .putInt("mapText", Math.max(100, Math.min(300, intOf(mapText, 300))))
                                .putBoolean("showInfo", showInfo.isChecked())
                                .putInt("theme", checkedIdx(thG, 7000, 3, 0))
                                .putInt("dayStart", Math.max(0, Math.min(23, intOf(dayStart, 6))))
                                .putInt("nightStart", Math.max(0, Math.min(23, intOf(nightStart, 18))))
                                .putBoolean("auto360", auto360.isChecked())
                                .putInt("auto360Min", Math.max(0, intOf(a360min, 5)))
                                .putInt("auto360Max", Math.max(1, intOf(a360max, 10)))
                                .putInt("auto360Mode", checkedIdx(a360mode, 9100, 2, 0))
                                .putInt("auto360Gap", Math.max(5, intOf(a360gap, 60)))
                                .putInt("auto360Pause", Math.max(0, intOf(a360pause, 20)))
                                .putBoolean("auto360PauseVideo", a360video.isChecked())
                                .putInt("boot360Retry", Math.max(0, intOf(retry, 5)))
                                .putInt("autoRadio", Math.max(0, intOf(auto, 1)))
                                .putBoolean("floating", floating.isChecked())
                                .putBoolean("floatLock", lock.isChecked())
                                .putInt("floatSize", Math.max(48, Math.min(160, intOf(fsize, 84))))
                                .putInt("floatColor", checkedIdx(colG, 6000, 4, 0))
                                .putBoolean("coverOn", coverOn.isChecked())
                                .putInt("coverSec", Math.max(1, Math.min(15, intOf(coverMs, 4))))
                                .putBoolean("statusFrame", statusFrame.isChecked())
                                .putInt("statusH", Math.max(24, Math.min(120, intOf(statusH, 44))))
                                .putBoolean("statusHome", statusHome.isChecked())
                                .putInt("vigStyle", checkedIdx(vigG, 4000, 3, 0))
                                .putInt("vigShape", checkedIdx(shapeG, 4100, 3, 0))
                                .putInt("vigSpread", Math.max(50, Math.min(150, intOf(spread, 100))))
                                .putInt("ytVignette", Math.max(0, Math.min(200, intOf(ytV, 120))))
                                .putInt("mapEngine", checkedIdx(engG, 5100, 2, 0))
                                .putInt("logoSize", Math.max(24, Math.min(240, intOf(logoSize, 160))))
                                .putInt("mapTilt", Math.max(0, Math.min(70, intOf(tilt, 55))))
                                .putBoolean("frameExt", frameExt.isChecked())
                                .putBoolean("noSystem", noSys.isChecked())
                                .putInt("floatDelay", Math.max(0, Math.min(59, intOf(fdelay, 5))))
                                .putInt("shieldH", Math.max(8, Math.min(30, intOf(shH, 18))))
                                .putInt("shieldAngle", Math.max(0, Math.min(60, intOf(shA, 15))))
                                .putInt("callMode", checkedIdx(rg, 1000, 3, 0))
                                .putString("macroAction", macro.getText().toString().trim())
                                .putBoolean("showStatus", status.isChecked())
                                .putBoolean("pipOn", pip.isChecked())
                                .putBoolean("avoidPip", avoid.isChecked())
                                .putInt("seat", seat)
                                .putBoolean("frame", frame.isChecked())
                                .putInt("uiScale", sc);
                        // theme values stay in charge unless a number was changed here
                        keepOrRemove(ed, "vignette", Math.max(0, Math.min(200, intOf(vignette, vigStrength))), themeJ.optInt("vignette", 200));
                        keepOrRemove(ed, "tileRadius", Math.max(0, Math.min(60, intOf(tileRad, tileRadius))), themeJ.optInt("tileRadius", 60));
                        StringBuilder dk = new StringBuilder();
                        for (String did : order) {
                            if (dk.length() > 0) dk.append(',');
                            dk.append(did);
                        }
                        ed.putString("dockItems", dk.toString());
                        if (seatChanged) ed.remove("floatX").remove("floatY");
                        ed.apply();
                        if (floating.isChecked() || frame.isChecked()) askOverlayPermission();
                        stopService(new Intent(MainActivity.this, FloatingService.class));
                        recreate();
                    }
                }).show();
    }

    // ------------------------------------------------------------------ calibration

    private void applyCalibration() {
        if (body != null) body.requestLayout();
        if (overlay != null) overlay.invalidate();
        FloatingService.refreshFrame();
    }

    private void calFloatRow(LinearLayout parent, String name, final String key, final float def,
                             final float lo, final float hi) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, rdp(6), 0, rdp(6));
        TextView n = new TextView(this);
        n.setText(name);
        n.setTextColor(WHITE);
        n.setTextSize(16);
        row.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView val = new TextView(this);
        val.setTextColor(CHROME);
        val.setTextSize(16);
        val.setGravity(Gravity.CENTER);
        val.setText(String.format(Locale.US, "%.1f", cfg.getFloat(key, def) * 100f));
        row.addView(val, new LinearLayout.LayoutParams(rdp(64), -2));
        final float[] dir = {-1f, 1f};
        String[] sign = {"−", "+"};
        for (int k = 0; k < 2; k++) {
            final float d = dir[k];
            Button b = flat(sign[k]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    float nv = clampF(cfg.getFloat(key, def) + d * 0.005f, lo, hi);
                    cfg.edit().putFloat(key, nv).apply();
                    val.setText(String.format(Locale.US, "%.1f", nv * 100f));
                    applyCalibration();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(rdp(52), -2);
            lp.leftMargin = rdp(6);
            row.addView(b, lp);
        }
        parent.addView(row);
    }

    private void calIntRow(LinearLayout parent, String name, final String key, final int def,
                           final int step, final int lo, final int hi) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, rdp(6), 0, rdp(6));
        TextView n = new TextView(this);
        n.setText(name);
        n.setTextColor(WHITE);
        n.setTextSize(16);
        row.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView val = new TextView(this);
        val.setTextColor(CHROME);
        val.setTextSize(16);
        val.setGravity(Gravity.CENTER);
        val.setText(String.valueOf(cfg.getInt(key, def)));
        row.addView(val, new LinearLayout.LayoutParams(rdp(64), -2));
        final int[] dir = {-1, 1};
        String[] sign = {"−", "+"};
        for (int k = 0; k < 2; k++) {
            final int d = dir[k];
            Button b = flat(sign[k]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int nv = Math.max(lo, Math.min(hi, cfg.getInt(key, def) + d * step));
                    cfg.edit().putInt(key, nv).apply();
                    val.setText(String.valueOf(nv));
                    applyCalibration();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(rdp(52), -2);
            lp.leftMargin = rdp(6);
            row.addView(b, lp);
        }
        parent.addView(row);
    }

    private void calibrate() {
        showGuides = true;
        if (overlay != null) overlay.invalidate();
        LinearLayout l = column();
        label(l, "Red box = the 360 window. Blue line = top of the contacts bar. Green line = top of the black bar. "
                + "Change the values until they line up. Changes apply at once.");
        if (pipOn()) {
            calFloatRow(l, "360 window width (%)", "pipW", 0.398f, 0.25f, 0.65f);
            calFloatRow(l, "360 window height (%)", "pipH", 0.515f, 0.20f, 0.85f);
            calFloatRow(l, "Black bar height (%)", "dockH", 0.065f, 0.03f, 0.25f);
        } else {
            label(l, "The 360 window is switched off, so only the bar height and the frame are shown here.");
        }
        calFloatRow(l, "Contacts bar height (%)", "barH", 0.15f, 0.08f, 0.30f);
        calIntRow(l, "Frame margin (dp)", "margin", 8, 1, 0, 40);
        calIntRow(l, "Corner roundness (dp)", "corner", 60, 2, 0, 90);
        new Panel("Calibrate layout", l, true)
                .button("Reset", new Runnable() {
                    @Override
                    public void run() {
                        cfg.edit().remove("pipW").remove("pipH").remove("dockH").remove("barH")
                                .remove("margin").remove("corner").apply();
                        applyCalibration();
                    }
                })
                .button("Done", null)
                .onClose(new Runnable() {
                    @Override
                    public void run() {
                        showGuides = false;
                        if (overlay != null) overlay.invalidate();
                    }
                }).show();
    }

    // ------------------------------------------------------------------ offline map file

    private static final int REQ_MAP = 77, REQ_MB = 80, REQ_LOGO = 81;

    private void pickFile(int req) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, req);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker on this device", Toast.LENGTH_LONG).show();
        }
    }

    private void pickMapFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_MAP);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker on this device. Use \"Find .map files\" instead.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_MAP && res == RESULT_OK && data != null && data.getData() != null) {
            copyMap(data.getData(), "offline.map", "mapFile");
        } else if (req == REQ_MB && res == RESULT_OK && data != null && data.getData() != null) {
            copyMap(data.getData(), "offline.mbtiles", "mbFile");
        } else if (req == REQ_LOGO && res == RESULT_OK && data != null && data.getData() != null) {
            copyMap(data.getData(), "logo.png", "logoFile");
        } else if (req == REQ_THEME && res == RESULT_OK && data != null && data.getData() != null) {
            importTheme(data.getData());
        } else if (req == REQ_SETTINGS && res == RESULT_OK && data != null && data.getData() != null) {
            importSettings(data.getData());
        }
    }

    private void copyMap(final Uri uri, final String fileName, final String key) {
        Toast.makeText(this, "Importing the map, please wait...", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = getExternalFilesDir("maps");
                    if (dir == null) dir = new File(getFilesDir(), "maps");
                    dir.mkdirs();
                    File out = new File(dir, fileName);
                    InputStream in = getContentResolver().openInputStream(uri);
                    OutputStream os = new FileOutputStream(out);
                    byte[] buf = new byte[1 << 16];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                    os.close();
                    in.close();
                    cfg.edit().putString(key, out.getAbsolutePath()).apply();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Map imported", Toast.LENGTH_LONG).show();
                            recreate();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Could not import the map file", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void findMapFiles() {
        File[] dirs = {
                getExternalFilesDir("maps"),
                getExternalFilesDir(null),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                new File(Environment.getExternalStorageDirectory(), "Maps"),
                new File(Environment.getExternalStorageDirectory(), "maps"),
                new File(Environment.getExternalStorageDirectory(), "mapsforge")};
        final ArrayList<File> found = new ArrayList<File>();
        for (File d : dirs) {
            if (d == null || !d.isDirectory()) continue;
            File[] list = d.listFiles();
            if (list == null) continue;
            for (File f : list) {
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".map")) found.add(f);
            }
        }
        if (found.isEmpty()) {
            Toast.makeText(this, "No .map files found. Put one in Downloads, or use the file picker.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[found.size()];
        for (int k = 0; k < names.length; k++) names[k] = found.get(k).getName();
        listPanel("Choose a map", names, new IdxCb() {
            @Override
            public void on(int which) {
                cfg.edit().putString("mapFile", found.get(which).getAbsolutePath()).apply();
                recreate();
            }
        });
    }

    // ------------------------------------------------------------------ overlay: chrome lines, guides, (fallback) frame

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
            float x1 = w * pipW();
            float y0 = h * (1f - pipH());
            float y1 = h * (1f - dockH());
            float g = dp(22);
            if (pipOn()) {

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

            if (showGuides) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(4f);
                p.setColor(0xFFFF2D2D);
                c.drawRect(2f, y0, x1, h - 2f, p);
                p.setColor(0xFF2D7BFF);
                float yb = h * (1f - dockH() - barH());
                c.drawLine(x1, yb, w, yb, p);
                p.setColor(0xFF2DFF6A);
                c.drawLine(0, y1, w, y1, p);
                p.setStyle(Paint.Style.FILL);
            }
            }

            // pure black margin with rounded inner corners, only when the frame service is not drawing it
            if (FloatingService.frameOn) return;
            float m = dp(cfg.getInt("margin", 8) / scale);
            float r = dp(cfg.getInt("corner", 60) / scale);
            if (m < 1f && r < 1f) return;
            corner.reset();
            corner.setFillType(Path.FillType.EVEN_ODD);
            corner.addRect(0, 0, w, h, Path.Direction.CW);
            corner.addRoundRect(new RectF(m, m, w - m, h - m), r, r, Path.Direction.CW);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.BLACK);
            c.drawPath(corner, p);
        }
    }

    // ------------------------------------------------------------------ icons

    static class Glyph extends View {
        static final int RADIO = 0, PREV = 1, PLAY = 2, NEXT = 3, R360 = 4,
                YT = 5, DRAWER = 6, MAP = 7, GEAR = 8, MUTE = 9, HOME = 10, ASSIST = 11,
                PAUSE = 12, THEME = 13, NAVI = 14, MUSIC = 15, DIAL = 16, CAR = 17, BACK = 18;
        private float frac = 1f;
        private int state = 0;
        private int glowColor = 0;
        private float glowAmt = 0f;
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        /** Soft coloured light behind the icon (colour 0 or amount 0 = none). */
        void setGlow(int color, float amount) {
            glowColor = color;
            glowAmt = amount;
            invalidate();
        }

        void setFrac(float f) {
            frac = f;
            invalidate();
        }

        void setState(int s) {
            state = s;
            invalidate();
        }
        private int type;

        void setType(int t) {
            type = t;
            invalidate();
        }
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);

        void setColor(int color) {
            fill.setColor(color);
            stroke.setColor(color);
            invalidate();
        }

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

        private String iconKey() {
            switch (type) {
                case RADIO: return "radio";
                case PREV: return "previous";
                case PLAY: return "play";
                case NEXT: return "next";
                case R360: return "360";
                case YT: return "youtube";
                case DRAWER: return "app-grid";
                case MAP: return "map-pin";
                case GEAR: return "settings";
                case MUTE: return "mute";
                case HOME: return "home";
                case PAUSE: return "pause";
                case THEME: return state == 0 ? "theme-auto" : "theme-dark";
                case NAVI: return "navigation";
                case MUSIC: return "music";
                case DIAL: return "phone";
                case CAR: return "car";
                case BACK: return "back";
                default: return null;
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), u = Math.min(w, h) * frac / 100f;
            if (glowColor != 0 && glowAmt > 0f) {
                float gr = Math.min(w, h) * 0.5f;
                int ga = (int) (Math.min(1f, glowAmt) * 0.7f * 255f);
                glowPaint.setShader(new RadialGradient(w / 2f, h / 2f, gr, (ga << 24) | (glowColor & 0x00FFFFFF),
                        0x00000000, Shader.TileMode.CLAMP));
                c.drawCircle(w / 2f, h / 2f, gr, glowPaint);
            }
            c.save();
            c.translate((w - 100f * u) / 2f, (h - 100f * u) / 2f);
            c.scale(u, u);
            Path p = new Path();
            stroke.setStrokeWidth(5);
            String key = iconKey();
            if (key != null && IconPack.hasOverride(key)) {
                IconPack.drawOverride(c, key, stroke);
                c.restore();
                return;
            }
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
                case MUTE:
                    p.moveTo(14, 38);
                    p.lineTo(32, 38);
                    p.lineTo(54, 18);
                    p.lineTo(54, 82);
                    p.lineTo(32, 62);
                    p.lineTo(14, 62);
                    p.close();
                    c.drawPath(p, fill);
                    c.drawLine(66, 38, 90, 62, stroke);
                    c.drawLine(90, 38, 66, 62, stroke);
                    break;
                case HOME:
                    p.moveTo(12, 50);
                    p.lineTo(50, 16);
                    p.lineTo(88, 50);
                    c.drawPath(p, stroke);
                    p.reset();
                    p.moveTo(24, 44);
                    p.lineTo(24, 84);
                    p.lineTo(76, 84);
                    p.lineTo(76, 44);
                    c.drawPath(p, stroke);
                    c.drawRect(42, 60, 58, 84, stroke);
                    break;
                case PAUSE:
                    c.drawRect(24, 18, 42, 82, fill);
                    c.drawRect(58, 18, 76, 82, fill);
                    break;
                case THEME:
                    if (state == 0) {
                        // automatic: circle, left half filled
                        stroke.setStrokeWidth(7);
                        c.drawCircle(50, 50, 34, stroke);
                        c.drawArc(new RectF(16, 16, 84, 84), 90, 180, true, fill);
                    } else {
                        // always dark: crescent moon
                        c.drawCircle(50, 50, 34, fill);
                        c.drawCircle(66, 38, 28, hole);
                    }
                    break;
                case ASSIST:
                    c.drawCircle(50, 50, 36, stroke);
                    c.drawCircle(50, 50, 14, fill);
                    break;
                case NAVI:
                    IconPack.draw(c, "navigation", stroke);
                    break;
                case MUSIC:
                    IconPack.draw(c, "music", stroke);
                    break;
                case DIAL:
                    IconPack.draw(c, "phone", stroke);
                    break;
                case CAR:
                    IconPack.draw(c, "360-car", stroke);
                    break;
                case BACK:
                    IconPack.draw(c, "back", stroke);
                    break;
                default:
                    break;
            }
            c.restore();
        }
    }
}
