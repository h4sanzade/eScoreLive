package com.materialdesign.escorelive.data.remote

import com.materialdesign.escorelive.FixturesResponse
import com.materialdesign.escorelive.TeamData
import com.materialdesign.escorelive.data.model.SearchTeamsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.Response

interface FootballApiService {

    companion object {
        const val API_KEY = "8767b67b14200a87603b7211cf4239dd"
        const val API_HOST = "v3.football.api-sports.io"
    }

    @GET("fixtures")
    suspend fun getLiveFixtures(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("live") live: String = "all"
    ): Response<FixturesResponse>

    @GET("fixtures")
    suspend fun getFixturesByDate(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("date") date: String,
        @Query("timezone") timezone: String = "Europe/London"
    ): Response<FixturesResponse>

    @GET("fixtures")
    suspend fun getFixturesByLeague(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): Response<FixturesResponse>

    @GET("fixtures")
    suspend fun getFixtureById(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("id") fixtureId: Long
    ): Response<FixturesResponse>

    @GET("fixtures/events")
    suspend fun getMatchEvents(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("fixture") fixtureId: Long
    ): Response<MatchEventsResponse>

    @GET("fixtures/lineups")
    suspend fun getMatchLineups(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("fixture") fixtureId: Long
    ): Response<MatchLineupsResponse>

    @GET("fixtures/statistics")
    suspend fun getMatchStatistics(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("fixture") fixtureId: Long
    ): Response<MatchStatisticsResponse>
    @GET("fixtures/headtohead")
    suspend fun getH2HMatches(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("h2h") h2h: String,
        @Query("last") last: Int = 10
    ): Response<FixturesResponse>

    @GET("standings")
    suspend fun getStandings(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): Response<StandingsResponse>

    @GET("teams")
    suspend fun searchTeams(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("search") search: String
    ): Response<SearchTeamsResponse>


}

// Response data classes for API
data class StandingsResponse(
    val response: List<StandingsData>
)

data class StandingsData(
    val league: StandingsLeague
)

data class StandingsLeague(
    val id: Int,
    val name: String,
    val country: String,
    val logo: String,
    val season: Int,
    val standings: List<List<TeamStanding>>
)

data class TeamStanding(
    val rank: Int,
    val team: TeamData,
    val points: Int,
    val goalsDiff: Int,
    val group: String,
    val form: String,
    val status: String,
    val description: String?,
    val all: StandingStats,
    val home: StandingStats,
    val away: StandingStats,
    val update: String
)

data class StandingStats(
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goals: GoalsStats
)

data class GoalsStats(
    val `for`: Int,
    val against: Int
)

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