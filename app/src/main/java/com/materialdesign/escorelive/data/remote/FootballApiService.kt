package com.materialdesign.escorelive.data.remote

import com.materialdesign.escorelive.FixturesResponse
import com.materialdesign.escorelive.TeamData
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.Response

interface FootballApiService {

    @GET("fixtures")
    suspend fun getLiveFixtures(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("live") live: String = "all"
    ): Response<FixturesResponse>

    @GET("fixtures")
    suspend fun getFixturesByDate(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("date") date: String,
        @Query("timezone") timezone: String = "Europe/London"
    ): Response<FixturesResponse>

    @GET("fixtures")
    suspend fun getFixturesByLeague(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("league") leagueId: Int,
        @Query("season") season: Int,
        @Query("live") live: String = "all"
    ): Response<FixturesResponse>

    @GET("fixtures")
    suspend fun getFixtureById(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("id") fixtureId: Long
    ): Response<FixturesResponse>

    @GET("fixtures/events")
    suspend fun getMatchEvents(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("fixture") fixtureId: Long
    ): Response<MatchEventsResponse>

    @GET("fixtures/lineups")
    suspend fun getMatchLineups(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("fixture") fixtureId: Long
    ): Response<MatchLineupsResponse>

    @GET("fixtures/statistics")
    suspend fun getMatchStatistics(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("fixture") fixtureId: Long
    ): Response<MatchStatisticsResponse>
}

// Response data classes for API
data class MatchEventsResponse(
    val response: List<EventData>
)

data class EventData(
    val id: Long?,
    val time: TimeData,
    val team: TeamData,
    val player: PlayerData,
    val assist: PlayerData?,
    val type: String,
    val detail: String,
    val comments: String?
)

data class TimeData(
    val elapsed: Int,
    val extra: Int?
)

data class PlayerData(
    val id: Long,
    val name: String
)

data class MatchLineupsResponse(
    val response: List<LineupData>
)

data class LineupData(
    val team: TeamData,
    val coach: CoachData,
    val formation: String,
    val startXI: List<StartingPlayer>,
    val substitutes: List<SubstitutePlayer>
)

data class CoachData(
    val id: Long,
    val name: String,
    val photo: String?
)

data class StartingPlayer(
    val player: PlayerInfo
)

data class SubstitutePlayer(
    val player: PlayerInfo
)

data class PlayerInfo(
    val id: Long,
    val name: String,
    val number: Int,
    val pos: String,
    val grid: String?
)

data class MatchStatisticsResponse(
    val response: List<StatisticsData>
)

data class StatisticsData(
    val team: TeamData,
    val statistics: List<StatisticItem>
)

data class StatisticItem(
    val type: String,
    val value: Any?
)