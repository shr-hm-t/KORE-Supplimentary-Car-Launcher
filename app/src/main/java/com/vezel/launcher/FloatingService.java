package com.vezel.launcher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

/** Two always-visible neon buttons (Mute and Home) that float over every app. Drag either one to move both. */
public class FloatingService extends Service {

    static final int[] COLORS = {0xFFFFFFFF, 0xFFFF2D2D, 0xFF2D7BFF, 0xFF2DFF6A};
    static final String[] COLOR_NAMES = {"White", "Red", "Blue", "Green"};

    private WindowManager wm;
    private LinearLayout root;
    private WindowManager.LayoutParams lp;
    private View muteBtn;
    private SharedPreferences cfg;
    private int neon;

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

    private View bubble(int glyph) {
        FrameLayout f = new FrameLayout(this);
        GradientDrawable glow = new GradientDrawable();
        glow.setShape(GradientDrawable.OVAL);
        glow.setColor((neon & 0x00FFFFFF) | 0x38000000);
        GradientDrawable core = new GradientDrawable();
        core.setShape(GradientDrawable.OVAL);
        core.setColor(0xE6000000);
        core.setStroke(dp(2), neon);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{glow, core});
        ld.setLayerInset(1, dp(5), dp(5), dp(5), dp(5));
        f.setBackground(ld);
        MainActivity.Glyph g = new MainActivity.Glyph(this, glyph);
        g.setColor(neon);
        f.addView(g, new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(64), dp(64));
        p.setMargins(dp(4), 0, dp(4), 0);
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
                        if (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8)) moved = true;
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

    @Override
    public void onCreate() {
        super.onCreate();
        cfg = getSharedPreferences("cfg", MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        int ci = cfg.getInt("floatColor", 0);
        if (ci < 0 || ci >= COLORS.length) ci = 0;
        neon = COLORS[ci];

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);

        muteBtn = bubble(MainActivity.Glyph.MUTE);
        View homeBtn = bubble(MainActivity.Glyph.HOME);
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
                Intent h = new Intent(Intent.ACTION_MAIN);
                h.addCategory(Intent.CATEGORY_HOME);
                h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(h);
            }
        });

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = cfg.getInt("floatX", dp(12));
        lp.y = cfg.getInt("floatY", dp(120));

        try {
            wm.addView(root, lp);
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void toggleMute() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean muted = Build.VERSION.SDK_INT >= 23 && am.isStreamMute(AudioManager.STREAM_MUSIC);
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                muted ? AudioManager.ADJUST_UNMUTE : AudioManager.ADJUST_MUTE, 0);
        muteBtn.setAlpha(muted ? 1f : 0.45f);
        Toast.makeText(this, muted ? "Sound on" : "Muted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (wm != null && root != null) wm.removeView(root);
        } catch (Exception ignored) {
        }
    }
}
