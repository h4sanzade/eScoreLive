// CompetitionViewModel.kt - Final version with Standings Support
package com.materialdesign.escorelive.presentation.competition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionTab
import com.materialdesign.escorelive.data.remote.dto.CompetitionUiState
import com.materialdesign.escorelive.data.remote.repository.CompetitionRepository
import com.materialdesign.escorelive.data.remote.repository.FootballRepository
import com.materialdesign.escorelive.data.remote.TeamStanding
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

    // Separate flow for regional competitions (grouped by country)
    private val _regionalCompetitions = MutableStateFlow<Map<String, List<Competition>>>(emptyMap())
    val regionalCompetitions: StateFlow<Map<String, List<Competition>>> = _regionalCompetitions.asStateFlow()

    // Standings data
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
        Log.d(TAG, "CompetitionViewModel initialized with Football API and Standings Support")

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

    fun loadCompetitionStandings(competition: Competition) {
        viewModelScope.launch {
            Log.d(TAG, "Loading standings for: ${competition.name} (ID: ${competition.id})")

            _standingsLoading.value = true
            _standingsError.value = null
            _currentStandingsCompetition.value = competition

            try {
                val leagueId = competition.id.toInt()
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                // Try current season first, then previous season
                var season = currentYear
                var standingsResult: Result<List<TeamStanding>>? = null

                for (i in 0..1) { // Try current and previous season
                    standingsResult = footballRepository.getStandings(leagueId, season - i)

                    standingsResult.onSuccess { standings ->
                        if (standings.isNotEmpty()) {
                            Log.d(TAG, "Loaded ${standings.size} teams for ${competition.name} (season ${season - i})")
                            _standingsData.value = standings
                            _standingsLoading.value = false
                            return@launch
                        }
                    }
                }

                // If no data found in both seasons
                standingsResult?.onFailure { exception ->
                    Log.e(TAG, "Failed to load standings for ${competition.name}", exception)
                    _standingsError.value = "No standings available for ${competition.name}"
                    _standingsData.value = emptyList()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception loading standings for ${competition.name}", e)
                _standingsError.value = "Failed to load standings: ${e.message}"
                _standingsData.value = emptyList()
            } finally {
                _standingsLoading.value = false
            }
        }
    }

    fun clearStandingsData() {
        _standingsData.value = emptyList()
        _standingsError.value = null
        _standingsLoading.value = false
        _currentStandingsCompetition.value = null
    }

    private fun updateRegionalData(competitions: List<Competition>) {
        val groupedByCountry = competitions.groupBy { it.country }

        // Filter out countries with no competitions and sort
        val filteredAndSorted = groupedByCountry
            .filterValues { it.isNotEmpty() }
            .mapValues { (_, competitions) ->
                // Sort competitions within each country: top competitions first, then alphabetically
                competitions.sortedWith(
                    compareBy<Competition> { !it.isTopCompetition }
                        .thenBy { it.name }
                )
            }

        _regionalCompetitions.value = filteredAndSorted

        Log.d(TAG, "Regional data updated: ${filteredAndSorted.size} countries")

        // Log some examples
        filteredAndSorted.forEach { (country, competitions) ->
            Log.d(TAG, "$country: ${competitions.size} competitions")
            competitions.take(2).forEach { competition ->
                Log.d(TAG, "  - ${competition.name} (${if (competition.isTopCompetition) "TOP" else "Regular"})")
            }
        }
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

            // Update the competition in our lists
            val updatedCompetition = competition.copy(isFavorite = !competition.isFavorite)

            allCompetitions = allCompetitions.map { comp ->
                if (comp.id == competition.id) updatedCompetition else comp
            }

            // Update regional data as well
            updateRegionalData(allCompetitions)

            _uiState.value = _uiState.value.copy(
                competitions = allCompetitions,
                favoriteCompetitionIds = getFavoriteIds()
            )

            updateFilteredCompetitions()
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

            // Search in cached data first for better performance
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
                // Fallback to API search if no local results
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

    fun searchByCountry(country: String) {
        viewModelScope.launch {
            Log.d(TAG, "Searching competitions by country: $country")
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getCompetitionsByCountry(country)
                .onSuccess { competitions ->
                    Log.d(TAG, "Found ${competitions.size} competitions for country: $country")
                    allCompetitions = competitions
                    updateRegionalData(competitions)
                    _uiState.value = _uiState.value.copy(
                        competitions = competitions,
                        isLoading = false
                    )
                    updateFilteredCompetitions()
                }
                .onFailure { exception ->
                    Log.e(TAG, "Country search failed for: $country", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No competitions found for $country"
                    )
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
                    // For region tab, we don't need to update filteredCompetitions
                    // The RegionAdapter uses regionalCompetitions StateFlow directly
                    _uiState.value = _uiState.value.copy(
                        filteredCompetitions = emptyList(), // Not used for region tab
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

    private fun getFavoriteIds(): Set<String> {
        return allCompetitions.filter { it.isFavorite }.map { it.id }.toSet()
    }

    fun getCompetitionById(id: String): Competition? {
        return allCompetitions.find { it.id == id }
    }

    fun isDataEmpty(): Boolean {
        return when (_uiState.value.selectedTab) {
            CompetitionTab.REGION -> _regionalCompetitions.value.isEmpty()
            else -> _uiState.value.filteredCompetitions.isEmpty()
        } && !_uiState.value.isLoading && _uiState.value.error == null
    }

    fun getPopularCountries(): List<String> {
        return listOf(
            "England", "Spain", "Germany", "Italy", "France",
            "Netherlands", "Portugal", "Turkey", "Brazil", "Argentina",
            "Belgium", "Scotland", "Austria", "Switzerland", "Greece",
            "Russia", "Ukraine", "Poland", "Czech Republic", "Denmark",
            "Sweden", "Norway", "Azerbaijan", "Kazakhstan"
        )
    }

    fun getCurrentSeasonCompetitions(): List<Competition> {
        return allCompetitions.filter { it.currentSeason }
    }

    fun getAllCountries(): List<String> {
        return allCompetitions.map { it.country }.distinct().sorted()
    }

    fun getCompetitionsByType(type: com.materialdesign.escorelive.data.remote.dto.CompetitionType): List<Competition> {
        return allCompetitions.filter { it.type == type }
    }

    fun getFavoriteCompetitionsCount(): Int {
        return allCompetitions.count { it.isFavorite }
    }

    fun getRegionalDataSize(): Int {
        return _regionalCompetitions.value.values.sumOf { it.size }
    }

    fun getCountriesWithCompetitions(): List<Pair<String, Int>> {
        return _regionalCompetitions.value.map { (country, competitions) ->
            country to competitions.size
        }.sortedWith(
            compareBy<Pair<String, Int>> { (country, _) ->
                val popularCountries = getPopularCountries()
                if (popularCountries.contains(country)) {
                    popularCountries.indexOf(country)
                } else {
                    Int.MAX_VALUE
                }
            }.thenBy { (country, _) -> country }
        )
    }

    override fun onCleared() {
        Log.d(TAG, "CompetitionViewModel cleared")
        super.onCleared()
    }
}