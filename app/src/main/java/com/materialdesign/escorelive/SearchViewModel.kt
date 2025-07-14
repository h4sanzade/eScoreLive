package com.materialdesign.escorelive.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.model.TeamSearchResult
import com.materialdesign.escorelive.data.model.FavoriteTeam
import com.materialdesign.escorelive.repository.FootballRepository
import com.materialdesign.escorelive.repository.FavoritesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<TeamSearchResult>>()
    val searchResults: LiveData<List<TeamSearchResult>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _favoriteTeams = MutableLiveData<List<FavoriteTeam>>()
    val favoriteTeams: LiveData<List<FavoriteTeam>> = _favoriteTeams

    private var searchJob: Job? = null

    init {
        loadFavoriteTeams()
    }

    fun searchTeams(query: String) {
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce search

            _isLoading.value = true
            _error.value = null

            try {
                repository.searchTeams(query.trim())
                    .onSuccess { teams ->
                        val favorites = favoritesRepository.getFavoriteTeams()
                        val favoriteIds = favorites.map { it.id }.toSet()

                        val searchResults = teams.map { team ->
                            TeamSearchResult(
                                id = team.id,
                                name = team.name,
                                logo = team.logo,
                                country = team.country,
                                code = team.code,
                                founded = team.founded,
                                isFavorite = favoriteIds.contains(team.id)
                            )
                        }

                        _searchResults.value = searchResults
                        Log.d("SearchViewModel", "Found ${searchResults.size} teams for query: $query")
                    }
                    .onFailure { exception ->
                        Log.e("SearchViewModel", "Failed to search teams", exception)
                        _error.value = exception.message
                    }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Exception searching teams", e)
                _error.value = e.message
            }

            _isLoading.value = false
        }
    }

    fun toggleFavorite(team: TeamSearchResult) {
        viewModelScope.launch {
            try {
                val favoriteTeam = FavoriteTeam(
                    id = team.id,
                    name = team.name,
                    logo = team.logo,
                    country = team.country
                )

                val isFavorite = favoritesRepository.toggleFavorite(favoriteTeam)

                // Update search results
                val currentResults = _searchResults.value?.toMutableList() ?: mutableListOf()
                val index = currentResults.indexOfFirst { it.id == team.id }
                if (index != -1) {
                    currentResults[index] = currentResults[index].copy(isFavorite = isFavorite)
                    _searchResults.value = currentResults
                }

                // Reload favorites
                loadFavoriteTeams()

                Log.d("SearchViewModel", "Team ${team.name} ${if (isFavorite) "added to" else "removed from"} favorites")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Failed to toggle favorite", e)
                _error.value = e.message
            }
        }
    }

    private fun loadFavoriteTeams() {
        viewModelScope.launch {
            try {
                val favorites = favoritesRepository.getFavoriteTeams()
                _favoriteTeams.value = favorites
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Failed to load favorite teams", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
    }
}