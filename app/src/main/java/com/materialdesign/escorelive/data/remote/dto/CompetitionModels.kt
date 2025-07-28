
package com.materialdesign.escorelive.data.remote.dto

import com.google.gson.annotations.SerializedName


data class LeaguesApiResponse(
    @SerializedName("success")
    val success: Int,
    @SerializedName("data")
    val data: List<CompetitionDto>
)


data class CompetitionDto(
    @SerializedName("league_id")
    val leagueId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("shortCode")
    val shortCode: String?,
    @SerializedName("country")
    val country: String,
    @SerializedName("flag")
    val flag: String?,
    @SerializedName("logo")
    val logo: String?,
    @SerializedName("season")
    val season: String?,
    @SerializedName("season_start")
    val seasonStart: String?,
    @SerializedName("season_end")
    val seasonEnd: String?,
    @SerializedName("is_cup")
    val isCup: Int? = 0,
    @SerializedName("type")
    val type: String? = "league"
)


data class Competition(
    val id: String,
    val name: String,
    val shortCode: String?,
    val country: String,
    val flagUrl: String?,
    val logoUrl: String?,
    val season: String?,
    val seasonStart: String?,
    val seasonEnd: String?,
    val isCup: Boolean = false,
    val type: CompetitionType = CompetitionType.LEAGUE,
    val isTopCompetition: Boolean = false,
    val isFavorite: Boolean = false
)


enum class CompetitionType(val displayName: String) {
    LEAGUE("League"),
    CUP("Cup"),
    TOURNAMENT("Tournament"),
    INTERNATIONAL("International")
}


enum class CompetitionTab {
    TOP,
    REGION,
    FAVORITES
}


data class CompetitionUiState(
    val competitions: List<Competition> = emptyList(),
    val filteredCompetitions: List<Competition> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedTab: CompetitionTab = CompetitionTab.TOP,
    val favoriteCompetitionIds: Set<String> = emptySet()
)