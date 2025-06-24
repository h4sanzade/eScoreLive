package com.materialdesign.escorelive.ui.matchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.repository.FootballRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _matchDetails = MutableLiveData<LiveMatch?>()
    val matchDetails: LiveData<LiveMatch?> = _matchDetails

    private val _matchEvents = MutableLiveData<List<MatchEvent>>()
    val matchEvents: LiveData<List<MatchEvent>> = _matchEvents

    private val _matchLineup = MutableLiveData<List<LineupPlayer>>()
    val matchLineup: LiveData<List<LineupPlayer>> = _matchLineup

    private val _matchStatistics = MutableLiveData<MatchStatistics?>()
    val matchStatistics: LiveData<MatchStatistics?> = _matchStatistics

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadMatchDetails(matchId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Load match basic details
            repository.getMatchDetails(matchId)
                .onSuccess { match ->
                    _matchDetails.value = match
                }
                .onFailure { exception ->
                    _error.value = "Failed to load match details: ${exception.message}"
                }

            // Load match events (goals, cards, substitutions)
            repository.getMatchEvents(matchId)
                .onSuccess { events ->
                    _matchEvents.value = events
                }
                .onFailure { exception ->
                    _error.value = "Failed to load match events: ${exception.message}"
                }

            // Load match lineup
            repository.getMatchLineup(matchId)
                .onSuccess { lineup ->
                    _matchLineup.value = lineup
                }
                .onFailure { exception ->
                    _error.value = "Failed to load lineup: ${exception.message}"
                }

            // Load match statistics
            repository.getMatchStatistics(matchId)
                .onSuccess { stats ->
                    _matchStatistics.value = stats
                }
                .onFailure { exception ->
                    _error.value = "Failed to load statistics: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

// Data classes for match details
data class MatchEvent(
    val id: Long,
    val minute: Int,
    val type: String, // goal, card, substitution
    val detail: String, // Normal Goal, Yellow Card, etc.
    val player: String,
    val assistPlayer: String? = null,
    val team: String,
    val isHomeTeam: Boolean
)

data class LineupPlayer(
    val id: Long,
    val name: String,
    val number: Int,
    val position: String,
    val isStarting: Boolean,
    val isHomeTeam: Boolean,
    val rating: Float? = null
)

data class MatchStatistics(
    val homePossession: Int,
    val awayPossession: Int,
    val homeShots: Int,
    val awayShots: Int,
    val homeShotsOnTarget: Int,
    val awayShotsOnTarget: Int,
    val homeCorners: Int,
    val awayCorners: Int,
    val homeYellowCards: Int,
    val awayYellowCards: Int,
    val homeRedCards: Int,
    val awayRedCards: Int,
    val homeFouls: Int,
    val awayFouls: Int,
    val homeOffsides: Int,
    val awayOffsides: Int
)