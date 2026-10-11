package com.fullhipertrofia.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (c.getSharedPreferences("fh", 0).getBoolean("on", false)) Reminders.set(c, true);
    }
}
