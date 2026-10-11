package com.fullhipertrofia.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class Reminders {
    static final String[] TITLES = {"Hora De Ejercitar", "Alimentate Bien", "Recuerda LA CREATINA"};
    static final long[] HOURS = {4, 3, 12};

    public static void set(Context c, boolean on) {
        c.getSharedPreferences("fh", 0).edit().putBoolean("on", on).apply();
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < 3; i++) {
            Intent in = new Intent(c, ReminderReceiver.class).putExtra("t", TITLES[i]);
            PendingIntent pi = PendingIntent.getBroadcast(c, i, in, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            long every = HOURS[i] * 3600000L;
            if (on) am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + every, every, pi);
            else am.cancel(pi);
        }
    }
}
