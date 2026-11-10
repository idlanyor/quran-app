package com.irengcloud.quran.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.reminders.DailyReminder
import com.irengcloud.quran.ui.DailyFeaturesViewModel
import java.util.Locale

@Composable
fun DailyReminderSettings(
    settings: ReaderSettingsState,
    features: DailyFeaturesViewModel,
    modifier: Modifier = Modifier,
    showTestAction: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reminder = remember(context) { DailyReminder(context) }
    var allowed by remember { mutableStateOf(reminder.canNotify()) }
    var message by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = reminder.canNotify()
        if (granted) features.setReminderEnabled(true)
        else message = "Izin notifikasi belum diberikan."
    }
    DisposableEffect(lifecycleOwner, reminder) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = reminder.canNotify()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().testTag("daily_reminder_card"),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsLabel(
                Icons.Outlined.Notifications, "Pengingat Ibadah",
                "Atur pengingat untuk membaca Al-Qur’an secara rutin.", Modifier.weight(1f)
            )
            SettingsSwitch(
                settings.dailyReminderEnabled,
                onCheckedChange = { enabled ->
                    message = null
                    if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        features.setReminderEnabled(enabled)
                    }
                },
                label = "Pengingat Ibadah",
                modifier = Modifier.testTag("daily_reminder_switch")
            )
        }
        Surface(
            onClick = {
                TimePickerDialog(
                    context, { _, hour, minute -> features.setReminderTime(hour, minute) },
                    settings.dailyReminderHour, settings.dailyReminderMinute, true
                ).show()
            },
            modifier = Modifier.fillMaxWidth().testTag("daily_reminder_time"),
            shape = RoundedCornerShape(13.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            shadowElevation = 2.dp
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Icon(Icons.Outlined.AccessTime, null, Modifier.size(22.dp), tint = scheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Waktu Pengingat", style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold
                    ))
                    Text(
                        String.format(Locale.US, "Setiap hari, %02d.%02d",
                            settings.dailyReminderHour, settings.dailyReminderMinute),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = scheme.primary)
            }
        }

        if (!allowed && (settings.dailyReminderEnabled || message != null || showTestAction)) {
            Text("Izinkan notifikasi agar pengingat bisa muncul.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                val intent = if (Build.VERSION.SDK_INT >= 26) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                }
                context.startActivity(intent)
            }) {
                Text("Buka Pengaturan Notifikasi")
            }
        }

        if (showTestAction) {
            TextButton(
                enabled = allowed,
                onClick = {
                    message = if (features.testReminder()) "Notifikasi percobaan dikirim."
                    else "Notifikasi diblokir oleh sistem HP."
                },
                modifier = Modifier.testTag("test_daily_reminder")
            ) {
                Text("Coba Notifikasi")
            }
        }

        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun SettingsLabel(
    icon: ImageVector,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold
            ))
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Switch(
        checked, onCheckedChange,
        modifier = modifier.semantics { contentDescription = label },
        colors = SwitchDefaults.colors(
            checkedTrackColor = Color(0xFF9B603E),
            checkedThumbColor = Color(0xFFFFF5E8)
        )
    )
}
