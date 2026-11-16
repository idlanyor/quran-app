package com.irengcloud.quran.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irengcloud.quran.data.Dua
import com.irengcloud.quran.data.DuaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DuaState(
    val items: List<Dua> = emptyList(), val loading: Boolean = true,
    val error: String? = null, val refreshFailed: Boolean = false
)

class DuaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DuaRepository(application)
    private val mutableState = MutableStateFlow(DuaState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load(refresh: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null, refreshFailed = false)
            try {
                val result = repository.load(refresh)
                mutableState.value = DuaState(items = result.items, loading = false, refreshFailed = result.refreshFailed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.value = mutableState.value.copy(loading = false,
                    error = "Doa belum dapat dimuat. Periksa koneksi internet, lalu coba lagi.")
            }
        }
    }
}
