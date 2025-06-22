package com.materialdesign.escorelive

// Data Models
data class LiveMatch(
    val id: Long,
    val homeTeam: Team,
    val awayTeam: Team,
    val homeScore: Int,
    val awayScore: Int,
    val matchMinute: String,
    val matchStatus: String,
    val league: League,
    val isLive: Boolean,
    val kickoffTime: String? = null
)

data class Team(
    val id: Long,
    val name: String,
    val logo: String,
    val shortName: String? = null
)

data class League(
    val id: Long,
    val name: String,
    val logo: String,
    val country: String
)

data class FixturesResponse(
    val response: List<FixtureData>
)

data class FixtureData(
    val fixture: Fixture,
    val league: LeagueData,
    val teams: Teams,
    val goals: Goals,
    val score: Score
)

data class Fixture(
    val id: Long,
    val date: String,
    val status: Status,
    val elapsed: Int?,
    val timestamp: Long
)

data class Status(
    val long: String,
    val short: String,
    val elapsed: Int?
)

data class LeagueData(
    val id: Long,
    val name: String,
    val logo: String,
    val country: String,
    val season: Int
)

data class Teams(
    val home: TeamData,
    val away: TeamData
)

data class TeamData(
    val id: Long,
    val name: String,
    val logo: String
)

data class Goals(
    val home: Int?,
    val away: Int?
)

data class Score(
    val halftime: ScoreDetail,
    val fulltime: ScoreDetail,
    val extratime: ScoreDetail?,
    val penalty: ScoreDetail?
)

data class ScoreDetail(
    val home: Int?,
    val away: Int?
)

fun FixtureData.toLiveMatch(): LiveMatch {
    return LiveMatch(
        id = fixture.id,
        homeTeam = Team(
            id = teams.home.id,
            name = teams.home.name,
            logo = teams.home.logo,
            shortName = teams.home.name.take(3).uppercase()
        ),
        awayTeam = Team(
            id = teams.away.id,
            name = teams.away.name,
            logo = teams.away.logo,
            shortName = teams.away.name.take(3).uppercase()
        ),
        homeScore = goals.home ?: 0,
        awayScore = goals.away ?: 0,
        matchMinute = when {
            fixture.status.short == "LIVE" || fixture.status.short == "1H" || fixture.status.short == "2H" -> {
                "${fixture.status.elapsed ?: 0}'"
            }
            fixture.status.short == "HT" -> "HT"
            fixture.status.short == "FT" -> "FT"
            fixture.status.short == "NS" -> "VS"
            else -> fixture.status.short
        },
        matchStatus = fixture.status.long,
        league = League(
            id = league.id,
            name = league.name,
            logo = league.logo,
            country = league.country
        ),
        isLive = fixture.status.short in listOf("LIVE", "1H", "2H", "HT"),
        kickoffTime = fixture.date
    )
}