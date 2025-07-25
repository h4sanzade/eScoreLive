// FilterLeaguesViewModel.kt
package com.materialdesign.escorelive.presentation.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.local.AccountDataStore
import com.materialdesign.escorelive.data.remote.repository.LeaguesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilterLeaguesViewModel @Inject constructor(
    private val leaguesRepository: LeaguesRepository,
    private val accountDataStore: AccountDataStore
) : ViewModel() {

    private val _leagues = MutableLiveData<List<League>>()
    val leagues: LiveData<List<League>> = _leagues

    private val _selectedLeagues = MutableLiveData<Set<String>>()
    val selectedLeagues: LiveData<Set<String>> = _selectedLeagues

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveCompleted = MutableLiveData<Boolean>()
    val saveCompleted: LiveData<Boolean> = _saveCompleted

    private var allLeagues: List<League> = emptyList()
    private var filteredLeagues: List<League> = emptyList()

    init {
        loadSelectedLeagues()
    }

    fun loadLeagues() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load leagues from API
                leaguesRepository.getAllLeagues()
                    .onSuccess { leaguesList ->
                        allLeagues = leaguesList
                        filteredLeagues = leaguesList
                        _leagues.value = leaguesList
                    }
                    .onFailure { exception ->
                        _error.value = "Failed to load leagues: ${exception.message}"
                        // Load mock data as fallback
                        loadMockLeagues()
                    }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                loadMockLeagues()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadMockLeagues() {
        // Mock data for development/testing
        allLeagues = listOf(
            League("1", "Premier League", "England", "https://media.api-sports.io/football/leagues/39.png"),
            League("2", "La Liga", "Spain", "https://media.api-sports.io/football/leagues/140.png"),
            League("3", "Bundesliga", "Germany", "https://media.api-sports.io/football/leagues/78.png"),
            League("4", "Serie A", "Italy", "https://media.api-sports.io/football/leagues/135.png"),
            League("5", "Ligue 1", "France", "https://media.api-sports.io/football/leagues/61.png"),
            League("6", "Champions League", "UEFA", "https://media.api-sports.io/football/leagues/2.png"),
            League("7", "Europa League", "UEFA", "https://media.api-sports.io/football/leagues/3.png"),
            League("8", "Premier League", "Azerbaijan", "https://media.api-sports.io/football/leagues/342.png"),
            League("9", "Super Lig", "Turkey", "https://media.api-sports.io/football/leagues/203.png"),
            League("10", "Eredivisie", "Netherlands", "https://media.api-sports.io/football/leagues/88.png")
        )
        filteredLeagues = allLeagues
        _leagues.value = allLeagues
    }

    private fun loadSelectedLeagues() {
        viewModelScope.launch {
            try {
                val savedLeagues = accountDataStore.getSelectedLeagues()
                _selectedLeagues.value = savedLeagues.toSet()
            } catch (e: Exception) {
                _selectedLeagues.value = emptySet()
            }
        }
    }

    fun searchLeagues(query: String) {
        if (query.isBlank()) {
            filteredLeagues = allLeagues
        } else {
            filteredLeagues = allLeagues.filter { league ->
                league.name.contains(query, ignoreCase = true) ||
                        league.country.contains(query, ignoreCase = true)
            }
        }
        _leagues.value = filteredLeagues
    }

    fun toggleLeagueSelection(league: League) {
        val currentSelected = _selectedLeagues.value?.toMutableSet() ?: mutableSetOf()

        if (currentSelected.contains(league.id)) {
            currentSelected.remove(league.id)
        } else {
            currentSelected.add(league.id)
        }

        _selectedLeagues.value = currentSelected
    }

    fun selectAllLeagues() {
        val allLeagueIds = filteredLeagues.map { it.id }.toSet()
        _selectedLeagues.value = allLeagueIds
    }

    fun clearAllSelections() {
        _selectedLeagues.value = emptySet()
    }

    fun saveSelectedLeagues() {
        viewModelScope.launch {
            try {
                val selectedIds = _selectedLeagues.value ?: emptySet()
                val selectedLeagueNames = allLeagues
                    .filter { selectedIds.contains(it.id) }
                    .map { it.name }

                accountDataStore.saveSelectedLeagues(selectedLeagueNames)
                _saveCompleted.value = true
            } catch (e: Exception) {
                _error.value = "Failed to save league filters: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

// Data class for League
data class League(
    val id: String,
    val name: String,
    val country: String,
    val flagUrl: String,
    val isSelected: Boolean = false
)