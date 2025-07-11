package com.materialdesign.escorelive.repository

import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.data.remote.FootballApiService
import com.materialdesign.escorelive.toLiveMatch
import com.materialdesign.escorelive.ui.matchdetail.MatchEvent
import com.materialdesign.escorelive.ui.matchdetail.LineupPlayer
import com.materialdesign.escorelive.ui.matchdetail.MatchStatistics
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.google.gson.Gson
import com.materialdesign.escorelive.data.remote.TeamStanding

@Singleton
class FootballRepository @Inject constructor(
    private val apiService: FootballApiService
) {

    suspend fun getLiveMatches(): Result<List<LiveMatch>> {
        return try {
            Log.d("FootballRepository", "Fetching live matches with API key: ${FootballApiService.API_KEY.take(10)}...")
            val response = apiService.getLiveFixtures()

            Log.d("FootballRepository", "Response code: ${response.code()}")
            Log.d("FootballRepository", "Response message: ${response.message()}")
            Log.d("FootballRepository", "Response headers: ${response.headers()}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("FootballRepository", "Response body is null: ${body == null}")

                // API yanıtını detaylı logla
                if (body != null) {
                    val gson = Gson()
                    val jsonString = gson.toJson(body)
                    Log.d("FootballRepository", "Full API Response: ${jsonString.take(500)}...") // İlk 500 karakter
                }

                val fixtures = body?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${fixtures.size} live fixtures")

                if (fixtures.isEmpty()) {
                    Log.w("FootballRepository", "No live matches at the moment")
                }

                val liveMatches = fixtures.mapNotNull { fixture ->
                    try {
                        val match = fixture.toLiveMatch()
                        Log.d("FootballRepository", "Live match: ${match.homeTeam.name} vs ${match.awayTeam.name} - League: ${match.league.name} (${match.league.id})")
                        match
                    } catch (e: Exception) {
                        Log.e("FootballRepository", "Error converting fixture to LiveMatch", e)
                        null
                    }
                }

                Log.d("FootballRepository", "Successfully converted ${liveMatches.size} live matches")
                Result.success(liveMatches)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("FootballRepository", "API Error: ${response.code()} - ${response.message()}")
                Log.e("FootballRepository", "Error body: $errorBody")

                // API-Sports specific error handling
                when (response.code()) {
                    401 -> Result.failure(Exception("Invalid API key. Please check your API key."))
                    403 -> Result.failure(Exception("Access forbidden. Your API key might not have access to this endpoint."))
                    429 -> Result.failure(Exception("Rate limit exceeded. Too many requests."))
                    500 -> Result.failure(Exception("API server error. Please try again later."))
                    else -> Result.failure(Exception("API Error: ${response.code()} - ${errorBody ?: response.message()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getLiveMatches", e)
            Log.e("FootballRepository", "Exception type: ${e.javaClass.simpleName}")
            Log.e("FootballRepository", "Exception message: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getMatchesByDate(date: String): Result<List<LiveMatch>> {
        return try {
            Log.d("FootballRepository", "Fetching matches for date: $date")
            val response = apiService.getFixturesByDate(date = date)

            Log.d("FootballRepository", "Response code for date $date: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body()
                val fixtures = body?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${fixtures.size} fixtures for date: $date")

                if (fixtures.isEmpty()) {
                    Log.w("FootballRepository", "No matches found for date: $date")
                }

                val matches = fixtures.mapNotNull { fixture ->
                    try {
                        val match = fixture.toLiveMatch()
                        Log.d("FootballRepository", "Match for $date: ${match.homeTeam.name} vs ${match.awayTeam.name} - League: ${match.league.name} (${match.league.id}) - Status: ${match.matchStatus}")
                        match
                    } catch (e: Exception) {
                        Log.e("FootballRepository", "Error converting fixture to LiveMatch for date $date", e)
                        null
                    }
                }

                Log.d("FootballRepository", "Successfully converted ${matches.size} matches for date: $date")
                Result.success(matches)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("FootballRepository", "API Error for date $date: ${response.code()} - ${response.message()}")
                Log.e("FootballRepository", "Error body: $errorBody")

                when (response.code()) {
                    401 -> Result.failure(Exception("Invalid API key"))
                    429 -> Result.failure(Exception("Rate limit exceeded"))
                    else -> Result.failure(Exception("API Error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchesByDate for date: $date", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchesByLeague(leagueId: Int, season: Int): Result<List<LiveMatch>> {
        return try {
            Log.d("FootballRepository", "Fetching matches for league: $leagueId, season: $season")
            val response = apiService.getFixturesByLeague(
                leagueId = leagueId,
                season = season
            )

            Log.d("FootballRepository", "Response code for league $leagueId: ${response.code()}")

            if (response.isSuccessful) {
                val fixtures = response.body()?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${fixtures.size} fixtures for league: $leagueId")

                val matches = fixtures.mapNotNull { fixture ->
                    try {
                        val match = fixture.toLiveMatch()
                        // Only log first few matches to avoid spam
                        if (fixtures.indexOf(fixture) < 3) {
                            Log.d("FootballRepository", "League $leagueId match: ${match.homeTeam.name} vs ${match.awayTeam.name} - ${match.matchStatus}")
                        }
                        match
                    } catch (e: Exception) {
                        Log.e("FootballRepository", "Error converting fixture to LiveMatch for league $leagueId", e)
                        null
                    }
                }

                Log.d("FootballRepository", "Successfully converted ${matches.size} matches for league: $leagueId")
                Result.success(matches)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("FootballRepository", "API Error for league $leagueId: ${response.code()} - ${response.message()}")
                Log.e("FootballRepository", "Error body: $errorBody")
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchesByLeague for league: $leagueId", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchDetails(matchId: Long): Result<LiveMatch> {
        return try {
            Log.d("FootballRepository", "Fetching match details for ID: $matchId")
            val response = apiService.getFixtureById(fixtureId = matchId)

            if (response.isSuccessful) {
                val fixture = response.body()?.response?.firstOrNull()
                if (fixture != null) {
                    val match = fixture.toLiveMatch()
                    Log.d("FootballRepository", "Match details: ${match.homeTeam.name} vs ${match.awayTeam.name}")
                    Result.success(match)
                } else {
                    Log.e("FootballRepository", "Match not found for ID: $matchId")
                    Result.failure(Exception("Match not found"))
                }
            } else {
                Log.e("FootballRepository", "API Error for match $matchId: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchDetails for ID: $matchId", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchEvents(matchId: Long): Result<List<MatchEvent>> {
        return try {
            Log.d("FootballRepository", "Fetching match events for ID: $matchId")
            val response = apiService.getMatchEvents(fixtureId = matchId)

            if (response.isSuccessful) {
                val events = response.body()?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${events.size} events for match: $matchId")

                val matchEvents = events.mapNotNull { eventData ->
                    try {
                        MatchEvent(
                            id = eventData.id ?: 0L,
                            minute = eventData.time.elapsed,
                            type = eventData.type,
                            detail = eventData.detail,
                            player = eventData.player.name,
                            assistPlayer = eventData.assist?.name,
                            team = eventData.team.name,
                            isHomeTeam = true // This should be determined from fixture data
                        )
                    } catch (e: Exception) {
                        Log.e("FootballRepository", "Error converting event for match $matchId", e)
                        null
                    }
                }

                Log.d("FootballRepository", "Successfully converted ${matchEvents.size} events for match: $matchId")
                Result.success(matchEvents)
            } else {
                Log.e("FootballRepository", "API Error for match events $matchId: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchEvents for ID: $matchId", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchLineup(matchId: Long): Result<List<LineupPlayer>> {
        return try {
            Log.d("FootballRepository", "Fetching match lineup for ID: $matchId")
            val response = apiService.getMatchLineups(fixtureId = matchId)

            if (response.isSuccessful) {
                val lineups = response.body()?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${lineups.size} lineups for match: $matchId")

                val players = mutableListOf<LineupPlayer>()

                lineups.forEachIndexed { teamIndex, lineup ->
                    val isHomeTeam = teamIndex == 0

                    // Starting XI
                    lineup.startXI.forEach { startingPlayer ->
                        players.add(
                            LineupPlayer(
                                id = startingPlayer.player.id,
                                name = startingPlayer.player.name,
                                number = startingPlayer.player.number,
                                position = startingPlayer.player.pos,
                                isStarting = true,
                                isHomeTeam = isHomeTeam
                            )
                        )
                    }

                    // Substitutes
                    lineup.substitutes.forEach { substitute ->
                        players.add(
                            LineupPlayer(
                                id = substitute.player.id,
                                name = substitute.player.name,
                                number = substitute.player.number,
                                position = substitute.player.pos,
                                isStarting = false,
                                isHomeTeam = isHomeTeam
                            )
                        )
                    }
                }

                Log.d("FootballRepository", "Successfully converted ${players.size} players for match: $matchId")
                Result.success(players)
            } else {
                Log.e("FootballRepository", "API Error for match lineup $matchId: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchLineup for ID: $matchId", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchStatistics(matchId: Long): Result<MatchStatistics> {
        return try {
            Log.d("FootballRepository", "Fetching match statistics for ID: $matchId")
            val response = apiService.getMatchStatistics(fixtureId = matchId)

            if (response.isSuccessful) {
                val stats = response.body()?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${stats.size} stat entries for match: $matchId")

                if (stats.size >= 2) {
                    val homeStats = stats[0].statistics
                    val awayStats = stats[1].statistics

                    val matchStats = MatchStatistics(
                        homePossession = getStatValue(homeStats, "Ball Possession")?.toString()?.replace("%", "")?.toIntOrNull() ?: 0,
                        awayPossession = getStatValue(awayStats, "Ball Possession")?.toString()?.replace("%", "")?.toIntOrNull() ?: 0,
                        homeShots = getStatValue(homeStats, "Total Shots")?.toString()?.toIntOrNull() ?: 0,
                        awayShots = getStatValue(awayStats, "Total Shots")?.toString()?.toIntOrNull() ?: 0,
                        homeShotsOnTarget = getStatValue(homeStats, "Shots on Goal")?.toString()?.toIntOrNull() ?: 0,
                        awayShotsOnTarget = getStatValue(awayStats, "Shots on Goal")?.toString()?.toIntOrNull() ?: 0,
                        homeCorners = getStatValue(homeStats, "Corner Kicks")?.toString()?.toIntOrNull() ?: 0,
                        awayCorners = getStatValue(awayStats, "Corner Kicks")?.toString()?.toIntOrNull() ?: 0,
                        homeYellowCards = getStatValue(homeStats, "Yellow Cards")?.toString()?.toIntOrNull() ?: 0,
                        awayYellowCards = getStatValue(awayStats, "Yellow Cards")?.toString()?.toIntOrNull() ?: 0,
                        homeRedCards = getStatValue(homeStats, "Red Cards")?.toString()?.toIntOrNull() ?: 0,
                        awayRedCards = getStatValue(awayStats, "Red Cards")?.toString()?.toIntOrNull() ?: 0,
                        homeFouls = getStatValue(homeStats, "Fouls")?.toString()?.toIntOrNull() ?: 0,
                        awayFouls = getStatValue(awayStats, "Fouls")?.toString()?.toIntOrNull() ?: 0,
                        homeOffsides = getStatValue(homeStats, "Offsides")?.toString()?.toIntOrNull() ?: 0,
                        awayOffsides = getStatValue(awayStats, "Offsides")?.toString()?.toIntOrNull() ?: 0
                    )

                    Log.d("FootballRepository", "Successfully converted statistics for match: $matchId")
                    Result.success(matchStats)
                } else {
                    Log.e("FootballRepository", "Insufficient statistics data for match: $matchId")
                    Result.failure(Exception("Insufficient statistics data"))
                }
            } else {
                Log.e("FootballRepository", "API Error for match statistics $matchId: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchStatistics for ID: $matchId", e)
            Result.failure(e)
        }
    }

    suspend fun getH2HMatches(homeTeamId: Long, awayTeamId: Long): Result<List<LiveMatch>> {
        return try {
            val h2h = "$homeTeamId-$awayTeamId"
            Log.d("FootballRepository", "Fetching H2H matches for: $h2h")
            val response = apiService.getH2HMatches(h2h = h2h)

            if (response.isSuccessful) {
                val fixtures = response.body()?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${fixtures.size} H2H matches")

                val h2hMatches = fixtures.mapNotNull { fixture ->
                    try {
                        fixture.toLiveMatch()
                    } catch (e: Exception) {
                        Log.e("FootballRepository", "Error converting H2H fixture", e)
                        null
                    }
                }

                Result.success(h2hMatches)
            } else {
                Log.e("FootballRepository", "API Error for H2H: ${response.code()}")
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getH2HMatches", e)
            Result.failure(e)
        }
    }

    suspend fun getStandings(leagueId: Int, season: Int): Result<List<TeamStanding>> {
        return try {
            Log.d("FootballRepository", "Fetching standings for league: $leagueId, season: $season")
            val response = apiService.getStandings(leagueId = leagueId, season = season)

            if (response.isSuccessful) {
                val standingsData = response.body()?.response?.firstOrNull()
                val standings = standingsData?.league?.standings?.firstOrNull() ?: emptyList()
                Log.d("FootballRepository", "API returned ${standings.size} teams in standings")
                Result.success(standings)
            } else {
                Log.e("FootballRepository", "API Error for standings: ${response.code()}")
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getStandings", e)
            Result.failure(e)
        }
    }

    private fun getStatValue(statistics: List<com.materialdesign.escorelive.data.remote.StatisticItem>, type: String): Any? {
        return statistics.find { it.type == type }?.value
    }
}