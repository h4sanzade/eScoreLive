// CompetitionViewModel.kt
package com.materialdesign.escorelive.presentation.competition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionTab
import com.materialdesign.escorelive.data.remote.dto.CompetitionUiState
import com.materialdesign.escorelive.data.remote.repository.CompetitionRepository
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

@OptIn(FlowPreview::class)
@HiltViewModel
class CompetitionViewModel @Inject constructor(
    private val repository: CompetitionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CompetitionViewModel"
        private const val SEARCH_DEBOUNCE_TIME = 500L
    }

    private val _uiState = MutableStateFlow(CompetitionUiState())
    val uiState: StateFlow<CompetitionUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var allCompetitions: List<Competition> = emptyList()

    init {
        Log.d(TAG, "CompetitionViewModel initialized")

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
            Log.d(TAG, "Loading competitions (forceRefresh: $forceRefresh)")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getAllCompetitions(forceRefresh)
                .onSuccess { competitions ->
                    Log.d(TAG, "Loaded ${competitions.size} competitions")
                    allCompetitions = competitions
                    updateFilteredCompetitions()
                    _uiState.value = _uiState.value.copy(
                        competitions = competitions,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to load competitions", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load competitions"
                    )
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
            Log.d(TAG, "Toggling favorite for: ${competition.name}")

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

            _uiState.value = _uiState.value.copy(
                competitions = allCompetitions,
                favoriteCompetitionIds = getFavoriteIds()
            )

            updateFilteredCompetitions()
        }
    }


    fun refresh() {
        Log.d(TAG, "Refreshing competitions")
        repository.clearCache()
        loadCompetitions(forceRefresh = true)
    }


    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }


    private fun performSearch(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                Log.d(TAG, "Empty search query, showing all competitions")
                updateFilteredCompetitions()
                return@launch
            }

            Log.d(TAG, "Performing search for: '$query'")
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.searchCompetitions(query)
                .onSuccess { searchResults ->
                    Log.d(TAG, "Search returned ${searchResults.size} results")

                    // Update the main competitions list with search results
                    allCompetitions = searchResults
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


    private fun updateFilteredCompetitions() {
        viewModelScope.launch {
            val currentTab = _uiState.value.selectedTab
            Log.d(TAG, "Updating filtered competitions for tab: $currentTab")

            _uiState.value = _uiState.value.copy(isLoading = true)

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
                    repository.getCompetitionsByRegion()
                        .onSuccess { competitionsByRegion ->
                            // Flatten the map to a list, grouped by region
                            val regionalCompetitions = competitionsByRegion.values.flatten()
                            Log.d(TAG, "Filtered ${regionalCompetitions.size} competitions by region")
                            _uiState.value = _uiState.value.copy(
                                filteredCompetitions = regionalCompetitions,
                                isLoading = false
                            )
                        }
                        .onFailure { exception ->
                            Log.e(TAG, "Failed to get competitions by region", exception)
                            _uiState.value = _uiState.value.copy(
                                filteredCompetitions = allCompetitions,
                                isLoading = false
                            )
                        }
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
        return _uiState.value.filteredCompetitions.isEmpty() &&
                !_uiState.value.isLoading &&
                _uiState.value.error == null
    }

    override fun onCleared() {
        Log.d(TAG, "CompetitionViewModel cleared")
        super.onCleared()
    }
}