package com.materialdesign.escorelive.repository

import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.data.remote.FootballApiService
import com.materialdesign.escorelive.toLiveMatch
import com.materialdesign.escorelive.ui.matchdetail.MatchEvent
import com.materialdesign.escorelive.ui.matchdetail.LineupPlayer
import com.materialdesign.escorelive.ui.matchdetail.MatchStatistics
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

    suspend fun getMatchDetails(matchId: Long): Result<LiveMatch> {
        return try {
            val response = apiService.getFixtureById(fixtureId = matchId)
            if (response.isSuccessful) {
                val fixture = response.body()?.response?.firstOrNull()
                if (fixture != null) {
                    Result.success(fixture.toLiveMatch())
                } else {
                    Result.failure(Exception("Match not found"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchEvents(matchId: Long): Result<List<MatchEvent>> {
        return try {
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
                            isHomeTeam = true // Bu değer fixture bilgisinden belirlenmeli
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                Result.success(matchEvents)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchLineup(matchId: Long): Result<List<LineupPlayer>> {
        return try {
            val response = apiService.getMatchLineups(fixtureId = matchId)
            if (response.isSuccessful) {
                val lineups = response.body()?.response ?: emptyList()
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

                Result.success(players)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchStatistics(matchId: Long): Result<MatchStatistics> {
        return try {
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
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getStatValue(statistics: List<com.materialdesign.escorelive.data.remote.StatisticItem>, type: String): Any? {
        return statistics.find { it.type == type }?.value
    }
}