package com.hyperathlete.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.data.weather.WeatherRepository
import com.hyperathlete.app.data.weather.WeatherState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(private val repo: WeatherRepository) : ViewModel() {
    val state: StateFlow<WeatherState> = repo.state

    /** Uses the cache when it's under 30 minutes old unless [force]. */
    fun refresh(locale: Locale, force: Boolean = false) {
        viewModelScope.launch { runCatching { repo.refresh(locale, force) } }
    }
}
