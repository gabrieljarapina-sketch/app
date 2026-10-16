package com.fullhipertrofia.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (ctx.getSharedPreferences("fh", Context.MODE_PRIVATE).getBoolean("rem", false)) {
            Reminders.set(ctx, true)
        }
    }
}
