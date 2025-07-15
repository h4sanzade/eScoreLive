package com.materialdesign.escorelive.presentation.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context
    // private val repository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _favoriteTeamsCount = MutableLiveData<Int>()
    val favoriteTeamsCount: LiveData<Int> = _favoriteTeamsCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadUserProfile()
        loadFavoriteTeamsCount()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Mock user profile for now
                val mockProfile = UserProfile(
                    id = 1,
                    username = "Football Fan",
                    email = "user@example.com",
                    profileImageUrl = null,
                    joinDate = "January 2024",
                    preferredLeagues = listOf("Premier League", "Champions League")
                )

                _userProfile.value = mockProfile
            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadFavoriteTeamsCount() {
        try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            val favoriteIds = prefs.getStringSet("favorite_team_ids", emptySet()) ?: emptySet()
            _favoriteTeamsCount.value = favoriteIds.size
        } catch (e: Exception) {
            _favoriteTeamsCount.value = 0
        }
    }

    fun refreshProfile() {
        loadUserProfile()
        loadFavoriteTeamsCount()
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Update profile logic here
                _userProfile.value = profile
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

// Data class for UserProfile
data class UserProfile(
    val id: Long,
    val username: String,
    val email: String,
    val profileImageUrl: String?,
    val joinDate: String,
    val preferredLeagues: List<String>
)