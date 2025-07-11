package com.materialdesign.escorelive.ui.allmatchs

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
class AllMatchesViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _matches = MutableLiveData<List<LiveMatch>>()
    val matches: LiveData<List<LiveMatch>> = _matches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedFilter = MutableLiveData<MatchFilter>()
    val selectedFilter: LiveData<MatchFilter> = _selectedFilter

    private var allMatchesList: List<LiveMatch> = emptyList()
    private var currentDisplayType: DisplayType = DisplayType.TODAY
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        _selectedFilter.value = MatchFilter.ALL
    }

    fun loadMatchesForDate(date: String, displayType: DisplayType) {
        currentDisplayType = displayType

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                when (displayType) {
                    DisplayType.PAST -> {
                        // For past dates, only load matches from that specific date
                        val matches = repository.getMatchesByDate(date).getOrNull() ?: emptyList()
                        allMatchesList = matches.filter { it.isFinished }
                        Log.d("AllMatchesViewModel", "Loaded ${allMatchesList.size} finished matches for past date: $date")
                    }
                    DisplayType.TODAY -> {
                        // For today, load live matches and today's matches
                        val liveMatches = repository.getLiveMatches().getOrNull() ?: emptyList()
                        val todayMatches = repository.getMatchesByDate(date).getOrNull() ?: emptyList()

                        val combinedSet = mutableSetOf<LiveMatch>()
                        combinedSet.addAll(liveMatches)
                        combinedSet.addAll(todayMatches)

                        allMatchesList = combinedSet.toList()
                        Log.d("AllMatchesViewModel", "Loaded ${allMatchesList.size} matches for today")
                    }
                    DisplayType.FUTURE -> {
                        // For future dates, only load matches from that specific date
                        val matches = repository.getMatchesByDate(date).getOrNull() ?: emptyList()
                        allMatchesList = matches.filter { it.isUpcoming }
                        Log.d("AllMatchesViewModel", "Loaded ${allMatchesList.size} upcoming matches for future date: $date")
                    }
                }

                allMatchesList = sortMatchesByPriority(allMatchesList)
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
        // Reload with current settings
        _matches.value?.let {
            applyCurrentFilter()
        }
    }

    fun setFilter(filter: MatchFilter) {
        _selectedFilter.value = filter
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val filter = _selectedFilter.value ?: MatchFilter.ALL

        val filteredMatches = when (currentDisplayType) {
            DisplayType.PAST -> {
                // For past dates, no filtering needed - all are finished
                allMatchesList
            }
            DisplayType.TODAY -> {
                // For today, apply filters
                when (filter) {
                    MatchFilter.ALL -> allMatchesList
                    MatchFilter.LIVE -> allMatchesList.filter { it.isLive }
                    MatchFilter.FINISHED -> allMatchesList.filter { it.isFinished }
                    MatchFilter.UPCOMING -> allMatchesList.filter { it.isUpcoming }
                }
            }
            DisplayType.FUTURE -> {
                // For future dates, no filtering needed - all are upcoming
                allMatchesList
            }
        }

        Log.d("AllMatchesViewModel", "Applied filter: $filter, showing ${filteredMatches.size} matches")
        _matches.value = filteredMatches
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
    ALL, LIVE, FINISHED, UPCOMING
}