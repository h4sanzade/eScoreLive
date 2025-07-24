package com.materialdesign.escorelive.data.remote.repository


import com.materialdesign.escorelive.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.materialdesign.escorelive.data.remote.AuthDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

@Singleton
class AuthRepository @Inject constructor(
    private val authDataStore: AuthDataStore
) {

    suspend fun login(username: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting login for: $username")

                // Check if credentials match stored registration data
                val isValidCredentials = authDataStore.validateLogin(username, password)

                if (isValidCredentials) {
                    // Get user data from DataStore
                    val userData = authDataStore.userData.first()

                    // Create User object
                    val user = User(
                        id = userData.userId.toLongOrNull() ?: System.currentTimeMillis(),
                        username = userData.username,
                        email = userData.email,
                        firstName = userData.firstName,
                        lastName = userData.lastName,
                        gender = "Not specified",
                        image = "https://dummyjson.com/icon/${userData.username}/128",
                        accessToken = generateMockToken(),
                        refreshToken = generateMockToken()
                    )

                    // Save login session
                    authDataStore.saveUserLogin(
                        userData.copy(
                            accessToken = user.accessToken,
                            refreshToken = user.refreshToken
                        )
                    )

                    Log.d("AuthRepository", "Login successful for: ${user.username}")
                    Result.success(user)
                } else {
                    Log.e("AuthRepository", "Invalid credentials for: $username")
                    Result.failure(Exception("Invalid username/email or password"))
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
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting registration for: $username")

                // Save registration data to DataStore
                authDataStore.saveUserRegistration(
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    email = email,
                    password = password
                )

                Log.d("AuthRepository", "Registration successful for: $username")
                Result.success("Registration successful! Please login with your credentials.")

            } catch (e: Exception) {
                Log.e("AuthRepository", "Registration exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getCurrentUser(): User? {
        return try {
            val userData = authDataStore.userData.first()
            val isLoggedIn = authDataStore.isLoggedIn.first()

            if (isLoggedIn && userData.username.isNotEmpty()) {
                User(
                    id = userData.userId.toLongOrNull() ?: 0L,
                    username = userData.username,
                    email = userData.email,
                    firstName = userData.firstName,
                    lastName = userData.lastName,
                    gender = "Not specified",
                    image = "https://dummyjson.com/icon/${userData.username}/128",
                    accessToken = userData.accessToken,
                    refreshToken = userData.refreshToken
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error getting current user", e)
            null
        }
    }

    suspend fun isUserLoggedIn(): Boolean {
        return try {
            authDataStore.isLoggedIn.first()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        authDataStore.setGuestMode(isGuest)
        Log.d("AuthRepository", "Guest mode set to: $isGuest")
    }

    suspend fun isGuestMode(): Boolean {
        return try {
            authDataStore.isGuestMode.first()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Logging out user")
                authDataStore.logout()
                Log.d("AuthRepository", "User logged out successfully")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error during logout", e)
            }
        }
    }

    fun clearAllData() {
        // This should be called in a coroutine
        Log.d("AuthRepository", "All auth data cleared")
    }

    private fun generateMockToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..50)
            .map { chars.random() }
            .joinToString("")
    }
}