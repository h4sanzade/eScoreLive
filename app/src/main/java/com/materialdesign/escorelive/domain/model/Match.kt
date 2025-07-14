package com.materialdesign.escorelive.domain.model

data class Match(
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
