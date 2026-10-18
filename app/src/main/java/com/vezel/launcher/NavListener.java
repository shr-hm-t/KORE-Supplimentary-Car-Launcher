package com.vezel.launcher;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;

/** Reads the ongoing turn-by-turn notification of the navigation app. */
public class NavListener extends NotificationListenerService {

    public static volatile CharSequence title;
    public static volatile CharSequence text;
    public static volatile Bitmap icon;
    public static volatile Runnable onChange;

    private static final String[] NAV_PACKAGES = {
            "com.google.android.apps.maps", "com.waze", "net.osmand", "net.osmand.plus"
    };

    @Override
    public void onListenerConnected() {
        try {
            StatusBarNotification[] list = getActiveNotifications();
            if (list != null) {
                for (StatusBarNotification s : list) handle(s);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        handle(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null && isNav(sbn.getPackageName())) {
            title = null;
            text = null;
            icon = null;
            fire();
        }
    }

    private boolean isNav(String pkg) {
        if (pkg == null) return false;
        for (String p : NAV_PACKAGES) if (p.equals(pkg)) return true;
        return false;
    }

    private void handle(StatusBarNotification sbn) {
        if (sbn == null || !isNav(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        if (n == null || n.extras == null) return;
        if ((n.flags & Notification.FLAG_ONGOING_EVENT) == 0) return;

        CharSequence t = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence x = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        Bitmap b = iconFromExtras(n);

        // Google Maps often puts the turn info in a custom layout instead of normal fields.
        boolean noText = (t == null || t.length() == 0);
        if (noText || b == null) {
            ArrayList<String> texts = new ArrayList<String>();
            Bitmap[] found = new Bitmap[1];
            scrape(n, texts, found);
            if (noText && texts.size() > 0) {
                t = texts.get(0);
                x = texts.size() > 1 ? texts.get(1) : x;
            }
            if (b == null) b = found[0];
        }
        title = t;
        text = x;
        icon = b;
        fire();
    }

    private Bitmap iconFromExtras(Notification n) {
        try {
            Icon ic = n.getLargeIcon();
            Bitmap b = drawableToBitmap(ic == null ? null : ic.loadDrawable(this));
            if (b != null) return b;
        } catch (Exception ignored) {
        }
        String[] keys = {Notification.EXTRA_LARGE_ICON, Notification.EXTRA_LARGE_ICON_BIG, Notification.EXTRA_PICTURE};
        for (String k : keys) {
            try {
                Parcelable p = n.extras.getParcelable(k);
                if (p instanceof Bitmap) return (Bitmap) p;
                if (p instanceof Icon) {
                    Bitmap b = drawableToBitmap(((Icon) p).loadDrawable(this));
                    if (b != null) return b;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private void scrape(Notification n, ArrayList<String> texts, Bitmap[] found) {
        try {
            RemoteViews rv = n.bigContentView != null ? n.bigContentView : n.contentView;
            if (rv == null) return;
            Context ctx = getApplicationContext();
            View v = rv.apply(ctx, new FrameLayout(ctx));
            walk(v, texts, found);
        } catch (Throwable ignored) {
        }
    }

    private void walk(View v, ArrayList<String> texts, Bitmap[] found) {
        if (v instanceof TextView) {
            CharSequence s = ((TextView) v).getText();
            if (s != null) {
                String str = s.toString().trim();
                if (str.length() > 0) texts.add(str);
            }
        } else if (v instanceof ImageView) {
            if (found[0] == null) {
                Bitmap b = drawableToBitmap(((ImageView) v).getDrawable());
                if (b != null && b.getWidth() >= 24) found[0] = b;
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i), texts, found);
        }
    }

    private static Bitmap drawableToBitmap(Drawable d) {
        if (d == null) return null;
        try {
            int w = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : 96;
            int h = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 96;
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            d.setBounds(0, 0, w, h);
            d.draw(c);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private static void fire() {
        Runnable r = onChange;
        if (r != null) r.run();
    }
              }
