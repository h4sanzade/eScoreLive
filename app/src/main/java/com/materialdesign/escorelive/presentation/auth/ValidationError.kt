package com.materialdesign.escorelive.presentation.auth

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