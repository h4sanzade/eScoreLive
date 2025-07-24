package com.materialdesign.escorelive.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import android.util.Patterns
import com.materialdesign.escorelive.data.remote.repository.AuthRepository

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginResult = MutableLiveData<AuthResult>()
    val loginResult: LiveData<AuthResult> = _loginResult

    private val _registerResult = MutableLiveData<AuthResult>()
    val registerResult: LiveData<AuthResult> = _registerResult

    private val _validationError = MutableLiveData<ValidationError>()
    val validationError: LiveData<ValidationError> = _validationError

    fun login(username: String, password: String) {
        if (!validateLogin(username, password)) {
            return
        }

        _loginResult.value = AuthResult.Loading
        Log.d("AuthViewModel", "Attempting login for username: $username")

        viewModelScope.launch {
            try {
                val result = authRepository.login(username, password)
                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Login successful for user: ${user.username}")
                        _loginResult.value = AuthResult.Success(user)
                    },
                    onFailure = { exception ->
                        Log.e("AuthViewModel", "Login failed", exception)
                        _loginResult.value = AuthResult.Error(
                            exception.message ?: "Login failed. Please check your credentials."
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login exception", e)
                _loginResult.value = AuthResult.Error(
                    "Network error. Please check your connection and try again."
                )
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (!validateRegistration(firstName, lastName, username, email, password, confirmPassword)) {
            return
        }

        _registerResult.value = AuthResult.Loading
        Log.d("AuthViewModel", "Attempting registration for username: $username")

        viewModelScope.launch {
            try {
                val result = authRepository.register(firstName, lastName, username, email, password)
                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Registration successful for user: ${user.username}")
                        _registerResult.value = AuthResult.Success(user)
                    },
                    onFailure = { exception ->
                        Log.e("AuthViewModel", "Registration failed", exception)
                        _registerResult.value = AuthResult.Error(
                            exception.message ?: "Registration failed. Please try again."
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Registration exception", e)
                _registerResult.value = AuthResult.Error(
                    "Network error. Please check your connection and try again."
                )
            }
        }
    }

    private fun validateLogin(username: String, password: String): Boolean {
        when {
            username.isEmpty() -> {
                _validationError.value = ValidationError.EmptyUsername
                return false
            }
            password.isEmpty() -> {
                _validationError.value = ValidationError.EmptyPassword
                return false
            }
            else -> {
                _validationError.value = ValidationError.None
                return true
            }
        }
    }

    private fun validateRegistration(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        when {
            firstName.isEmpty() -> {
                _validationError.value = ValidationError.EmptyFirstName
                return false
            }
            lastName.isEmpty() -> {
                _validationError.value = ValidationError.EmptyLastName
                return false
            }
            username.isEmpty() -> {
                _validationError.value = ValidationError.EmptyUsername
                return false
            }
            email.isEmpty() -> {
                _validationError.value = ValidationError.EmptyEmail
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _validationError.value = ValidationError.InvalidEmail
                return false
            }
            password.isEmpty() -> {
                _validationError.value = ValidationError.EmptyPassword
                return false
            }
            password.length < 6 -> {
                _validationError.value = ValidationError.WeakPassword
                return false
            }
            password != confirmPassword -> {
                _validationError.value = ValidationError.PasswordMismatch
                return false
            }
            else -> {
                _validationError.value = ValidationError.None
                return true
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                Log.d("AuthViewModel", "User logged out successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Logout error", e)
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    fun getCurrentUser(): User? {
        return authRepository.getCurrentUser()
    }

    fun setGuestMode(isGuest: Boolean) {
        authRepository.setGuestMode(isGuest)
    }

    fun isGuestMode(): Boolean {
        return authRepository.isGuestMode()
    }

    fun clearError() {
        _validationError.value = ValidationError.None
    }
}

// Sealed classes for better state management
sealed class AuthResult {
    object Loading : AuthResult()
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class ValidationError {
    object None : ValidationError()
    object EmptyFirstName : ValidationError()
    object EmptyLastName : ValidationError()
    object EmptyUsername : ValidationError()
    object EmptyEmail : ValidationError()
    object InvalidEmail : ValidationError()
    object EmptyPassword : ValidationError()
    object WeakPassword : ValidationError()
    object PasswordMismatch : ValidationError()
}