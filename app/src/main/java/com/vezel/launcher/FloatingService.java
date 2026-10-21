package com.vezel.launcher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

/**
 * Runs two things that sit above every app:
 * the black screen frame (margin + rounded corners, touches pass straight through) and
 * two neon buttons (Mute and Home) that take touches.
 */
public class FloatingService extends Service {

    static final int[] COLORS = {0xFFFFFFFF, 0xFFFF2D2D, 0xFF2D7BFF, 0xFF2DFF6A};
    static final String[] COLOR_NAMES = {"White", "Red", "Blue", "Green"};

    /** True while the frame window is on screen (the launcher then skips drawing its own copy). */
    static volatile boolean frameOn = false;
    private static FloatingService inst;

    static void refreshFrame() {
        final FloatingService s = inst;
        if (s != null && s.frame != null) s.frame.postInvalidate();
    }

    private WindowManager wm;
    private LinearLayout root;
    private FrameView frame;
    private WindowManager.LayoutParams lp;
    private View muteBtn;
    private View shield;
    private final Handler ui = new Handler();
    private SharedPreferences cfg;
    private int neon;
    private boolean muted = false;
    private int prevVol = -1;

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        return START_STICKY;
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ------------------------------------------------------------------ frame

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
            if (m < 1f && r < 1f) return;
            path.reset();
            path.setFillType(Path.FillType.EVEN_ODD);
            path.addRect(0, 0, w, h, Path.Direction.CW);
            path.addRoundRect(new RectF(m, m, w - m, h - m), r, r, Path.Direction.CW);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.BLACK);
            c.drawPath(path, p);
        }
    }

    // ------------------------------------------------------------------ buttons

    private View bubble(int glyph) {
        int size = dp(Math.max(48, Math.min(160, cfg.getInt("floatSize", 84))));
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
                            cfg.edit().putInt("floatX", lp.x).putInt("floatY", lp.y).apply();
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

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        inst = this;
        cfg = getSharedPreferences("cfg", MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // 1) frame: first, so the buttons sit above it. Touches go straight through.
        if (cfg.getBoolean("frame", true)) {
            frame = new FrameView(this);
            WindowManager.LayoutParams fl = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            fl.gravity = Gravity.TOP | Gravity.START;
            try {
                wm.addView(frame, fl);
                frameOn = true;
            } catch (Exception e) {
                frame = null;
            }
        }

        // 2) the two buttons
        if (cfg.getBoolean("floating", true)) {
            int ci = cfg.getInt("floatColor", 0);
            if (ci < 0 || ci >= COLORS.length) ci = 0;
            neon = COLORS[ci];
            root = new LinearLayout(this);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);

            muteBtn = bubble(MainActivity.Glyph.MUTE);
            final boolean back = cfg.getInt("floatSecond", 0) == 0;
            View homeBtn = bubble(back ? MainActivity.Glyph.BACK : MainActivity.Glyph.HOME);
            root.addView(muteBtn);
            root.addView(homeBtn);

            attachDrag(muteBtn, new Runnable() {
                @Override
                public void run() {
                    toggleMute();
                }
            });
            attachDrag(homeBtn, new Runnable() {
                @Override
                public void run() {
                    if (back) {
                        if (AppWatcher.back()) return;
                        // this unit does not allow it: act as Home instead
                        Toast.makeText(FloatingService.this,
                                "Back is not possible on this unit, going Home", Toast.LENGTH_SHORT).show();
                    }
                    Intent h = new Intent(Intent.ACTION_MAIN);
                    h.addCategory(Intent.CATEGORY_HOME);
                    h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(h);
                }
            });

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
            boolean driverRight = cfg.getInt("seat", 0) == 0;
            int defX = dp(14);   // left side by default; drag to move
            int defY = (int) (dm.heightPixels * 0.40f);
            lp.x = cfg.getInt("floatX", defX);
            lp.y = cfg.getInt("floatY", defY);
            try {
                wm.addView(root, lp);
            } catch (Exception e) {
                root = null;
            }
        }
        if (frame == null && root == null && cfg.getInt("shieldMode", 0) == 0) {
            stopSelf();
            return;
        }
        if (cfg.getInt("frameMode", 0) + cfg.getInt("floatMode", 0) + cfg.getInt("shieldMode", 0) > 0) {
            AppWatcher.start(this, new Runnable() {
                @Override
                public void run() {
                    applyRules();
                }
            });
        }
        applyRules();
    }

    // ------------------------------------------------------------------ per-app rules

    /** mode 0 = everywhere, 1 = only the chosen apps, 2 = everywhere except the chosen apps. */
    private boolean allowed(String modeKey, String appsKey, String pkg) {
        int mode = cfg.getInt(modeKey, 0);
        if (mode == 0 || !AppWatcher.running) return true;
        boolean in = ("," + cfg.getString(appsKey, "") + ",").contains("," + pkg + ",");
        return mode == 1 ? in : !in;
    }

    private void applyRules() {
        String pkg = AppWatcher.pkg;
        boolean home = pkg.length() == 0 || pkg.equals(getPackageName());
        if (frame != null) {
            boolean show = home || allowed("frameMode", "frameApps", pkg);
            frame.setVisibility(show ? View.VISIBLE : View.GONE);
            frameOn = show;
        }
        if (root != null) {
            root.setVisibility(allowed("floatMode", "floatApps", pkg) ? View.VISIBLE : View.GONE);
        }
        boolean wantShield = AppWatcher.running && cfg.getInt("shieldMode", 0) > 0
                && pkg.equals(cfg.getString("shieldApp", cfg.getString("pkg360", "")));
        if (wantShield && shield == null) {
            shield = new View(this);
            shield.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    return true;   // swallow the touch
                }
            });
            DisplayMetrics dm = getResources().getDisplayMetrics();
            boolean whole = cfg.getInt("shieldMode", 0) == 2;
            int h = whole ? WindowManager.LayoutParams.MATCH_PARENT
                    : Math.round(dm.heightPixels * Math.max(5, Math.min(60, cfg.getInt("shieldH", 14))) / 100f);
            WindowManager.LayoutParams sl = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, h, overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            sl.gravity = Gravity.BOTTOM | Gravity.START;
            try {
                wm.addView(shield, sl);
                if (root != null) {      // keep the floating buttons above the shield
                    wm.removeView(root);
                    wm.addView(root, lp);
                }
            } catch (Exception ex) {
                shield = null;
            }
        } else if (!wantShield && shield != null) {
            try {
                wm.removeView(shield);
            } catch (Exception ignored) {
            }
            shield = null;
        }
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
        try {
            if (wm != null && shield != null) wm.removeView(shield);
        } catch (Exception ignored) {
        }
        if (inst == this) inst = null;
        try {
            if (wm != null && root != null) wm.removeView(root);
        } catch (Exception ignored) {
        }
        try {
            if (wm != null && frame != null) wm.removeView(frame);
        } catch (Exception ignored) {
        }
    }
}
