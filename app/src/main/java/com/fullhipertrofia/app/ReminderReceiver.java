package com.fullhipertrofia.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        show(c, i.getStringExtra("t"));
    }

    public static void show(Context c, String t) {
        if (t == null) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("fh", "Avisos", NotificationManager.IMPORTANCE_DEFAULT));
        }
        PendingIntent pi = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, "fh") : new Notification.Builder(c);
        b.setContentTitle(t).setContentText("FULL HIPERTROFIA")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pi).setAutoCancel(true);
        nm.notify(t.hashCode(), b.build());
    }
}
