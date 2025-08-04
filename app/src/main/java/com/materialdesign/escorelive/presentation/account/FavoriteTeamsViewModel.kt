package com.materialdesign.escorelive.presentation.account

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.remote.repository.FootballRepository
import com.materialdesign.escorelive.domain.model.Team
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

    companion object {
        private const val TAG = "FavoriteTeamsViewModel"
    }

    fun loadFavoriteTeams() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Loading favorite teams...")
                loadFavoriteTeamIds()
                Log.d(TAG, "Loaded favorite team IDs: $favoriteTeamIds")

                if (favoriteTeamIds.isEmpty()) {
                    Log.d(TAG, "No favorite teams found")
                    _favoriteTeams.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                val favoriteTeamsList = mutableListOf<TeamSearchResult>()

                for (teamId in favoriteTeamIds) {
                    val team = createMockTeam(teamId) ?: continue
                    val teamSearchResult = TeamSearchResult(
                        team = team,
                        leagueId = getLeagueIdForTeam(teamId),
                        leagueName = getLeagueNameForTeam(teamId),
                        season = 2024
                    )
                    favoriteTeamsList.add(teamSearchResult)
                    Log.d(TAG, "Added favorite team: ${team.name}")
                }

                if (favoriteTeamsList.isEmpty() && favoriteTeamIds.isNotEmpty()) {
                    Log.d(TAG, "No cached teams found, searching via API...")

                    val popularTeams = listOf("Arsenal", "Chelsea", "Manchester United", "Barcelona", "Real Madrid")
                    for (teamName in popularTeams.take(favoriteTeamIds.size)) {
                        try {
                            val searchResult = footballRepository.searchTeamsAdvanced(teamName)
                            searchResult.onSuccess { results ->
                                results.firstOrNull()?.let { result ->
                                    favoriteTeamsList.add(result)
                                    Log.d(TAG, "Found team via search: ${result.team.name}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to search for team: $teamName", e)
                        }
                    }
                }

                val sortedTeams = favoriteTeamsList.sortedBy { it.team.name }
                _favoriteTeams.value = sortedTeams
                Log.d(TAG, "Final favorite teams count: ${sortedTeams.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading favorite teams", e)
                _error.value = "Error loading favorite teams: ${e.message}"
                _favoriteTeams.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun createMockTeam(teamId: Long): Team? {
        return when (teamId.toInt()) {
            42 -> Team(42, "Arsenal", "https://media.api-sports.io/football/teams/42.png", "ARS")
            49 -> Team(49, "Chelsea", "https://media.api-sports.io/football/teams/49.png", "CHE")
            33 -> Team(33, "Manchester United", "https://media.api-sports.io/football/teams/33.png", "MUN")
            50 -> Team(50, "Manchester City", "https://media.api-sports.io/football/teams/50.png", "MCI")
            40 -> Team(40, "Liverpool", "https://media.api-sports.io/football/teams/40.png", "LIV")
            47 -> Team(47, "Tottenham", "https://media.api-sports.io/football/teams/47.png", "TOT")
            529 -> Team(529, "Barcelona", "https://media.api-sports.io/football/teams/529.png", "BAR")
            541 -> Team(541, "Real Madrid", "https://media.api-sports.io/football/teams/541.png", "RMA")
            530 -> Team(530, "Atletico Madrid", "https://media.api-sports.io/football/teams/530.png", "ATM")
            157 -> Team(157, "Bayern Munich", "https://media.api-sports.io/football/teams/157.png", "BAY")
            165 -> Team(165, "Borussia Dortmund", "https://media.api-sports.io/football/teams/165.png", "BVB")
            496 -> Team(496, "Juventus", "https://media.api-sports.io/football/teams/496.png", "JUV")
            505 -> Team(505, "Inter Milan", "https://media.api-sports.io/football/teams/505.png", "INT")
            489 -> Team(489, "AC Milan", "https://media.api-sports.io/football/teams/489.png", "MIL")
            85 -> Team(85, "Paris Saint-Germain", "https://media.api-sports.io/football/teams/85.png", "PSG")
            559 -> Team(559, "Galatasaray", "https://media.api-sports.io/football/teams/559.png", "GAL")
            562 -> Team(562, "Fenerbahce", "https://media.api-sports.io/football/teams/562.png", "FEN")
            558 -> Team(558, "Besiktas", "https://media.api-sports.io/football/teams/558.png", "BES")
            564 -> Team(564, "Trabzonspor", "https://media.api-sports.io/football/teams/564.png", "TRA")
            else -> {
                Log.w(TAG, "Unknown team ID: $teamId, creating generic team")
                Team(teamId, "Team $teamId", "", "T${teamId.toString().take(2)}")
            }
        }
    }

    private fun loadFavoriteTeamIds() {
        try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            val favoriteIds = prefs.getStringSet("favorite_team_ids", emptySet()) ?: emptySet()
            favoriteTeamIds.clear()
            favoriteTeamIds.addAll(favoriteIds.mapNotNull {
                try {
                    it.toLong()
                } catch (e: NumberFormatException) {
                    Log.w(TAG, "Invalid team ID format: $it")
                    null
                }
            })
            Log.d(TAG, "Loaded ${favoriteTeamIds.size} favorite team IDs from SharedPreferences")

            if (favoriteTeamIds.isEmpty()) {
                Log.d(TAG, "No favorites found, adding demo favorites")
                favoriteTeamIds.addAll(listOf(42L, 49L, 529L)) // Arsenal, Chelsea, Barcelona
                saveFavoriteTeamIds()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading favorite team IDs", e)
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
                Log.d(TAG, "Removing team $teamId from favorites")
                favoriteTeamIds.remove(teamId)
                saveFavoriteTeamIds()

                val currentList = _favoriteTeams.value?.toMutableList() ?: mutableListOf()
                currentList.removeAll { it.team.id == teamId }
                _favoriteTeams.value = currentList

                updateAccountTeamsCount(favoriteTeamIds.size)
                Log.d(TAG, "Successfully removed team $teamId, remaining: ${favoriteTeamIds.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove team from favorites", e)
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
            Log.d(TAG, "Saved ${favoriteTeamIds.size} favorite team IDs")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorite team IDs", e)
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
            Log.d(TAG, "Updated account teams count to: $count")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating account teams count", e)
        }
    }

    fun isTeamFavorite(teamId: Long): Boolean {
        return favoriteTeamIds.contains(teamId)
    }

    fun clearError() {
        _error.value = null
    }

    fun addDemoFavorites() {
        viewModelScope.launch {
            try {
                favoriteTeamIds.addAll(listOf(42L, 49L, 529L, 541L)) // Arsenal, Chelsea, Barcelona, Real Madrid
                saveFavoriteTeamIds()
                loadFavoriteTeams()
                Log.d(TAG, "Added demo favorites")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding demo favorites", e)
            }
        }
    }
}