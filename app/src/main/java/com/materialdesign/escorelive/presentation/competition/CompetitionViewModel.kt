package com.materialdesign.escorelive.presentation.ui.competition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompetitionViewModel @Inject constructor(
    // private val repository: CompetitionRepository
) : ViewModel() {

    private val _competitions = MutableLiveData<List<Competition>>()
    val competitions: LiveData<List<Competition>> = _competitions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadCompetitions()
    }

    private fun loadCompetitions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Mock data for now
                val mockCompetitions = listOf(
                    Competition(1, "Premier League", "England", "https://example.com/pl.png"),
                    Competition(2, "La Liga", "Spain", "https://example.com/laliga.png"),
                    Competition(3, "Bundesliga", "Germany", "https://example.com/bundesliga.png"),
                    Competition(4, "Serie A", "Italy", "https://example.com/seriea.png"),
                    Competition(5, "Ligue 1", "France", "https://example.com/ligue1.png")
                )

                _competitions.value = mockCompetitions
            } catch (e: Exception) {
                _error.value = "Failed to load competitions: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshCompetitions() {
        loadCompetitions()
    }

    fun clearError() {
        _error.value = null
    }
}

// Data class for Competition
data class Competition(
    val id: Int,
    val name: String,
    val country: String,
    val logo: String
)