package com.materialdesign.escorelive.data.remote.repository

import android.content.Context
import android.util.Log
import com.materialdesign.escorelive.data.remote.CompetitionApiService
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionType
import com.materialdesign.escorelive.data.remote.dto.LeagueResponseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompetitionRepository @Inject constructor(
    private val apiService: CompetitionApiService,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "CompetitionRepository"
        private const val FAVORITES_PREF = "competition_favorites"

        private val TOP_COMPETITION_IDS = setOf(
            "39", "140", "78", "135", "61", // Premier League, La Liga, Bundesliga, Serie A, Ligue 1
            "2", "3", "848", // Champions League, Europa League, Europa Conference League
            "4", "1", "21", // World Cup, UEFA Nations League, Confederations Cup
            "203", "342", "88", "94", // Turkish Super League, Azerbaijan Premier League, Eredivisie, Primeira Liga
            "13", "16", "17", // Copa Libertadores, Copa America, AFC Asian Cup
            "45", "144", "179", // FA Cup, Copa del Rey, DFB Pokal
            "81", "137", "262" // DFB Pokal, Coppa Italia, Coupe de France
        )
    }

    private val competitionsCache = mutableMapOf<String, List<Competition>>()
    private var allCompetitions: List<Competition> = emptyList()


    suspend fun getAllCompetitions(forceRefresh: Boolean = false): Result<List<Competition>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!forceRefresh && allCompetitions.isNotEmpty()) {
                    Log.d(TAG, "Returning cached competitions: ${allCompetitions.size}")
                    return@withContext Result.success(allCompetitions)
                }

                Log.d(TAG, "Fetching competitions from Football API...")
                Log.d(TAG, "API URL: https://v3.football.api-sports.io/leagues")

                val response = apiService.getAllLeagues()
                Log.d(TAG, "API Response Code: ${response.code()}")
                Log.d(TAG, "API Response Message: ${response.message()}")

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    Log.d(TAG, "Response body: $apiResponse")

                    if (apiResponse != null) {
                        Log.d(TAG, "API returned ${apiResponse.response.size} competitions")

                        val competitions = apiResponse.response.mapNotNull { leagueResponse ->
                            Log.d(TAG, "Processing competition: ${leagueResponse.league.name} - ${leagueResponse.country.name}")
                            mapResponseToCompetition(leagueResponse)
                        }.sortedBy { it.name }

                        allCompetitions = competitions
                        Log.d(TAG, "Successfully loaded ${competitions.size} competitions")
                        Result.success(competitions)
                    } else {
                        Log.e(TAG, "API returned null response")
                        Result.failure(Exception("API returned null response"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "API call failed: ${response.code()} - ${response.message()}")
                    Log.e(TAG, "Error body: $errorBody")
                    Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getAllCompetitions", e)
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun getCurrentSeasonCompetitions(): Result<List<Competition>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching current season competitions...")

                val response = apiService.getCurrentSeasonLeagues()

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        val competitions = apiResponse.response.mapNotNull { leagueResponse ->
                            // Only include leagues with current season
                            val currentSeason = leagueResponse.seasons.find { it.current }
                            if (currentSeason != null) {
                                mapResponseToCompetition(leagueResponse)
                            } else null
                        }.sortedBy { it.name }

                        Log.d(TAG, "Successfully loaded ${competitions.size} current season competitions")
                        Result.success(competitions)
                    } else {
                        Result.failure(Exception("API returned null response"))
                    }
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getCurrentSeasonCompetitions", e)
                Result.failure(e)
            }
        }
    }


    suspend fun searchCompetitions(query: String): Result<List<Competition>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(allCompetitions)
                }

                Log.d(TAG, "Searching competitions with query: '$query'")

                if (allCompetitions.isNotEmpty()) {
                    val filteredCompetitions = allCompetitions.filter { competition ->
                        competition.name.contains(query, ignoreCase = true) ||
                                competition.country.contains(query, ignoreCase = true) ||
                                competition.shortCode?.contains(query, ignoreCase = true) == true
                    }

                    if (filteredCompetitions.isNotEmpty()) {
                        Log.d(TAG, "Found ${filteredCompetitions.size} competitions in cache")
                        return@withContext Result.success(filteredCompetitions)
                    }
                }

                val response = apiService.searchLeagues(searchTerm = query)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        val competitions = apiResponse.response.mapNotNull { leagueResponse ->
                            mapResponseToCompetition(leagueResponse)
                        }.sortedBy { it.name }

                        Log.d(TAG, "API search returned ${competitions.size} competitions")
                        Result.success(competitions)
                    } else {
                        Log.e(TAG, "Search API returned null response")
                        Result.failure(Exception("No competitions found for '$query'"))
                    }
                } else {
                    Log.e(TAG, "Search API call failed: ${response.code()}")
                    Result.failure(Exception("Search failed: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in searchCompetitions", e)
                Result.failure(e)
            }
        }
    }


    suspend fun getCompetitionsByCountry(country: String): Result<List<Competition>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting competitions for country: $country")

                val response = apiService.getLeaguesByCountry(country = country)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        val competitions = apiResponse.response.mapNotNull { leagueResponse ->
                            mapResponseToCompetition(leagueResponse)
                        }.sortedBy { it.name }

                        Log.d(TAG, "Found ${competitions.size} competitions for country: $country")
                        Result.success(competitions)
                    } else {
                        Result.failure(Exception("No competitions found for country: $country"))
                    }
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getCompetitionsByCountry", e)
                Result.failure(e)
            }
        }
    }


    suspend fun getTopCompetitions(): Result<List<Competition>> {
        return getAllCompetitions().map { competitions ->
            competitions.filter { competition ->
                TOP_COMPETITION_IDS.contains(competition.id) || competition.isTopCompetition
            }.sortedWith(compareBy<Competition> { competition ->
                TOP_COMPETITION_IDS.indexOf(competition.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }.thenBy { it.name })
        }
    }


    suspend fun getCompetitionsByRegion(): Result<Map<String, List<Competition>>> {
        return getAllCompetitions().map { competitions ->
            competitions.groupBy { it.country }.toSortedMap()
        }
    }


    suspend fun getFavoriteCompetitions(): Result<List<Competition>> {
        return getAllCompetitions().map { competitions ->
            val favoriteIds = getFavoriteCompetitionIds()
            competitions.filter { competition ->
                favoriteIds.contains(competition.id)
            }.sortedBy { it.name }
        }
    }


    fun addToFavorites(competitionId: String) {
        val favorites = getFavoriteCompetitionIds().toMutableSet()
        favorites.add(competitionId)
        saveFavoriteCompetitionIds(favorites)
        Log.d(TAG, "Added competition $competitionId to favorites")
    }

    fun removeFromFavorites(competitionId: String) {
        val favorites = getFavoriteCompetitionIds().toMutableSet()
        favorites.remove(competitionId)
        saveFavoriteCompetitionIds(favorites)
        Log.d(TAG, "Removed competition $competitionId from favorites")
    }


    fun isFavorite(competitionId: String): Boolean {
        return getFavoriteCompetitionIds().contains(competitionId)
    }


    private fun getFavoriteCompetitionIds(): Set<String> {
        val prefs = context.getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
        return prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    }


    private fun saveFavoriteCompetitionIds(favoriteIds: Set<String>) {
        val prefs = context.getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("favorite_ids", favoriteIds)
            .apply()
    }


    private fun mapResponseToCompetition(leagueResponse: LeagueResponseDto): Competition? {
        return try {
            val league = leagueResponse.league
            val country = leagueResponse.country
            val currentSeason = leagueResponse.seasons.find { it.current }

            Competition(
                id = league.id.toString(),
                name = league.name,
                shortCode = null,
                country = country.name,
                flagUrl = country.flag,
                logoUrl = league.logo,
                season = currentSeason?.year?.toString(),
                seasonStart = currentSeason?.start,
                seasonEnd = currentSeason?.end,
                isCup = league.type.equals("Cup", ignoreCase = true),
                type = when {
                    league.type.equals("Cup", ignoreCase = true) -> CompetitionType.CUP
                    league.name.contains("International", ignoreCase = true) -> CompetitionType.INTERNATIONAL
                    league.name.contains("World", ignoreCase = true) -> CompetitionType.INTERNATIONAL
                    league.name.contains("Champions", ignoreCase = true) -> CompetitionType.TOURNAMENT
                    league.name.contains("Europa", ignoreCase = true) -> CompetitionType.TOURNAMENT
                    else -> CompetitionType.LEAGUE
                },
                isTopCompetition = TOP_COMPETITION_IDS.contains(league.id.toString()),
                isFavorite = isFavorite(league.id.toString()),
                currentSeason = currentSeason?.current ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping competition: ${leagueResponse.league.name}", e)
            null
        }
    }


    fun clearCache() {
        allCompetitions = emptyList()
        competitionsCache.clear()
        Log.d(TAG, "Competition cache cleared")
    }
}