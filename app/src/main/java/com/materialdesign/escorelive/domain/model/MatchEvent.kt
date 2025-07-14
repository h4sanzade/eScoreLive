package com.materialdesign.escorelive.domain.model

data class MatchEvent(
    val id: Long,
    val minute: Int,
    val type: String, // goal, card, substitution
    val detail: String, // Normal Goal, Yellow Card, etc.
    val player: String,
    val assistPlayer: String? = null,
    val team: String,
    val isHomeTeam: Boolean
)
