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
import android.util.Log

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

            try {
                repository.getLiveMatches()
                    .onSuccess { matches ->
                        // Filter for actually live matches
                        val actuallyLive = matches.filter { it.isLive }
                        _liveMatches.value = actuallyLive
                        Log.d("MainMenuViewModel", "Loaded ${actuallyLive.size} live matches")
                    }
                    .onFailure { exception ->
                        Log.e("MainMenuViewModel", "Failed to load live matches", exception)
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                Log.e("MainMenuViewModel", "Exception loading live matches", e)
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    fun loadMatchesByDate(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getMatchesByDate(date)
                    .onSuccess { matches ->
                        // DEBUG: Log all matches for the selected date
                        Log.d("MainMenuViewModel", "Loaded ${matches.size} matches for date: $date")
                        matches.forEach { match ->
                            Log.d("MainMenuViewModel", "Match: ${match.homeTeam.name} vs ${match.awayTeam.name} - League: ${match.league.name} (ID: ${match.league.id}) - Status: ${match.matchStatus}")
                        }

                        val sortedMatches = sortMatchesByStatus(matches)
                        _todayMatches.value = sortedMatches
                    }
                    .onFailure { exception ->
                        Log.e("MainMenuViewModel", "Failed to load matches for date: $date", exception)
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                Log.e("MainMenuViewModel", "Exception loading matches for date: $date", e)
                _error.value = e.message
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
        }.thenBy { match ->
            // Prioritize important leagues
            when (match.league.id.toInt()) {
                2 -> 0   // Champions League
                3 -> 1   // Europa League
                848 -> 2 // Europa League Play-offs
                39 -> 3  // Premier League
                140 -> 4 // La Liga
                78 -> 5  // Bundesliga
                135 -> 6 // Serie A
                61 -> 7  // Ligue 1
                342 -> 8 // Azerbaijan Premier League
                203 -> 9 // Turkish Super League
                else -> 10
            }
        })
    }

    fun selectDate(date: String) {
        Log.d("MainMenuViewModel", "Selected date: $date")
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
            Log.d("MainMenuViewModel", "Refreshing data for date: $selectedDate")
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