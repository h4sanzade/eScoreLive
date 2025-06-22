package com.materialdesign.escorelive


import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FootballRepository @Inject constructor(
    private val apiService: FootballApiService
) {

    suspend fun getLiveMatches(): Result<List<LiveMatch>> {
        return try {
            val response = apiService.getLiveFixtures()
            if (response.isSuccessful) {
                val fixtures = response.body()?.response ?: emptyList()
                val liveMatches = fixtures.map { fixture ->
                    fixture.toLiveMatch()
                }
                Result.success(liveMatches)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchesByDate(date: String): Result<List<LiveMatch>> {
        return try {
            val response = apiService.getFixturesByDate(date = date)
            if (response.isSuccessful) {
                val fixtures = response.body()?.response ?: emptyList()
                val matches = fixtures.map { fixture ->
                    fixture.toLiveMatch()
                }
                Result.success(matches)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchesByLeague(leagueId: Int, season: Int): Result<List<LiveMatch>> {
        return try {
            val response = apiService.getFixturesByLeague(
                leagueId = leagueId,
                season = season
            )

            if (response.isSuccessful) {
                val fixtures = response.body()?.response ?: emptyList()
                val matches = fixtures.mapNotNull { fixture ->
                    try {
                        fixture.toLiveMatch()
                    } catch (e: Exception) {
                        null
                    }
                }
                Result.success(matches)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}