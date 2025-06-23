package com.materialdesign.escorelive

import java.text.SimpleDateFormat
import java.util.*

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
    val kickoffTime: String? = null,
    val isFinished: Boolean = false,
    val isUpcoming: Boolean = false,
    val kickoffTimeFormatted: String? = null
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
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())

    val kickoffTimeFormatted = try {
        val date = inputFormat.parse(fixture.date)
        date?.let { timeFormat.format(it) }
    } catch (e: Exception) {
        null
    }

    // Determine match state
    val isLive = fixture.status.short in listOf("LIVE", "1H", "2H", "HT")
    val isFinished = fixture.status.short in listOf("FT", "AET", "PEN", "PST", "CANC", "ABD", "AWD", "WO")
    val isUpcoming = fixture.status.short in listOf("NS", "TBD")

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
            isLive && fixture.status.elapsed != null -> "${fixture.status.elapsed}'"
            fixture.status.short == "HT" -> "HT"
            fixture.status.short == "FT" -> "FT"
            fixture.status.short == "AET" -> "AET"
            fixture.status.short == "PEN" -> "PEN"
            fixture.status.short == "PST" -> "PST"
            fixture.status.short == "CANC" -> "CANC"
            fixture.status.short == "ABD" -> "ABD"
            fixture.status.short == "AWD" -> "AWD"
            fixture.status.short == "WO" -> "WO"
            isUpcoming -> kickoffTimeFormatted ?: "TBD"
            else -> fixture.status.short
        },
        matchStatus = fixture.status.long,
        league = League(
            id = league.id,
            name = league.name,
            logo = league.logo,
            country = league.country
        ),
        isLive = isLive,
        isFinished = isFinished,
        isUpcoming = isUpcoming,
        kickoffTime = fixture.date,
        kickoffTimeFormatted = kickoffTimeFormatted
    )
}