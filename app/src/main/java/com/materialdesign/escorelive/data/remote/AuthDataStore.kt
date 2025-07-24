package com.materialdesign.escorelive.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USERNAME = stringPreferencesKey("username")
        private val EMAIL = stringPreferencesKey("email")
        private val FIRST_NAME = stringPreferencesKey("first_name")
        private val LAST_NAME = stringPreferencesKey("last_name")
        private val PASSWORD = stringPreferencesKey("password")
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    val isGuestMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_GUEST_MODE] ?: false
    }

    // Get user data
    val userData: Flow<UserData> = dataStore.data.map { preferences ->
        UserData(
            userId = preferences[USER_ID] ?: "",
            username = preferences[USERNAME] ?: "",
            email = preferences[EMAIL] ?: "",
            firstName = preferences[FIRST_NAME] ?: "",
            lastName = preferences[LAST_NAME] ?: "",
            password = preferences[PASSWORD] ?: "",
            accessToken = preferences[ACCESS_TOKEN] ?: "",
            refreshToken = preferences[REFRESH_TOKEN] ?: ""
        )
    }


    suspend fun saveUserLogin(userData: UserData) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[IS_GUEST_MODE] = false
            preferences[USER_ID] = userData.userId
            preferences[USERNAME] = userData.username
            preferences[EMAIL] = userData.email
            preferences[FIRST_NAME] = userData.firstName
            preferences[LAST_NAME] = userData.lastName
            preferences[PASSWORD] = userData.password
            preferences[ACCESS_TOKEN] = userData.accessToken
            preferences[REFRESH_TOKEN] = userData.refreshToken
        }
    }

    suspend fun saveUserRegistration(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String
    ) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences[IS_GUEST_MODE] = false
            preferences[USER_ID] = System.currentTimeMillis().toString()
            preferences[USERNAME] = username
            preferences[EMAIL] = email
            preferences[FIRST_NAME] = firstName
            preferences[LAST_NAME] = lastName
            preferences[PASSWORD] = password
        }
    }

    suspend fun validateLogin(username: String, password: String): Boolean {
        return try {
            val preferences = dataStore.data.first()
            val storedUsername = preferences[USERNAME] ?: ""
            val storedPassword = preferences[PASSWORD] ?: ""
            val storedEmail = preferences[EMAIL] ?: ""

            (storedUsername == username && storedPassword == password) ||
                    (storedEmail == username && storedPassword == password)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_GUEST_MODE] = isGuest
            preferences[IS_LOGGED_IN] = !isGuest
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences[IS_GUEST_MODE] = false
            preferences[ACCESS_TOKEN] = ""
            preferences[REFRESH_TOKEN] = ""
        }
    }
    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

data class UserData(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val password: String = "",
    val accessToken: String = "",
    val refreshToken: String = ""
)