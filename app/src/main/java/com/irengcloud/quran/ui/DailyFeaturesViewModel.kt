package com.irengcloud.quran.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.os.Build
import android.os.CancellationSignal
import androidx.core.location.LocationManagerCompat
import kotlin.coroutines.resumeWithException
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
import com.irengcloud.quran.data.RegionLocationRepository
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
import kotlinx.coroutines.currentCoroutineContext
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
    val error: String? = null,
    val resolvingManualLocation: Boolean = false,
    val qiblaError: String? = null
)

class DailyFeaturesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferencesRepository(application)
    private val repository by lazy { PrayerRepository(application) }
    private val regionLocations by lazy { RegionLocationRepository(application) }
    private val reminder = DailyReminder(application)
    private val _prayer = MutableStateFlow(PrayerUiState())
    val prayer = _prayer.asStateFlow()
    val qiblaLocation = prefs.qiblaLocationFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private var monthJob: Job? = null
    private var regionJob: Job? = null
    private var locationJob: Job? = null
    private var manualLocationJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = prefs.readerSettingsFlow.first()
            val location = prefs.qiblaLocationFlow.first()
            if (settings.prayerCity.isNotBlank() && settings.prayerProvince.isNotBlank() &&
                (location == null || location.isManualRegion &&
                    (location.regionCity != settings.prayerCity || location.regionProvince != settings.prayerProvince))) {
                resolveManualQibla()
            }
        }
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
        locationJob?.cancel()
        manualLocationJob?.cancel()
        manualLocationJob = viewModelScope.launch {
            prefs.updatePrayerLocation(state.selectedProvince, city, clearQiblaLocation = true)
            resolveManualRegion(state.selectedProvince, city)
        }
    }

    fun resolveManualQibla() {
        locationJob?.cancel()
        manualLocationJob?.cancel()
        manualLocationJob = viewModelScope.launch {
            val settings = prefs.readerSettingsFlow.first()
            if (settings.prayerProvince.isNotBlank() && settings.prayerCity.isNotBlank()) {
                resolveManualRegion(settings.prayerProvince, settings.prayerCity)
            }
        }
    }

    private suspend fun resolveManualRegion(province: String, city: String) {
        _prayer.update { it.copy(resolvingManualLocation = true, locating = false, qiblaError = null) }
        try {
            val location = regionLocations.resolve(province, city)
            currentCoroutineContext().ensureActive()
            prefs.saveQiblaLocation(location)
            _prayer.update { it.copy(resolvingManualLocation = false, qiblaError = null) }
        } catch (_: TimeoutCancellationException) {
            _prayer.update { it.copy(resolvingManualLocation = false, qiblaError = "Pencarian titik wilayah habis waktu. Coba lagi saat koneksi tersedia.") }
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            _prayer.update { it.copy(resolvingManualLocation = false, qiblaError = error.message ?: "Titik wilayah belum dapat dimuat.") }
        }
    }

    fun detectLocation() {
        if (locationJob?.isActive == true) return
        manualLocationJob?.cancel()
        locationJob = viewModelScope.launch {
            _prayer.update { it.copy(locating = true, resolvingManualLocation = false, error = null, qiblaError = null) }
            var coordinatesSaved = false
            try {
                val location = currentLocation()
                prefs.saveQiblaLocation(QiblaLocation(location.latitude, location.longitude,
                    if (location.hasAltitude()) location.altitude else 0.0, location.accuracy, location.time))
                coordinatesSaved = true
                val address = reverseGeocode(location)
                    ?: error("Alamat wilayah belum ditemukan. Pilih provinsi dan kota secara manual.")
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
                _prayer.update { it.copy(locating = false, error = if (coordinatesSaved) "Lokasi kiblat sudah tersimpan. Wilayah jadwal sholat belum terdeteksi; pilih provinsi dan kota." else "GPS dan jaringan belum memberi lokasi. Coba di area terbuka atau pilih wilayah manual.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _prayer.update { it.copy(locating = false, error = if (coordinatesSaved) "Lokasi kiblat sudah tersimpan. Wilayah jadwal sholat belum terdeteksi; pilih provinsi dan kota." else e.message ?: "Lokasi belum dapat dideteksi. Pilih wilayah manual.") } }
        }
    }

    private suspend fun reverseGeocode(location: Location): Address? {
        check(Geocoder.isPresent()) { "Layanan alamat wilayah tidak tersedia." }
        val geocoder = Geocoder(getApplication(), Locale.forLanguageTag("id-ID"))
        return if (Build.VERSION.SDK_INT >= 33) {
            withTimeout(10_000) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }
                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Alamat wilayah belum tersedia."))
                        }
                    })
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            }
        }
    }

    private suspend fun currentLocation(): Location = withTimeout(45_000) {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        check(fine || coarse) { "Izinkan akses lokasi atau pilih wilayah secara manual." }
        val available = manager.allProviders.toSet()
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (fine) add(LocationManager.GPS_PROVIDER)
        }.filter { it in available && manager.isProviderEnabled(it) }
        check(providers.isNotEmpty()) { "Aktifkan lokasi HP atau pilih wilayah secara manual." }
        fun valid(location: Location): Boolean = location.latitude.isFinite() && location.longitude.isFinite() &&
            location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0
        @Suppress("MissingPermission")
        val cached = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { valid(it) && android.os.SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..600_000_000_000L }
            .minByOrNull { it.accuracy }
        if (cached != null) return@withTimeout cached
        suspendCancellableCoroutine { continuation ->
            val signals = providers.map { CancellationSignal() }
            val executor = ContextCompat.getMainExecutor(context)
            var remaining = providers.size
            fun unavailable() {
                remaining--
                if (remaining == 0 && continuation.isActive) {
                    continuation.resumeWithException(IllegalStateException("GPS dan jaringan belum memberi lokasi. Coba di area terbuka atau pilih wilayah manual."))
                }
            }
            continuation.invokeOnCancellation { signals.forEach { it.cancel() } }
            providers.forEachIndexed { index, provider ->
                if (continuation.isActive) {
                    try {
                        @Suppress("MissingPermission")
                        LocationManagerCompat.getCurrentLocation(manager, provider, signals[index], executor) { location ->
                            if (continuation.isActive) {
                                if (location != null && valid(location)) {
                                    signals.forEach { it.cancel() }
                                    continuation.resume(location)
                                } else unavailable()
                            }
                        }
                    } catch (_: Exception) { unavailable() }
                }
            }
        }
    }
}
