package com.fenixlifereset.crm

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONObject

class WebAlarmBridge(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun syncAlarms(jsonList: String) {
        try {
            val items = AlarmItem.listFromJsonString(jsonList)
            AlarmScheduler.syncAlarms(context, items)
        } catch (_: Exception) {
        }
    }

    @JavascriptInterface
    fun scheduleAlarm(jsonItem: String) {
        try {
            val item = AlarmItem.fromJson(JSONObject(jsonItem))
            AlarmScheduler.scheduleAlarm(context, item)
            showToast("Alarma programada para ${item.contactName}")
        } catch (_: Exception) {
        }
    }

    @JavascriptInterface
    fun cancelAlarm(id: String) {
        try {
            AlarmScheduler.cancelAlarm(context, id)
        } catch (_: Exception) {
        }
    }

    @JavascriptInterface
    fun testAlarm() {
        val triggerAt = System.currentTimeMillis() + 5_000L
        AlarmScheduler.scheduleAlarm(
            context,
            AlarmItem(
                id = "fenix_test_alarm",
                contactName = "Alarma de prueba · Fénix",
                reason = "¡Bloquea tu celular! Sonará en 5 segundos",
                timestampMs = triggerAt,
                whatsappUrl = "",
                snoozeMinutes = 5
            )
        )
        showToast("Bloquea tu celular: la alarma sonará en 5 segundos")
    }

    private fun showToast(msg: String) {
        mainHandler.post {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
}
