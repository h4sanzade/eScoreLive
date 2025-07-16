package com.materialdesign.escorelive.data.remote.repository

import com.materialdesign.escorelive.domain.model.Match
import com.materialdesign.escorelive.domain.model.Team
import com.materialdesign.escorelive.data.remote.FootballApiService
import com.materialdesign.escorelive.data.remote.mappers.FixtureMapper
import com.materialdesign.escorelive.data.remote.mappers.TeamMapper
import com.materialdesign.escorelive.ui.matchdetail.MatchEvent
import com.materialdesign.escorelive.ui.matchdetail.LineupPlayer
import com.materialdesign.escorelive.ui.matchdetail.MatchStatistics
import com.materialdesign.escorelive.presentation.search.TeamSearchResult
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.google.gson.Gson
import com.materialdesign.escorelive.data.remote.StatisticItem
import com.materialdesign.escorelive.data.remote.TeamStanding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@Singleton
class FootballRepository @Inject constructor(
    private val apiService: FootballApiService
) {

    // Cache for teams to avoid repeated API calls
    private val teamsCache = mutableMapOf<Long, Team>()
    private val leagueTeamsCache = mutableMapOf<Int, List<Team>>()
    private val searchCache = mutableMapOf<String, List<TeamSearchResult>>()

    // Genişletilmiş popüler ligler listesi (daha fazla ülke ve lig)
    private val popularLeagues = listOf(
        // Avrupa'nın büyük ligleri
        LeagueInfo(39, "Premier League", "England", 2024),
        LeagueInfo(140, "La Liga", "Spain", 2024),
        LeagueInfo(78, "Bundesliga", "Germany", 2024),
        LeagueInfo(135, "Serie A", "Italy", 2024),
        LeagueInfo(61, "Ligue 1", "France", 2024),
        LeagueInfo(94, "Primeira Liga", "Portugal", 2024),
        LeagueInfo(88, "Eredivisie", "Netherlands", 2024),
        LeagueInfo(144, "Jupiler Pro League", "Belgium", 2024),
        LeagueInfo(203, "Super Lig", "Turkey", 2024),
        LeagueInfo(197, "Greek Super League", "Greece", 2024),
        LeagueInfo(218, "Swiss Super League", "Switzerland", 2024),
        LeagueInfo(113, "Allsvenskan", "Sweden", 2024),
        LeagueInfo(103, "Eliteserien", "Norway", 2024),
        LeagueInfo(119, "Superligaen", "Denmark", 2024),
        LeagueInfo(345, "Czech First League", "Czech Republic", 2024),
        LeagueInfo(268, "Ekstraklasa", "Poland", 2024),
        LeagueInfo(271, "First League", "Bulgaria", 2024),
        LeagueInfo(318, "A Liga", "Slovenia", 2024),
        LeagueInfo(333, "Prva Liga", "Serbia", 2024),

        // Doğu Avrupa ve Balkanlar
        LeagueInfo(266, "Premier League", "Ukraine", 2024),
        LeagueInfo(235, "Premier League", "Russia", 2024),
        LeagueInfo(327, "Liga I", "Romania", 2024),
        LeagueInfo(276, "HNL", "Croatia", 2024),
        LeagueInfo(332, "1. Liga", "Slovakia", 2024),
        LeagueInfo(274, "NB I", "Hungary", 2024),

        // Orta Doğu
        LeagueInfo(253, "Saudi Pro League", "Saudi Arabia", 2024),
        LeagueInfo(202, "UAE Pro League", "UAE", 2024),
        LeagueInfo(278, "Persian Gulf Pro League", "Iran", 2024),
        LeagueInfo(179, "Qatar Stars League", "Qatar", 2024),
        LeagueInfo(267, "Premier League", "Israel", 2024),

        // Asya
        LeagueInfo(98, "J1 League", "Japan", 2024),
        LeagueInfo(292, "K League 1", "South Korea", 2024),
        LeagueInfo(169, "Super League", "China", 2024),
        LeagueInfo(323, "Indian Super League", "India", 2024),
        LeagueInfo(188, "A-League", "Australia", 2024),
        LeagueInfo(286, "Thai League 1", "Thailand", 2024),
        LeagueInfo(289, "V.League 1", "Vietnam", 2024),
        LeagueInfo(299, "Malaysia Super League", "Malaysia", 2024),
        LeagueInfo(188, "Liga 1", "Indonesia", 2024),

        // Amerika
        LeagueInfo(253, "MLS", "USA", 2024),
        LeagueInfo(71, "Serie A", "Brazil", 2024),
        LeagueInfo(128, "Liga Profesional", "Argentina", 2024),
        LeagueInfo(265, "Primera División", "Chile", 2024),
        LeagueInfo(239, "Liga MX", "Mexico", 2024),
        LeagueInfo(274, "Primera A", "Colombia", 2024),

        // Afrika
        LeagueInfo(307, "Premier Division", "South Africa", 2024),
        LeagueInfo(233, "Premier League", "Egypt", 2024),
        LeagueInfo(200, "Botola Pro", "Morocco", 2024),
        LeagueInfo(244, "Ligue 1", "Tunisia", 2024),
        LeagueInfo(274, "Ligue 1", "Algeria", 2024),

        // Uluslararası Turnuvalar
        LeagueInfo(2, "Champions League", "UEFA", 2024),
        LeagueInfo(3, "Europa League", "UEFA", 2024),
        LeagueInfo(848, "Conference League", "UEFA", 2024),
        LeagueInfo(4, "Euro Championship", "UEFA", 2024),
        LeagueInfo(1, "World Cup", "FIFA", 2024),
        LeagueInfo(45, "Copa America", "CONMEBOL", 2024),
        LeagueInfo(5, "Nations League", "UEFA", 2024),

        // Azerbaycan ve çevre ülkeler
        LeagueInfo(342, "Premier League", "Azerbaijan", 2024),
        LeagueInfo(327, "Erovnuli Liga", "Georgia", 2024),
        LeagueInfo(342, "Premier League", "Armenia", 2024),
        LeagueInfo(327, "Superliga", "Kazakhstan", 2024),

        // Türkiye çevre ligleri
        LeagueInfo(204, "TFF 1. Lig", "Turkey", 2024),
        LeagueInfo(205, "TFF 2. Lig", "Turkey", 2024),
        LeagueInfo(206, "TFF 3. Lig", "Turkey", 2024)
    )

    data class LeagueInfo(
        val id: Int,
        val name: String,
        val country: String,
        val season: Int
    )

    // GENİŞLETİLMİŞ API TABANLI TAKIM ARAMA FONKSİYONU
    suspend fun searchTeamsAdvanced(query: String): Result<List<TeamSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FootballRepository", "Starting enhanced API search for: '$query'")

                if (query.length < 2) {
                    Log.d("FootballRepository", "Query too short, returning empty")
                    return@withContext Result.success(emptyList())
                }

                // Önce cache'den kontrol et
                val cacheKey = query.lowercase()
                searchCache[cacheKey]?.let { cachedResults ->
                    if (cachedResults.isNotEmpty()) {
                        Log.d("FootballRepository", "Found ${cachedResults.size} teams in cache for '$query'")
                        return@withContext Result.success(cachedResults)
                    }
                }

                val allResults = mutableSetOf<TeamSearchResult>()

                // 1. API'den gerçek arama yap
                try {
                    Log.d("FootballRepository", "Calling API search for: '$query'")
                    val response = apiService.searchTeams(searchQuery = query)

                    if (response.isSuccessful) {
                        val apiResults = response.body()?.response?.mapNotNull { teamSearchData ->
                            val team = TeamMapper.mapToTeam(teamSearchData.team)
                            cacheTeam(team)

                            findTeamLeague(team.id)?.let { leagueInfo ->
                                TeamSearchResult(
                                    team = team,
                                    leagueId = leagueInfo.id,
                                    leagueName = leagueInfo.name,
                                    season = leagueInfo.season
                                )
                            }
                        } ?: emptyList()

                        allResults.addAll(apiResults)
                        Log.d("FootballRepository", "API search returned ${apiResults.size} teams")
                    } else {
                        Log.w("FootballRepository", "API search failed with code: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.w("FootballRepository", "API search failed, trying league-by-league search", e)
                }

                // 2. Eğer API'den yeterli sonuç yoksa, tüm popüler liglerde arama yap
                if (allResults.size < 10) {
                    Log.d("FootballRepository", "API results insufficient, searching in popular leagues")

                    // Popüler liglerde takım arama
                    val leagueSearchResults = searchInPopularLeagues(query)
                    allResults.addAll(leagueSearchResults)
                    Log.d("FootballRepository", "League search added ${leagueSearchResults.size} teams")
                }

                // 3. Eğer hala yeterli sonuç yoksa manuel takımları ekle
                if (allResults.size < 5) {
                    val manualResults = searchManualTeams(query)
                    allResults.addAll(manualResults)
                    Log.d("FootballRepository", "Added ${manualResults.size} manual results")
                }

                // Sonuçları filtrele ve sırala
                val filteredResults = allResults
                    .filter { result ->
                        result.team.name.contains(query, ignoreCase = true)
                    }
                    .distinctBy { it.team.id }
                    .sortedWith(compareBy<TeamSearchResult> { result ->
                        when {
                            result.team.name.equals(query, ignoreCase = true) -> 0
                            result.team.name.startsWith(query, ignoreCase = true) -> 1
                            else -> 2
                        }
                    }.thenBy { it.team.name })
                    .take(20) // Daha fazla sonuç göster

                // Cache'e kaydet
                searchCache[cacheKey] = filteredResults

                Log.d("FootballRepository", "Final result: ${filteredResults.size} teams for '$query'")
                Result.success(filteredResults)

            } catch (e: Exception) {
                Log.e("FootballRepository", "Exception in searchTeamsAdvanced for '$query'", e)
                Result.failure(e)
            }
        }
    }

    // Popüler liglerde takım arama fonksiyonu
    private suspend fun searchInPopularLeagues(query: String): List<TeamSearchResult> {
        val results = mutableListOf<TeamSearchResult>()

        // Öncelikli ligler (büyük ligler)
        val priorityLeagues = listOf(39, 140, 78, 135, 61, 203, 342, 2, 3)

        // Önce öncelikli liglerde ara
        for (league in popularLeagues.filter { it.id in priorityLeagues }) {
            try {
                val response = apiService.getTeamsByLeague(leagueId = league.id, season = league.season)
                if (response.isSuccessful) {
                    val teams = response.body()?.response?.mapNotNull { teamData ->
                        val team = TeamMapper.mapToTeam(teamData.team)
                        if (team.name.contains(query, ignoreCase = true)) {
                            cacheTeam(team)
                            TeamSearchResult(
                                team = team,
                                leagueId = league.id,
                                leagueName = league.name,
                                season = league.season
                            )
                        } else null
                    } ?: emptyList()

                    results.addAll(teams)

                    // Eğer yeterli sonuç bulduysa diğer ligleri arama
                    if (results.size >= 15) break
                }
            } catch (e: Exception) {
                Log.w("FootballRepository", "Error searching in league ${league.name}", e)
                continue
            }
        }

        // Eğer hala yeterli sonuç yoksa diğer liglerde de ara
        if (results.size < 10) {
            for (league in popularLeagues.filter { it.id !in priorityLeagues }.take(10)) {
                try {
                    val response = apiService.getTeamsByLeague(leagueId = league.id, season = league.season)
                    if (response.isSuccessful) {
                        val teams = response.body()?.response?.mapNotNull { teamData ->
                            val team = TeamMapper.mapToTeam(teamData.team)
                            if (team.name.contains(query, ignoreCase = true)) {
                                cacheTeam(team)
                                TeamSearchResult(
                                    team = team,
                                    leagueId = league.id,
                                    leagueName = league.name,
                                    season = league.season
                                )
                            } else null
                        } ?: emptyList()

                        results.addAll(teams)

                        if (results.size >= 15) break
                    }
                } catch (e: Exception) {
                    Log.w("FootballRepository", "Error searching in league ${league.name}", e)
                    continue
                }
            }
        }

        return results
    }

    // GENİŞLETİLMİŞ API TABANLI ÖNERİ FONKSİYONU
    suspend fun getTeamSuggestions(query: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FootballRepository", "Getting enhanced API-based suggestions for: '$query'")

                if (query.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val suggestions = mutableSetOf<String>()

                // ÖNCE API'DEN GERÇEK ARAMA YAP
                try {
                    val searchResult = searchTeamsAdvanced(query)

                    searchResult.onSuccess { results ->
                        // Arama sonuçlarından takım isimlerini al
                        results.forEach { teamSearchResult ->
                            val teamName = teamSearchResult.team.name

                            // Query ile başlayan veya içeren takımları öneriye ekle
                            if (teamName.contains(query, ignoreCase = true)) {
                                suggestions.add(teamName)
                            }

                            // Takım adının kelimelerini kontrol et
                            teamName.split(" ", "-", "_").forEach { word ->
                                if (word.startsWith(query, ignoreCase = true) && word.length > query.length) {
                                    suggestions.add(word)
                                }
                            }
                        }

                        Log.d("FootballRepository", "API search generated ${suggestions.size} suggestions")
                    }
                } catch (e: Exception) {
                    Log.w("FootballRepository", "API search for suggestions failed", e)
                }

                // Eğer API'den yeterli öneri yoksa popüler takım isimlerini ekle
                if (suggestions.size < 5) {
                    val popularTeamNames = getPopularTeamNames()
                    popularTeamNames.forEach { teamName ->
                        if (teamName.contains(query, ignoreCase = true)) {
                            suggestions.add(teamName)
                        }

                        // Takım adının kelimelerini kontrol et
                        teamName.split(" ", "-", "_").forEach { word ->
                            if (word.startsWith(query, ignoreCase = true) && word.length > query.length) {
                                suggestions.add(word)
                            }
                        }
                    }
                    Log.d("FootballRepository", "Popular teams added suggestions, total: ${suggestions.size}")
                }

                // Önerileri sırala ve sınırla
                val sortedSuggestions = suggestions
                    .filter { it.length >= query.length } // En az query kadar uzun olmalı
                    .sortedWith(compareBy<String> { suggestion ->
                        when {
                            suggestion.equals(query, ignoreCase = true) -> 0
                            suggestion.startsWith(query, ignoreCase = true) -> 1
                            suggestion.contains(query, ignoreCase = true) -> 2
                            else -> 3
                        }
                    }.thenBy { it.length }.thenBy { it })
                    .take(10) // Daha fazla öneri

                Log.d("FootballRepository", "Final enhanced suggestions (${sortedSuggestions.size}): $sortedSuggestions")
                Result.success(sortedSuggestions)

            } catch (e: Exception) {
                Log.e("FootballRepository", "Exception in getTeamSuggestions", e)
                Result.failure(e)
            }
        }
    }

    // Popüler takım isimlerini getir
    private fun getPopularTeamNames(): List<String> {
        return listOf(
            // İngilizce takımlar
            "Arsenal", "Chelsea", "Manchester United", "Manchester City", "Liverpool", "Tottenham",
            "Leicester City", "West Ham", "Newcastle", "Brighton", "Aston Villa", "Crystal Palace",

            // İspanyol takımlar
            "Barcelona", "Real Madrid", "Atletico Madrid", "Sevilla", "Valencia", "Villarreal",
            "Athletic Bilbao", "Real Sociedad", "Betis", "Espanyol",

            // Alman takımlar
            "Bayern Munich", "Borussia Dortmund", "RB Leipzig", "Bayer Leverkusen", "Borussia Monchengladbach",
            "Eintracht Frankfurt", "Wolfsburg", "Schalke", "Werder Bremen", "Hamburg",

            // İtalyan takımlar
            "Juventus", "Inter Milan", "AC Milan", "Roma", "Napoli", "Lazio", "Atalanta", "Fiorentina",
            "Torino", "Genoa", "Sampdoria", "Bologna",

            // Fransız takımlar
            "Paris Saint-Germain", "Marseille", "Lyon", "Monaco", "Lille", "Nice", "Rennes", "Nantes",
            "Bordeaux", "Saint-Etienne",

            // Türk takımlar
            "Galatasaray", "Fenerbahce", "Besiktas", "Trabzonspor", "Basaksehir", "Antalyaspor",
            "Konyaspor", "Sivasspor", "Alanyaspor", "Rizespor", "Kayserispor", "Samsunspor",
            "Gaziantepspor", "Denizlispor", "Goztepe", "Kasimpasa", "Yeni Malatyaspor",

            // Azerbaycan takımlar
            "Qarabag", "Neftchi", "Sabah", "Zira", "Sumqayit", "Kapaz", "Sabail", "Turan Tovuz",

            // Diğer popüler takımlar
            "Ajax", "PSV", "Feyenoord", "Anderlecht", "Club Brugge", "Benfica", "Porto", "Sporting",
            "Celtic", "Rangers", "Olympiacos", "Panathinaikos", "Red Star Belgrade", "Dynamo Kiev",
            "Shakhtar Donetsk", "Sparta Prague", "Slavia Prague", "Legia Warsaw", "Dinamo Zagreb"
        )
    }

    // GENİŞLETİLMİŞ Manuel takım listesi
    private fun searchManualTeams(query: String): List<TeamSearchResult> {
        val manualTeams = listOf(
            // Premier League
            ManualTeam(42, "Arsenal", 39, "Premier League", "https://media.api-sports.io/football/teams/42.png"),
            ManualTeam(49, "Chelsea", 39, "Premier League", "https://media.api-sports.io/football/teams/49.png"),
            ManualTeam(33, "Manchester United", 39, "Premier League", "https://media.api-sports.io/football/teams/33.png"),
            ManualTeam(50, "Manchester City", 39, "Premier League", "https://media.api-sports.io/football/teams/50.png"),
            ManualTeam(40, "Liverpool", 39, "Premier League", "https://media.api-sports.io/football/teams/40.png"),
            ManualTeam(47, "Tottenham", 39, "Premier League", "https://media.api-sports.io/football/teams/47.png"),
            ManualTeam(66, "Aston Villa", 39, "Premier League", "https://media.api-sports.io/football/teams/66.png"),
            ManualTeam(34, "Newcastle", 39, "Premier League", "https://media.api-sports.io/football/teams/34.png"),
            ManualTeam(51, "Brighton", 39, "Premier League", "https://media.api-sports.io/football/teams/51.png"),
            ManualTeam(48, "West Ham", 39, "Premier League", "https://media.api-sports.io/football/teams/48.png"),

            // La Liga
            ManualTeam(529, "Barcelona", 140, "La Liga", "https://media.api-sports.io/football/teams/529.png"),
            ManualTeam(541, "Real Madrid", 140, "La Liga", "https://media.api-sports.io/football/teams/541.png"),
            ManualTeam(530, "Atletico Madrid", 140, "La Liga", "https://media.api-sports.io/football/teams/530.png"),
            ManualTeam(536, "Sevilla", 140, "La Liga", "https://media.api-sports.io/football/teams/536.png"),
            ManualTeam(532, "Valencia", 140, "La Liga", "https://media.api-sports.io/football/teams/532.png"),
            ManualTeam(533, "Villarreal", 140, "La Liga", "https://media.api-sports.io/football/teams/533.png"),
            ManualTeam(531, "Athletic Bilbao", 140, "La Liga", "https://media.api-sports.io/football/teams/531.png"),
            ManualTeam(548, "Real Sociedad", 140, "La Liga", "https://media.api-sports.io/football/teams/548.png"),

            // Bundesliga
            ManualTeam(157, "Bayern Munich", 78, "Bundesliga", "https://media.api-sports.io/football/teams/157.png"),
            ManualTeam(165, "Borussia Dortmund", 78, "Bundesliga", "https://media.api-sports.io/football/teams/165.png"),
            ManualTeam(173, "RB Leipzig", 78, "Bundesliga", "https://media.api-sports.io/football/teams/173.png"),
            ManualTeam(168, "Bayer Leverkusen", 78, "Bundesliga", "https://media.api-sports.io/football/teams/168.png"),
            ManualTeam(161, "Borussia Monchengladbach", 78, "Bundesliga", "https://media.api-sports.io/football/teams/161.png"),
            ManualTeam(169, "Eintracht Frankfurt", 78, "Bundesliga", "https://media.api-sports.io/football/teams/169.png"),

            // Serie A
            ManualTeam(496, "Juventus", 135, "Serie A", "https://media.api-sports.io/football/teams/496.png"),
            ManualTeam(505, "Inter Milan", 135, "Serie A", "https://media.api-sports.io/football/teams/505.png"),
            ManualTeam(489, "AC Milan", 135, "Serie A", "https://media.api-sports.io/football/teams/489.png"),
            ManualTeam(497, "Roma", 135, "Serie A", "https://media.api-sports.io/football/teams/497.png"),
            ManualTeam(492, "Napoli", 135, "Serie A", "https://media.api-sports.io/football/teams/492.png"),
            ManualTeam(487, "Lazio", 135, "Serie A", "https://media.api-sports.io/football/teams/487.png"),
            ManualTeam(499, "Atalanta", 135, "Serie A", "https://media.api-sports.io/football/teams/499.png"),

            // Ligue 1
            ManualTeam(85, "Paris Saint-Germain", 61, "Ligue 1", "https://media.api-sports.io/football/teams/85.png"),
            ManualTeam(79, "Marseille", 61, "Ligue 1", "https://media.api-sports.io/football/teams/79.png"),
            ManualTeam(80, "Lyon", 61, "Ligue 1", "https://media.api-sports.io/football/teams/80.png"),
            ManualTeam(82, "Monaco", 61, "Ligue 1", "https://media.api-sports.io/football/teams/82.png"),
            ManualTeam(94, "Lille", 61, "Ligue 1", "https://media.api-sports.io/football/teams/94.png"),

            // Türkiye Super Lig
            ManualTeam(559, "Galatasaray", 203, "Super Lig", "https://media.api-sports.io/football/teams/559.png"),
            ManualTeam(562, "Fenerbahce", 203, "Super Lig", "https://media.api-sports.io/football/teams/562.png"),
            ManualTeam(558, "Besiktas", 203, "Super Lig", "https://media.api-sports.io/football/teams/558.png"),
            ManualTeam(564, "Trabzonspor", 203, "Super Lig", "https://media.api-sports.io/football/teams/564.png"),
            ManualTeam(612, "Basaksehir", 203, "Super Lig", "https://media.api-sports.io/football/teams/612.png"),
            ManualTeam(565, "Antalyaspor", 203, "Super Lig", "https://media.api-sports.io/football/teams/565.png"),
            ManualTeam(567, "Konyaspor", 203, "Super Lig", "https://media.api-sports.io/football/teams/567.png"),
            ManualTeam(563, "Sivasspor", 203, "Super Lig", "https://media.api-sports.io/football/teams/563.png"),
            ManualTeam(579, "Alanyaspor", 203, "Super Lig", "https://media.api-sports.io/football/teams/579.png"),
            ManualTeam(566, "Rizespor", 203, "Super Lig", "https://media.api-sports.io/football/teams/566.png"),
            ManualTeam(568, "Kayserispor", 203, "Super Lig", "https://media.api-sports.io/football/teams/568.png"),
            ManualTeam(570, "Samsunspor", 203, "Super Lig", "https://media.api-sports.io/football/teams/570.png"),
            ManualTeam(571, "Gaziantepspor", 203, "Super Lig", "https://media.api-sports.io/football/teams/571.png"),
            ManualTeam(572, "Goztepe", 203, "Super Lig", "https://media.api-sports.io/football/teams/572.png"),
            ManualTeam(573, "Kasimpasa", 203, "Super Lig", "https://media.api-sports.io/football/teams/573.png"),

            // Azerbaycan Premier League
            ManualTeam(553, "Qarabag", 342, "Premier League", "https://media.api-sports.io/football/teams/553.png"),
            ManualTeam(554, "Neftchi", 342, "Premier League", "https://media.api-sports.io/football/teams/554.png"),
            ManualTeam(555, "Sabah", 342, "Premier League", "https://media.api-sports.io/football/teams/555.png"),
            ManualTeam(556, "Zira", 342, "Premier League", "https://media.api-sports.io/football/teams/556.png"),
            ManualTeam(557, "Sumqayit", 342, "Premier League", "https://media.api-sports.io/football/teams/557.png"),
            ManualTeam(558, "Kapaz", 342, "Premier League", "https://media.api-sports.io/football/teams/558.png"),
            ManualTeam(559, "Sabail", 342, "Premier League", "https://media.api-sports.io/football/teams/559.png"),

            // Diğer popüler takımlar
            ManualTeam(194, "Ajax", 88, "Eredivisie", "https://media.api-sports.io/football/teams/194.png"),
            ManualTeam(195, "PSV", 88, "Eredivisie", "https://media.api-sports.io/football/teams/195.png"),
            ManualTeam(196, "Feyenoord", 88, "Eredivisie", "https://media.api-sports.io/football/teams/196.png"),
            ManualTeam(211, "Benfica", 94, "Primeira Liga", "https://media.api-sports.io/football/teams/211.png"),
            ManualTeam(212, "Porto", 94, "Primeira Liga", "https://media.api-sports.io/football/teams/212.png"),
            ManualTeam(213, "Sporting", 94, "Primeira Liga", "https://media.api-sports.io/football/teams/213.png")
        )

        return manualTeams
            .filter { it.name.contains(query, ignoreCase = true) }
            .map { manualTeam ->
                val team = Team(
                    id = manualTeam.id.toLong(),
                    name = manualTeam.name,
                    logo = manualTeam.logoUrl,
                    shortName = manualTeam.name.take(3).uppercase()
                )
                cacheTeam(team)

                TeamSearchResult(
                    team = team,
                    leagueId = manualTeam.leagueId,
                    leagueName = manualTeam.leagueName,
                    season = 2024
                )
            }
    }

    data class ManualTeam(
        val id: Int,
        val name: String,
        val leagueId: Int,
        val leagueName: String,
        val logoUrl: String
    )

    private suspend fun findTeamLeague(teamId: Long): LeagueInfo? {
        // Genişletilmiş manuel takımlar için ID tabanlı mapping
        return when (teamId.toInt()) {
            // Premier League
            in listOf(42, 49, 33, 50, 40, 47, 66, 34, 51, 48) -> LeagueInfo(39, "Premier League", "England", 2024)
            // La Liga
            in listOf(529, 541, 530, 536, 532, 533, 531, 548) -> LeagueInfo(140, "La Liga", "Spain", 2024)
            // Bundesliga
            in listOf(157, 165, 173, 168, 161, 169) -> LeagueInfo(78, "Bundesliga", "Germany", 2024)
            // Serie A
            in listOf(496, 505, 489, 497, 492, 487, 499) -> LeagueInfo(135, "Serie A", "Italy", 2024)
            // Ligue 1
            in listOf(85, 79, 80, 82, 94) -> LeagueInfo(61, "Ligue 1", "France", 2024)
            // Türkiye Super Lig
            in listOf(559, 562, 558, 564, 612, 565, 567, 563, 579, 566, 568, 570, 571, 572, 573) -> LeagueInfo(203, "Super Lig", "Turkey", 2024)
            // Azerbaycan Premier League
            in listOf(553, 554, 555, 556, 557, 558, 559) -> LeagueInfo(342, "Premier League", "Azerbaijan", 2024)
            // Eredivisie
            in listOf(194, 195, 196) -> LeagueInfo(88, "Eredivisie", "Netherlands", 2024)
            // Primeira Liga
            in listOf(211, 212, 213) -> LeagueInfo(94, "Primeira Liga", "Portugal", 2024)
            else -> {
                // API'den kontrol et
                for (league in popularLeagues) {
                    try {
                        val response = apiService.getTeamsByLeague(leagueId = league.id, season = league.season)
                        if (response.isSuccessful) {
                            val teamExists = response.body()?.response?.any { it.team.id == teamId } == true
                            if (teamExists) {
                                return league
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
                popularLeagues.firstOrNull() // Fallback
            }
        }
    }

    // Mevcut fonksiyonları güncelle
    suspend fun searchTeams(query: String): Result<List<Team>> {
        return searchTeamsAdvanced(query).map { results ->
            results.map { it.team }
        }
    }

    // HIZLI ARAMA - Suggestion'a tıklandığında
    suspend fun searchTeamByExactName(teamName: String): Result<List<TeamSearchResult>> {
        Log.d("FootballRepository", "searchTeamByExactName called with: '$teamName'")

        return withContext(Dispatchers.IO) {
            try {
                val result = searchTeamsAdvanced(teamName)

                result.fold(
                    onSuccess = { results ->
                        // Tam eşleşme öncelikli
                        val exactMatch = results.find {
                            it.team.name.equals(teamName, ignoreCase = true)
                        }

                        if (exactMatch != null) {
                            Log.d("FootballRepository", "Found exact match for '$teamName'")
                            Result.success(listOf(exactMatch))
                        } else if (results.isNotEmpty()) {
                            Log.d("FootballRepository", "Found similar match for '$teamName'")
                            Result.success(results.take(1))
                        } else {
                            Log.d("FootballRepository", "No matches found for '$teamName'")
                            Result.success(emptyList())
                        }
                    },
                    onFailure = { exception ->
                        Log.e("FootballRepository", "Exact search failed for '$teamName'", exception)
                        Result.failure(exception)
                    }
                )

            } catch (e: Exception) {
                Log.e("FootballRepository", "Exception in searchTeamByExactName", e)
                Result.failure(e)
            }
        }
    }

    // Cache temizleme fonksiyonu
    fun clearSearchCache() {
        searchCache.clear()
        Log.d("FootballRepository", "Search cache cleared")
    }

    private fun cacheTeam(team: Team) {
        teamsCache[team.id] = team
    }

    fun getCachedTeam(teamId: Long): Team? {
        return teamsCache[teamId]
    }

    // Diğer metodlar - Basitleştirilmiş hata yönetimi ile
    suspend fun getLiveMatches(): Result<List<Match>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FootballRepository", "Fetching live matches...")
                val response = apiService.getLiveFixtures()

                if (response.isSuccessful) {
                    val fixtures = response.body()?.response ?: emptyList()
                    Log.d("FootballRepository", "API returned ${fixtures.size} live fixtures")

                    val liveMatches = fixtures.mapNotNull { fixture ->
                        try {
                            val match = FixtureMapper.mapToMatch(fixture)
                            cacheTeam(match.homeTeam)
                            cacheTeam(match.awayTeam)
                            match
                        } catch (e: Exception) {
                            Log.e("FootballRepository", "Error converting fixture", e)
                            null
                        }
                    }

                    Result.success(liveMatches)
                } else {
                    Log.e("FootballRepository", "API Error: ${response.code()}")
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("FootballRepository", "Exception in getLiveMatches", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getMatchesByDate(date: String): Result<List<Match>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFixturesByDate(date = date)
                if (response.isSuccessful) {
                    val fixtures = response.body()?.response ?: emptyList()
                    val matches = fixtures.mapNotNull { fixture ->
                        try {
                            val match = FixtureMapper.mapToMatch(fixture)
                            cacheTeam(match.homeTeam)
                            cacheTeam(match.awayTeam)
                            match
                        } catch (e: Exception) {
                            null
                        }
                    }
                    Result.success(matches)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMatchesByLeague(leagueId: Int, season: Int): Result<List<Match>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFixturesByLeague(leagueId = leagueId, season = season)
                if (response.isSuccessful) {
                    val fixtures = response.body()?.response ?: emptyList()
                    val matches = fixtures.mapNotNull { fixture ->
                        try {
                            val match = FixtureMapper.mapToMatch(fixture)
                            cacheTeam(match.homeTeam)
                            cacheTeam(match.awayTeam)
                            match
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val leagueTeams = mutableSetOf<Team>()
                    matches.forEach { match ->
                        leagueTeams.add(match.homeTeam)
                        leagueTeams.add(match.awayTeam)
                    }
                    leagueTeamsCache[leagueId] = leagueTeams.toList()

                    Result.success(matches)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMatchDetails(matchId: Long): Result<Match> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFixtureById(fixtureId = matchId)
                if (response.isSuccessful) {
                    val fixture = response.body()?.response?.firstOrNull()
                    if (fixture != null) {
                        val match = FixtureMapper.mapToMatch(fixture)
                        cacheTeam(match.homeTeam)
                        cacheTeam(match.awayTeam)
                        Result.success(match)
                    } else {
                        Result.failure(Exception("Match not found"))
                    }
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getH2HMatches(homeTeamId: Long, awayTeamId: Long): Result<List<Match>> {
        return withContext(Dispatchers.IO) {
            try {
                val h2h = "$homeTeamId-$awayTeamId"
                val response = apiService.getH2HMatches(h2h = h2h)
                if (response.isSuccessful) {
                    val fixtures = response.body()?.response ?: emptyList()
                    val h2hMatches = fixtures.mapNotNull { fixture ->
                        try {
                            FixtureMapper.mapToMatch(fixture)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    Result.success(h2hMatches)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMatchEvents(matchId: Long): Result<List<MatchEvent>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMatchEvents(fixtureId = matchId)
                if (response.isSuccessful) {
                    val events = response.body()?.response ?: emptyList()
                    val matchEvents = events.mapNotNull { eventData ->
                        try {
                            MatchEvent(
                                id = eventData.id ?: 0L,
                                minute = eventData.time.elapsed,
                                type = eventData.type,
                                detail = eventData.detail,
                                player = eventData.player.name,
                                assistPlayer = eventData.assist?.name,
                                team = eventData.team.name,
                                isHomeTeam = true
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    Result.success(matchEvents)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMatchLineup(matchId: Long): Result<List<LineupPlayer>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMatchLineups(fixtureId = matchId)
                if (response.isSuccessful) {
                    val lineups = response.body()?.response ?: emptyList()
                    val players = mutableListOf<LineupPlayer>()

                    lineups.forEachIndexed { teamIndex, lineup ->
                        val isHomeTeam = teamIndex == 0

                        lineup.startXI.forEach { startingPlayer ->
                            players.add(
                                LineupPlayer(
                                    id = startingPlayer.player.id,
                                    name = startingPlayer.player.name,
                                    number = startingPlayer.player.number,
                                    position = startingPlayer.player.pos,
                                    isStarting = true,
                                    isHomeTeam = isHomeTeam
                                )
                            )
                        }

                        lineup.substitutes.forEach { substitute ->
                            players.add(
                                LineupPlayer(
                                    id = substitute.player.id,
                                    name = substitute.player.name,
                                    number = substitute.player.number,
                                    position = substitute.player.pos,
                                    isStarting = false,
                                    isHomeTeam = isHomeTeam
                                )
                            )
                        }
                    }
                    Result.success(players)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMatchStatistics(matchId: Long): Result<MatchStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMatchStatistics(fixtureId = matchId)
                if (response.isSuccessful) {
                    val stats = response.body()?.response ?: emptyList()
                    if (stats.size >= 2) {
                        val homeStats = stats[0].statistics
                        val awayStats = stats[1].statistics

                        val matchStats = MatchStatistics(
                            homePossession = getStatValue(homeStats, "Ball Possession")?.toString()?.replace("%", "")?.toIntOrNull() ?: 0,
                            awayPossession = getStatValue(awayStats, "Ball Possession")?.toString()?.replace("%", "")?.toIntOrNull() ?: 0,
                            homeShots = getStatValue(homeStats, "Total Shots")?.toString()?.toIntOrNull() ?: 0,
                            awayShots = getStatValue(awayStats, "Total Shots")?.toString()?.toIntOrNull() ?: 0,
                            homeShotsOnTarget = getStatValue(homeStats, "Shots on Goal")?.toString()?.toIntOrNull() ?: 0,
                            awayShotsOnTarget = getStatValue(awayStats, "Shots on Goal")?.toString()?.toIntOrNull() ?: 0,
                            homeCorners = getStatValue(homeStats, "Corner Kicks")?.toString()?.toIntOrNull() ?: 0,
                            awayCorners = getStatValue(awayStats, "Corner Kicks")?.toString()?.toIntOrNull() ?: 0,
                            homeYellowCards = getStatValue(homeStats, "Yellow Cards")?.toString()?.toIntOrNull() ?: 0,
                            awayYellowCards = getStatValue(awayStats, "Yellow Cards")?.toString()?.toIntOrNull() ?: 0,
                            homeRedCards = getStatValue(homeStats, "Red Cards")?.toString()?.toIntOrNull() ?: 0,
                            awayRedCards = getStatValue(awayStats, "Red Cards")?.toString()?.toIntOrNull() ?: 0,
                            homeFouls = getStatValue(homeStats, "Fouls")?.toString()?.toIntOrNull() ?: 0,
                            awayFouls = getStatValue(awayStats, "Fouls")?.toString()?.toIntOrNull() ?: 0,
                            homeOffsides = getStatValue(homeStats, "Offsides")?.toString()?.toIntOrNull() ?: 0,
                            awayOffsides = getStatValue(awayStats, "Offsides")?.toString()?.toIntOrNull() ?: 0
                        )
                        Result.success(matchStats)
                    } else {
                        Result.failure(Exception("Insufficient statistics data"))
                    }
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getStandings(leagueId: Int, season: Int): Result<List<TeamStanding>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStandings(leagueId = leagueId, season = season)
                if (response.isSuccessful) {
                    val standingsData = response.body()?.response?.firstOrNull()
                    val standings = standingsData?.league?.standings?.firstOrNull() ?: emptyList()
                    Result.success(standings)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getStatValue(statistics: List<StatisticItem>, type: String): Any? {
        return statistics.find { it.type == type }?.value
    }

    // Utility methods
    fun getTeamsCacheSize(): Int = teamsCache.size

    fun getSearchCacheSize(): Int = searchCache.size

    fun clearAllCaches() {
        teamsCache.clear()
        leagueTeamsCache.clear()
        searchCache.clear()
        Log.d("FootballRepository", "All caches cleared")
    }

    // For debugging
    fun getPopularLeagues(): List<LeagueInfo> = popularLeagues.toList()

    fun isTeamCached(teamId: Long): Boolean = teamsCache.containsKey(teamId)

    // Test method for debugging
    fun testFavoriteTeamMatches() {
        Log.d("FootballRepository", "Testing favorite team matches with IDs: $favoriteTeamIds")
        if (favoriteTeamIds.isNotEmpty()) {
            loadFavoriteTeamMatches()
        } else {
            Log.d("FootballRepository", "No favorite teams to test")
        }
    }

    // Method to manually add test favorite teams (for debugging)
    fun addTestFavoriteTeams() {
        favoriteTeamIds.addAll(listOf(42L, 529L, 559L)) // Arsenal, Barcelona, Galatasaray
        Log.d("FootballRepository", "Added test favorite teams: $favoriteTeamIds")
    }

    // Get current matches by status
    fun getCurrentLiveMatches(): List<Match> {
        return emptyList() // Bu method gerçek implementation gerektirir
    }

    fun getCurrentUpcomingMatches(): List<Match> {
        return emptyList() // Bu method gerçek implementation gerektirir
    }

    fun getCurrentFavoriteMatches(): List<Match> {
        return emptyList() // Bu method gerçek implementation gerektirir
    }

    fun getCurrentTodayMatches(): List<Match> {
        return emptyList() // Bu method gerçek implementation gerektirir
    }

    // Force refresh all data
    fun forceRefreshAll() {
        Log.d("FootballRepository", "Force refreshing all data")
        clearAllCaches()
    }

    // Additional utility methods
    fun hasAnyFavoriteTeams(): Boolean {
        return favoriteTeamIds.isNotEmpty()
    }

    fun getFavoriteTeamsCount(): Int {
        return favoriteTeamIds.size
    }

    fun getAllMatchesCount(): Int {
        return 0 // Bu method gerçek implementation gerektirir
    }

    fun getFilteredMatchesCount(): Int {
        return 0 // Bu method gerçek implementation gerektirir
    }

    // Get matches filtered by current tab selection
    fun getMatchesForTab(tabType: String, date: String): Result<List<Match>> {
        return when (tabType.lowercase()) {
            "upcoming" -> getMatchesByDate(date)
            "score", "live" -> getLiveMatches()
            "favorites" -> getFavoriteTeamsMatches(setOf()) // Empty set for now
            else -> getMatchesByDate(date)
        }
    }

    private val favoriteTeamIds = mutableSetOf<Long>()

    private fun loadFavoriteTeamMatches() {
        // Bu method implementation gerektirir
        Log.d("FootballRepository", "Loading favorite team matches")
    }

    override fun onCleared() {
        Log.d("FootballRepository", "FootballRepository onCleared called")
        // Repository cleared
    }
}
return league
}
}
} catch (e: Exception) {
    continue
}
}
null
}
}
}

// Favori takımların maçlarını getir (geçmiş, live, gelecek)
suspend fun getFavoriteTeamsMatches(favoriteTeamIds: Set<Long>): Result<List<Match>> {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("FootballRepository", "Getting matches for ${favoriteTeamIds.size} favorite teams")

            if (favoriteTeamIds.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val allMatches = mutableListOf<Match>()

            // Her favori takım için maçları getir
            for (teamId in favoriteTeamIds) {
                try {
                    // Takımın ligini bul
                    val teamLeague = findTeamLeagueById(teamId)

                    if (teamLeague != null) {
                        // Bu ligdeki tüm maçları getir
                        val leagueMatches = getMatchesByLeague(teamLeague.id, teamLeague.season)

                        leagueMatches.onSuccess { matches ->
                            // Sadece bu takımın maçlarını filtrele
                            val teamMatches = matches.filter { match ->
                                match.homeTeam.id == teamId || match.awayTeam.id == teamId
                            }
                            allMatches.addAll(teamMatches)
                            Log.d("FootballRepository", "Found ${teamMatches.size} matches for team $teamId")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("FootballRepository", "Error getting matches for team $teamId", e)
                    continue
                }
            }

            // Maçları tarihe göre sırala (son maçlar önce)
            val sortedMatches = allMatches
                .distinctBy { it.id }
                .sortedWith(compareByDescending<Match> { match ->
                    try {
                        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault())
                        match.kickoffTime?.let { inputFormat.parse(it)?.time } ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                })

            Log.d("FootballRepository", "Total favorite team matches: ${sortedMatches.size}")
            Result.success(sortedMatches)

        } catch (e: Exception) {
            Log.e("FootballRepository", "Exception in getFavoriteTeamsMatches", e)
            Result.failure(e)
        }
    }
}

// Takım ID'sine göre lige bul
private suspend fun findTeamLeagueById(teamId: Long): LeagueInfo? {
    // Önce manuel mapping
    return when (teamId.toInt()) {
        // Premier League
        in listOf(42, 49, 33, 50, 40, 47, 66, 34, 51, 48) -> LeagueInfo(39, "Premier League", "England", 2024)
        // La Liga
        in listOf(529, 541, 530, 536, 532, 533, 531, 548) -> LeagueInfo(140, "La Liga", "Spain", 2024)
        // Bundesliga
        in listOf(157, 165, 173, 168, 161, 169) -> LeagueInfo(78, "Bundesliga", "Germany", 2024)
        // Serie A
        in listOf(496, 505, 489, 497, 492, 487, 499) -> LeagueInfo(135, "Serie A", "Italy", 2024)
        // Ligue 1
        in listOf(85, 79, 80, 82, 94) -> LeagueInfo(61, "Ligue 1", "France", 2024)
        // Türkiye Super Lig
        in listOf(559, 562, 558, 564, 612, 565, 567, 563, 579, 566, 568, 570, 571, 572, 573) -> LeagueInfo(203, "Super Lig", "Turkey", 2024)
        // Azerbaycan Premier League
        in listOf(553, 554, 555, 556, 557, 558, 559) -> LeagueInfo(342, "Premier League", "Azerbaijan", 2024)
        // Eredivisie
        in listOf(194, 195, 196) -> LeagueInfo(88, "Eredivisie", "Netherlands", 2024)
        // Primeira Liga
        in listOf(211, 212, 213) -> LeagueInfo(94, "Primeira Liga", "Portugal", 2024)
        else -> {
            // API'den kontrol et
            for (league in popularLeagues) {
                try {
                    val response = apiService.getTeamsByLeague(leagueId = league.id, season = league.season)
                    if (response.isSuccessful) {
                        val teamExists = response.body()?.response?.any { it.team.id == teamId } == true
                        if (teamExists) {