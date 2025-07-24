package com.materialdesign.escorelive.data.remote


import com.materialdesign.escorelive.domain.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    companion object {
        const val BASE_URL = "https://dummyjson.com/"
    }

    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<User>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body refreshRequest: RefreshTokenRequest
    ): Response<RefreshTokenResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<User>

    // Note: DummyJSON doesn't have a registration endpoint
    // In a real app, you would have something like:
    // @POST("auth/register")
    // suspend fun register(@Body registerRequest: RegisterRequest): Response<User>
}

// Data classes for API requests and responses
data class LoginRequest(
    val username: String,
    val password: String,
    val expiresInMins: Int = 60
)

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)