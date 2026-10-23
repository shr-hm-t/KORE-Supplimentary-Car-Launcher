package com.rehome.launcher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Runs the things that sit above every app:
 * the black frame (margin, rounded corners, optional extended bottom bar with 360 and primary contact,
 * optional split status-bar slabs; it blocks touches on its own black parts) and the floating buttons.
 */
public class FloatingService extends Service {

    static final int[] COLORS = {0xFFFFFFFF, 0xFFFF2D2D, 0xFF2D7BFF, 0xFF2DFF6A};
    static final String[] COLOR_NAMES = {"White", "Red", "Blue", "Green"};
    static final String[] BTN_IDS = {"home", "back", "mute", "radio", "cam"};
    static final String[] BTN_NAMES = {"Home", "Back (may not work on this unit)", "Mute (may not work on this unit)",
            "Radio", "360 camera"};

    /** True while the frame is on screen (the launcher then skips drawing its own copy). */
    static volatile boolean frameOn = false;
    /** Set by the launcher once its interface is up; until then the service only shows the start-up cover. */
    static volatile boolean uiAllowed = false;
    private boolean built = false;
    private static FloatingService inst;

    /** Black screen that fades out, hides the start-up screens of the 360 app. */
    static void cover() {
        final FloatingService s = inst;
        if (s != null) s.ui.post(new Runnable() {
            @Override
            public void run() {
                s.showCover(false);
            }
        });
    }

    static void refreshFrame() {
        final FloatingService s = inst;
        if (s != null) s.ui.post(new Runnable() {
            @Override
            public void run() {
                s.rebuildFrame();
                s.applyRules();
            }
        });
    }

    private WindowManager wm;
    private LinearLayout root;
    private WindowManager.LayoutParams lp;
    private View muteBtn;
    private SharedPreferences cfg;
    private int neon;
    private boolean muted = false;
    private int prevVol = -1;
    private final Handler ui = new Handler();

    // frame parts
    private FrameView frameView;
    private final ArrayList<View> blockers = new ArrayList<View>();
    private View tabBox;
    private StatusView statusView;
    private MainActivity.Glyph camGlyph;
    private boolean extShown = false;

    private boolean statusWanted() {
        return cfg.getBoolean("showStatus", false) && cfg.getBoolean("statusFrame", true);
    }

    private int statusH() {
        return statusWanted() ? dp(Math.max(24, Math.min(120, cfg.getInt("statusH", 44)))) : 0;
    }

    /** Two glass slabs behind the system status icons, in the style of the contacts bar. */
    private class StatusView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        StatusView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float m = dp(cfg.getInt("margin", 8));
            float gap = dp(5);
            float sw = w * 0.30f;
            RectF l = new RectF(m + gap, gap, sw, h - gap);
            RectF r = new RectF(w - sw, gap, w - m - gap, h - gap);
            float rad = Math.min(l.height() / 2f, dp(cfg.getInt("corner", 60)));
            for (RectF b : new RectF[]{l, r}) {
                p.setStyle(Paint.Style.FILL);
                p.setShader(new android.graphics.LinearGradient(0, b.top, 0, b.bottom,
                        new int[]{0xCC1A242E, 0xEE0A1016}, null, android.graphics.Shader.TileMode.CLAMP));
                c.drawRoundRect(b, rad, rad, p);
                p.setShader(null);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1.5f, dp(1)));
                p.setColor(0x66FFFFFF);
                c.drawRoundRect(b, rad, rad, p);
            }
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i != null && i.getBooleanExtra("bootCover", false)) showCover(true);
        if (uiAllowed && !built && wm != null) buildAll();
        return START_STICKY;
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    // ------------------------------------------------------------------ settings helpers

    private boolean mirror() {
        return cfg.getInt("seat", 0) == 1;
    }

    private String posKey(String axis) {
        return axis + (mirror() ? "L" : "R");   // one saved position per driver side
    }

    private View coverView;

    /** Soft glowing start-up screen: dark navy with a slowly breathing amber and blue glow, and the logo. */
    private class CoverView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private android.graphics.Bitmap logo;
        private final long t0 = System.currentTimeMillis();

        CoverView(Context c) {
            super(c);
            String lf = cfg.getString("logoFile", "");
            if (lf.length() > 0 && new java.io.File(lf).exists()) {
                logo = android.graphics.BitmapFactory.decodeFile(lf);
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            double t = (System.currentTimeMillis() - t0) / 1000.0;
            float b1 = (float) (0.5 + 0.5 * Math.sin(t * 1.4));
            float b2 = (float) (0.5 + 0.5 * Math.sin(t * 1.1 + 2.0));
            p.setStyle(Paint.Style.FILL);
            p.setShader(new android.graphics.LinearGradient(0, 0, 0, h, 0xFF0E141B, 0xFF070A0E, android.graphics.Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            float r = Math.max(w, h) * 0.55f;
            p.setShader(new android.graphics.RadialGradient(w * 0.12f, h * 0.95f, r,
                    ((int) (60 + 70 * b1) << 24) | 0xC8641A, 0x00000000, android.graphics.Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(new android.graphics.RadialGradient(w * 0.88f, h * 0.05f, r,
                    ((int) (60 + 70 * b2) << 24) | 0x2D6CC8, 0x00000000, android.graphics.Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);
            if (logo != null) {
                float s = Math.min(w, h) * 0.30f;
                p.setAlpha((int) (150 + 100 * b1));
                c.drawBitmap(logo, null, new RectF(w / 2f - s / 2f, h / 2f - s / 2f, w / 2f + s / 2f, h / 2f + s / 2f), p);
                p.setAlpha(255);
            }
            postInvalidateOnAnimation();
        }
    }

    private void showCover(boolean boot) {
        int mode = cfg.getInt("coverMode", 2);
        if (mode == 0 || (!boot && mode < 2) || wm == null) return;
        hideCover(false);
        final View v = new CoverView(this);
        v.setAlpha(0f);
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                return true;
            }
        });
        WindowManager.LayoutParams cl = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        cl.gravity = Gravity.TOP | Gravity.START;
        try {
            wm.addView(v, cl);
        } catch (Exception e) {
            return;
        }
        coverView = v;
        v.animate().alpha(1f).setDuration(boot ? 150 : 350).start();
        long ms = (boot ? Math.max(1, Math.min(30, cfg.getInt("coverBootSec", 10)))
                : Math.max(1, Math.min(15, cfg.getInt("coverSec", 4)))) * 1000L;
        ui.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (coverView != v) return;
                v.animate().alpha(0f).setDuration(900).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (coverView == v) hideCover(false);
                    }
                }).start();
            }
        }, ms);
    }

    private void hideCover(boolean unused) {
        if (coverView != null) {
            try {
                wm.removeView(coverView);
            } catch (Exception ignored) {
            }
            coverView = null;
        }
    }

    private boolean extWanted() {
        return cfg.getBoolean("frame", true) && cfg.getBoolean("frameExt", false) && !cfg.getBoolean("pipOn", true);
    }

    private int extH(int screenH) {
        return Math.round(screenH * Math.max(8, Math.min(30, cfg.getInt("shieldH", 15))) / 100f);
    }

    // ------------------------------------------------------------------ frame drawing

    private class FrameView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        FrameView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float m = dp(cfg.getInt("margin", 8));
            float r = dp(cfg.getInt("corner", 60));
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.BLACK);
            float st = statusH();   // the corner frame starts at the bottom of the status bar frame
            if (m >= 1f || r >= 1f) {
                path.reset();
                path.setFillType(Path.FillType.EVEN_ODD);
                path.addRect(0, st, w, h, Path.Direction.CW);
                path.addRoundRect(new RectF(m, st > 0 ? st : m, w - m, h - m), r, r, Path.Direction.CW);
                c.drawPath(path, p);
            }
            if (extShown) {
                float eh = extH((int) h);
                float bh = eh * 0.7f;
                c.drawRect(0, h - bh, w, h, p);
                path.reset();
                float[] t = tabGeometry(w, h);
                path.moveTo(t[0], t[2]);
                path.lineTo(t[1], t[2]);
                path.lineTo(t[1] + t[4], h - bh);
                path.lineTo(t[0] - t[4], h - bh);
                path.close();
                c.drawPath(path, p);
            }
        }
    }

    /** {tabLeft, tabRight, tabTop, bandTop, slant} in screen pixels (mirrored when the driver sits on the left). */
    private float[] tabGeometry(float w, float h) {
        float eh = extH((int) h);
        float bh = eh * 0.7f;
        float ang = Math.max(0, Math.min(60, cfg.getInt("shieldAngle", 15)));
        float slant = (eh - bh) * (float) Math.tan(Math.toRadians(ang));
        float l = w * 0.28f, r = w * 0.60f;
        if (mirror()) {
            float nl = w - r, nr = w - l;
            l = nl;
            r = nr;
        }
        return new float[]{l, r, h - eh, h - bh, slant};
    }

    // ------------------------------------------------------------------ touch-blocking windows

    private View solid() {
        View v = new View(this);
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                return true;
            }
        });
        return v;
    }

    private void addBlocker(View v, int w, int h, int gravity, int x, int y) {
        WindowManager.LayoutParams l = new WindowManager.LayoutParams(w, h, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        l.gravity = gravity;
        l.x = x;
        l.y = y;
        try {
            wm.addView(v, l);
            blockers.add(v);
        } catch (Exception ignored) {
        }
    }

    private void removeFrame() {
        for (View v : blockers) {
            try {
                wm.removeView(v);
            } catch (Exception ignored) {
            }
        }
        blockers.clear();
        tabBox = null;
        if (statusView != null) {
            try {
                wm.removeView(statusView);
            } catch (Exception ignored) {
            }
            statusView = null;
        }
        if (frameView != null) {
            try {
                wm.removeView(frameView);
            } catch (Exception ignored) {
            }
            frameView = null;
        }
        frameOn = false;
    }

    private void rebuildFrame() {
        removeFrame();
        if (!cfg.getBoolean("frame", true)) return;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int W = dm.widthPixels, H = dm.heightPixels;
        extShown = extWanted();
        frameView = new FrameView(this);
        WindowManager.LayoutParams fl = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        fl.gravity = Gravity.TOP | Gravity.START;
        try {
            wm.addView(frameView, fl);
        } catch (Exception e) {
            frameView = null;
            return;
        }
        // the black parts do not let touches through
        int m = dp(cfg.getInt("margin", 8));
        int sh = statusH();
        if (m > 0) {
            if (sh == 0) addBlocker(solid(), W, m, Gravity.TOP | Gravity.START, 0, 0);
            addBlocker(solid(), m, H - sh, Gravity.TOP | Gravity.START, 0, sh);
            addBlocker(solid(), m, H - sh, Gravity.TOP | Gravity.END, 0, sh);
            if (!extShown) addBlocker(solid(), W, m, Gravity.BOTTOM | Gravity.START, 0, 0);
        }
        if (sh > 0) {
            // glass slabs behind the status icons (they also block touches)
            statusView = new StatusView(this);
            WindowManager.LayoutParams sl = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, sh, overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            sl.gravity = Gravity.TOP | Gravity.START;
            try {
                wm.addView(statusView, sl);
            } catch (Exception e) {
                statusView = null;
            }
            int sw = Math.round(W * 0.30f);
            addBlocker(solid(), sw, sh, Gravity.TOP | Gravity.START, 0, 0);
            addBlocker(solid(), sw, sh, Gravity.TOP | Gravity.END, 0, 0);
        }
        if (extShown) {
            float eh = extH(H), bh = eh * 0.7f;
            addBlocker(solid(), W, Math.round(bh), Gravity.BOTTOM | Gravity.START, 0, 0);
            float[] t = tabGeometry(W, H);
            int tw = Math.round(t[1] - t[0] + 2 * t[4]);
            FrameLayout box = new FrameLayout(this);
            box.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    return true;
                }
            });
            int bs = Math.round(eh * 0.62f);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            View cam = bubble(MainActivity.Glyph.R360, bs);
            camGlyph = (MainActivity.Glyph) ((FrameLayout) cam).getChildAt(0);
            View call = bubble(MainActivity.Glyph.DIAL, bs);
            if (mirror()) {
                row.addView(call);
                row.addView(cam);
            } else {
                row.addView(cam);
                row.addView(call);
            }
            box.addView(row, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
            cam.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AppWatcher.pkg.equals(cfg.getString("pkg360", ""))) {
                        goHome();   // the 360 app is on screen: this button is Home
                        return;
                    }
                    Intent i = getPackageManager().getLaunchIntentForPackage(cfg.getString("pkg360", ""));
                    if (i == null) return;
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    showCover(false);
                    startActivity(i);
                }
            });
            call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(FloatingService.this, AssistActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            });
            addBlocker(box, tw, Math.round(eh), Gravity.BOTTOM | Gravity.START, Math.round(t[0] - t[4]), 0);
            tabBox = box;
        }
        // keep the floating buttons above the frame
        if (root != null) {
            try {
                wm.removeView(root);
                wm.addView(root, lp);
            } catch (Exception ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ rules (system apps, where the buttons show)

    private boolean isSystemApp(String pkg) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, 0);
            return (ai.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean inList(String key, String pkg, String dflt) {
        String v = cfg.getString(key, "");
        if (v.length() == 0) v = dflt;
        return ("," + v + ",").contains("," + pkg + ",");
    }

    private void applyRules() {
        String pkg = AppWatcher.pkg;
        boolean known = AppWatcher.running;
        boolean home = !known || pkg.length() == 0 || pkg.equals(getPackageName());
        String cam = cfg.getString("pkg360", "");
        boolean hideSys = known && !home && cfg.getBoolean("noSystem", true) && !pkg.equals(cam) && isSystemApp(pkg);
        int vis = hideSys ? View.GONE : View.VISIBLE;
        if (frameView != null) {
            frameView.setVisibility(vis);
            frameOn = !hideSys;
        }
        for (View v : blockers) v.setVisibility(vis);
        if (root != null) {
            // only over the 360 app, and not while the extended frame has its own Home button
            boolean show = known && !home && cam.length() > 0 && pkg.equals(cam) && !extShown;
            root.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (statusView != null) {
            boolean onHome = known && home;
            boolean showStatus = !(onHome && !cfg.getBoolean("statusHome", true));
            statusView.setVisibility(vis == View.VISIBLE && showStatus ? View.VISIBLE : View.GONE);
        }
        if (camGlyph != null) {
            camGlyph.setType(known && pkg.equals(cam) ? MainActivity.Glyph.HOME : MainActivity.Glyph.R360);
        }
    }

    // ------------------------------------------------------------------ buttons

    private View bubble(int glyph) {
        return bubble(glyph, dp(Math.max(48, Math.min(160, cfg.getInt("floatSize", 84)))));
    }

    private View bubble(int glyph, int size) {
        FrameLayout f = new FrameLayout(this);
        GradientDrawable glow = new GradientDrawable();
        glow.setShape(GradientDrawable.OVAL);
        glow.setColor((neon & 0x00FFFFFF) | 0x38000000);
        GradientDrawable core = new GradientDrawable();
        core.setShape(GradientDrawable.OVAL);
        core.setColor(0xE6000000);
        core.setStroke(dp(2.5f), neon);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{glow, core});
        int in = Math.max(dp(3), size / 12);
        ld.setLayerInset(1, in, in, in, in);
        f.setBackground(ld);
        MainActivity.Glyph g = new MainActivity.Glyph(this, glyph);
        g.setColor(neon);
        g.setFrac(0.58f);
        f.addView(g, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
        p.setMargins(dp(5), 0, dp(5), 0);
        f.setLayoutParams(p);
        return f;
    }

    private void attachDrag(final View v, final Runnable onClick) {
        v.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private int startX, startY;
            private boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startX = lp.x;
                        startY = lp.y;
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX;
                        float dy = e.getRawY() - downY;
                        boolean locked = cfg.getBoolean("floatLock", false);
                        if (!locked && (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8))) moved = true;
                        if (moved) {
                            lp.x = startX + (int) dx;
                            lp.y = startY + (int) dy;
                            try {
                                wm.updateViewLayout(root, lp);
                            } catch (Exception ignored) {
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) {
                            cfg.edit().putInt(posKey("floatX"), lp.x).putInt(posKey("floatY"), lp.y).apply();
                        } else {
                            onClick.run();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        inst = this;
        cfg = getSharedPreferences("cfg", MODE_PRIVATE);
        int nci = cfg.getInt("floatColor", 0);
        neon = COLORS[nci < 0 || nci >= COLORS.length ? 0 : nci];
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (uiAllowed) buildAll();
    }

    private void buildAll() {
        if (built) return;
        built = true;
        // 1) floating buttons (added first so rebuildFrame can keep them on top)
        if (cfg.getBoolean("floating", true)) {
            root = new LinearLayout(this);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);
            View hb = makeButton("home");   // only the Home button
            if (hb != null) root.addView(hb);
            if (root.getChildCount() == 0) {
                root = null;
            } else {
                lp = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        overlayType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT);
                lp.gravity = Gravity.TOP | Gravity.START;
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int size = dp(Math.max(48, Math.min(160, cfg.getInt("floatSize", 84))));
                int defX = mirror() ? dm.widthPixels - size * 2 - dp(36) : dp(14);
                int defY = (int) (dm.heightPixels * 0.40f);
                lp.x = cfg.getInt(posKey("floatX"), defX);
                lp.y = cfg.getInt(posKey("floatY"), defY);
                try {
                    wm.addView(root, lp);
                } catch (Exception e) {
                    root = null;
                }
            }
        }
        // 2) frame
        rebuildFrame();
        if (frameView == null && root == null) {
            stopSelf();
            return;
        }
        AppWatcher.start(this, new Runnable() {
            @Override
            public void run() {
                applyRules();
            }
        });
        applyRules();
    }

    private View makeButton(String id) {
        View b;
        Runnable act;
        if (id.equals("home")) {
            b = bubble(MainActivity.Glyph.HOME);
            act = new Runnable() {
                @Override
                public void run() {
                    goHome();
                }
            };
        } else if (id.equals("back")) {
            b = bubble(MainActivity.Glyph.BACK);
            act = new Runnable() {
                @Override
                public void run() {
                    if (AppWatcher.back()) return;
                    Toast.makeText(FloatingService.this, "Back is not possible on this unit, going Home", Toast.LENGTH_SHORT).show();
                    goHome();
                }
            };
        } else if (id.equals("mute")) {
            b = bubble(MainActivity.Glyph.MUTE);
            muteBtn = b;
            act = new Runnable() {
                @Override
                public void run() {
                    toggleMute();
                }
            };
        } else if (id.equals("radio")) {
            b = bubble(MainActivity.Glyph.RADIO);
            act = new Runnable() {
                @Override
                public void run() {
                    launch(cfg.getString("pkgRadio", ""));
                }
            };
        } else if (id.equals("apps")) {
            b = bubble(MainActivity.Glyph.DRAWER);
            act = new Runnable() {
                @Override
                public void run() {
                    goHome();
                }
            };
        } else if (id.equals("cam")) {
            b = bubble(MainActivity.Glyph.R360);
            act = new Runnable() {
                @Override
                public void run() {
                    launch(cfg.getString("pkg360", ""));
                }
            };
        } else {
            return null;
        }
        attachDrag(b, act);
        return b;
    }

    private void launch(String pkg) {
        if (pkg.length() == 0) return;
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (pkg.equals(cfg.getString("pkg360", ""))) showCover(false);
        startActivity(i);
    }

    private void goHome() {
        Intent h = new Intent(Intent.ACTION_MAIN);
        h.addCategory(Intent.CATEGORY_HOME);
        h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(h);
    }

    // ------------------------------------------------------------------ mute

    private void toggleMute() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int mode = cfg.getInt("muteMode", 0);
        String note;
        switch (mode) {
            case 1: {
                boolean m = Build.VERSION.SDK_INT >= 23 && am.isStreamMute(AudioManager.STREAM_MUSIC);
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        m ? AudioManager.ADJUST_UNMUTE : AudioManager.ADJUST_MUTE, 0);
                muted = !m;
                break;
            }
            case 2: {
                prevVol = -1;
                muted = !muted;
                if (muted) {
                    prevVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                } else {
                    int back = prevVol > 0 ? prevVol : cfg.getInt("lastVol", 8);
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, back, 0);
                }
                if (muted && prevVol > 0) cfg.edit().putInt("lastVol", prevVol).apply();
                break;
            }
            case 3: {
                muted = !muted;
                int key = muted ? KeyEvent.KEYCODE_MEDIA_PAUSE : KeyEvent.KEYCODE_MEDIA_PLAY;
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
                break;
            }
            case 4: {
                muted = !muted;
                String action = cfg.getString("muteAction", "");
                if (action.length() > 0) sendBroadcast(new Intent(action));
                else Toast.makeText(this, "Set the broadcast action in settings", Toast.LENGTH_LONG).show();
                break;
            }
            default: {
                // same call the system makes for the physical mute key: toggles the stream in use
                muted = !muted;
                if (Build.VERSION.SDK_INT >= 23) {
                    am.adjustSuggestedStreamVolume(AudioManager.ADJUST_TOGGLE_MUTE,
                            AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI);
                }
                break;
            }
        }
        note = muted ? "Muted" : "Sound on";
        if (muteBtn != null) muteBtn.setAlpha(muted ? 0.45f : 1f);
        Toast.makeText(this, note, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        frameOn = false;
        AppWatcher.stop();
        removeFrame();
        hideCover(false);
        if (inst == this) inst = null;
        try {
            if (wm != null && root != null) wm.removeView(root);
        } catch (Exception ignored) {
        }
    }
}
