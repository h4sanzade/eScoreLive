// CompetitionApiService.kt
package com.materialdesign.escorelive.data.remote

import com.materialdesign.escorelive.data.remote.dto.LeaguesApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for SoccersAPI competitions
 */
interface CompetitionApiService {

    companion object {
        const val BASE_URL = "https://api.soccersapi.com/v2.2/"
        const val USERNAME = "h4sanzade"
        const val TOKEN = "c660199e0f3aa383e4bc220b3b6a9db7"
    }

    /**
     * Get all leagues/competitions
     * URL: https://api.soccersapi.com/v2.2/leagues/?user=h4sanzade&token=c660199e0f3aa383e4bc220b3b6a9db7&t=list
     */
    @GET("leagues/")
    suspend fun getAllLeagues(
        @Query("user") user: String = USERNAME,
        @Query("token") token: String = TOKEN,
        @Query("t") type: String = "list"
    ): Response<LeaguesApiResponse>

    /**
     * Search leagues by name or country
     */
    @GET("leagues/")
    suspend fun searchLeagues(
        @Query("user") user: String = USERNAME,
        @Query("token") token: String = TOKEN,
        @Query("t") type: String = "search",
        @Query("name") searchTerm: String
    ): Response<LeaguesApiResponse>

    /**
     * Get leagues by country
     */
    @GET("leagues/")
    suspend fun getLeaguesByCountry(
        @Query("user") user: String = USERNAME,
        @Query("token") token: String = TOKEN,
        @Query("t") type: String = "list",
        @Query("country") country: String
    ): Response<LeaguesApiResponse>
}