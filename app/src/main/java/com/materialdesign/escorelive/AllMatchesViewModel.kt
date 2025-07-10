package com.materialdesign.escorelive.ui.allmatchs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.repository.FootballRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class AllMatchesViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _allMatches = MutableLiveData<List<LiveMatch>>()
    val allMatches: LiveData<List<LiveMatch>> = _allMatches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedFilter = MutableLiveData<MatchFilter>()
    val selectedFilter: LiveData<MatchFilter> = _selectedFilter

    private var allMatchesList: List<LiveMatch> = emptyList()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Popular leagues to filter
    private val popularLeagues = setOf(
        // Premier League
        39,
        // La Liga
        140,
        // Bundesliga
        78,
        // Serie A
        135,
        // Ligue 1
        61,
        // Champions League
        2,
        // Europa League
        3,
        // Turkish Super League
        203,
        // Turkish 1. Lig
        204,
        // Azerbaijan Premier League
        342,
        // World Cup
        1,
        // European Championship
        4,
        // Nations League
        5
    )

    init {
        _selectedFilter.value = MatchFilter.ALL
    }

    fun loadAllMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Load live matches
                val liveMatches = repository.getLiveMatches().getOrNull() ?: emptyList()

                // Load today's matches
                val today = dateFormat.format(Date())
                val todayMatches = repository.getMatchesByDate(today).getOrNull() ?: emptyList()

                // Load matches for the next few days from popular leagues
                val upcomingMatches = mutableListOf<LiveMatch>()
                for (i in 1..3) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_MONTH, i)
                    val futureDate = dateFormat.format(calendar.time)

                    val dayMatches = repository.getMatchesByDate(futureDate).getOrNull() ?: emptyList()
                    upcomingMatches.addAll(dayMatches)
                }

                // Load matches from popular leagues
                val leagueMatches = mutableListOf<LiveMatch>()
                for (leagueId in popularLeagues.take(5)) { // Limit API calls
                    try {
                        val matches = repository.getMatchesByLeague(leagueId, 2024).getOrNull() ?: emptyList()
                        leagueMatches.addAll(matches)
                        delay(100) // Small delay to avoid hitting API rate limits
                    } catch (e: Exception) {
                        // Continue if one league fails
                    }
                }

                // Combine all matches and remove duplicates
                val combinedMatches = (liveMatches + todayMatches + upcomingMatches + leagueMatches)
                    .distinctBy { it.id }
                    .filter { match ->
                        // Filter only popular leagues
                        popularLeagues.contains(match.league.id.toInt())
                    }

                allMatchesList = sortMatchesByPriority(combinedMatches)
                applyCurrentFilter()

            } catch (e: Exception) {
                _error.value = "Failed to load matches: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshMatches() {
        loadAllMatches()
    }

    fun setFilter(filter: MatchFilter) {
        _selectedFilter.value = filter
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val filter = _selectedFilter.value ?: MatchFilter.ALL
        val filteredMatches = when (filter) {
            MatchFilter.ALL -> allMatchesList
            MatchFilter.LIVE -> allMatchesList.filter { it.isLive }
            MatchFilter.TODAY -> {
                val today = dateFormat.format(Date())
                allMatchesList.filter { match ->
                    match.kickoffTime?.startsWith(today) == true
                }
            }
            MatchFilter.FINISHED -> allMatchesList.filter { it.isFinished }
            MatchFilter.UPCOMING -> allMatchesList.filter { it.isUpcoming }
        }

        _allMatches.value = filteredMatches
    }

    private fun sortMatchesByPriority(matches: List<LiveMatch>): List<LiveMatch> {
        return matches.sortedWith(compareBy<LiveMatch> { match ->
            when {
                match.isLive -> 0 // Live matches first
                match.isUpcoming -> 1 // Upcoming matches second
                match.isFinished -> 2 // Finished matches last
                else -> 3
            }
        }.thenBy { match ->
            // Sort by kickoff time within each category
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                match.kickoffTime?.let { inputFormat.parse(it)?.time } ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }.thenBy { match ->
            // Sort by league popularity (Champions League, Premier League, etc.)
            when (match.league.id.toInt()) {
                2 -> 0 // Champions League
                39 -> 1 // Premier League
                140 -> 2 // La Liga
                78 -> 3 // Bundesliga
                135 -> 4 // Serie A
                61 -> 5 // Ligue 1
                203 -> 6 // Turkish Super League
                else -> 7
            }
        })
    }

    fun clearError() {
        _error.value = null
    }
}

enum class MatchFilter {
    ALL, LIVE, TODAY, FINISHED, UPCOMING
}