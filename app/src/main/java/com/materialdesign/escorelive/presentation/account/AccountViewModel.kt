// AccountViewModel.kt
package com.materialdesign.escorelive.presentation.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.data.local.AccountDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountDataStore: AccountDataStore
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
                // Load user data from DataStore
                val userData = accountDataStore.getUserData()
                _userData.value = userData

                // Load favorite counts
                val counts = accountDataStore.getFavoriteCounts()
                _favoriteCounts.value = counts

                // Load app settings
                val settings = accountDataStore.getAppSettings()
                _appSettings.value = settings

            } catch (e: Exception) {
                _error.value = "Failed to load user data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfileImage(imageUri: String) {
        viewModelScope.launch {
            try {
                accountDataStore.saveProfileImageUri(imageUri)

                // Update current user data
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

                // Update current settings
                _appSettings.value = _appSettings.value?.copy(notificationsEnabled = enabled)
            } catch (e: Exception) {
                _error.value = "Failed to save notifications setting: ${e.message}"
            }
        }
    }

    fun updateDarkThemeSetting(enabled: Boolean) {
        viewModelScope.launch {
            try {
                accountDataStore.saveDarkThemeSetting(enabled)

                // Update current settings
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

                // Update current settings
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

                // Update current settings
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

// Data classes
data class UserData(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val profileImageUri: String = ""
)

data class FavoriteCounts(
    val competitions: Int = 0,
    val teams: Int = 0,
    val players: Int = 0
)

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = true,
    val selectedLanguage: String = "English",
    val selectedLeagues: List<String> = emptyList()
)