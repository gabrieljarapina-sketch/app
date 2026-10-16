package com.fullhipertrofia.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/** Los mismos tres avisos de la app: ejercitar (cada 4 h), comer (cada 3 h) y creatina (cada 12 h). */
object Reminders {
    private val ITEMS = arrayOf(
        Pair("Hora De Ejercitar", 4L),
        Pair("Alimentate Bien", 3L),
        Pair("Recuerda LA CREATINA", 12L)
    )

    fun set(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences("fh", Context.MODE_PRIVATE).edit().putBoolean("rem", on).apply()
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ITEMS.forEachIndexed { i, item ->
            val pi = PendingIntent.getBroadcast(
                ctx, i,
                Intent(ctx, ReminderReceiver::class.java).putExtra("msg", item.first),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)
            if (on) {
                val ms = item.second * 3_600_000L
                am.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + ms, ms, pi
                )
            }
        }
    }
}
