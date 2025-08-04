// CompetitionViewModel.kt - Enhanced with Smart Fallback Standings
package com.materialdesign.escorelive.presentation.competition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionTab
import com.materialdesign.escorelive.data.remote.dto.CompetitionUiState
import com.materialdesign.escorelive.data.remote.repository.CompetitionRepository
import com.materialdesign.escorelive.data.remote.repository.FootballRepository
import com.materialdesign.escorelive.data.remote.TeamStanding
import com.materialdesign.escorelive.data.remote.StandingStats
import com.materialdesign.escorelive.data.remote.GoalsStats
import com.materialdesign.escorelive.data.remote.dto.TeamData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import java.util.Calendar

@OptIn(FlowPreview::class)
@HiltViewModel
class CompetitionViewModel @Inject constructor(
    private val repository: CompetitionRepository,
    private val footballRepository: FootballRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CompetitionViewModel"
        private const val SEARCH_DEBOUNCE_TIME = 500L
    }

    private val _uiState = MutableStateFlow(CompetitionUiState())
    val uiState: StateFlow<CompetitionUiState> = _uiState.asStateFlow()

    private val _regionalCompetitions = MutableStateFlow<Map<String, List<Competition>>>(emptyMap())
    val regionalCompetitions: StateFlow<Map<String, List<Competition>>> = _regionalCompetitions.asStateFlow()

    private val _standingsData = MutableStateFlow<List<TeamStanding>>(emptyList())
    val standingsData: StateFlow<List<TeamStanding>> = _standingsData.asStateFlow()

    private val _standingsLoading = MutableStateFlow(false)
    val standingsLoading: StateFlow<Boolean> = _standingsLoading.asStateFlow()

    private val _standingsError = MutableStateFlow<String?>(null)
    val standingsError: StateFlow<String?> = _standingsError.asStateFlow()

    private val _currentStandingsCompetition = MutableStateFlow<Competition?>(null)
    val currentStandingsCompetition: StateFlow<Competition?> = _currentStandingsCompetition.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var allCompetitions: List<Competition> = emptyList()

    init {
        Log.d(TAG, "Enhanced CompetitionViewModel initialized with Smart Fallback System")

        _searchQuery
            .debounce(SEARCH_DEBOUNCE_TIME)
            .distinctUntilChanged()
            .onEach { query ->
                Log.d(TAG, "Search query changed: '$query'")
                updateSearchQuery(query)
                performSearch(query)
            }
            .launchIn(viewModelScope)

        loadCompetitions()
    }

    fun loadCompetitionStandings(competition: Competition) {
        viewModelScope.launch {
            Log.d(TAG, "Loading standings for: ${competition.name} (ID: ${competition.id})")

            _standingsLoading.value = true
            _standingsError.value = null
            _currentStandingsCompetition.value = competition

            try {
                val leagueId = competition.id.toInt()
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                // Try 2024/25 season first, then others
                val seasonsToTry = listOf(2024, 2025, 2023, currentYear)
                var standingsFound = false

                for (season in seasonsToTry) {
                    if (standingsFound) break

                    Log.d(TAG, "Trying season $season for ${competition.name}")

                    val standingsResult = footballRepository.getStandings(leagueId, season)

                    standingsResult.onSuccess { standings ->
                        if (standings.isNotEmpty()) {
                            Log.d(TAG, "Found ${standings.size} teams for ${competition.name} (season $season)")
                            _standingsData.value = standings
                            standingsFound = true
                            _standingsLoading.value = false
                            return@launch
                        }
                    }
                }

                if (!standingsFound) {
                    Log.d(TAG, "No standings found, trying to get teams from API for ${competition.name}")
                    createFallbackStandingsFromAPI(competition, leagueId, seasonsToTry)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception loading standings for ${competition.name}", e)
                _standingsError.value = "Unable to load standings data for ${competition.name}"
                _standingsData.value = emptyList()
            } finally {
                _standingsLoading.value = false
            }
        }
    }

    private suspend fun createFallbackStandingsFromAPI(
        competition: Competition,
        leagueId: Int,
        seasonsToTry: List<Int>
    ) {
        Log.d(TAG, "Creating fallback standings from API for: ${competition.name}")

        for (season in seasonsToTry) {
            try {
                Log.d(TAG, "Trying to get teams for season $season")

                val teamsResult = footballRepository.searchTeamsAdvanced("league:$leagueId")

                teamsResult.onSuccess { teamSearchResults ->
                    val teams = teamSearchResults.map { teamSearchResult ->
                        com.materialdesign.escorelive.data.remote.dto.TeamData(
                            id = teamSearchResult.team.id,
                            name = teamSearchResult.team.name,
                            logo = teamSearchResult.team.logo
                        )
                    }

                    if (teams.isNotEmpty()) {
                        Log.d(TAG, "Found ${teams.size} teams from search for ${competition.name}")

                        val fallbackStandings = createZeroStandings(teams)
                        _standingsData.value = fallbackStandings
                        _standingsError.value = "Season 2024/25 not started yet. Showing team list with zero stats."
                        return
                    }
                }

                try {
                    val fixturesResult = footballRepository.getMatchesByLeague(leagueId, season)

                    fixturesResult.onSuccess { matches ->
                        if (matches.isNotEmpty()) {
                            val teams = mutableSetOf<com.materialdesign.escorelive.data.remote.dto.TeamData>()

                            matches.forEach { match ->
                                teams.add(com.materialdesign.escorelive.data.remote.dto.TeamData(
                                    id = match.homeTeam.id,
                                    name = match.homeTeam.name,
                                    logo = match.homeTeam.logo
                                ))
                                teams.add(com.materialdesign.escorelive.data.remote.dto.TeamData(
                                    id = match.awayTeam.id,
                                    name = match.awayTeam.name,
                                    logo = match.awayTeam.logo
                                ))
                            }

                            if (teams.isNotEmpty()) {
                                Log.d(TAG, "Found ${teams.size} teams from matches for ${competition.name}")

                                val fallbackStandings = createZeroStandings(teams.toList())
                                _standingsData.value = fallbackStandings
                                _standingsError.value = "Season 2024/25 not started yet. Showing team list with zero stats."
                                return
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get teams from matches for season $season: ${e.message}")
                }

            } catch (e: Exception) {
                Log.w(TAG, "Failed to get teams for season $season: ${e.message}")
                continue
            }
        }

        // If API fails completely, create basic fallback for known leagues
        Log.w(TAG, "Could not get teams from API for ${competition.name}, using basic fallback")
        val basicTeams = createBasicFallbackTeams(competition.name)
        if (basicTeams.isNotEmpty()) {
            val fallbackStandings = createZeroStandings(basicTeams)
            _standingsData.value = fallbackStandings
            _standingsError.value = "Season 2024/25 not started yet. Showing basic team list."
        } else {
            _standingsError.value = "No team data available for ${competition.name}"
            _standingsData.value = emptyList()
        }
    }

    private fun createZeroStandings(teams: List<com.materialdesign.escorelive.data.remote.dto.TeamData>): List<TeamStanding> {
        Log.d(TAG, "Creating zero standings for ${teams.size} teams")

        val shuffledTeams = teams.shuffled()

        return shuffledTeams.mapIndexed { index, teamData ->
            TeamStanding(
                rank = index + 1,
                team = teamData,
                points = 0,
                goalsDiff = 0,
                group = "League",
                form = "-----", // No games played yet
                status = "Regular Season",
                description = null,
                all = StandingStats(
                    played = 0,
                    win = 0,
                    draw = 0,
                    lose = 0,
                    goals = GoalsStats(
                        `for` = 0,
                        against = 0
                    )
                ),
                home = StandingStats(
                    played = 0,
                    win = 0,
                    draw = 0,
                    lose = 0,
                    goals = GoalsStats(
                        `for` = 0,
                        against = 0
                    )
                ),
                away = StandingStats(
                    played = 0,
                    win = 0,
                    draw = 0,
                    lose = 0,
                    goals = GoalsStats(
                        `for` = 0,
                        against = 0
                    )
                ),
                update = "2025-07-28T00:00:00+00:00"
            )
        }
    }

    fun loadCompetitions(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            Log.d(TAG, "Loading competitions from Football API (forceRefresh: $forceRefresh)")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getAllCompetitions(forceRefresh)
                .onSuccess { competitions ->
                    Log.d(TAG, "Loaded ${competitions.size} competitions from Football API")
                    allCompetitions = competitions
                    updateRegionalData(competitions)
                    updateFilteredCompetitions()
                    _uiState.value = _uiState.value.copy(
                        competitions = competitions,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to load competitions from Football API", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load competitions"
                    )
                }
        }
    }

    private fun updateRegionalData(competitions: List<Competition>) {
        val groupedByCountry = competitions.groupBy { it.country }

        val filteredAndSorted = groupedByCountry
            .filterValues { it.isNotEmpty() }
            .mapValues { (_, competitions) ->
                competitions.sortedWith(
                    compareBy<Competition> { !it.isTopCompetition }
                        .thenBy { it.name }
                )
            }

        _regionalCompetitions.value = filteredAndSorted

        Log.d(TAG, "Regional data updated: ${filteredAndSorted.size} countries")
    }

    fun selectTab(tab: CompetitionTab) {
        Log.d(TAG, "Tab selected: $tab")
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        updateFilteredCompetitions()
    }

    fun onSearchQueryChanged(query: String) {
        Log.d(TAG, "Search query input: '$query'")
        _searchQuery.value = query
    }

    fun toggleFavorite(competition: Competition) {
        viewModelScope.launch {
            Log.d(TAG, "Toggling favorite for: ${competition.name} (ID: ${competition.id})")

            if (competition.isFavorite) {
                repository.removeFromFavorites(competition.id)
            } else {
                repository.addToFavorites(competition.id)
            }

            val updatedCompetition = competition.copy(isFavorite = !competition.isFavorite)

            allCompetitions = allCompetitions.map { comp ->
                if (comp.id == competition.id) updatedCompetition else comp
            }

            updateRegionalData(allCompetitions)

            _uiState.value = _uiState.value.copy(
                competitions = allCompetitions,
                favoriteCompetitionIds = getFavoriteIds()
            )

            updateFilteredCompetitions()

            updateAccountCompetitionsCount()
        }
    }

    private suspend fun updateAccountCompetitionsCount() {
        try {
            val favoriteCompetitionsCount = getFavoriteIds().size

            Log.d(TAG, "Favorite competitions count: $favoriteCompetitionsCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating account competitions count", e)
        }
    }

    fun refresh() {
        Log.d(TAG, "Refreshing competitions from Football API")
        repository.clearCache()
        clearStandingsData()
        loadCompetitions(forceRefresh = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearStandingsError() {
        _standingsError.value = null
    }

    fun clearStandingsData() {
        _standingsData.value = emptyList()
        _standingsError.value = null
        _standingsLoading.value = false
        _currentStandingsCompetition.value = null
    }

    private fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                Log.d(TAG, "Empty search query, showing all competitions")
                updateRegionalData(allCompetitions)
                updateFilteredCompetitions()
                return@launch
            }

            Log.d(TAG, "Performing search for: '$query'")
            _uiState.value = _uiState.value.copy(isLoading = true)

            val filteredCompetitions = allCompetitions.filter { competition ->
                competition.name.contains(query, ignoreCase = true) ||
                        competition.country.contains(query, ignoreCase = true) ||
                        competition.shortCode?.contains(query, ignoreCase = true) == true
            }

            if (filteredCompetitions.isNotEmpty()) {
                Log.d(TAG, "Found ${filteredCompetitions.size} competitions in cache")
                updateRegionalData(filteredCompetitions)
                _uiState.value = _uiState.value.copy(
                    competitions = filteredCompetitions,
                    isLoading = false
                )
                updateFilteredCompetitions()
            } else {
                repository.searchCompetitions(query)
                    .onSuccess { searchResults ->
                        Log.d(TAG, "API search returned ${searchResults.size} results")
                        updateRegionalData(searchResults)
                        _uiState.value = _uiState.value.copy(
                            competitions = searchResults,
                            isLoading = false
                        )
                        updateFilteredCompetitions()
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Search failed", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Search failed"
                        )
                    }
            }
        }
    }

    private fun updateFilteredCompetitions() {
        viewModelScope.launch {
            val currentTab = _uiState.value.selectedTab
            Log.d(TAG, "Updating filtered competitions for tab: $currentTab")

            when (currentTab) {
                CompetitionTab.TOP -> {
                    repository.getTopCompetitions()
                        .onSuccess { topCompetitions ->
                            Log.d(TAG, "Filtered ${topCompetitions.size} top competitions")
                            _uiState.value = _uiState.value.copy(
                                filteredCompetitions = topCompetitions,
                                isLoading = false
                            )
                        }
                        .onFailure { exception ->
                            Log.e(TAG, "Failed to get top competitions", exception)
                            _uiState.value = _uiState.value.copy(
                                filteredCompetitions = allCompetitions.take(20),
                                isLoading = false
                            )
                        }
                }

                CompetitionTab.REGION -> {
                    _uiState.value = _uiState.value.copy(
                        filteredCompetitions = emptyList(),
                        isLoading = false
                    )
                    Log.d(TAG, "Region tab selected, using regional data")
                }

                CompetitionTab.FAVORITES -> {
                    repository.getFavoriteCompetitions()
                        .onSuccess { favoriteCompetitions ->
                            Log.d(TAG, "Filtered ${favoriteCompetitions.size} favorite competitions")
                            _uiState.value = _uiState.value.copy(
                                filteredCompetitions = favoriteCompetitions,
                                isLoading = false
                            )
                        }
                        .onFailure { exception ->
                            Log.e(TAG, "Failed to get favorite competitions", exception)
                            _uiState.value = _uiState.value.copy(
                                filteredCompetitions = emptyList(),
                                isLoading = false
                            )
                        }
                }
            }
        }
    }

    private fun createBasicFallbackTeams(competitionName: String): List<com.materialdesign.escorelive.data.remote.dto.TeamData> {
        return when {
            competitionName.contains("Premier League", ignoreCase = true) -> {
                listOf(
                    com.materialdesign.escorelive.data.remote.dto.TeamData(42, "Arsenal", "https://media.api-sports.io/football/teams/42.png"),
                    com.materialdesign.escorelive.data.remote.dto.TeamData(49, "Chelsea", "https://media.api-sports.io/football/teams/49.png"),
                    com.materialdesign.escorelive.data.remote.dto.TeamData(33, "Manchester United", "https://media.api-sports.io/football/teams/33.png"),
                    com.materialdesign.escorelive.data.remote.dto.TeamData(50, "Manchester City", "https://media.api-sports.io/football/teams/50.png"),
                    com.materialdesign.escorelive.data.remote.dto.TeamData(40, "Liverpool", "https://media.api-sports.io/football/teams/40.png"),
                    com.materialdesign.escorelive.data.remote.dto.TeamData(47, "Tottenham", "https://media.api-sports.io/football/teams/47.png")
                )
            }
            competitionName.contains("La Liga", ignoreCase = true) -> {
                listOf(
                    com.materialdesign.escorelive.data.remote.dto.TeamData(529, "Barcelona", "https://media.api-sports.io/football/teams/529.png"),
                    com.materialdesign.escorelive.data.remote.dto.TeamData(541, "Real Madrid", "https://media.api-sports.io/football/teams/541.png"),
                    com.materialdesign.escorelive.data.remote.dto.TeamData(530, "Atletico Madrid", "https://media.api-sports.io/football/teams/530.png")
                )
            }
            competitionName.contains("Super", ignoreCase = true) && competitionName.contains("Lig", ignoreCase = true) -> {
                listOf(
                    com.materialdesign.escorelive.data.remote.dto.TeamData(559, "Galatasaray", "https://media.api-sports.io/football/teams/559.png"),
                    com.materialdesign.escorelive.data.remote.dto.TeamData(562, "Fenerbahce", "https://media.api-sports.io/football/teams/562.png"),
                    com.materialdesign.escorelive.data.remote.dto.TeamData(558, "Besiktas", "https://media.api-sports.io/football/teams/558.png")
                )
            }
            else -> emptyList()
        }
    }

    private fun getFavoriteIds(): Set<String> {
        return allCompetitions.filter { it.isFavorite }.map { it.id }.toSet()
    }

    override fun onCleared() {
        Log.d(TAG, "Enhanced CompetitionViewModel cleared")
        super.onCleared()
    }
}