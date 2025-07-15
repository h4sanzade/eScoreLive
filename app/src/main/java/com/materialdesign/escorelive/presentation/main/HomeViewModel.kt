package com.materialdesign.escorelive.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.domain.model.Match
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import com.materialdesign.escorelive.data.remote.repository.FootballRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FootballRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _liveMatches = MutableLiveData<List<Match>>()
    val liveMatches: LiveData<List<Match>> = _liveMatches

    private val _todayMatches = MutableLiveData<List<Match>>()
    val todayMatches: LiveData<List<Match>> = _todayMatches

    private val _favoriteMatches = MutableLiveData<List<Match>>()
    val favoriteMatches: LiveData<List<Match>> = _favoriteMatches

    private val _upcomingMatches = MutableLiveData<List<Match>>()
    val upcomingMatches: LiveData<List<Match>> = _upcomingMatches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val favoriteTeamIds = mutableSetOf<Long>()

    init {
        val today = dateFormat.format(Date())
        _selectedDate.value = today
        loadFavoriteTeamIds()
        loadInitialData()
    }

    private fun loadInitialData() {
        val today = dateFormat.format(Date())
        loadUpcomingMatches(today)
    }

    private fun loadFavoriteTeamIds() {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favoriteIds = prefs.getStringSet("favorite_team_ids", emptySet()) ?: emptySet()
        favoriteTeamIds.clear()
        favoriteTeamIds.addAll(favoriteIds.map { it.toLong() })
    }

    fun addToFavorites(teamId: Long) {
        favoriteTeamIds.add(teamId)
        saveFavoriteTeamIds()
        loadFavoriteTeamMatches()
    }

    fun removeFromFavorites(teamId: Long) {
        favoriteTeamIds.remove(teamId)
        saveFavoriteTeamIds()
        loadFavoriteTeamMatches()
    }

    fun isTeamFavorite(teamId: Long): Boolean {
        return favoriteTeamIds.contains(teamId)
    }

    private fun saveFavoriteTeamIds() {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("favorite_team_ids", favoriteTeamIds.map { it.toString() }.toSet())
            .apply()
    }

    fun loadUpcomingMatches(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getMatchesByDate(date)
                    .onSuccess { matches ->
                        val upcomingMatches = matches.filter { it.isUpcoming }
                        val sortedMatches = sortMatchesByStatus(upcomingMatches)
                        _todayMatches.value = sortedMatches
                        _upcomingMatches.value = sortedMatches
                        Log.d("HomeViewModel", "Loaded ${upcomingMatches.size} upcoming matches for date: $date")
                    }
                    .onFailure { exception ->
                        Log.e("HomeViewModel", "Failed to load upcoming matches for date: $date", exception)
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception loading upcoming matches for date: $date", e)
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    fun loadLiveAndFinishedMatches(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Load live matches
                val liveMatchesResult = repository.getLiveMatches()
                // Load today's finished matches
                val todayMatchesResult = repository.getMatchesByDate(date)

                liveMatchesResult.onSuccess { liveMatches ->
                    todayMatchesResult.onSuccess { todayMatches ->
                        val finishedMatches = todayMatches.filter { it.isFinished }
                        val combinedMatches = mutableListOf<Match>()
                        combinedMatches.addAll(liveMatches.filter { it.isLive })
                        combinedMatches.addAll(finishedMatches)

                        val sortedMatches = sortMatchesByStatus(combinedMatches)
                        _todayMatches.value = sortedMatches
                        _liveMatches.value = liveMatches.filter { it.isLive }
                        Log.d("HomeViewModel", "Loaded ${liveMatches.size} live and ${finishedMatches.size} finished matches")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception loading live and finished matches", e)
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    fun loadFavoriteTeamMatches() {
        if (favoriteTeamIds.isEmpty()) {
            _favoriteMatches.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val allFavoriteMatches = mutableListOf<Match>()
                val today = Calendar.getInstance()
                val endDate = Calendar.getInstance()
                endDate.add(Calendar.DAY_OF_MONTH, 7) // Next 7 days

                // Load matches for each favorite team for the next week
                for (teamId in favoriteTeamIds) {
                    val currentDate = today.clone() as Calendar
                    while (currentDate.before(endDate) || currentDate.equals(endDate)) {
                        val dateStr = dateFormat.format(currentDate.time)
                        repository.getMatchesByDate(dateStr)
                            .onSuccess { matches ->
                                val teamMatches = matches.filter { match ->
                                    match.homeTeam.id == teamId || match.awayTeam.id == teamId
                                }
                                allFavoriteMatches.addAll(teamMatches)
                            }
                        currentDate.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                val sortedMatches = sortMatchesByStatus(allFavoriteMatches.distinctBy { it.id })
                _favoriteMatches.value = sortedMatches
                Log.d("HomeViewModel", "Loaded ${sortedMatches.size} favorite team matches")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception loading favorite team matches", e)
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    fun loadLiveMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getLiveMatches()
                    .onSuccess { matches ->
                        val actuallyLive = matches.filter { it.isLive }
                        _liveMatches.value = actuallyLive
                        Log.d("HomeViewModel", "Loaded ${actuallyLive.size} live matches")
                    }
                    .onFailure { exception ->
                        Log.e("HomeViewModel", "Failed to load live matches", exception)
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception loading live matches", e)
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
                        Log.d("HomeViewModel", "Loaded ${matches.size} matches for date: $date")
                        matches.forEach { match ->
                            Log.d("HomeViewModel", "Match: ${match.homeTeam.name} vs ${match.awayTeam.name} - League: ${match.league.name} (ID: ${match.league.id}) - Status: ${match.matchStatus}")
                        }

                        val sortedMatches = sortMatchesByStatus(matches)
                        _todayMatches.value = sortedMatches
                    }
                    .onFailure { exception ->
                        Log.e("HomeViewModel", "Failed to load matches for date: $date", exception)
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception loading matches for date: $date", e)
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    private fun sortMatchesByStatus(matches: List<Match>): List<Match> {
        return matches.sortedWith(compareBy<Match> { match ->
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
        Log.d("HomeViewModel", "Selected date: $date")
        _selectedDate.value = date
        loadMatchesByDate(date)

        val today = dateFormat.format(Date())
        if (date == today) {
            loadLiveMatches()
        }
    }

    fun refreshData() {
        _selectedDate.value?.let { selectedDate ->
            Log.d("HomeViewModel", "Refreshing data for date: $selectedDate")
            loadMatchesByDate(selectedDate)

            val today = dateFormat.format(Date())
            if (selectedDate == today) {
                loadLiveMatches()
            }
        }

        // Also refresh favorite matches
        loadFavoriteTeamMatches()
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

    // Additional utility methods for better functionality
    fun getFavoriteTeamIds(): Set<Long> {
        return favoriteTeamIds.toSet()
    }

    fun getFavoriteTeamsCount(): Int {
        return favoriteTeamIds.size
    }

    fun hasAnyFavoriteTeams(): Boolean {
        return favoriteTeamIds.isNotEmpty()
    }

    // Method to get matches filtered by current tab selection
    fun getMatchesForTab(tabType: String, date: String) {
        when (tabType.lowercase()) {
            "upcoming" -> loadUpcomingMatches(date)
            "score", "live" -> loadLiveAndFinishedMatches(date)
            "favorites" -> loadFavoriteTeamMatches()
            else -> loadMatchesByDate(date)
        }
    }
}