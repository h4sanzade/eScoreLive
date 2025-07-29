package com.materialdesign.escorelive.presentation.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountDataStore: AccountDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _userData = MutableLiveData<UserData?>()
    val userData: LiveData<UserData?> = _userData

    private val _favoriteCounts = MutableLiveData<FavoriteCounts>()
    val favoriteCounts: LiveData<FavoriteCounts> = _favoriteCounts

    private val _appSettings = MutableLiveData<AppSettings>()
    val appSettings: LiveData<AppSettings> = _appSettings

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    fun loadUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userData = accountDataStore.getUserData()
                _userData.value = userData

                val counts = loadActualFavoriteCounts()
                _favoriteCounts.value = counts

                val settings = accountDataStore.getAppSettings()
                _appSettings.value = settings

            } catch (e: Exception) {
                _error.value = "Failed to load user data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadActualFavoriteCounts(): FavoriteCounts {
        // Get actual favorite teams count
        val teamsCount = getFavoriteTeamsCount()

        // Get actual favorite competitions count
        val competitionsCount = getFavoriteCompetitionsCount()

        // Players remains 0 as requested
        val playersCount = 0

        // Update the stored counts
        accountDataStore.updateFavoriteCounts(competitionsCount, teamsCount, playersCount)

        return FavoriteCounts(
            competitions = competitionsCount,
            teams = teamsCount,
            players = playersCount
        )
    }

    private fun getFavoriteTeamsCount(): Int {
        return try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            val favoriteIds = prefs.getStringSet("favorite_team_ids", emptySet()) ?: emptySet()
            favoriteIds.size
        } catch (e: Exception) {
            0
        }
    }

    private fun getFavoriteCompetitionsCount(): Int {
        return try {
            val prefs = context.getSharedPreferences("competition_favorites", Context.MODE_PRIVATE)
            val favoriteIds = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
            favoriteIds.size
        } catch (e: Exception) {
            0
        }
    }

    fun updateProfileImage(imageUri: String) {
        viewModelScope.launch {
            try {
                accountDataStore.saveProfileImageUri(imageUri)
                _userData.value = _userData.value?.copy(profileImageUri = imageUri)
            } catch (e: Exception) {
                _error.value = "Failed to save profile image: ${e.message}"
            }
        }
    }

    fun updateNotificationsSetting(enabled: Boolean) {
        viewModelScope.launch {
            try {
                accountDataStore.saveNotificationsSetting(enabled)
            } catch (e: Exception) {
                _error.value = "Failed to save notifications setting: ${e.message}"
            }
        }
    }

    fun updateDarkThemeSetting(enabled: Boolean) {
        viewModelScope.launch {
            try {
                accountDataStore.saveDarkThemeSetting(enabled)
                _appSettings.value = _appSettings.value?.copy(darkThemeEnabled = enabled)
            } catch (e: Exception) {
                _error.value = "Failed to save dark theme setting: ${e.message}"
            }
        }
    }

    fun updateLanguageSetting(language: String) {
        viewModelScope.launch {
            try {
                accountDataStore.saveLanguageSetting(language)
                _appSettings.value = _appSettings.value?.copy(selectedLanguage = language)
            } catch (e: Exception) {
                _error.value = "Failed to save language setting: ${e.message}"
            }
        }
    }

    fun updateSelectedLeagues(leagues: List<String>) {
        viewModelScope.launch {
            try {
                accountDataStore.saveSelectedLeagues(leagues)
                _appSettings.value = _appSettings.value?.copy(selectedLeagues = leagues)
            } catch (e: Exception) {
                _error.value = "Failed to save league filters: ${e.message}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                accountDataStore.clearUserData()
                _logoutEvent.value = true
            } catch (e: Exception) {
                _error.value = "Failed to logout: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}