// CompetitionRepository.kt
package com.materialdesign.escorelive.data.remote.repository

import android.content.Context
import android.util.Log
import com.materialdesign.escorelive.data.remote.CompetitionApiService
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionDto
import com.materialdesign.escorelive.data.remote.dto.CompetitionType
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

        // Top competitions based on popularity
        private val TOP_COMPETITION_IDS = setOf(
            "39", "140", "78", "135", "61", // Premier League, La Liga, Bundesliga, Serie A, Ligue 1
            "2", "3", "848", // Champions League, Europa League, Europa Conference League
            "4", "1", "21", // World Cup, UEFA Nations League, Confederations Cup
            "203", "342", "88", "94" // Turkish Super League, Azerbaijan Premier League, Eredivisie, Primeira Liga
        )
    }

    private val competitionsCache = mutableMapOf<String, List<Competition>>()
    private var allCompetitions: List<Competition> = emptyList()

    /**
     * Get all competitions from API
     */
    suspend fun getAllCompetitions(forceRefresh: Boolean = false): Result<List<Competition>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!forceRefresh && allCompetitions.isNotEmpty()) {
                    Log.d(TAG, "Returning cached competitions: ${allCompetitions.size}")
                    return@withContext Result.success(allCompetitions)
                }

                Log.d(TAG, "Fetching competitions from API...")
                Log.d(TAG, "API URL: https://api.soccersapi.com/v2.2/leagues/?user=h4sanzade&token=c660199e0f3aa383e4bc220b3b6a9db7&t=list")

                val response = apiService.getAllLeagues()
                Log.d(TAG, "API Response Code: ${response.code()}")
                Log.d(TAG, "API Response Message: ${response.message()}")

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    Log.d(TAG, "Response body: $apiResponse")

                    if (apiResponse?.success == 1) {
                        Log.d(TAG, "API returned ${apiResponse.data.size} competitions")

                        val competitions = apiResponse.data.mapNotNull { dto ->
                            Log.d(TAG, "Processing competition: ${dto.name} - ${dto.country}")
                            mapDtoToCompetition(dto)
                        }.sortedBy { it.name }

                        allCompetitions = competitions
                        Log.d(TAG, "Successfully loaded ${competitions.size} competitions")
                        Result.success(competitions)
                    } else {
                        Log.e(TAG, "API returned success=${apiResponse?.success}, data size=${apiResponse?.data?.size}")
                        Result.failure(Exception("API returned success=0"))
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

    /**
     * Search competitions by name or country
     */
    suspend fun searchCompetitions(query: String): Result<List<Competition>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(allCompetitions)
                }

                Log.d(TAG, "Searching competitions with query: '$query'")

                // First try to filter from cached data
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

                // If not found in cache, try API search
                val response = apiService.searchLeagues(searchTerm = query)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == 1) {
                        val competitions = apiResponse.data.mapNotNull { dto ->
                            mapDtoToCompetition(dto)
                        }.sortedBy { it.name }

                        Log.d(TAG, "API search returned ${competitions.size} competitions")
                        Result.success(competitions)
                    } else {
                        Log.e(TAG, "Search API returned success=0")
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

    /**
     * Get top competitions
     */
    suspend fun getTopCompetitions(): Result<List<Competition>> {
        return getAllCompetitions().map { competitions ->
            competitions.filter { competition ->
                TOP_COMPETITION_IDS.contains(competition.id) || competition.isTopCompetition
            }.sortedWith(compareBy<Competition> { competition ->
                // Sort by predefined order first
                TOP_COMPETITION_IDS.indexOf(competition.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }.thenBy { it.name })
        }
    }

    /**
     * Get competitions grouped by region
     */
    suspend fun getCompetitionsByRegion(): Result<Map<String, List<Competition>>> {
        return getAllCompetitions().map { competitions ->
            competitions.groupBy { it.country }.toSortedMap()
        }
    }

    /**
     * Get favorite competitions
     */
    suspend fun getFavoriteCompetitions(): Result<List<Competition>> {
        return getAllCompetitions().map { competitions ->
            val favoriteIds = getFavoriteCompetitionIds()
            competitions.filter { competition ->
                favoriteIds.contains(competition.id)
            }.sortedBy { it.name }
        }
    }

    /**
     * Add competition to favorites
     */
    fun addToFavorites(competitionId: String) {
        val favorites = getFavoriteCompetitionIds().toMutableSet()
        favorites.add(competitionId)
        saveFavoriteCompetitionIds(favorites)
        Log.d(TAG, "Added competition $competitionId to favorites")
    }

    /**
     * Remove competition from favorites
     */
    fun removeFromFavorites(competitionId: String) {
        val favorites = getFavoriteCompetitionIds().toMutableSet()
        favorites.remove(competitionId)
        saveFavoriteCompetitionIds(favorites)
        Log.d(TAG, "Removed competition $competitionId from favorites")
    }

    /**
     * Check if competition is in favorites
     */
    fun isFavorite(competitionId: String): Boolean {
        return getFavoriteCompetitionIds().contains(competitionId)
    }

    /**
     * Get favorite competition IDs from SharedPreferences
     */
    private fun getFavoriteCompetitionIds(): Set<String> {
        val prefs = context.getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
        return prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    }

    /**
     * Save favorite competition IDs to SharedPreferences
     */
    private fun saveFavoriteCompetitionIds(favoriteIds: Set<String>) {
        val prefs = context.getSharedPreferences(FAVORITES_PREF, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("favorite_ids", favoriteIds)
            .apply()
    }

    /**
     * Map DTO to domain model
     */
    private fun mapDtoToCompetition(dto: CompetitionDto): Competition? {
        return try {
            Competition(
                id = dto.leagueId,
                name = dto.name,
                shortCode = dto.shortCode,
                country = dto.country,
                flagUrl = dto.flag,
                logoUrl = dto.logo,
                season = dto.season,
                seasonStart = dto.seasonStart,
                seasonEnd = dto.seasonEnd,
                isCup = dto.isCup == 1,
                type = when {
                    dto.isCup == 1 -> CompetitionType.CUP
                    dto.type?.lowercase()?.contains("international") == true -> CompetitionType.INTERNATIONAL
                    dto.type?.lowercase()?.contains("tournament") == true -> CompetitionType.TOURNAMENT
                    else -> CompetitionType.LEAGUE
                },
                isTopCompetition = TOP_COMPETITION_IDS.contains(dto.leagueId),
                isFavorite = isFavorite(dto.leagueId)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping competition DTO: ${dto.name}", e)
            null
        }
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        allCompetitions = emptyList()
        competitionsCache.clear()
        Log.d(TAG, "Competition cache cleared")
    }
}