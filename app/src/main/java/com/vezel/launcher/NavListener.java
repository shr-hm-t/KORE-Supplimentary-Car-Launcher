package com.vezel.launcher;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

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
        title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        text = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        Bitmap b = null;
        try {
            Icon ic = n.getLargeIcon();
            if (ic != null) {
                Drawable d = ic.loadDrawable(this);
                if (d != null) {
                    int w = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : 96;
                    int h = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 96;
                    b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(b);
                    d.setBounds(0, 0, w, h);
                    d.draw(c);
                }
            }
        } catch (Exception ignored) {
        }
        icon = b;
        fire();
    }

    private static void fire() {
        Runnable r = onChange;
        if (r != null) r.run();
    }
}
