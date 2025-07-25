// LeaguesRepository.kt
package com.materialdesign.escorelive.data.remote.repository

import com.materialdesign.escorelive.presentation.filter.League
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaguesRepository @Inject constructor(
    private val apiService: LeaguesApiService
) {

    suspend fun getAllLeagues(): Result<List<League>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAllLeagues()

                if (response.isSuccessful) {
                    val leaguesResponse = response.body()
                    if (leaguesResponse?.status == "success") {
                        val leagues = leaguesResponse.data.map { leagueDto ->
                            League(
                                id = leagueDto.id.toString(),
                                name = leagueDto.name,
                                country = leagueDto.country,
                                logoUrl = leagueDto.flag ?: leagueDto.logo
                            )
                        }
                        Result.success(leagues)
                    } else {
                        Result.failure(Exception("API Error: ${leaguesResponse?.message}"))
                    }
                } else {
                    Result.failure(Exception("HTTP Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun searchLeagues(query: String): Result<List<League>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchLeagues(query)

                if (response.isSuccessful) {
                    val leaguesResponse = response.body()
                    if (leaguesResponse?.status == "success") {
                        val leagues = leaguesResponse.data.map { leagueDto ->
                            League(
                                id = leagueDto.id.toString(),
                                name = leagueDto.name,
                                country = leagueDto.country,
                                logoUrl = leagueDto.flag ?: leagueDto.logo
                            )
                        }
                        Result.success(leagues)
                    } else {
                        Result.failure(Exception("API Error: ${leaguesResponse?.message}"))
                    }
                } else {
                    Result.failure(Exception("HTTP Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getLeaguesByCountry(country: String): Result<List<League>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getLeaguesByCountry(country)

                if (response.isSuccessful) {
                    val leaguesResponse = response.body()
                    if (leaguesResponse?.status == "success") {
                        val leagues = leaguesResponse.data.map { leagueDto ->
                            League(
                                id = leagueDto.id.toString(),
                                name = leagueDto.name,
                                country = leagueDto.country,
                                logoUrl = leagueDto.flag ?: leagueDto.logo
                            )
                        }
                        Result.success(leagues)
                    } else {
                        Result.failure(Exception("API Error: ${leaguesResponse?.message}"))
                    }
                } else {
                    Result.failure(Exception("HTTP Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

// API Service interface
interface LeaguesApiService {

    @retrofit2.http.GET("leagues")
    suspend fun getAllLeagues(): retrofit2.Response<LeaguesApiResponse>

    @retrofit2.http.GET("leagues/search")
    suspend fun searchLeagues(
        @retrofit2.http.Query("q") query: String
    ): retrofit2.Response<LeaguesApiResponse>

    @retrofit2.http.GET("leagues/country")
    suspend fun getLeaguesByCountry(
        @retrofit2.http.Query("country") country: String
    ): retrofit2.Response<LeaguesApiResponse>
}

// API Response models
data class LeaguesApiResponse(
    val status: String,
    val message: String?,
    val data: List<LeagueDto>
)

data class LeagueDto(
    val id: Long,
    val name: String,
    val country: String,
    val logo: String,
    val flag: String?
)