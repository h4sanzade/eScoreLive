package com.materialdesign.escorelive.domain.model

data class LineupPlayer(
    val id: Long,
    val name: String,
    val number: Int,
    val position: String,
    val isStarting: Boolean,
    val isHomeTeam: Boolean,
    val rating: Float? = null
)