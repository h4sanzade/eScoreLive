package com.materialdesign.escorelive


import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _liveMatches = MutableLiveData<List<LiveMatch>>()
    val liveMatches: LiveData<List<LiveMatch>> = _liveMatches

    private val _todayMatches = MutableLiveData<List<LiveMatch>>()
    val todayMatches: LiveData<List<LiveMatch>> = _todayMatches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        val today = dateFormat.format(Date())
        _selectedDate.value = today
        loadLiveMatches()
        loadMatchesByDate(today)
    }

    fun loadLiveMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getLiveMatches()
                .onSuccess { matches ->
                    _liveMatches.value = matches
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }

            _isLoading.value = false
        }
    }

    fun loadMatchesByDate(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getMatchesByDate(date)
                .onSuccess { matches ->
                    _todayMatches.value = matches
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }

            _isLoading.value = false
        }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        loadMatchesByDate(date)
    }

    fun refreshData() {
        loadLiveMatches()
        _selectedDate.value?.let { date ->
            loadMatchesByDate(date)
        }
    }

    fun clearError() {
        _error.value = null
    }
}