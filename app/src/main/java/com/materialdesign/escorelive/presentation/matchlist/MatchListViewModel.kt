package com.materialdesign.escorelive.presentation.ui.matchlist

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
class MatchListViewModel @Inject constructor(
    private val repository: FootballRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _matches = MutableLiveData<List<Match>>()
    val matches: LiveData<List<Match>> = _matches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedFilter = MutableLiveData<MatchFilter>()
    val selectedFilter: LiveData<MatchFilter> = _selectedFilter

    private var allMatchesList: List<Match> = emptyList()
    private var currentDisplayType: DisplayType = DisplayType.TODAY
    private var isFavoritesMode = false
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val favoriteTeamIds = mutableSetOf<Long>()

    init {
        _selectedFilter.value = MatchFilter.ALL
        loadFavoriteTeamIds()
    }

    private fun loadFavoriteTeamIds() {
        try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            val favoriteIds = prefs.getStringSet("favorite_team_ids", emptySet()) ?: emptySet()
            favoriteTeamIds.clear()
            favoriteTeamIds.addAll(favoriteIds.map { it.toLong() })
            Log.d("MatchListViewModel", "Loaded ${favoriteTeamIds.size} favorite team IDs")
        } catch (e: Exception) {
            Log.e("MatchListViewModel", "Error loading favorite team IDs", e)
        }
    }

    fun loadMatchesForDate(date: String, displayType: DisplayType) {
        currentDisplayType = displayType
        isFavoritesMode = false

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                when (displayType) {
                    DisplayType.PAST -> {
                        val matches = repository.getMatchesByDate(date).getOrNull() ?: emptyList()
                        allMatchesList = matches.filter { it.isFinished }
                        Log.d("MatchListViewModel", "Loaded ${allMatchesList.size} finished matches for past date: $date")
                    }
                    DisplayType.TODAY -> {
                        val liveMatches = repository.getLiveMatches().getOrNull() ?: emptyList()
                        val todayMatches = repository.getMatchesByDate(date).getOrNull() ?: emptyList()

                        val combinedSet = mutableSetOf<Match>()
                        combinedSet.addAll(liveMatches)
                        combinedSet.addAll(todayMatches)

                        allMatchesList = combinedSet.toList()
                        Log.d("MatchListViewModel", "Loaded ${allMatchesList.size} matches for today")
                    }
                    DisplayType.FUTURE -> {
                        val matches = repository.getMatchesByDate(date).getOrNull() ?: emptyList()
                        allMatchesList = matches.filter { it.isUpcoming }
                        Log.d("MatchListViewModel", "Loaded ${allMatchesList.size} upcoming matches for future date: $date")
                    }
                }

                allMatchesList = sortMatchesByPriority(allMatchesList)
                applyCurrentFilter()

            } catch (e: Exception) {
                Log.e("MatchListViewModel", "Failed to load matches", e)
                _error.value = "Failed to load matches: ${e.message}"
                allMatchesList = emptyList()
                _matches.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFavoriteTeamMatches() {
        isFavoritesMode = true

        if (favoriteTeamIds.isEmpty()) {
            Log.d("MatchListViewModel", "No favorite teams found")
            allMatchesList = emptyList()
            _matches.value = emptyList()
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("MatchListViewModel", "Loading matches for ${favoriteTeamIds.size} favorite teams")

                repository.getFavoriteTeamsMatches(favoriteTeamIds)
                    .onSuccess { matches ->
                        allMatchesList = sortMatchesByPriority(matches)
                        applyCurrentFilter()
                        Log.d("MatchListViewModel", "Loaded ${allMatchesList.size} favorite team matches")
                    }
                    .onFailure { exception ->
                        Log.e("MatchListViewModel", "Failed to load favorite team matches", exception)
                        _error.value = "Failed to load favorite team matches: ${exception.message}"
                        allMatchesList = emptyList()
                        _matches.value = emptyList()
                    }

            } catch (e: Exception) {
                Log.e("MatchListViewModel", "Exception loading favorite team matches", e)
                _error.value = "Failed to load matches: ${e.message}"
                allMatchesList = emptyList()
                _matches.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshMatches() {
        if (isFavoritesMode) {
            loadFavoriteTeamMatches()
        } else {
            applyCurrentFilter()
        }
    }

    fun setFilter(filter: MatchFilter) {
        _selectedFilter.value = filter
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val filter = _selectedFilter.value ?: MatchFilter.ALL

        val filteredMatches = if (isFavoritesMode) {
            // For favorites mode, apply filters to all favorite team matches
            when (filter) {
                MatchFilter.ALL -> allMatchesList
                MatchFilter.LIVE -> allMatchesList.filter { it.isLive }
                MatchFilter.FINISHED -> allMatchesList.filter { it.isFinished }
                MatchFilter.UPCOMING -> allMatchesList.filter { it.isUpcoming }
            }
        } else {
            // For date-based mode
            when (currentDisplayType) {
                DisplayType.PAST -> {
                    allMatchesList // All are finished already
                }
                DisplayType.TODAY -> {
                    when (filter) {
                        MatchFilter.ALL -> allMatchesList
                        MatchFilter.LIVE -> allMatchesList.filter { it.isLive }
                        MatchFilter.FINISHED -> allMatchesList.filter { it.isFinished }
                        MatchFilter.UPCOMING -> allMatchesList.filter { it.isUpcoming }
                    }
                }
                DisplayType.FUTURE -> {
                    allMatchesList // All are upcoming already
                }
            }
        }

        Log.d("MatchListViewModel", "Applied filter: $filter, showing ${filteredMatches.size} matches (favorites mode: $isFavoritesMode)")
        _matches.value = filteredMatches
    }

    private fun sortMatchesByPriority(matches: List<Match>): List<Match> {
        return matches.sortedWith(compareBy<Match> { match ->
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

    // Utility methods
    fun getCurrentDisplayType(): DisplayType {
        return currentDisplayType
    }

    fun isFavoritesModeActive(): Boolean {
        return isFavoritesMode
    }

    fun getAllMatchesCount(): Int {
        return allMatchesList.size
    }

    fun getFilteredMatchesCount(): Int {
        return _matches.value?.size ?: 0
    }

    fun hasAnyMatches(): Boolean {
        return allMatchesList.isNotEmpty()
    }

    fun getFavoriteTeamsCount(): Int {
        return favoriteTeamIds.size
    }

    fun hasFavoriteTeams(): Boolean {
        return favoriteTeamIds.isNotEmpty()
    }

    // For debugging
    fun getMatchesByStatus(): Map<String, Int> {
        return mapOf(
            "live" to allMatchesList.count { it.isLive },
            "finished" to allMatchesList.count { it.isFinished },
            "upcoming" to allMatchesList.count { it.isUpcoming }
        )
    }

    override fun onCleared() {
        Log.d("MatchListViewModel", "MatchListViewModel onCleared")
        super.onCleared()
    }
}