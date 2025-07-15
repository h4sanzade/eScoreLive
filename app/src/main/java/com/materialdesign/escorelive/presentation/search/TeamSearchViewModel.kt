package com.materialdesign.escorelive.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.domain.model.Team
import com.materialdesign.escorelive.data.remote.TeamStanding
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import com.materialdesign.escorelive.data.remote.repository.FootballRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.util.Calendar

@HiltViewModel
class TeamSearchViewModel @Inject constructor(
    private val repository: FootballRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<TeamSearchResult>>()
    val searchResults: LiveData<List<TeamSearchResult>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedTeamStandings = MutableLiveData<List<TeamStanding>>()
    val selectedTeamStandings: LiveData<List<TeamStanding>> = _selectedTeamStandings

    private val favoriteTeamIds = mutableSetOf<Long>()
    private var currentSearchQuery = ""

    init {
        loadFavoriteTeamIds()
    }


    private fun loadFavoriteTeamIds() {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favoriteIds = prefs.getStringSet("favorite_team_ids", emptySet()) ?: emptySet()
        favoriteTeamIds.clear()
        favoriteTeamIds.addAll(favoriteIds.map { it.toLong() })
    }


    fun searchTeams(query: String) {
        if (query.length < 2) return

        currentSearchQuery = query

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                delay(300) // Debounce search

                val searchResults = mutableSetOf<TeamSearchResult>()

                // Search through popular leagues to find teams
                val popularLeagues = listOf(
                    39 to "Premier League",      // England
                    140 to "La Liga",           // Spain
                    78 to "Bundesliga",         // Germany
                    135 to "Serie A",           // Italy
                    61 to "Ligue 1",            // France
                    2 to "Champions League",     // UEFA
                    3 to "Europa League",        // UEFA
                    203 to "Süper Lig",         // Turkey
                    342 to "Premier League"      // Azerbaijan
                )

                for ((leagueId, leagueName) in popularLeagues) {
                    try {
                        val currentSeason = Calendar.getInstance().get(Calendar.YEAR)
                        repository.getMatchesByLeague(leagueId, currentSeason)
                            .onSuccess { matches ->
                                matches.forEach { match ->
                                    // Check if home team matches search query
                                    if (match.homeTeam.name.contains(query, ignoreCase = true)) {
                                        searchResults.add(
                                            TeamSearchResult(
                                                team = match.homeTeam,
                                                leagueId = leagueId,
                                                leagueName = leagueName,
                                                season = currentSeason
                                            )
                                        )
                                    }
                                    // Check if away team matches search query
                                    if (match.awayTeam.name.contains(query, ignoreCase = true)) {
                                        searchResults.add(
                                            TeamSearchResult(
                                                team = match.awayTeam,
                                                leagueId = leagueId,
                                                leagueName = leagueName,
                                                season = currentSeason
                                            )
                                        )
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        Log.e("TeamSearchViewModel", "Error searching league $leagueId", e)
                    }
                }

                // Convert to list and sort by name
                val teamsList = searchResults.toList()
                    .distinctBy { it.team.id } // Remove duplicates by team ID
                    .sortedBy { it.team.name }

                _searchResults.value = teamsList

                Log.d("TeamSearchViewModel", "Found ${teamsList.size} teams matching '$query'")

            } catch (e: Exception) {
                Log.e("TeamSearchViewModel", "Exception in searchTeams", e)
                _error.value = "Failed to search teams: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    fun loadTeamStandings(teamSearchResult: TeamSearchResult) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getStandings(teamSearchResult.leagueId, teamSearchResult.season)
                    .onSuccess { standings ->
                        _selectedTeamStandings.value = standings
                        Log.d("TeamSearchViewModel", "Loaded standings for ${teamSearchResult.team.name} in ${teamSearchResult.leagueName}")
                    }
                    .onFailure { exception ->
                        // Try previous season if current season fails
                        repository.getStandings(teamSearchResult.leagueId, teamSearchResult.season - 1)
                            .onSuccess { standings ->
                                _selectedTeamStandings.value = standings
                                Log.d("TeamSearchViewModel", "Loaded standings for ${teamSearchResult.team.name} from previous season")
                            }
                            .onFailure {
                                Log.e("TeamSearchViewModel", "Failed to load standings", exception)
                                _error.value = "Failed to load standings for ${teamSearchResult.team.name}"
                            }
                    }
            } catch (e: Exception) {
                Log.e("TeamSearchViewModel", "Exception in loadTeamStandings", e)
                _error.value = "Failed to load standings: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    fun getTeamPositionInStandings(teamId: Long): TeamStanding? {
        return _selectedTeamStandings.value?.find { it.team.id == teamId }
    }

    fun addToFavorites(teamId: Long) {
        favoriteTeamIds.add(teamId)
        saveFavoriteTeamIds()
        Log.d("TeamSearchViewModel", "Added team $teamId to favorites")
    }

    fun removeFromFavorites(teamId: Long) {
        favoriteTeamIds.remove(teamId)
        saveFavoriteTeamIds()
        Log.d("TeamSearchViewModel", "Removed team $teamId from favorites")
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

    fun clearSearch() {
        currentSearchQuery = ""
        _searchResults.value = emptyList()
        _selectedTeamStandings.value = emptyList()
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
}

// Data class for search results with league information
data class TeamSearchResult(
    val team: Team,
    val leagueId: Int,
    val leagueName: String,
    val season: Int
)