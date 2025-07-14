package com.materialdesign.escorelive.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.data.model.FavoriteTeam
import com.materialdesign.escorelive.repository.FootballRepository
import com.materialdesign.escorelive.repository.FavoritesRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _matches = MutableLiveData<List<LiveMatch>>()
    val matches: LiveData<List<LiveMatch>> = _matches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private val _selectedContentFilter = MutableLiveData<ContentFilter>()
    val selectedContentFilter: LiveData<ContentFilter> = _selectedContentFilter

    private val _favoriteTeams = MutableLiveData<List<FavoriteTeam>>()
    val favoriteTeams: LiveData<List<FavoriteTeam>> = _favoriteTeams

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        val today = dateFormat.format(Date())
        _selectedDate.value = today
        _selectedContentFilter.value = ContentFilter.SCORE
        loadFavoriteTeams()
        loadMatchesForCurrentFilter()
    }

    fun loadMatchesForCurrentFilter() {
        val filter = _selectedContentFilter.value ?: ContentFilter.SCORE
        val selectedDate = _selectedDate.value ?: dateFormat.format(Date())

        when (filter) {
            ContentFilter.UPCOMING -> loadUpcomingMatches(selectedDate)
            ContentFilter.SCORE -> loadFinishedMatches(selectedDate)
            ContentFilter.FAVORITES -> loadFavoriteMatches()
        }
    }

    fun setContentFilter(filter: ContentFilter) {
        _selectedContentFilter.value = filter
        loadMatchesForCurrentFilter()
    }

    fun selectDate(date: String) {
        Log.d("MainMenuViewModel", "Selected date: $date")
        _selectedDate.value = date
        loadMatchesForCurrentFilter()
    }

    private fun loadUpcomingMatches(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val today = dateFormat.format(Date())
                val targetDate = if (date <= today) {
                    // If selected date is today or past, show upcoming matches from today onwards
                    today
                } else {
                    date
                }

                repository.getMatchesByDate(targetDate)
                    .onSuccess { matches ->
                        val upcomingMatches = matches.filter { it.isUpcoming }
                        val sortedMatches = sortMatchesByTime(upcomingMatches)
                        _matches.value = sortedMatches
                        Log.d("MainMenuViewModel", "Loaded ${sortedMatches.size} upcoming matches")
                    }
                    .onFailure { exception ->
                        Log.e("MainMenuViewModel", "Failed to load upcoming matches", exception)
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                Log.e("MainMenuViewModel", "Exception loading upcoming matches", e)
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    private fun loadFinishedMatches(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val today = dateFormat.format(Date())
                val targetDate = if (date > today) {
                    // If selected date is in future, show today's finished matches
                    today
                } else {
                    date
                }

                repository.getMatchesByDate(targetDate)
                    .onSuccess { matches ->
                        val finishedMatches = matches.filter { it.isFinished }
                        val importantMatches = filterImportantMatches(finishedMatches)
                        val sortedMatches = sortMatchesByImportance(importantMatches)
                        _matches.value = sortedMatches
                        Log.d("MainMenuViewModel", "Loaded ${sortedMatches.size} finished matches")
                    }
                    .onFailure { exception ->
                        Log.e("MainMenuViewModel", "Failed to load finished matches", exception)
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                Log.e("MainMenuViewModel", "Exception loading finished matches", e)
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    private fun loadFavoriteMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val favorites = favoritesRepository.getFavoriteTeams()

                if (favorites.isEmpty()) {
                    _matches.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                val favoriteTeamIds = favorites.map { it.id }
                val today = dateFormat.format(Date())

                // Load matches for today and upcoming days
                val allMatches = mutableListOf<LiveMatch>()

                // Load today's matches
                repository.getMatchesByDate(today)
                    .onSuccess { todayMatches ->
                        val favoriteMatches = todayMatches.filter { match ->
                            favoriteTeamIds.contains(match.homeTeam.id) ||
                                    favoriteTeamIds.contains(match.awayTeam.id)
                        }
                        allMatches.addAll(favoriteMatches)
                    }

                // Load next 7 days for upcoming matches
                val calendar = Calendar.getInstance()
                for (i in 1..7) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    val futureDate = dateFormat.format(calendar.time)

                    repository.getMatchesByDate(futureDate)
                        .onSuccess { futureMatches ->
                            val favoriteMatches = futureMatches.filter { match ->
                                (favoriteTeamIds.contains(match.homeTeam.id) ||
                                        favoriteTeamIds.contains(match.awayTeam.id)) &&
                                        (match.isUpcoming || match.isLive)
                            }
                            allMatches.addAll(favoriteMatches)
                        }
                }

                val sortedMatches = sortMatchesByStatus(allMatches)
                _matches.value = sortedMatches
                Log.d("MainMenuViewModel", "Loaded ${sortedMatches.size} favorite team matches")

            } catch (e: Exception) {
                Log.e("MainMenuViewModel", "Exception loading favorite matches", e)
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    private fun filterImportantMatches(matches: List<LiveMatch>): List<LiveMatch> {
        val importantLeagueIds = setOf(
            2L,   // Champions League
            3L,   // Europa League
            39L,  // Premier League
            140L, // La Liga
            78L,  // Bundesliga
            135L, // Serie A
            61L,  // Ligue 1
            342L, // Azerbaijan Premier League
            203L, // Turkish Super League
            848L  // Europa League Play-offs
        )

        return matches.filter { match ->
            importantLeagueIds.contains(match.league.id)
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

    private fun sortMatchesByTime(matches: List<LiveMatch>): List<LiveMatch> {
        return matches.sortedBy { match ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                match.kickoffTime?.let { inputFormat.parse(it)?.time } ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }
    }

    private fun sortMatchesByImportance(matches: List<LiveMatch>): List<LiveMatch> {
        return matches.sortedWith(compareBy<LiveMatch> { match ->
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
        }.thenBy { match ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                match.kickoffTime?.let { inputFormat.parse(it)?.time } ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        })
    }

    private fun loadFavoriteTeams() {
        viewModelScope.launch {
            try {
                val favorites = favoritesRepository.getFavoriteTeams()
                _favoriteTeams.value = favorites
            } catch (e: Exception) {
                Log.e("MainMenuViewModel", "Failed to load favorite teams", e)
            }
        }
    }

    fun refreshData() {
        loadFavoriteTeams()
        loadMatchesForCurrentFilter()
    }

    fun clearError() {
        _error.value = null
    }

    fun isSelectedDateToday(): Boolean {
        val today = dateFormat.format(Date())
        return _selectedDate.value == today
    }
}

enum class ContentFilter {
    UPCOMING, SCORE, FAVORITES
}