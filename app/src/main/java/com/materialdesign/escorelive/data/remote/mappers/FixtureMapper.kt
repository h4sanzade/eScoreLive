package com.materialdesign.escorelive.data.remote.mappers

import com.materialdesign.escorelive.data.remote.dto.FixtureData
import com.materialdesign.escorelive.domain.model.Match
import com.materialdesign.escorelive.domain.model.Team
import com.materialdesign.escorelive.domain.model.League
import java.text.SimpleDateFormat
import java.util.*

object FixtureMapper {

    fun mapToMatch(fixtureData: FixtureData): Match {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())

        val kickoffTimeFormatted = try {
            val date = inputFormat.parse(fixtureData.fixture.date)
            date?.let { timeFormat.format(it) }
        } catch (e: Exception) {
            null
        }

        // Determine match state
        val isLive = fixtureData.fixture.status.short in listOf("LIVE", "1H", "2H", "HT")
        val isFinished = fixtureData.fixture.status.short in listOf("FT", "AET", "PEN", "PST", "CANC", "ABD", "AWD", "WO")
        val isUpcoming = fixtureData.fixture.status.short in listOf("NS", "TBD")

        return Match(
            id = fixtureData.fixture.id,
            homeTeam = TeamMapper.mapToTeam(fixtureData.teams.home),
            awayTeam = TeamMapper.mapToTeam(fixtureData.teams.away),
            homeScore = fixtureData.goals.home ?: 0,
            awayScore = fixtureData.goals.away ?: 0,
            matchMinute = getMatchMinute(fixtureData, isLive, isUpcoming, kickoffTimeFormatted),
            matchStatus = fixtureData.fixture.status.long,
            league = LeagueMapper.mapToLeague(fixtureData.league),
            isLive = isLive,
            isFinished = isFinished,
            isUpcoming = isUpcoming,
            kickoffTime = fixtureData.fixture.date,
            kickoffTimeFormatted = kickoffTimeFormatted
        )
    }

    private fun getMatchMinute(
        fixtureData: FixtureData,
        isLive: Boolean,
        isUpcoming: Boolean,
        kickoffTimeFormatted: String?
    ): String {
        return when {
            isLive && fixtureData.fixture.status.elapsed != null -> "${fixtureData.fixture.status.elapsed}'"
            fixtureData.fixture.status.short == "HT" -> "HT"
            fixtureData.fixture.status.short == "FT" -> "FT"
            fixtureData.fixture.status.short == "AET" -> "AET"
            fixtureData.fixture.status.short == "PEN" -> "PEN"
            fixtureData.fixture.status.short == "PST" -> "PST"
            fixtureData.fixture.status.short == "CANC" -> "CANC"
            fixtureData.fixture.status.short == "ABD" -> "ABD"
            fixtureData.fixture.status.short == "AWD" -> "AWD"
            fixtureData.fixture.status.short == "WO" -> "WO"
            isUpcoming -> kickoffTimeFormatted ?: "TBD"
            else -> fixtureData.fixture.status.short
        }
    }
}