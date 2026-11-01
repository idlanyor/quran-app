package com.irengcloud.quran.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.irengcloud.quran.MainActivity
import com.irengcloud.quran.R
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.data.local.UserPreferencesRepository
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun nextReminderTime(now: Long, hour: Int, minute: Int, zone: TimeZone = TimeZone.getDefault()): Long {
    require(hour in 0..23 && minute in 0..59)
    return Calendar.getInstance(zone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}

class DailyReminder(private val context: Context) {
    companion object {
        const val CHANNEL = "daily_reading"
        const val ACTION_REMIND = "com.irengcloud.quran.DAILY_READING_REMINDER"
        const val ACTION_OPEN_READER = "com.irengcloud.quran.OPEN_LAST_READ"
        const val NOTIFICATION_ID = 5001
    }

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Pengingat baca Al-Qur'an", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun schedule(settings: ReaderSettingsState) {
        val operation = PendingIntent.getBroadcast(context, 5000,
            Intent(context, DailyReminderReceiver::class.java).setAction(ACTION_REMIND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.cancel(operation)
        if (settings.dailyReminderEnabled) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                nextReminderTime(System.currentTimeMillis(), settings.dailyReminderHour, settings.dailyReminderMinute),
                operation)
        } else {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    fun show(): Boolean {
        if (!canNotify()) return false
        val openReader = PendingIntent.getActivity(context, 5001,
            Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_READER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_reading_reminder)
            .setContentTitle("Waktunya membaca Al-Qur'an")
            .setContentText("Luangkan waktu untuk tilawah. Ketuk untuk melanjutkan bacaan terakhir.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openReader)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) { false }
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supported = setOf(DailyReminder.ACTION_REMIND, Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)
        if (intent.action !in supported) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = UserPreferencesRepository(context.applicationContext).readerSettingsFlow.first()
                val reminder = DailyReminder(context.applicationContext)
                reminder.schedule(settings)
                if (intent.action == DailyReminder.ACTION_REMIND && settings.dailyReminderEnabled) reminder.show()
            } finally { pending.finish() }
        }
    }
}
