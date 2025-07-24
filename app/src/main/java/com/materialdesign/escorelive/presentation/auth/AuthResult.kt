package com.materialdesign.escorelive.presentation.auth

import com.materialdesign.escorelive.domain.model.User

sealed class AuthResult {
    object Loading : AuthResult()
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
