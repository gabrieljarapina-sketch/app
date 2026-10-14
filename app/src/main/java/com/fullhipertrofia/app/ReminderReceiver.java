package com.fullhipertrofia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Muestra el aviso programado como notificación del teléfono y programa el siguiente. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (!c.getSharedPreferences("fh", 0).getBoolean("rem", false)) return;
        int idx = i.getIntExtra("idx", 0);
        if (idx < 0 || idx >= Notifier.MSG.length) idx = 0;
        Notifier.show(c, Notifier.MSG[idx], 10 + idx);
        Notifier.schedule(c, idx);
    }
}
