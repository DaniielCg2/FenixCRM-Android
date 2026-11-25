package com.fenixlifereset.crm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmRingActivity : AppCompatActivity() {

    private var alarmId: String = "alarm"
    private var contactName: String = "Seguimiento Fénix"
    private var reason: String = "Es hora de escribirle a tu contacto"
    private var whatsappUrl: String = ""
    private var snoozeMinutes: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Encender la pantalla incluso si el celular está bloqueado
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_alarm_ring)

        alarmId = intent.getStringExtra("id") ?: "alarm"
        contactName = intent.getStringExtra("contactName") ?: "Seguimiento Fénix"
        reason = intent.getStringExtra("reason") ?: "Es hora de escribirle a tu contacto"
        whatsappUrl = intent.getStringExtra("whatsappUrl") ?: ""
        snoozeMinutes = intent.getIntExtra("snoozeMinutes", 5).coerceIn(1, 60)

        val tvAlarmTime = findViewById<TextView>(R.id.tvAlarmTime)
        val tvContactName = findViewById<TextView>(R.id.tvContactName)
        val tvAlarmReason = findViewById<TextView>(R.id.tvAlarmReason)
        val btnWhatsApp = findViewById<MaterialButton>(R.id.btnWhatsApp)
        val btnSnooze = findViewById<MaterialButton>(R.id.btnSnooze)
        val seekDismiss = findViewById<SeekBar>(R.id.seekDismiss)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale("es", "ES"))
        tvAlarmTime.text = timeFormat.format(Date())
        tvContactName.text = contactName
        tvAlarmReason.text = reason
        btnSnooze.text = "Posponer $snoozeMinutes minutos"

        if (whatsappUrl.isBlank()) {
            btnWhatsApp.visibility = View.GONE
        } else {
            btnWhatsApp.setOnClickListener {
                stopAlarmService()
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)))
                } catch (_: Exception) {
                    Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }

        btnSnooze.setOnClickListener {
            stopAlarmService()
            val nextTime = System.currentTimeMillis() + snoozeMinutes * 60_000L
            AlarmScheduler.scheduleAlarm(
                this,
                AlarmItem(
                    id = "${alarmId}_snooze",
                    contactName = contactName,
                    reason = reason,
                    timestampMs = nextTime,
                    whatsappUrl = whatsappUrl,
                    snoozeMinutes = snoozeMinutes
                )
            )
            Toast.makeText(this, "Alarma pospuesta $snoozeMinutes minutos", Toast.LENGTH_LONG).show()
            finish()
        }

        // Control interactivo: deslizar hasta el final para apagar
        seekDismiss.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress >= 85) {
                    stopAlarmService()
                    finish()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null && seekBar.progress < 85) {
                    seekBar.progress = 0
                }
            }
        })
    }

    private fun stopAlarmService() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(stopIntent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Evitar apagar la alarma accidentalmente con el botón Atrás
    }
}
