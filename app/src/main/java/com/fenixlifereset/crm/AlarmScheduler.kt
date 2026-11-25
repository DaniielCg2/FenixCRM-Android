package com.fenixlifereset.crm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

object AlarmScheduler {
    private const val PREFS_NAME = "fenix_alarms_prefs"
    private const val KEY_ALARMS = "saved_alarms_json"

    private fun requestCodeFor(id: String): Int = id.hashCode() and 0x7fffffff

    fun scheduleAlarm(context: Context, item: AlarmItem) {
        if (item.timestampMs <= System.currentTimeMillis()) return

        saveAlarm(context, item)
        registerInAlarmManager(context, item)
    }

    fun cancelAlarm(context: Context, id: String) {
        removeAlarmFromPrefs(context, id)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCodeFor(id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun syncAlarms(context: Context, items: List<AlarmItem>) {
        // Cancelar alarmas anteriores guardadas
        val existing = getSavedAlarms(context)
        for (old in existing) {
            cancelAlarm(context, old.id)
        }
        // Programar las futuras actuales
        val now = System.currentTimeMillis()
        for (item in items) {
            if (item.timestampMs > now) {
                scheduleAlarm(context, item)
            }
        }
    }

    fun restoreAllAlarms(context: Context) {
        val now = System.currentTimeMillis()
        val saved = getSavedAlarms(context)
        for (item in saved) {
            if (item.timestampMs > now) {
                registerInAlarmManager(context, item)
            }
        }
    }

    private fun registerInAlarmManager(context: Context, item: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Respaldo si el usuario no otorgó permiso exacto aún
            val fallbackPending = buildReceiverPendingIntent(context, item)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.timestampMs, fallbackPending)
            return
        }

        val showIntent = Intent(context, MainActivity::class.java)
        val showPending = PendingIntent.getActivity(
            context,
            requestCodeFor(item.id),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val receiverPending = buildReceiverPendingIntent(context, item)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(item.timestampMs, showPending)
        alarmManager.setAlarmClock(alarmClockInfo, receiverPending)
    }

    private fun buildReceiverPendingIntent(context: Context, item: AlarmItem): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("id", item.id)
            putExtra("contactName", item.contactName)
            putExtra("reason", item.reason)
            putExtra("whatsappUrl", item.whatsappUrl)
            putExtra("snoozeMinutes", item.snoozeMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(item.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun saveAlarm(context: Context, item: AlarmItem) {
        val list = getSavedAlarms(context).filter { it.id != item.id }.toMutableList()
        list.add(item)
        writeList(context, list)
    }

    private fun removeAlarmFromPrefs(context: Context, id: String) {
        val list = getSavedAlarms(context).filter { it.id != id }
        writeList(context, list)
    }

    private fun getSavedAlarms(context: Context): List<AlarmItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ALARMS, "[]") ?: "[]"
        return try {
            AlarmItem.listFromJsonString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeList(context: Context, items: List<AlarmItem>) {
        val array = JSONArray()
        for (item in items) {
            array.put(item.toJson())
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALARMS, array.toString())
            .apply()
    }
}
