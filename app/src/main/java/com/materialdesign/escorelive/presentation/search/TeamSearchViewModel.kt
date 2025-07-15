package com.materialdesign.escorelive.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.domain.model.Team
import com.materialdesign.escorelive.domain.model.Match
import com.materialdesign.escorelive.data.remote.TeamStanding
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import com.materialdesign.escorelive.data.remote.repository.FootballRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@HiltViewModel
class TeamSearchViewModel @Inject constructor(
    private val repository: FootballRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<TeamSearchResult>>()
    val searchResults: LiveData<List<TeamSearchResult>> = _searchResults

    private val _suggestions = MutableLiveData<List<String>>()
    val suggestions: LiveData<List<String>> = _suggestions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedTeamStandings = MutableLiveData<List<TeamStanding>>()
    val selectedTeamStandings: LiveData<List<TeamStanding>> = _selectedTeamStandings

    private val _favoriteMatches = MutableLiveData<List<Match>>()
    val favoriteMatches: LiveData<List<Match>> = _favoriteMatches

    private val favoriteTeamIds = mutableSetOf<Long>()
    private var currentSearchQuery = ""
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null

    // Debugging için arama geçmişi
    private val searchHistory = mutableListOf<String>()

    init {
        loadFavoriteTeamIds()
        Log.d("TeamSearchViewModel", "ViewModel initialized")
    }

    private fun loadFavoriteTeamIds() {
        try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            val favoriteIds = prefs.getStringSet("favorite_team_ids", emptySet()) ?: emptySet()
            favoriteTeamIds.clear()
            favoriteTeamIds.addAll(favoriteIds.map { it.toLong() })
            Log.d("TeamSearchViewModel", "Loaded ${favoriteTeamIds.size} favorite teams")
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error loading favorite teams", e)
        }
    }

    // BASIT ARAMA FONKSİYONU
    fun searchTeams(query: String) {
        Log.d("TeamSearchViewModel", "searchTeams called with: '$query'")

        if (query.length < 2) {
            Log.d("TeamSearchViewModel", "Query too short, clearing results")
            _searchResults.value = emptyList()
            _suggestions.value = emptyList()
            currentSearchQuery = ""
            return
        }

        currentSearchQuery = query

        // Önceki işi iptal et
        searchJob?.cancel()

        searchJob = viewModelScope.launch(Dispatchers.Main) {
            try {
                Log.d("TeamSearchViewModel", "Starting search job for: '$query'")
                _isLoading.value = true
                _error.value = null

                // Debounce
                delay(500)

                if (!isActive) {
                    Log.d("TeamSearchViewModel", "Job cancelled during delay")
                    return@launch
                }

                Log.d("TeamSearchViewModel", "Calling repository search...")

                val result = repository.searchTeamsAdvanced(query)

                if (!isActive) {
                    Log.d("TeamSearchViewModel", "Job cancelled after repository call")
                    return@launch
                }

                result.fold(
                    onSuccess = { results ->
                        Log.d("TeamSearchViewModel", "Search successful: ${results.size} results")
                        _searchResults.value = results

                        results.take(3).forEach { result ->
                            Log.d("TeamSearchViewModel", "Result: ${result.team.name} (${result.leagueName})")
                        }
                    },
                    onFailure = { exception ->
                        Log.e("TeamSearchViewModel", "Search failed for '$query'", exception)
                        _error.value = "Search failed: ${exception.message}"
                        _searchResults.value = emptyList()
                    }
                )

            } catch (e: Exception) {
                if (isActive) {
                    Log.e("TeamSearchViewModel", "Exception in searchTeams", e)
                    _error.value = "Search error: ${e.message}"
                    _searchResults.value = emptyList()
                } else {
                    Log.d("TeamSearchViewModel", "Job was cancelled, ignoring exception")
                }
            } finally {
                if (isActive) {
                    _isLoading.value = false
                }
            }
        }
    }

    // BASIT ÖNERİ FONKSİYONU
    fun getSuggestions(query: String) {
        Log.d("TeamSearchViewModel", "getSuggestions called with: '$query'")

        if (query.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }

        // Önceki işi iptal et
        suggestionJob?.cancel()

        suggestionJob = viewModelScope.launch(Dispatchers.Main) {
            try {
                Log.d("TeamSearchViewModel", "Starting suggestion job for: '$query'")

                // Kısa debounce
                delay(200)

                if (!isActive) {
                    Log.d("TeamSearchViewModel", "Suggestion job cancelled during delay")
                    return@launch
                }

                val result = repository.getTeamSuggestions(query)

                if (!isActive) {
                    Log.d("TeamSearchViewModel", "Suggestion job cancelled after repository call")
                    return@launch
                }

                result.fold(
                    onSuccess = { suggestions ->
                        Log.d("TeamSearchViewModel", "Got ${suggestions.size} suggestions: $suggestions")
                        _suggestions.value = suggestions
                    },
                    onFailure = { exception ->
                        Log.w("TeamSearchViewModel", "Failed to get suggestions for '$query'", exception)
                        _suggestions.value = emptyList()
                    }
                )

            } catch (e: Exception) {
                if (isActive) {
                    Log.w("TeamSearchViewModel", "Exception in getSuggestions", e)
                    _suggestions.value = emptyList()
                } else {
                    Log.d("TeamSearchViewModel", "Suggestion job was cancelled, ignoring exception")
                }
            }
        }
    }

    // HIZLI ARAMA - Suggestion'a tıklandığında
    fun searchTeamByExactName(teamName: String) {
        Log.d("TeamSearchViewModel", "searchTeamByExactName called with: '$teamName'")

        viewModelScope.launch(Dispatchers.Main) {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d("TeamSearchViewModel", "Searching exact name: '$teamName'")

                val result = repository.searchTeamsAdvanced(teamName)

                result.fold(
                    onSuccess = { results ->
                        // Tam eşleşme öncelikli
                        val exactMatch = results.find {
                            it.team.name.equals(teamName, ignoreCase = true)
                        }

                        if (exactMatch != null) {
                            Log.d("TeamSearchViewModel", "Found exact match for '$teamName'")
                            _searchResults.value = listOf(exactMatch)
                        } else if (results.isNotEmpty()) {
                            Log.d("TeamSearchViewModel", "Found similar match for '$teamName'")
                            _searchResults.value = results.take(1)
                        } else {
                            Log.d("TeamSearchViewModel", "No matches found for '$teamName'")
                            _searchResults.value = emptyList()
                        }

                        currentSearchQuery = teamName
                    },
                    onFailure = { exception ->
                        Log.e("TeamSearchViewModel", "Exact search failed for '$teamName'", exception)
                        _error.value = "Team not found: ${exception.message}"
                        _searchResults.value = emptyList()
                    }
                )

            } catch (e: Exception) {
                Log.e("TeamSearchViewModel", "Exception in searchTeamByExactName", e)
                _error.value = "Search error: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTeamStandings(teamSearchResult: TeamSearchResult) {
        Log.d("TeamSearchViewModel", "loadTeamStandings called for: ${teamSearchResult.team.name}")

        viewModelScope.launch(Dispatchers.Main) {
            try {
                _isLoading.value = true
                _error.value = null

                val result = repository.getStandings(teamSearchResult.leagueId, teamSearchResult.season)

                result.fold(
                    onSuccess = { standings ->
                        Log.d("TeamSearchViewModel", "Loaded standings: ${standings.size} teams")
                        _selectedTeamStandings.value = standings
                    },
                    onFailure = { exception ->
                        // Try previous season if current season fails
                        Log.w("TeamSearchViewModel", "Current season failed, trying previous season")

                        val prevResult = repository.getStandings(teamSearchResult.leagueId, teamSearchResult.season - 1)
                        prevResult.fold(
                            onSuccess = { standings ->
                                Log.d("TeamSearchViewModel", "Loaded standings from previous season: ${standings.size} teams")
                                _selectedTeamStandings.value = standings
                            },
                            onFailure = {
                                Log.e("TeamSearchViewModel", "Failed to load standings", exception)
                                _error.value = "Failed to load standings for ${teamSearchResult.team.name}"
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("TeamSearchViewModel", "Exception in loadTeamStandings", e)
                _error.value = "Failed to load standings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
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
                Log.d("TeamSearchViewModel", "Loading matches for ${favoriteTeamIds.size} favorite teams")

                repository.getFavoriteTeamsMatches(favoriteTeamIds)
                    .onSuccess { matches ->
                        val sortedMatches = sortMatchesByStatus(matches)
                        _favoriteMatches.value = sortedMatches
                        Log.d("TeamSearchViewModel", "Loaded ${sortedMatches.size} favorite team matches")
                    }
                    .onFailure { exception ->
                        Log.e("TeamSearchViewModel", "Failed to load favorite team matches", exception)
                        _error.value = "Failed to load favorite team matches: ${exception.message}"
                        _favoriteMatches.value = emptyList()
                    }
            } catch (e: Exception) {
                Log.e("TeamSearchViewModel", "Exception loading favorite team matches", e)
                _error.value = "Failed to load matches: ${e.message}"
                _favoriteMatches.value = emptyList()
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
        })
    }

    fun getTeamPositionInStandings(teamId: Long): TeamStanding? {
        return _selectedTeamStandings.value?.find { it.team.id == teamId }
    }

    fun addToFavorites(teamId: Long) {
        try {
            favoriteTeamIds.add(teamId)
            saveFavoriteTeamIds()
            Log.d("TeamSearchViewModel", "Added team $teamId to favorites")
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error adding to favorites", e)
        }
    }

    fun removeFromFavorites(teamId: Long) {
        try {
            favoriteTeamIds.remove(teamId)
            saveFavoriteTeamIds()
            Log.d("TeamSearchViewModel", "Removed team $teamId from favorites")
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error removing from favorites", e)
        }
    }

    fun isTeamFavorite(teamId: Long): Boolean {
        return favoriteTeamIds.contains(teamId)
    }

    private fun saveFavoriteTeamIds() {
        try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            prefs.edit()
                .putStringSet("favorite_team_ids", favoriteTeamIds.map { it.toString() }.toSet())
                .apply()
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error saving favorite teams", e)
        }
    }

    fun clearSearch() {
        Log.d("TeamSearchViewModel", "clearSearch called")

        currentSearchQuery = ""
        _searchResults.value = emptyList()
        _selectedTeamStandings.value = emptyList()
        _suggestions.value = emptyList()

        // Cancel ongoing jobs
        searchJob?.cancel()
        suggestionJob?.cancel()
        searchJob = null
        suggestionJob = null

        // Clear repository cache
        repository.clearSearchCache()
    }

    fun hasSearchQuery(): Boolean {
        return currentSearchQuery.isNotEmpty()
    }

    fun clearError() {
        _error.value = null
    }

    fun getFavoriteTeamsCount(): Int {
        return favoriteTeamIds.size
    }

    // POPÜLER TAKIMLAR - Başlangıçta gösterilecek
    fun getPopularTeams(): List<String> {
        return listOf(
            "Arsenal", "Chelsea", "Manchester United", "Manchester City",
            "Liverpool", "Tottenham", "Barcelona", "Real Madrid",
            "Bayern Munich", "Juventus", "Paris Saint-Germain",
            "Galatasaray", "Fenerbahce", "Besiktas"
        )
    }

    // TEST FONKSİYONU - Debug için
    fun testSearch() {
        Log.d("TeamSearchViewModel", "testSearch called")
        searchTeams("Arsenal")
    }

    fun addToSearchHistory(query: String) {
        try {
            if (query.isNotEmpty() && !searchHistory.contains(query)) {
                searchHistory.add(0, query)
                if (searchHistory.size > 10) {
                    searchHistory.removeAt(10)
                }
                Log.d("TeamSearchViewModel", "Added '$query' to search history")
            }
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error adding to search history", e)
        }
    }

    fun getSearchHistory(): List<String> {
        return searchHistory.toList()
    }

    fun getFavoriteTeamIds(): Set<Long> {
        return favoriteTeamIds.toSet()
    }

    fun hasAnyFavoriteTeams(): Boolean {
        return favoriteTeamIds.isNotEmpty()
    }

    override fun onCleared() {
        Log.d("TeamSearchViewModel", "onCleared called")
        super.onCleared()
        searchJob?.cancel()
        suggestionJob?.cancel()
    }
}

// Data class for search results with league information
data class TeamSearchResult(
    val team: Team,
    val leagueId: Int,
    val leagueName: String,
    val season: Int
)