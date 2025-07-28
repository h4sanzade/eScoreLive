package com.materialdesign.escorelive.data.remote.dto

import com.google.gson.annotations.SerializedName
data class LeaguesApiResponse(
    val response: List<LeagueResponseDto>
)

data class CountriesApiResponse(
    val response: List<CountryDto>
)

data class LeagueResponseDto(
    val league: LeagueDto,
    val country: CountryDto,
    val seasons: List<SeasonDto>
)

data class LeagueDto(
    val id: Int,
    val name: String,
    val type: String, // "League", "Cup"
    val logo: String
)

data class CountryDto(
    val name: String,
    val code: String?,
    val flag: String?
)

data class SeasonDto(
    val year: Int,
    val start: String,
    val end: String,
    val current: Boolean,
    val coverage: CoverageDto?
)

data class CoverageDto(
    val fixtures: FixtureCoverageDto?,
    val standings: Boolean,
    val players: Boolean,
    val top_scorers: Boolean,
    val top_assists: Boolean,
    val top_cards: Boolean,
    val injuries: Boolean,
    val predictions: Boolean,
    val odds: Boolean
)

data class FixtureCoverageDto(
    val events: Boolean,
    val lineups: Boolean,
    val statistics_fixtures: Boolean,
    val statistics_players: Boolean
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
    val isFavorite: Boolean = false,
    val currentSeason: Boolean = false
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