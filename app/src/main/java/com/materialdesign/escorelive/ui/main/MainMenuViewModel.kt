package com.materialdesign.escorelive.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.repository.FootballRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _liveMatches = MutableLiveData<List<LiveMatch>>()
    val liveMatches: LiveData<List<LiveMatch>> = _liveMatches

    private val _todayMatches = MutableLiveData<List<LiveMatch>>()
    val todayMatches: LiveData<List<LiveMatch>> = _todayMatches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        val today = dateFormat.format(Date())
        _selectedDate.value = today
        loadLiveMatches()
        loadMatchesByDate(today)
    }

    fun loadLiveMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getLiveMatches()
                .onSuccess { matches ->
                    // Filter for actually live matches
                    val actuallyLive = matches.filter { it.isLive }
                    _liveMatches.value = actuallyLive
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }

            _isLoading.value = false
        }
    }

    fun loadMatchesByDate(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getMatchesByDate(date)
                .onSuccess { matches ->
                    val sortedMatches = sortMatchesByStatus(matches)
                    _todayMatches.value = sortedMatches
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }

            _isLoading.value = false
        }
    }

    private fun sortMatchesByStatus(matches: List<LiveMatch>): List<LiveMatch> {
        return matches.sortedWith(compareBy<LiveMatch> { match ->
            when {
                match.isLive -> 0
                match.isUpcoming -> 1
                match.isFinished -> 2
                else -> 3
            }
        }.thenBy { match ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                match.kickoffTime?.let { inputFormat.parse(it)?.time } ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        })
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        loadMatchesByDate(date)

        // If today is selected, also load live matches
        val today = dateFormat.format(Date())
        if (date == today) {
            loadLiveMatches()
        }
    }

    fun refreshData() {
        _selectedDate.value?.let { selectedDate ->
            loadMatchesByDate(selectedDate)

            // If today is selected, also refresh live matches
            val today = dateFormat.format(Date())
            if (selectedDate == today) {
                loadLiveMatches()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun isSelectedDateToday(): Boolean {
        val today = dateFormat.format(Date())
        return _selectedDate.value == today
    }

    fun isSelectedDateInPast(): Boolean {
        val selectedDateStr = _selectedDate.value ?: return false
        val today = dateFormat.format(Date())

        return try {
            val selectedCalendar = Calendar.getInstance()
            val todayCalendar = Calendar.getInstance()

            selectedCalendar.time = dateFormat.parse(selectedDateStr) ?: Date()
            todayCalendar.time = dateFormat.parse(today) ?: Date()

            // Compare dates only (ignore time)
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
            selectedCalendar.set(Calendar.MINUTE, 0)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)

            todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            todayCalendar.set(Calendar.MINUTE, 0)
            todayCalendar.set(Calendar.SECOND, 0)
            todayCalendar.set(Calendar.MILLISECOND, 0)

            selectedCalendar.before(todayCalendar)
        } catch (e: Exception) {
            false
        }
    }

    fun isSelectedDateInFuture(): Boolean {
        val selectedDateStr = _selectedDate.value ?: return false
        val today = dateFormat.format(Date())

        return try {
            val selectedCalendar = Calendar.getInstance()
            val todayCalendar = Calendar.getInstance()

            selectedCalendar.time = dateFormat.parse(selectedDateStr) ?: Date()
            todayCalendar.time = dateFormat.parse(today) ?: Date()

            // Compare dates only (ignore time)
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
            selectedCalendar.set(Calendar.MINUTE, 0)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)

            todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            todayCalendar.set(Calendar.MINUTE, 0)
            todayCalendar.set(Calendar.SECOND, 0)
            todayCalendar.set(Calendar.MILLISECOND, 0)

            selectedCalendar.after(todayCalendar)
        } catch (e: Exception) {
            false
        }
    }
}