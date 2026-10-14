package com.fullhipertrofia.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** Notificaciones del teléfono y avisos programados. */
public class Notifier {
    static final String CH = "fh_reminders";
    static final String[] MSG = {"Hora De Ejercitar", "Alimentate Bien", "Recuerda LA CREATINA"};
    static final long[] HORAS = {4, 3, 12};

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "Avisos FULL HIPERTROFIA", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }
        }
    }

    static void show(Context c, String msg, int id) {
        ensureChannel(c);
        if (Build.VERSION.SDK_INT >= 33
                && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent open = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CH)
                : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("FULL HIPERTROFIA")
                .setContentText(msg)
                .setAutoCancel(true)
                .setContentIntent(pi);
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_HIGH);
        nm.notify(id, b.build());
    }

    static PendingIntent pending(Context c, int idx) {
        Intent i = new Intent(c, ReminderReceiver.class).putExtra("idx", idx);
        return PendingIntent.getBroadcast(c, 100 + idx, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void schedule(Context c, int idx) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        long t = System.currentTimeMillis() + HORAS[idx] * 3600000L;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pending(c, idx));
    }

    static void scheduleAll(Context c) {
        for (int i = 0; i < MSG.length; i++) schedule(c, i);
    }

    static void cancelAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < MSG.length; i++) am.cancel(pending(c, i));
    }
}
