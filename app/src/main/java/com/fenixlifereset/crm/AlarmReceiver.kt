package com.fenixlifereset.crm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FenixCRM:AlarmWakeLock"
        )
        wakeLock.acquire(10_000L)

        try {
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("id", intent.getStringExtra("id") ?: "alarm")
                putExtra("contactName", intent.getStringExtra("contactName") ?: "Seguimiento Fénix")
                putExtra("reason", intent.getStringExtra("reason") ?: "Es hora de escribirle a tu contacto")
                putExtra("whatsappUrl", intent.getStringExtra("whatsappUrl") ?: "")
                putExtra("snoozeMinutes", intent.getIntExtra("snoozeMinutes", 5))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
