package com.vaibhav.jee

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Locale
import org.json.JSONArray

/**
 * Local reminders for explicitly dated entries.
 * Enter the date/time in the detail field as yyyy-MM-dd HH:mm (24-hour local time).
 * Uses inexact allow-while-idle alarms: Android may defer delivery under battery restrictions.
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "vaibhav_study_reminders"
    private const val ACTION_REMIND = "com.vaibhav.jee.ACTION_REMIND"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VAIBHAV study reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Mission Jeet classes, homework and backlog reminders" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun schedule(context: Context, kind: String, title: String, detail: String) {
        val match = Regex("""(\d{4}-\d{2}-\d{2} \d{2}:\d{2})""").find(detail) ?: return
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply { isLenient = false }
                .parse(match.value)?.time
        }.getOrNull() ?: return

        val now = System.currentTimeMillis()
        val offsets = when (kind) {
            "schedule" -> listOf(30L to "Class starts in 30 minutes", 10L to "Class starts in 10 minutes")
            "homework" -> listOf(24L * 60 to "Homework deadline in 24 hours", 2L * 60 to "Homework deadline in 2 hours")
            "backlogs" -> listOf(0L to "Backlog reminder")
            else -> emptyList()
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        offsets.forEachIndexed { index, (minutes, message) ->
            val triggerAt = if (kind == "backlogs") parsed else parsed - minutes * 60_000L
            if (triggerAt <= now) return@forEachIndexed
            val requestCode = (kind + title + index).hashCode()
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_REMIND
                putExtra("title", title)
                putExtra("message", message)
                putExtra("requestCode", requestCode)
            }
            val pending = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.createChannel(context)
        val title = intent.getStringExtra("title") ?: "Study reminder"
        val message = intent.getStringExtra("message") ?: "Check your VAIBHAV study plan."
        val id = intent.getIntExtra("requestCode", title.hashCode())
        val notification = NotificationCompat.Builder(context, "vaibhav_study_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}


/** Rebuilds local alarms after device reboot from the app's saved entries. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("vaibhav_data", Context.MODE_PRIVATE)
        listOf("schedule", "homework", "backlogs").forEach { kind ->
            val entries = runCatching { JSONArray(prefs.getString(kind, "[]") ?: "[]") }.getOrNull()
                ?: return@forEach
            for (i in 0 until entries.length()) {
                val item = entries.optJSONObject(i) ?: continue
                ReminderScheduler.schedule(
                    context,
                    kind,
                    item.optString("title"),
                    item.optString("detail").substringAfter(" • ", item.optString("detail"))
                )
            }
        }
    }
}
