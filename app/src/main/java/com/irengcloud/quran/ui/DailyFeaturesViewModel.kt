package com.irengcloud.quran.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irengcloud.quran.data.PrayerMonth
import com.irengcloud.quran.data.QiblaLocation
import com.irengcloud.quran.data.PrayerRepository
import com.irengcloud.quran.data.matchCity
import com.irengcloud.quran.data.matchProvince
import com.irengcloud.quran.data.prayerTimeZone
import com.irengcloud.quran.data.local.UserPreferencesRepository
import com.irengcloud.quran.reminders.DailyReminder
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class PrayerUiState(
    val data: PrayerMonth? = null,
    val provinces: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val selectedProvince: String = "",
    val loading: Boolean = false,
    val locating: Boolean = false,
    val loadingRegions: Boolean = false,
    val error: String? = null
)

class DailyFeaturesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferencesRepository(application)
    private val repository by lazy { PrayerRepository(application) }
    private val reminder = DailyReminder(application)
    private val _prayer = MutableStateFlow(PrayerUiState())
    val prayer = _prayer.asStateFlow()
    val qiblaLocation = prefs.qiblaLocationFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private var monthJob: Job? = null
    private var regionJob: Job? = null
    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.readerSettingsFlow.map { Triple(it.dailyReminderEnabled, it.dailyReminderHour, it.dailyReminderMinute) }
                .distinctUntilChanged().collect { (enabled, hour, minute) ->
                    reminder.schedule(com.irengcloud.quran.data.local.ReaderSettingsState(
                        dailyReminderEnabled = enabled, dailyReminderHour = hour, dailyReminderMinute = minute))
                }
        }
    }

    fun setReminderEnabled(enabled: Boolean) { viewModelScope.launch { prefs.updateDailyReminder(enabled = enabled) } }
    fun setReminderTime(hour: Int, minute: Int) { viewModelScope.launch { prefs.updateDailyReminder(hour = hour, minute = minute) } }
    fun testReminder(): Boolean = reminder.show()

    fun loadRegions(province: String) {
        locationJob?.cancel()
        regionJob?.cancel()
        regionJob = viewModelScope.launch {
            _prayer.update { it.copy(loadingRegions = true, locating = false, error = null) }
            try {
                val provinces = repository.provinces()
                val selected = province.takeIf { it in provinces }.orEmpty()
                val cities = if (selected.isNotEmpty()) repository.cities(selected) else emptyList()
                ensureActive()
                _prayer.update { it.copy(provinces = provinces, cities = cities, selectedProvince = selected, loadingRegions = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _prayer.update { it.copy(loadingRegions = false, error = "Daftar wilayah belum dapat dimuat. Periksa koneksi lalu coba lagi.") } }
        }
    }

    fun refreshPrayer(province: String, city: String, force: Boolean = false) {
        monthJob?.cancel()
        if (province.isBlank() || city.isBlank()) return
        monthJob = viewModelScope.launch {
            _prayer.update { it.copy(loading = true, data = it.data?.takeIf { data -> data.province == province && data.city == city }, error = null) }
            try {
                val date = Calendar.getInstance(prayerTimeZone(province))
                val data = repository.month(province, city, date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, force)
                ensureActive()
                _prayer.update { it.copy(data = data, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _prayer.update { it.copy(loading = false, error = "Jadwal bulan ini belum tersedia atau koneksi bermasalah. Ketuk Muat ulang untuk mencoba lagi.") } }
        }
    }

    fun selectCity(city: String) {
        val state = _prayer.value
        if (city !in state.cities) return
        viewModelScope.launch { prefs.updatePrayerLocation(state.selectedProvince, city) }
    }

    fun detectLocation() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            _prayer.update { it.copy(locating = true, error = null) }
            try {
                val location = currentLocation()
                prefs.saveQiblaLocation(QiblaLocation(location.latitude, location.longitude,
                    if (location.hasAltitude()) location.altitude else 0.0, location.accuracy, location.time))
                val address = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    Geocoder(getApplication(), Locale("id", "ID"))
                        .getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                } ?: error("Alamat lokasi tidak ditemukan. Pilih provinsi dan kota secara manual.")
                val provinces = repository.provinces()
                val province = matchProvince(address.adminArea.orEmpty(), provinces)
                    ?: error("Provinsi lokasi ini belum dikenali. Pilih wilayah secara manual.")
                val cities = repository.cities(province)
                val city = matchCity(listOfNotNull(address.subAdminArea, address.locality), province, cities)
                    ?: error("Kabupaten/kota belum cocok. Silakan pilih kota secara manual.")
                regionJob?.cancel()
                prefs.updatePrayerLocation(province, city)
                _prayer.update { it.copy(locating = false, loadingRegions = false, provinces = provinces,
                    cities = cities, selectedProvince = province) }
            } catch (_: TimeoutCancellationException) {
                _prayer.update { it.copy(locating = false, error = "Lokasi belum didapat. Aktifkan lokasi HP, lalu coba lagi atau pilih wilayah manual.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _prayer.update { it.copy(locating = false, error = e.message ?: "Lokasi belum dapat dideteksi. Pilih wilayah manual.") } }
        }
    }

    private suspend fun currentLocation(): Location = withTimeout(20_000) {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        check(fine || coarse) { "Izinkan akses lokasi atau pilih wilayah secara manual." }
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { (it != LocationManager.GPS_PROVIDER || fine) && manager.isProviderEnabled(it) }
        check(providers.isNotEmpty()) { "Aktifkan lokasi HP atau pilih wilayah secara manual." }
        @Suppress("MissingPermission")
        val cached = providers.mapNotNull { manager.getLastKnownLocation(it) }
            .filter { android.os.SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..600_000_000_000L }
            .minByOrNull { it.accuracy }
        if (cached != null) return@withTimeout cached
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
                @Deprecated("Legacy location callback")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            try {
                @Suppress("DEPRECATION", "MissingPermission")
                manager.requestSingleUpdate(providers.first(), listener, Looper.getMainLooper())
            } catch (e: Exception) { continuation.cancel(e) }
        }
    }
}
