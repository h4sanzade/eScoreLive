// AccountDataStore.kt
package com.materialdesign.escorelive.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.materialdesign.escorelive.presentation.account.AppSettings
import com.materialdesign.escorelive.presentation.account.FavoriteCounts
import com.materialdesign.escorelive.presentation.account.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // User data keys
        private val FIRST_NAME = stringPreferencesKey("first_name")
        private val LAST_NAME = stringPreferencesKey("last_name")
        private val EMAIL = stringPreferencesKey("email")
        private val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")

        // Favorites count keys
        private val COMPETITIONS_COUNT = intPreferencesKey("competitions_count")
        private val TEAMS_COUNT = intPreferencesKey("teams_count")
        private val PLAYERS_COUNT = intPreferencesKey("players_count")

        // App settings keys
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        private val SELECTED_LEAGUES = stringSetPreferencesKey("selected_leagues")
    }

    // User data operations
    suspend fun getUserData(): UserData {
        return dataStore.data.map { preferences ->
            UserData(
                firstName = preferences[FIRST_NAME] ?: "",
                lastName = preferences[LAST_NAME] ?: "",
                email = preferences[EMAIL] ?: "",
                profileImageUri = preferences[PROFILE_IMAGE_URI] ?: ""
            )
        }.first()
    }

    suspend fun saveUserData(
        firstName: String,
        lastName: String,
        email: String
    ) {
        dataStore.edit { preferences ->
            preferences[FIRST_NAME] = firstName
            preferences[LAST_NAME] = lastName
            preferences[EMAIL] = email
        }
    }

    suspend fun saveProfileImageUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE_URI] = uri
        }
    }

    // Favorites count operations
    suspend fun getFavoriteCounts(): FavoriteCounts {
        return dataStore.data.map { preferences ->
            FavoriteCounts(
                competitions = preferences[COMPETITIONS_COUNT] ?: 5,
                teams = preferences[TEAMS_COUNT] ?: 12,
                players = preferences[PLAYERS_COUNT] ?: 28
            )
        }.first()
    }

    suspend fun updateFavoriteCounts(
        competitions: Int,
        teams: Int,
        players: Int
    ) {
        dataStore.edit { preferences ->
            preferences[COMPETITIONS_COUNT] = competitions
            preferences[TEAMS_COUNT] = teams
            preferences[PLAYERS_COUNT] = players
        }
    }

    suspend fun incrementTeamsCount() {
        dataStore.edit { preferences ->
            val currentCount = preferences[TEAMS_COUNT] ?: 0
            preferences[TEAMS_COUNT] = currentCount + 1
        }
    }

    suspend fun decrementTeamsCount() {
        dataStore.edit { preferences ->
            val currentCount = preferences[TEAMS_COUNT] ?: 0
            if (currentCount > 0) {
                preferences[TEAMS_COUNT] = currentCount - 1
            }
        }
    }

    // App settings operations
    suspend fun getAppSettings(): AppSettings {
        return dataStore.data.map { preferences ->
            AppSettings(
                notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
                darkThemeEnabled = preferences[DARK_THEME_ENABLED] ?: true,
                selectedLanguage = preferences[SELECTED_LANGUAGE] ?: "English",
                selectedLeagues = preferences[SELECTED_LEAGUES]?.toList() ?: emptyList()
            )
        }.first()
    }

    suspend fun saveNotificationsSetting(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveDarkThemeSetting(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_ENABLED] = enabled
        }
    }

    suspend fun saveLanguageSetting(language: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = language
        }
    }

    suspend fun saveSelectedLeagues(leagues: List<String>) {
        dataStore.edit { preferences ->
            preferences[SELECTED_LEAGUES] = leagues.toSet()
        }
    }

    // Notification settings
    suspend fun isNotificationsEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }.first()
    }

    // Theme settings
    suspend fun isDarkThemeEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[DARK_THEME_ENABLED] ?: true
        }.first()
    }

    // Language settings
    suspend fun getSelectedLanguage(): String {
        return dataStore.data.map { preferences ->
            preferences[SELECTED_LANGUAGE] ?: "English"
        }.first()
    }

    // League filter settings
    suspend fun getSelectedLeagues(): List<String> {
        return dataStore.data.map { preferences ->
            preferences[SELECTED_LEAGUES]?.toList() ?: emptyList()
        }.first()
    }

    // Clear all data (for logout)
    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Clear only user personal data (keep settings)
    suspend fun clearPersonalData() {
        dataStore.edit { preferences ->
            preferences.remove(FIRST_NAME)
            preferences.remove(LAST_NAME)
            preferences.remove(EMAIL)
            preferences.remove(PROFILE_IMAGE_URI)
            preferences.remove(COMPETITIONS_COUNT)
            preferences.remove(TEAMS_COUNT)
            preferences.remove(PLAYERS_COUNT)
        }
    }
}