package com.materialdesign.escorelive.repository

import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.data.remote.FootballApiService
import com.materialdesign.escorelive.data.remote.TeamStanding
import com.materialdesign.escorelive.data.model.TeamInfo
import com.materialdesign.escorelive.toLiveMatch
import com.materialdesign.escorelive.ui.matchdetail.MatchEvent
import com.materialdesign.escorelive.ui.matchdetail.LineupPlayer
import com.materialdesign.escorelive.ui.matchdetail.MatchStatistics
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.google.gson.Gson

@Singleton
class FootballRepository @Inject constructor(
    private val apiService: FootballApiService
) {

    suspend fun getLiveMatches(): Result<List<LiveMatch>> {
        return try {
            Log.d("FootballRepository", "Fetching live matches")
            val response = apiService.getLiveFixtures()

            if (response.isSuccessful) {
                val fixtures = response.body()?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${fixtures.size} live fixtures")

                val liveMatches = fixtures.mapNotNull { fixture ->
                    try {
                        fixture.toLiveMatch()
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

                when (response.code()) {
                    401 -> Result.failure(Exception("Invalid API key"))
                    403 -> Result.failure(Exception("Access forbidden"))
                    429 -> Result.failure(Exception("Rate limit exceeded"))
                    500 -> Result.failure(Exception("API server error"))
                    else -> Result.failure(Exception("API Error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getLiveMatches", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchesByDate(date: String): Result<List<LiveMatch>> {
        return try {
            Log.d("FootballRepository", "Fetching matches for date: $date")
            val response = apiService.getFixturesByDate(date = date)

            if (response.isSuccessful) {
                val fixtures = response.body()?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${fixtures.size} fixtures for date: $date")

                val matches = fixtures.mapNotNull { fixture ->
                    try {
                        fixture.toLiveMatch()
                    } catch (e: Exception) {
                        Log.e("FootballRepository", "Error converting fixture to LiveMatch", e)
                        null
                    }
                }

                Log.d("FootballRepository", "Successfully converted ${matches.size} matches for date: $date")
                Result.success(matches)
            } else {
                Log.e("FootballRepository", "API Error for date $date: ${response.code()}")
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchesByDate", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchesByLeague(leagueId: Int, season: Int): Result<List<LiveMatch>> {
        return try {
            Log.d("FootballRepository", "Fetching matches for league: $leagueId, season: $season")
            val response = apiService.getFixturesByLeague(leagueId = leagueId, season = season)

            if (response.isSuccessful) {
                val fixtures = response.body()?.response ?: emptyList()
                Log.d("FootballRepository", "API returned ${fixtures.size} fixtures for league: $leagueId")

                val matches = fixtures.mapNotNull { fixture ->
                    try {
                        fixture.toLiveMatch()
                    } catch (e: Exception) {
                        Log.e("FootballRepository", "Error converting fixture", e)
                        null
                    }
                }

                Result.success(matches)
            } else {
                Log.e("FootballRepository", "API Error for league $leagueId: ${response.code()}")
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchesByLeague", e)
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
                    Result.success(match)
                } else {
                    Result.failure(Exception("Match not found"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchDetails", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchEvents(matchId: Long): Result<List<MatchEvent>> {
        return try {
            Log.d("FootballRepository", "Fetching match events for ID: $matchId")
            val response = apiService.getMatchEvents(fixtureId = matchId)

            if (response.isSuccessful) {
                val events = response.body()?.response ?: emptyList()
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
                            isHomeTeam = true
                        )
                    } catch (e: Exception) {
                        Log.e("FootballRepository", "Error converting event", e)
                        null
                    }
                }

                Result.success(matchEvents)
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchEvents", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchLineup(matchId: Long): Result<List<LineupPlayer>> {
        return try {
            Log.d("FootballRepository", "Fetching match lineup for ID: $matchId")
            val response = apiService.getMatchLineups(fixtureId = matchId)

            if (response.isSuccessful) {
                val lineups = response.body()?.response ?: emptyList()
                val players = mutableListOf<LineupPlayer>()

                lineups.forEachIndexed { teamIndex, lineup ->
                    val isHomeTeam = teamIndex == 0

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

                Result.success(players)
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchLineup", e)
            Result.failure(e)
        }
    }

    suspend fun getMatchStatistics(matchId: Long): Result<MatchStatistics> {
        return try {
            Log.d("FootballRepository", "Fetching match statistics for ID: $matchId")
            val response = apiService.getMatchStatistics(fixtureId = matchId)

            if (response.isSuccessful) {
                val stats = response.body()?.response ?: emptyList()

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

                    Result.success(matchStats)
                } else {
                    Result.failure(Exception("Insufficient statistics data"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getMatchStatistics", e)
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
                Result.success(standings)
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getStandings", e)
            Result.failure(e)
        }
    }

    suspend fun searchTeams(query: String): Result<List<TeamInfo>> {
        return try {
            Log.d("FootballRepository", "Searching teams for query: $query")
            val response = apiService.searchTeams(search = query)

            if (response.isSuccessful) {
                val body = response.body()
                val teams = body?.response?.map { it.team } ?: emptyList()
                Log.d("FootballRepository", "API returned ${teams.size} teams for query: $query")

                Result.success(teams)
            } else {
                Log.e("FootballRepository", "API Error for search: ${response.code()}")
                when (response.code()) {
                    401 -> Result.failure(Exception("Invalid API key"))
                    429 -> Result.failure(Exception("Rate limit exceeded"))
                    else -> Result.failure(Exception("Search failed"))
                }
            }
        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in searchTeams", e)
            Result.failure(e)
        }
    }

    private fun getStatValue(statistics: List<com.materialdesign.escorelive.data.remote.StatisticItem>, type: String): Any? {
        return statistics.find { it.type == type }?.value
    }
}