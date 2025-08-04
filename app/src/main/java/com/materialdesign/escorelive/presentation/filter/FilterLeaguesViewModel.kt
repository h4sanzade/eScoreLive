package com.materialdesign.escorelive.presentation.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.remote.repository.CompetitionRepository
import com.materialdesign.escorelive.presentation.account.AccountDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class FilterLeaguesViewModel @Inject constructor(
    private val competitionRepository: CompetitionRepository,
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

    companion object {
        private const val TAG = "FilterLeaguesViewModel"
    }

    init {
        loadSelectedLeagues()
    }

    fun loadLeagues() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Loading leagues from API...")

                competitionRepository.getAllCompetitions(forceRefresh = true)
                    .onSuccess { competitions ->
                        Log.d(TAG, "Successfully loaded ${competitions.size} competitions from API")

                        allLeagues = competitions.map { competition ->
                            League(
                                id = competition.id,
                                name = competition.name,
                                country = competition.country,
                                logoUrl = competition.logoUrl ?: competition.flagUrl ?: ""
                            )
                        }.distinctBy { "${it.name}_${it.country}" }

                        filteredLeagues = allLeagues
                        _leagues.value = allLeagues

                        Log.d(TAG, "Processed ${allLeagues.size} unique leagues")
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to load competitions from API", exception)
                        _error.value = "Failed to load leagues: ${exception.message}"

                        loadExtendedMockLeagues()
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadLeagues", e)
                _error.value = "Failed to load leagues: ${e.message}"

                loadExtendedMockLeagues()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadExtendedMockLeagues() {
        Log.d(TAG, "Loading extended mock leagues as fallback")

        allLeagues = listOf(
            // England
            League("39", "Premier League", "England", "https://media.api-sports.io/football/leagues/39.png"),
            League("40", "Championship", "England", "https://media.api-sports.io/football/leagues/40.png"),
            League("41", "League One", "England", "https://media.api-sports.io/football/leagues/41.png"),
            League("42", "League Two", "England", "https://media.api-sports.io/football/leagues/42.png"),
            League("45", "FA Cup", "England", "https://media.api-sports.io/football/leagues/45.png"),
            League("48", "EFL Cup", "England", "https://media.api-sports.io/football/leagues/48.png"),

            // Spain
            League("140", "La Liga", "Spain", "https://media.api-sports.io/football/leagues/140.png"),
            League("141", "Segunda División", "Spain", "https://media.api-sports.io/football/leagues/141.png"),
            League("143", "Copa del Rey", "Spain", "https://media.api-sports.io/football/leagues/143.png"),

            // Germany
            League("78", "Bundesliga", "Germany", "https://media.api-sports.io/football/leagues/78.png"),
            League("79", "2. Bundesliga", "Germany", "https://media.api-sports.io/football/leagues/79.png"),
            League("81", "DFB Pokal", "Germany", "https://media.api-sports.io/football/leagues/81.png"),

            // Italy
            League("135", "Serie A", "Italy", "https://media.api-sports.io/football/leagues/135.png"),
            League("136", "Serie B", "Italy", "https://media.api-sports.io/football/leagues/136.png"),
            League("137", "Coppa Italia", "Italy", "https://media.api-sports.io/football/leagues/137.png"),

            // France
            League("61", "Ligue 1", "France", "https://media.api-sports.io/football/leagues/61.png"),
            League("62", "Ligue 2", "France", "https://media.api-sports.io/football/leagues/62.png"),
            League("66", "Coupe de France", "France", "https://media.api-sports.io/football/leagues/66.png"),

            // Turkey - Multiple Leagues
            League("203", "Süper Lig", "Turkey", "https://media.api-sports.io/football/leagues/203.png"),
            League("204", "1. Lig", "Turkey", "https://media.api-sports.io/football/leagues/204.png"),
            League("205", "2. Lig", "Turkey", "https://media.api-sports.io/football/leagues/205.png"),
            League("206", "Türkiye Kupası", "Turkey", "https://media.api-sports.io/football/leagues/206.png"),
            League("559", "Ziraat Türkiye Kupası", "Turkey", "https://media.api-sports.io/football/leagues/559.png"),

            // Azerbaijan - Separate Country
            League("342", "Premyer Liqa", "Azerbaijan", "https://media.api-sports.io/football/leagues/342.png"),
            League("343", "Birinci Dəstə", "Azerbaijan", "https://media.api-sports.io/football/leagues/343.png"),
            League("344", "Azərbaycan Kuboku", "Azerbaijan", "https://media.api-sports.io/football/leagues/344.png"),

            // Netherlands
            League("88", "Eredivisie", "Netherlands", "https://media.api-sports.io/football/leagues/88.png"),
            League("89", "Eerste Divisie", "Netherlands", "https://media.api-sports.io/football/leagues/89.png"),
            League("90", "KNVB Beker", "Netherlands", "https://media.api-sports.io/football/leagues/90.png"),

            // Portugal
            League("94", "Primeira Liga", "Portugal", "https://media.api-sports.io/football/leagues/94.png"),
            League("95", "Liga Portugal 2", "Portugal", "https://media.api-sports.io/football/leagues/95.png"),
            League("96", "Taça de Portugal", "Portugal", "https://media.api-sports.io/football/leagues/96.png"),

            // International
            League("2", "UEFA Champions League", "UEFA", "https://media.api-sports.io/football/leagues/2.png"),
            League("3", "UEFA Europa League", "UEFA", "https://media.api-sports.io/football/leagues/3.png"),
            League("848", "UEFA Europa Conference League", "UEFA", "https://media.api-sports.io/football/leagues/848.png"),
            League("4", "World Cup", "FIFA", "https://media.api-sports.io/football/leagues/4.png"),
            League("5", "UEFA Nations League", "UEFA", "https://media.api-sports.io/football/leagues/5.png"),

            // More countries
            League("87", "Serie A", "Brazil", "https://media.api-sports.io/football/leagues/87.png"),
            League("128", "Liga MX", "Mexico", "https://media.api-sports.io/football/leagues/128.png"),
            League("253", "Major League Soccer", "USA", "https://media.api-sports.io/football/leagues/253.png"),
            League("169", "Super League", "Greece", "https://media.api-sports.io/football/leagues/169.png"),
            League("218", "Jupiler Pro League", "Belgium", "https://media.api-sports.io/football/leagues/218.png"),
            League("119", "Superliga", "Denmark", "https://media.api-sports.io/football/leagues/119.png"),
            League("103", "Eliteserien", "Norway", "https://media.api-sports.io/football/leagues/103.png"),
            League("113", "Allsvenskan", "Sweden", "https://media.api-sports.io/football/leagues/113.png"),
        )

        filteredLeagues = allLeagues
        _leagues.value = allLeagues

        Log.d(TAG, "Loaded ${allLeagues.size} extended mock leagues")
    }

    private fun loadSelectedLeagues() {
        viewModelScope.launch {
            try {
                val savedLeagues = accountDataStore.getAppSettings().selectedLeagues
                _selectedLeagues.value = savedLeagues.toSet()
                Log.d(TAG, "Loaded ${savedLeagues.size} selected leagues from settings")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading selected leagues", e)
                _selectedLeagues.value = emptySet()
            }
        }
    }

    fun searchLeagues(query: String) {
        Log.d(TAG, "Searching leagues with query: '$query'")

        if (query.isBlank()) {
            filteredLeagues = allLeagues
        } else {
            filteredLeagues = allLeagues.filter { league ->
                league.name.contains(query, ignoreCase = true) ||
                        league.country.contains(query, ignoreCase = true) ||

                        league.name.contains("süper", ignoreCase = true) && query.contains("super", ignoreCase = true) ||
                        league.name.contains("lig", ignoreCase = true) && query.contains("league", ignoreCase = true) ||
                        league.name.contains("kupası", ignoreCase = true) && query.contains("cup", ignoreCase = true) ||

                        (query.contains("cup", ignoreCase = true) &&
                                (league.name.contains("cup", ignoreCase = true) ||
                                        league.name.contains("copa", ignoreCase = true) ||
                                        league.name.contains("coupe", ignoreCase = true) ||
                                        league.name.contains("pokal", ignoreCase = true) ||
                                        league.name.contains("kupası", ignoreCase = true))) ||

                        (query.contains("second", ignoreCase = true) &&
                                (league.name.contains("2", ignoreCase = true) ||
                                        league.name.contains("segunda", ignoreCase = true) ||
                                        league.name.contains("championship", ignoreCase = true)))
            }
        }

        _leagues.value = filteredLeagues
        Log.d(TAG, "Search returned ${filteredLeagues.size} results")
    }

    fun toggleLeagueSelection(league: League) {
        val currentSelected = _selectedLeagues.value?.toMutableSet() ?: mutableSetOf()
        val leagueKey = "${league.name}_${league.country}"

        if (currentSelected.any { it.contains(league.name) && it.contains(league.country) }) {
            currentSelected.removeAll { it.contains(league.name) && it.contains(league.country) }
            Log.d(TAG, "Removed league: ${league.name} (${league.country})")
        } else {
            currentSelected.add(leagueKey)
            Log.d(TAG, "Added league: ${league.name} (${league.country})")
        }

        _selectedLeagues.value = currentSelected
    }

    fun selectAllLeagues() {
        val allLeagueKeys = filteredLeagues.map { "${it.name}_${it.country}" }.toSet()
        _selectedLeagues.value = allLeagueKeys
        Log.d(TAG, "Selected all ${allLeagueKeys.size} leagues")
    }

    fun clearAllSelections() {
        _selectedLeagues.value = emptySet()
        Log.d(TAG, "Cleared all league selections")
    }

    fun saveSelectedLeagues() {
        viewModelScope.launch {
            try {
                val selectedLeagueNames = _selectedLeagues.value?.map { leagueKey ->
                    leagueKey.substringBeforeLast("_")
                } ?: emptyList()

                accountDataStore.saveSelectedLeagues(selectedLeagueNames)
                _saveCompleted.value = true
                Log.d(TAG, "Saved ${selectedLeagueNames.size} selected leagues")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving selected leagues", e)
                _error.value = "Failed to save league filters: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

data class League(
    val id: String,
    val name: String,
    val country: String,
    val logoUrl: String
)