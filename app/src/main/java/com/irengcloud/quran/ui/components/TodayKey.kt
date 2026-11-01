package com.irengcloud.quran.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

/** Refresh on resume and at midnight even if the app stays open. */
@Composable
fun rememberTodayKey(zone: TimeZone = TimeZone.getDefault()): String {
    val owner = LocalLifecycleOwner.current
    fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(Date())
    val day by produceState(remember(zone.id) { today() }, owner, zone.id) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = today()
                val midnight = Calendar.getInstance(zone).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                delay((midnight - System.currentTimeMillis()).coerceIn(1, 60_000))
            }
        }
    }
    return day
}
