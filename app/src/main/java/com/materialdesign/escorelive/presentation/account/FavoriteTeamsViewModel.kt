package com.materialdesign.escorelive.presentation.account

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.remote.repository.FootballRepository
import com.materialdesign.escorelive.presentation.search.TeamSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteTeamsViewModel @Inject constructor(
    private val footballRepository: FootballRepository,
    private val accountDataStore: AccountDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _favoriteTeams = MutableLiveData<List<TeamSearchResult>>()
    val favoriteTeams: LiveData<List<TeamSearchResult>> = _favoriteTeams

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val favoriteTeamIds = mutableSetOf<Long>()

    fun loadFavoriteTeams() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                loadFavoriteTeamIds()

                if (favoriteTeamIds.isEmpty()) {
                    _favoriteTeams.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                val favoriteTeamsList = mutableListOf<TeamSearchResult>()

                for (teamId in favoriteTeamIds) {
                    val cachedTeam = footballRepository.getCachedTeam(teamId)
                    if (cachedTeam != null) {
                        // Create a basic TeamSearchResult for cached team
                        val teamSearchResult = TeamSearchResult(
                            team = cachedTeam,
                            leagueId = getLeagueIdForTeam(teamId),
                            leagueName = getLeagueNameForTeam(teamId),
                            season = 2024
                        )
                        favoriteTeamsList.add(teamSearchResult)
                    }
                }

                // If we don't have cached data for some teams, try to search for them
                if (favoriteTeamsList.size < favoriteTeamIds.size) {
                    for (teamId in favoriteTeamIds) {
                        if (favoriteTeamsList.none { it.team.id == teamId }) {
                            try {
                                val searchResult = footballRepository.searchTeamsAdvanced("id:$teamId")
                                searchResult.onSuccess { results ->
                                    results.firstOrNull { it.team.id == teamId }?.let { result ->
                                        favoriteTeamsList.add(result)
                                    }
                                }
                            } catch (e: Exception) {
                                // Continue with other teams
                            }
                        }
                    }
                }

                _favoriteTeams.value = favoriteTeamsList.sortedBy { it.team.name }

            } catch (e: Exception) {
                _error.value = "Error loading favorite teams: ${e.message}"
                _favoriteTeams.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadFavoriteTeamIds() {
        try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            val favoriteIds = prefs.getStringSet("favorite_team_ids", emptySet()) ?: emptySet()
            favoriteTeamIds.clear()
            favoriteTeamIds.addAll(favoriteIds.map { it.toLong() })
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private fun getLeagueIdForTeam(teamId: Long): Int {
        return when (teamId.toInt()) {
            in listOf(42, 49, 33, 50, 40, 47) -> 39 // Premier League
            in listOf(529, 541, 530) -> 140 // La Liga
            in listOf(157, 165) -> 78 // Bundesliga
            in listOf(496, 505, 489) -> 135 // Serie A
            85 -> 61 // Ligue 1
            in listOf(559, 562, 558, 564) -> 203 // Turkish Super League
            else -> 39 // Default to Premier League
        }
    }

    private fun getLeagueNameForTeam(teamId: Long): String {
        return when (teamId.toInt()) {
            in listOf(42, 49, 33, 50, 40, 47) -> "Premier League"
            in listOf(529, 541, 530) -> "La Liga"
            in listOf(157, 165) -> "Bundesliga"
            in listOf(496, 505, 489) -> "Serie A"
            85 -> "Ligue 1"
            in listOf(559, 562, 558, 564) -> "Super Lig"
            else -> "Premier League"
        }
    }

    fun removeFromFavorites(teamId: Long) {
        viewModelScope.launch {
            try {
                favoriteTeamIds.remove(teamId)
                saveFavoriteTeamIds()

                // Remove from current list immediately
                val currentList = _favoriteTeams.value?.toMutableList() ?: mutableListOf()
                currentList.removeAll { it.team.id == teamId }
                _favoriteTeams.value = currentList

                // Update account count
                updateAccountTeamsCount(favoriteTeamIds.size)

            } catch (e: Exception) {
                _error.value = "Failed to remove from favorites: ${e.message}"
            }
        }
    }

    private fun saveFavoriteTeamIds() {
        try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            prefs.edit()
                .putStringSet("favorite_team_ids", favoriteTeamIds.map { it.toString() }.toSet())
                .apply()
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private suspend fun updateAccountTeamsCount(count: Int) {
        try {
            val currentCounts = accountDataStore.getFavoriteCounts()
            accountDataStore.updateFavoriteCounts(
                competitions = currentCounts.competitions,
                teams = count,
                players = currentCounts.players
            )
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    fun isTeamFavorite(teamId: Long): Boolean {
        return favoriteTeamIds.contains(teamId)
    }

    fun clearError() {
        _error.value = null
    }
}