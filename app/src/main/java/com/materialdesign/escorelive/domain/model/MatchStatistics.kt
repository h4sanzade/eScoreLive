package com.materialdesign.escorelive.domain.model

data class MatchStatistics(
    val homePossession: Int,
    val awayPossession: Int,
    val homeShots: Int,
    val awayShots: Int,
    val homeShotsOnTarget: Int,
    val awayShotsOnTarget: Int,
    val homeCorners: Int,
    val awayCorners: Int,
    val homeYellowCards: Int,
    val awayYellowCards: Int,
    val homeRedCards: Int,
    val awayRedCards: Int,
    val homeFouls: Int,
    val awayFouls: Int,
    val homeOffsides: Int,
    val awayOffsides: Int
)