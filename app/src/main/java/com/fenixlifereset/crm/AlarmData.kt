package com.fenixlifereset.crm

import org.json.JSONArray
import org.json.JSONObject

data class AlarmItem(
    val id: String,
    val contactName: String,
    val reason: String,
    val timestampMs: Long,
    val whatsappUrl: String,
    val snoozeMinutes: Int = 5
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("contactName", contactName)
        put("reason", reason)
        put("timestampMs", timestampMs)
        put("whatsappUrl", whatsappUrl)
        put("snoozeMinutes", snoozeMinutes)
    }

    companion object {
        fun fromJson(obj: JSONObject): AlarmItem {
            return AlarmItem(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                contactName = obj.optString("contactName", "Seguimiento Fénix"),
                reason = obj.optString("reason", "Es hora de escribirle a tu contacto"),
                timestampMs = obj.optLong("timestampMs", 0L),
                whatsappUrl = obj.optString("whatsappUrl", ""),
                snoozeMinutes = obj.optInt("snoozeMinutes", 5).coerceIn(1, 60)
            )
        }

        fun listFromJsonString(raw: String): List<AlarmItem> {
            val result = mutableListOf<AlarmItem>()
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                result.add(fromJson(item))
            }
            return result
        }
    }
}
