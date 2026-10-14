package com.fullhipertrofia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Reprograma los avisos cuando el teléfono se reinicia. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())
                && c.getSharedPreferences("fh", 0).getBoolean("rem", false)) {
            Notifier.scheduleAll(c);
        }
    }
}
