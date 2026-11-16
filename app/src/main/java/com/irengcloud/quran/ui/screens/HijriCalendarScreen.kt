package com.irengcloud.quran.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.irengcloud.quran.R
import com.irengcloud.quran.data.HijriCalendar
import com.irengcloud.quran.data.HijriDay
import com.irengcloud.quran.data.HijriMonth
import com.irengcloud.quran.data.HijriMonthNames
import com.irengcloud.quran.ui.components.AppArtwork
import com.irengcloud.quran.ui.components.rememberTodayKey
import java.util.TimeZone
import kotlinx.coroutines.launch

@Composable
fun HijriCalendarScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val zoneId = TimeZone.getDefault().id
    val todayKey = rememberTodayKey()
    val today = remember(todayKey, zoneId) { HijriCalendar.date(todayKey, zoneId) }
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var month by rememberSaveable { mutableIntStateOf(today.month) }
    var selectedKey by rememberSaveable { mutableStateOf(todayKey) }
    var previousTodayKey by rememberSaveable { mutableStateOf(todayKey) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    val displayedMonth = remember(year, month, zoneId) { HijriCalendar.month(year, month, zoneId) }
    val occasions = remember(displayedMonth) { displayedMonth.days.filter { it.occasions.isNotEmpty() } }
    val selectedDay = remember(selectedKey, zoneId) { HijriCalendar.date(selectedKey, zoneId) }
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(todayKey) {
        if (selectedKey == previousTodayKey) {
            year = today.year
            month = today.month
            selectedKey = todayKey
        }
        previousTodayKey = todayKey
    }

    fun showMonth(targetYear: Int, targetMonth: Int) {
        val next = HijriCalendar.month(targetYear, targetMonth, zoneId)
        year = next.year
        month = next.month
        selectedKey = if (today.year == year && today.month == month) todayKey
            else next.days.first().gregorianKey
    }

    Surface(modifier.fillMaxSize(), color = scheme.background, contentColor = scheme.onBackground) {
        Box(contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("hijri_calendar"),
                state = listState,
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                item(key = "header") {
                    Box(Modifier.fillMaxWidth().heightIn(min = 174.dp).background(Color(0xFF242338))) {
                        AppArtwork(R.drawable.settings_header_v2, Modifier.matchParentSize(),
                            contentScale = ContentScale.FillBounds)
                        Column(
                            Modifier.align(Alignment.TopCenter).padding(start = 55.dp, end = 55.dp, top = 18.dp, bottom = 43.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(31.dp), tint = Color(0xFFE4B789))
                            Text("Kalender Hijriyah", style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFFFFF2E5), textAlign = TextAlign.Center)
                            Text(today.hijriLabel, style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF0D5B9), textAlign = TextAlign.Center)
                        }
                        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(9.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color(0xFFFFF2E5))
                        }
                        Spacer(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(27.dp)
                            .background(scheme.background, RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp)))
                    }
                }
                item(key = "calendar") {
                    Surface(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp), color = scheme.surface,
                        border = BorderStroke(1.dp, scheme.outlineVariant), shadowElevation = 1.dp
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showMonth(if (month == 0) year - 1 else year, (month + 11) % 12) },
                                    enabled = year > HijriCalendar.MIN_YEAR || month > 0,
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = scheme.primary),
                                    modifier = Modifier.testTag("hijri_previous_month")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Bulan sebelumnya")
                                }
                                TextButton(
                                    onClick = { showMonthPicker = true },
                                    modifier = Modifier.weight(1f).testTag("hijri_choose_month")
                                ) {
                                    Text(displayedMonth.label, style = MaterialTheme.typography.titleLarge,
                                        color = scheme.onSurface, textAlign = TextAlign.Center)
                                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { showMonth(if (month == 11) year + 1 else year, (month + 1) % 12) },
                                    enabled = year < HijriCalendar.MAX_YEAR || month < 11,
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = scheme.primary),
                                    modifier = Modifier.testTag("hijri_next_month")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Bulan berikutnya")
                                }
                            }
                            Text(
                                "${HijriCalendar.format(displayedMonth.days.first(), "d MMM yyyy", zoneId)} – " +
                                    HijriCalendar.format(displayedMonth.days.last(), "d MMM yyyy", zoneId),
                                Modifier.fillMaxWidth().padding(bottom = 17.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant, textAlign = TextAlign.Center
                            )
                            HijriMonthGrid(displayedMonth, todayKey, selectedKey, zoneId,
                                onSelect = { selectedKey = it.gregorianKey })
                            Row(
                                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                            ) {
                                Spacer(Modifier.size(5.dp).clip(CircleShape).background(scheme.primary))
                                Text("Hari besar Islam", style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item(key = "legend") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Angka kecil: Masehi\nNama di bawahnya: pasaran", Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        TextButton(
                            onClick = { year = today.year; month = today.month; selectedKey = todayKey },
                            modifier = Modifier.testTag("hijri_today")
                        ) {
                            Icon(Icons.Outlined.Today, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Hari ini")
                        }
                    }
                }
                item(key = "selected_date") {
                    Surface(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("hijri_selected_date"),
                        shape = RoundedCornerShape(17.dp), color = scheme.primaryContainer,
                        contentColor = scheme.onPrimaryContainer
                    ) {
                        Row(
                            Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(selectedDay.day.toString(), style = MaterialTheme.typography.displayLarge)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${HijriMonthNames[selectedDay.month]} ${selectedDay.year} H",
                                    style = MaterialTheme.typography.titleLarge)
                                Text(HijriCalendar.format(selectedDay, "EEEE, d MMMM yyyy", zoneId),
                                    style = MaterialTheme.typography.bodySmall)
                                selectedDay.occasions.forEach {
                                    Text(it.title, style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
                item(key = "javanese_date") {
                    val jawa = selectedDay.javanese
                    Surface(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp)
                            .testTag("javanese_selected_date"),
                        shape = RoundedCornerShape(17.dp), color = scheme.surface,
                        border = BorderStroke(1.dp, scheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Penanggalan Jawa", style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant)
                            Text(jawa.weton, style = MaterialTheme.typography.titleLarge, color = scheme.primary)
                            Text(jawa.date?.label ?: "Tanggal Jawa tersedia untuk 24 Maret 1936–25 Agustus 2052.",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Kalender Jawa Asapon · Tanggal mengacu pada siang hari Masehi.",
                                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
                item(key = "occasions") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 17.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text("Hari Besar Bulan Ini", style = MaterialTheme.typography.titleLarge)
                        if (occasions.isEmpty()) {
                            Text("Tidak ada hari besar yang dicatat pada bulan ini.",
                                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        occasions.forEach { date ->
                            Surface(
                                onClick = {
                                    selectedKey = date.gregorianKey
                                    scope.launch { listState.animateScrollToItem(3) }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("hijri_occasion_${date.day}"),
                                shape = RoundedCornerShape(14.dp), color = scheme.surface,
                                border = BorderStroke(1.dp, scheme.outlineVariant)
                            ) {
                                Row(
                                    Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Outlined.CalendarMonth, null, tint = scheme.primary, modifier = Modifier.size(24.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(date.occasions.joinToString { it.title }, style = MaterialTheme.typography.titleSmall)
                                        Text("${date.day} ${HijriMonthNames[date.month]} · " +
                                            HijriCalendar.format(date, "d MMMM yyyy", zoneId),
                                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "method") {
                    Text(
                        "Perhitungan Umm al-Qura. Awal Ramadan dan hari raya mengikuti penetapan setempat.",
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 15.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showMonthPicker) {
        HijriMonthPicker(year, month, onDismiss = { showMonthPicker = false }) { targetYear, targetMonth ->
            showMonthPicker = false
            showMonth(targetYear, targetMonth)
        }
    }
}

@Composable
private fun HijriMonthGrid(
    month: HijriMonth,
    todayKey: String,
    selectedKey: String,
    zoneId: String,
    onSelect: (HijriDay) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val slots = List<HijriDay?>(month.leadingEmptyCells) { null } + month.days
    Row(Modifier.fillMaxWidth()) {
        listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Ahad").forEach { label ->
            Text(label, Modifier.weight(1f).padding(bottom = 8.dp),
                style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                color = scheme.onSurfaceVariant)
        }
    }
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        slots.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(7) { index ->
                    val day = week.getOrNull(index)
                    if (day == null) {
                        Spacer(Modifier.weight(1f).heightIn(min = 54.dp))
                    } else {
                        val selected = day.gregorianKey == selectedKey
                        val isToday = day.gregorianKey == todayKey
                        Surface(
                            Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                                .selectable(selected, role = Role.RadioButton, onClick = { onSelect(day) })
                                .semantics {
                                    contentDescription = day.hijriLabel + ", " +
                                        HijriCalendar.format(day, "EEEE d MMMM yyyy", zoneId) +
                                        ", pasaran " + day.javanese.pasaran +
                                        (day.javanese.date?.let { ", tanggal Jawa " + it.label } ?: "") +
                                        (if (isToday) ", hari ini" else "") +
                                        day.occasions.joinToString(prefix = if (day.occasions.isEmpty()) "" else ", ") { it.title }
                                }
                                .testTag("hijri_day_${day.day}"),
                            shape = RoundedCornerShape(11.dp),
                            color = if (selected) scheme.primary else Color.Transparent,
                            contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                            border = if (isToday && !selected) BorderStroke(1.dp, scheme.primary) else null
                        ) {
                            Column(
                                Modifier.heightIn(min = 74.dp).padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(day.day.toString(), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium)
                                Text(day.gregorianDay.toString(), style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) scheme.onPrimary.copy(alpha = 0.8f) else scheme.onSurfaceVariant)
                                Text(day.javanese.pasaran, style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) scheme.onPrimary else scheme.primary)
                                Spacer(Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape)
                                    .background(if (day.occasions.isEmpty()) Color.Transparent
                                        else if (selected) scheme.onPrimary else scheme.primary))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HijriMonthPicker(
    year: Int,
    month: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var yearText by rememberSaveable { mutableStateOf(year.toString()) }
    var chosenMonth by rememberSaveable { mutableIntStateOf(month) }
    var expanded by remember { mutableStateOf(false) }
    val validYear = yearText.toIntOrNull()?.takeIf { it in HijriCalendar.MIN_YEAR..HijriCalendar.MAX_YEAR }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Bulan Hijriyah") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedButton(onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("hijri_month_input")) {
                        Text(HijriMonthNames[chosenMonth], Modifier.weight(1f))
                        Icon(Icons.Outlined.ExpandMore, null)
                    }
                    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        HijriMonthNames.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { chosenMonth = index; expanded = false },
                                modifier = Modifier.testTag("hijri_month_option_$index")
                            )
                        }
                    }
                }
                OutlinedTextField(
                    yearText,
                    onValueChange = { input -> yearText = input.filter(Char::isDigit).take(4) },
                    label = { Text("Tahun Hijriyah") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, isError = validYear == null,
                    supportingText = { Text("${HijriCalendar.MIN_YEAR}–${HijriCalendar.MAX_YEAR} H") },
                    modifier = Modifier.fillMaxWidth().testTag("hijri_year_input")
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        confirmButton = {
            TextButton(onClick = { validYear?.let { onConfirm(it, chosenMonth) } }, enabled = validYear != null) {
                Text("Tampilkan")
            }
        }
    )
}
