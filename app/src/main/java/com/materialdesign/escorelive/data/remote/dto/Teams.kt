package com.materialdesign.escorelive.data.remote.dto

data class Teams(
    val home: TeamData,
    val away: TeamData
)

data class TeamData(
    val id: Long,
    val name: String,
    val logo: String
)