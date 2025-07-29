package com.materialdesign.escorelive.presentation.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteCompetitionsViewModel @Inject constructor(
    private val competitionRepository: CompetitionRepository,
    private val accountDataStore: AccountDataStore
) : ViewModel() {

    private val _favoriteCompetitions = MutableLiveData<List<Competition>>()
    val favoriteCompetitions: LiveData<List<Competition>> = _favoriteCompetitions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadFavoriteCompetitions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                competitionRepository.getFavoriteCompetitions()
                    .onSuccess { competitions ->
                        _favoriteCompetitions.value = competitions
                    }
                    .onFailure { exception ->
                        _error.value = "Failed to load favorite competitions: ${exception.message}"
                        _favoriteCompetitions.value = emptyList()
                    }
            } catch (e: Exception) {
                _error.value = "Error loading favorites: ${e.message}"
                _favoriteCompetitions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeFromFavorites(competition: Competition) {
        viewModelScope.launch {
            try {
                competitionRepository.removeFromFavorites(competition.id)

                // Remove from current list immediately
                val currentList = _favoriteCompetitions.value?.toMutableList() ?: mutableListOf()
                currentList.removeAll { it.id == competition.id }
                _favoriteCompetitions.value = currentList

                // Update account count
                updateAccountCompetitionsCount(currentList.size)

            } catch (e: Exception) {
                _error.value = "Failed to remove from favorites: ${e.message}"
            }
        }
    }

    private suspend fun updateAccountCompetitionsCount(count: Int) {
        try {
            val currentCounts = accountDataStore.getFavoriteCounts()
            accountDataStore.updateFavoriteCounts(
                competitions = count,
                teams = currentCounts.teams,
                players = currentCounts.players
            )
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    fun clearError() {
        _error.value = null
    }
}