package com.materialdesign.escorelive.data.model

data class TeamSearchResult(
    val id: Long,
    val name: String,
    val logo: String,
    val country: String,
    val code: String? = null,
    val founded: Int? = null,
    val isFavorite: Boolean = false
)

data class FavoriteTeam(
    val id: Long,
    val name: String,
    val logo: String,
    val country: String,
    val dateAdded: Long = System.currentTimeMillis()
)

data class SearchTeamsResponse(
    val response: List<TeamSearchData>
)

data class TeamSearchData(
    val team: TeamInfo,
    val venue: VenueInfo
)

data class TeamInfo(
    val id: Long,
    val name: String,
    val code: String?,
    val country: String,
    val founded: Int?,
    val national: Boolean,
    val logo: String
)

data class VenueInfo(
    val id: Long,
    val name: String,
    val address: String?,
    val city: String,
    val capacity: Int?,
    val surface: String?,
    val image: String?
)