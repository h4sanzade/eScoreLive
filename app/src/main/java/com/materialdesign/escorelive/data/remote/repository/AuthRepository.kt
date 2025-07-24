package com.materialdesign.escorelive.data.remote.repository

import com.materialdesign.escorelive.data.remote.AuthApiService
import com.materialdesign.escorelive.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.materialdesign.escorelive.data.remote.LoginRequest
import com.materialdesign.escorelive.data.remote.RegisterRequest

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    @ApplicationContext private val context: Context
) {

    private val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_GUEST_MODE = "is_guest_mode"
    }

    suspend fun login(username: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting login for: $username")

                val loginRequest = LoginRequest(
                    username = username,
                    password = password,
                    expiresInMins = 60 // Token expires in 60 minutes
                )

                val response = authApiService.login(loginRequest)

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        Log.d("AuthRepository", "Login successful, received user: ${loginResponse.username}")

                        // Save login data
                        saveUserSession(
                            user = loginResponse,
                            accessToken = loginResponse.accessToken,
                            refreshToken = loginResponse.refreshToken
                        )

                        Result.success(loginResponse)
                    } else {
                        Log.e("AuthRepository", "Login response body is null")
                        Result.failure(Exception("Invalid response from server"))
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Invalid username or password"
                        401 -> "Invalid credentials"
                        404 -> "User not found"
                        500 -> "Server error, please try again later"
                        else -> "Login failed: ${response.message()}"
                    }
                    Log.e("AuthRepository", "Login failed with code: ${response.code()}, message: $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Login exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String
    ): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting registration for: $username")

                // Note: DummyJSON doesn't have a real registration endpoint,
                // so we'll simulate it by creating a user object locally
                // In a real app, you would call a registration API endpoint

                val registerRequest = RegisterRequest(
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    email = email,
                    password = password
                )

                // Simulate API call delay
                kotlinx.coroutines.delay(1000)

                // Create mock user for successful registration
                val mockUser = User(
                    id = System.currentTimeMillis(), // Generate unique ID
                    username = username,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    gender = "Not specified",
                    image = "https://dummyjson.com/icon/$username/128",
                    accessToken = generateMockToken(),
                    refreshToken = generateMockToken()
                )

                Log.d("AuthRepository", "Registration successful for: $username")

                // Save user session
                saveUserSession(
                    user = mockUser,
                    accessToken = mockUser.accessToken,
                    refreshToken = mockUser.refreshToken
                )

                Result.success(mockUser)

            } catch (e: Exception) {
                Log.e("AuthRepository", "Registration exception", e)
                Result.failure(e)
            }
        }
    }

    private fun saveUserSession(user: User, accessToken: String, refreshToken: String) {
        try {
            val userJson = gson.toJson(user)

            sharedPreferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_DATA, userJson)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_GUEST_MODE, false)
                .apply()

            Log.d("AuthRepository", "User session saved successfully")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving user session", e)
        }
    }

    fun getCurrentUser(): User? {
        return try {
            val userJson = sharedPreferences.getString(KEY_USER_DATA, null)
            if (userJson != null) {
                gson.fromJson(userJson, User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error getting current user", e)
            null
        }
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && !isGuestMode()
    }

    fun setGuestMode(isGuest: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_GUEST_MODE, isGuest)
            .putBoolean(KEY_IS_LOGGED_IN, !isGuest)
            .apply()
        Log.d("AuthRepository", "Guest mode set to: $isGuest")
    }

    fun isGuestMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_GUEST_MODE, false)
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Logging out user")

                // Clear all stored data
                sharedPreferences.edit()
                    .remove(KEY_ACCESS_TOKEN)
                    .remove(KEY_REFRESH_TOKEN)
                    .remove(KEY_USER_DATA)
                    .putBoolean(KEY_IS_LOGGED_IN, false)
                    .putBoolean(KEY_IS_GUEST_MODE, false)
                    .apply()

                Log.d("AuthRepository", "User logged out successfully")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error during logout", e)
            }
        }
    }

    suspend fun refreshToken(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = getRefreshToken()
                if (refreshToken.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("No refresh token available"))
                }

                // For DummyJSON, we'll simulate token refresh
                // In a real app, you would call the refresh token endpoint
                kotlinx.coroutines.delay(500)

                val newAccessToken = generateMockToken()

                sharedPreferences.edit()
                    .putString(KEY_ACCESS_TOKEN, newAccessToken)
                    .apply()

                Log.d("AuthRepository", "Token refreshed successfully")
                Result.success(newAccessToken)

            } catch (e: Exception) {
                Log.e("AuthRepository", "Token refresh failed", e)
                Result.failure(e)
            }
        }
    }

    private fun generateMockToken(): String {
        // Generate a mock JWT-like token for demo purposes
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..50)
            .map { chars.random() }
            .joinToString("")
    }

    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
        Log.d("AuthRepository", "All auth data cleared")
    }
}