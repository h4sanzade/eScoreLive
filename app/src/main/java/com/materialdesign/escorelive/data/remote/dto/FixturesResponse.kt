package com.materialdesign.escorelive.data.remote.dto

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