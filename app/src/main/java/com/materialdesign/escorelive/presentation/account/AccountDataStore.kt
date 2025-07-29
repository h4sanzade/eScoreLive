package com.materialdesign.escorelive.presentation.account

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val FIRST_NAME = stringPreferencesKey("first_name")
        private val LAST_NAME = stringPreferencesKey("last_name")
        private val EMAIL = stringPreferencesKey("email")
        private val USERNAME = stringPreferencesKey("username")
        private val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")

        private val COMPETITIONS_COUNT = intPreferencesKey("competitions_count")
        private val TEAMS_COUNT = intPreferencesKey("teams_count")
        private val PLAYERS_COUNT = intPreferencesKey("players_count")

        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        private val SELECTED_LEAGUES = stringSetPreferencesKey("selected_leagues")
    }

    suspend fun getUserData(): UserData {
        return dataStore.data.map { preferences ->
            UserData(
                firstName = preferences[FIRST_NAME] ?: "",
                lastName = preferences[LAST_NAME] ?: "",
                email = preferences[EMAIL] ?: "",
                username = preferences[USERNAME] ?: "",
                profileImageUri = preferences[PROFILE_IMAGE_URI] ?: ""
            )
        }.first()
    }

    suspend fun saveUserData(
        firstName: String,
        lastName: String,
        email: String,
        username: String = ""
    ) {
        dataStore.edit { preferences ->
            preferences[FIRST_NAME] = firstName
            preferences[LAST_NAME] = lastName
            preferences[EMAIL] = email
            preferences[USERNAME] = username
        }
    }

    suspend fun saveProfileImageUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE_URI] = uri
        }
    }

    suspend fun getFavoriteCounts(): FavoriteCounts {
        return dataStore.data.map { preferences ->
            FavoriteCounts(
                competitions = preferences[COMPETITIONS_COUNT] ?: 0,
                teams = preferences[TEAMS_COUNT] ?: 0,
                players = preferences[PLAYERS_COUNT] ?: 0
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

    suspend fun isNotificationsEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }.first()
    }

    suspend fun isDarkThemeEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[DARK_THEME_ENABLED] ?: true
        }.first()
    }

    suspend fun getSelectedLanguage(): String {
        return dataStore.data.map { preferences ->
            preferences[SELECTED_LANGUAGE] ?: "English"
        }.first()
    }

    suspend fun getSelectedLeagues(): List<String> {
        return dataStore.data.map { preferences ->
            preferences[SELECTED_LEAGUES]?.toList() ?: emptyList()
        }.first()
    }

    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun clearPersonalData() {
        dataStore.edit { preferences ->
            preferences.remove(FIRST_NAME)
            preferences.remove(LAST_NAME)
            preferences.remove(EMAIL)
            preferences.remove(USERNAME)
            preferences.remove(PROFILE_IMAGE_URI)
            preferences.remove(COMPETITIONS_COUNT)
            preferences.remove(TEAMS_COUNT)
            preferences.remove(PLAYERS_COUNT)
        }
    }

    // Add this method to only clear session data, not personal data
    suspend fun clearSessionData() {
        dataStore.edit { preferences ->
            // Only clear app settings and session-related data
            preferences.remove(NOTIFICATIONS_ENABLED)
            preferences.remove(DARK_THEME_ENABLED)
            preferences.remove(SELECTED_LANGUAGE)
            preferences.remove(SELECTED_LEAGUES)
        }
    }
}

data class UserData(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val username: String = "",
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