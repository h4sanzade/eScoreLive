package com.materialdesign.escorelive.domain.model

data class Team(
    val id: Long,
    val name: String,
    val logo: String,
    val shortName: String? = null
)