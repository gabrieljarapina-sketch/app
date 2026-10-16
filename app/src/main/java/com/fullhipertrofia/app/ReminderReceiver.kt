package com.fullhipertrofia.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        // Horario de descanso: no avisa entre las 22:00 y las 07:00.
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (h >= 22 || h < 7) return
        val msg = intent.getStringExtra("msg") ?: return
        Notifier.show(ctx, msg)
    }
}
