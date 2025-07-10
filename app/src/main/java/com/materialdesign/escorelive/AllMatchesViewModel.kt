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
import android.util.Log

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

    // Expanded leagues list - including all European competitions
    private val popularLeagues = setOf(
        // Major European Leagues
        39,  // Premier League
        140, // La Liga
        78,  // Bundesliga
        135, // Serie A
        61,  // Ligue 1
        94,  // Primeira Liga
        88,  // Eredivisie

        // European Competitions
        2,   // Champions League
        3,   // Europa League
        848, // Europa League Play-offs
        4,   // Euro Championship
        5,   // Nations League

        // Other Popular Leagues
        203, // Turkish Super League
        204, // Turkish 1. Lig
        342, // Azerbaijan Premier League
        235, // Russian Premier League

        // World Competitions
        1,   // World Cup
        17,  // Africa Cup of Nations
        9,   // Copa America

        // Additional European Leagues
        179, // Scottish Premiership
        144, // Belgian Pro League
        103, // Eliteserien (Norway)
        113, // Allsvenskan (Sweden)
        119, // Superligaen (Denmark)

        // Conference League
        848, // Conference League Play-offs

        // Additional Competitions
        81,  // DFB Pokal
        137, // Coppa Italia
        48,  // FA Cup
        143  // Copa del Rey
    )

    init {
        _selectedFilter.value = MatchFilter.ALL
    }

    fun loadAllMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val allMatchesSet = mutableSetOf<LiveMatch>()

                // 1. Load live matches first
                try {
                    val liveMatches = repository.getLiveMatches().getOrNull() ?: emptyList()
                    allMatchesSet.addAll(liveMatches)
                    Log.d("AllMatchesViewModel", "Loaded ${liveMatches.size} live matches")
                } catch (e: Exception) {
                    Log.e("AllMatchesViewModel", "Failed to load live matches", e)
                }

                // 2. Load today's matches
                val today = dateFormat.format(Date())
                try {
                    val todayMatches = repository.getMatchesByDate(today).getOrNull() ?: emptyList()
                    allMatchesSet.addAll(todayMatches)
                    Log.d("AllMatchesViewModel", "Loaded ${todayMatches.size} matches for today")

                    // Debug: Log today's matches
                    todayMatches.forEach { match ->
                        Log.d("AllMatchesViewModel", "Today: ${match.homeTeam.name} vs ${match.awayTeam.name} - League: ${match.league.name} (ID: ${match.league.id})")
                    }
                } catch (e: Exception) {
                    Log.e("AllMatchesViewModel", "Failed to load today's matches", e)
                }

                // 3. Load upcoming matches (next 7 days)
                for (i in 1..7) {
                    try {
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_MONTH, i)
                        val futureDate = dateFormat.format(calendar.time)

                        val dayMatches = repository.getMatchesByDate(futureDate).getOrNull() ?: emptyList()
                        allMatchesSet.addAll(dayMatches)

                        if (dayMatches.isNotEmpty()) {
                            Log.d("AllMatchesViewModel", "Loaded ${dayMatches.size} matches for $futureDate")
                        }

                        delay(50) // Small delay to avoid API rate limits
                    } catch (e: Exception) {
                        Log.e("AllMatchesViewModel", "Failed to load matches for day $i", e)
                    }
                }

                // 4. Load matches from popular leagues (current season)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                for (leagueId in popularLeagues.take(15)) { // Increased to 15 leagues
                    try {
                        val matches = repository.getMatchesByLeague(leagueId, currentYear).getOrNull() ?: emptyList()
                        allMatchesSet.addAll(matches)

                        if (matches.isNotEmpty()) {
                            Log.d("AllMatchesViewModel", "Loaded ${matches.size} matches for league ID: $leagueId")
                        }

                        delay(100) // Delay to avoid hitting API rate limits
                    } catch (e: Exception) {
                        Log.e("AllMatchesViewModel", "Failed to load matches for league $leagueId", e)
                    }
                }

                // 5. Convert set to list and sort
                allMatchesList = allMatchesSet.toList()
                allMatchesList = sortMatchesByPriority(allMatchesList)

                Log.d("AllMatchesViewModel", "Total unique matches loaded: ${allMatchesList.size}")

                applyCurrentFilter()

            } catch (e: Exception) {
                Log.e("AllMatchesViewModel", "Failed to load matches", e)
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
        val today = dateFormat.format(Date())

        val filteredMatches = when (filter) {
            MatchFilter.ALL -> allMatchesList
            MatchFilter.LIVE -> allMatchesList.filter { it.isLive }
            MatchFilter.TODAY -> {
                allMatchesList.filter { match ->
                    match.kickoffTime?.startsWith(today) == true
                }
            }
            MatchFilter.FINISHED -> allMatchesList.filter { it.isFinished }
            MatchFilter.UPCOMING -> allMatchesList.filter { it.isUpcoming }
        }

        Log.d("AllMatchesViewModel", "Applied filter: $filter, showing ${filteredMatches.size} matches")
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
            // Sort by league importance
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

    fun clearError() {
        _error.value = null
    }
}

enum class MatchFilter {
    ALL, LIVE, TODAY, FINISHED, UPCOMING
}