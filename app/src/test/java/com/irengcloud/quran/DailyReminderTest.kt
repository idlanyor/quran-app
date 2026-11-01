package com.irengcloud.quran

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.reminders.DailyReminder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyReminderTest {
    @Test fun updatingReminderReplacesAlarmAndDisablingCancelsIt() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val reminder = DailyReminder(app)
        val alarms = Shadows.shadowOf(app.getSystemService(AlarmManager::class.java))
        reminder.schedule(ReaderSettingsState(dailyReminderEnabled = true, dailyReminderHour = 5))
        reminder.schedule(ReaderSettingsState(dailyReminderEnabled = true, dailyReminderHour = 7))
        assertEquals(1, alarms.scheduledAlarms.size)
        assertTrue(alarms.scheduledAlarms.single().triggerAtTime > System.currentTimeMillis())
        reminder.schedule(ReaderSettingsState(dailyReminderEnabled = false))
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
    @Test fun notificationNeedsPermissionAndOpensReaderWhenAllowed() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val reminder = DailyReminder(app)
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(reminder.show())
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(reminder.show())
        val notification = Shadows.shadowOf(app.getSystemService(NotificationManager::class.java))
            .getNotification(DailyReminder.NOTIFICATION_ID)
        assertNotNull(notification)
        val intent = Shadows.shadowOf(notification.contentIntent).savedIntent
        assertEquals(DailyReminder.ACTION_OPEN_READER, intent.action)
    }
}
