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

    private val teamsCache = mutableMapOf<Long, Team>()
    private val leagueTeamsCache = mutableMapOf<Int, List<Team>>()
    private val searchCache = mutableMapOf<String, List<TeamSearchResult>>()

    private val popularLeagues = listOf(
        LeagueInfo(39, "Premier League", "England", 2024),
        LeagueInfo(140, "La Liga", "Spain", 2024),
        LeagueInfo(78, "Bundesliga", "Germany", 2024),
        LeagueInfo(135, "Serie A", "Italy", 2024),
        LeagueInfo(61, "Ligue 1", "France", 2024),
        LeagueInfo(203, "Super Lig", "Turkey", 2024),
        LeagueInfo(2, "Champions League", "UEFA", 2024),
        LeagueInfo(3, "Europa League", "UEFA", 2024),
        LeagueInfo(342, "Premier League", "Azerbaijan", 2024)
    )

    data class LeagueInfo(
        val id: Int,
        val name: String,
        val country: String,
        val season: Int
    )

    suspend fun searchTeamsAdvanced(query: String): Result<List<TeamSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FootballRepository", "Starting API search for: '$query'")

                if (query.length < 2) {
                    Log.d("FootballRepository", "Query too short, returning empty")
                    return@withContext Result.success(emptyList())
                }

                val cacheKey = query.lowercase()
                searchCache[cacheKey]?.let { cachedResults ->
                    if (cachedResults.isNotEmpty()) {
                        Log.d("FootballRepository", "Found ${cachedResults.size} teams in cache for '$query'")
                        return@withContext Result.success(cachedResults)
                    }
                }

                val allResults = mutableSetOf<TeamSearchResult>()

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
                    Log.w("FootballRepository", "API search failed, using fallback", e)
                }

                if (allResults.size < 5) {
                    val manualResults = searchManualTeams(query)
                    allResults.addAll(manualResults)
                    Log.d("FootballRepository", "Added ${manualResults.size} manual results")
                }

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
                    .take(15)

                searchCache[cacheKey] = filteredResults

                Log.d("FootballRepository", "Final result: ${filteredResults.size} teams for '$query'")
                Result.success(filteredResults)

            } catch (e: Exception) {
                Log.e("FootballRepository", "Exception in searchTeamsAdvanced for '$query'", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getTeamSuggestions(query: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FootballRepository", "Getting API-based suggestions for: '$query'")

                if (query.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val suggestions = mutableSetOf<String>()

                try {
                    val searchResult = searchTeamsAdvanced(query)

                    searchResult.onSuccess { results ->
                        results.forEach { teamSearchResult ->
                            val teamName = teamSearchResult.team.name

                            if (teamName.contains(query, ignoreCase = true)) {
                                suggestions.add(teamName)
                            }

                            teamName.split(" ").forEach { word ->
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

                if (suggestions.size < 3) {
                    teamsCache.values.forEach { team ->
                        if (team.name.contains(query, ignoreCase = true)) {
                            suggestions.add(team.name)
                        }

                        team.name.split(" ").forEach { word ->
                            if (word.startsWith(query, ignoreCase = true) && word.length > query.length) {
                                suggestions.add(word)
                            }
                        }
                    }
                    Log.d("FootballRepository", "Cache added suggestions, total: ${suggestions.size}")
                }


                val sortedSuggestions = suggestions
                    .filter { it.length >= query.length }
                    .sortedWith(compareBy<String> { suggestion ->
                        when {
                            suggestion.equals(query, ignoreCase = true) -> 0
                            suggestion.startsWith(query, ignoreCase = true) -> 1
                            suggestion.contains(query, ignoreCase = true) -> 2
                            else -> 3
                        }
                    }.thenBy { it.length }.thenBy { it })
                    .take(8)

                Log.d("FootballRepository", "Final API-based suggestions (${sortedSuggestions.size}): $sortedSuggestions")
                Result.success(sortedSuggestions)

            } catch (e: Exception) {
                Log.e("FootballRepository", "Exception in getTeamSuggestions", e)
                Result.failure(e)
            }
        }
    }

    private fun searchManualTeams(query: String): List<TeamSearchResult> {
        val manualTeams = listOf(
            ManualTeam(42, "Arsenal", 39, "Premier League", "https://media.api-sports.io/football/teams/42.png"),
            ManualTeam(49, "Chelsea", 39, "Premier League", "https://media.api-sports.io/football/teams/49.png"),
            ManualTeam(33, "Manchester United", 39, "Premier League", "https://media.api-sports.io/football/teams/33.png"),
            ManualTeam(50, "Manchester City", 39, "Premier League", "https://media.api-sports.io/football/teams/50.png"),
            ManualTeam(40, "Liverpool", 39, "Premier League", "https://media.api-sports.io/football/teams/40.png"),
            ManualTeam(47, "Tottenham", 39, "Premier League", "https://media.api-sports.io/football/teams/47.png"),
            ManualTeam(529, "Barcelona", 140, "La Liga", "https://media.api-sports.io/football/teams/529.png"),
            ManualTeam(541, "Real Madrid", 140, "La Liga", "https://media.api-sports.io/football/teams/541.png"),
            ManualTeam(530, "Atletico Madrid", 140, "La Liga", "https://media.api-sports.io/football/teams/530.png"),
            ManualTeam(157, "Bayern Munich", 78, "Bundesliga", "https://media.api-sports.io/football/teams/157.png"),
            ManualTeam(165, "Borussia Dortmund", 78, "Bundesliga", "https://media.api-sports.io/football/teams/165.png"),
            ManualTeam(496, "Juventus", 135, "Serie A", "https://media.api-sports.io/football/teams/496.png"),
            ManualTeam(505, "Inter Milan", 135, "Serie A", "https://media.api-sports.io/football/teams/505.png"),
            ManualTeam(489, "AC Milan", 135, "Serie A", "https://media.api-sports.io/football/teams/489.png"),
            ManualTeam(85, "Paris Saint-Germain", 61, "Ligue 1", "https://media.api-sports.io/football/teams/85.png"),
            ManualTeam(559, "Galatasaray", 203, "Super Lig", "https://media.api-sports.io/football/teams/559.png"),
            ManualTeam(562, "Fenerbahce", 203, "Super Lig", "https://media.api-sports.io/football/teams/562.png"),
            ManualTeam(558, "Besiktas", 203, "Super Lig", "https://media.api-sports.io/football/teams/558.png"),
            ManualTeam(564, "Trabzonspor", 203, "Super Lig", "https://media.api-sports.io/football/teams/564.png")
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
        return when (teamId.toInt()) {
            in listOf(42, 49, 33, 50, 40, 47) -> LeagueInfo(39, "Premier League", "England", 2024)
            in listOf(529, 541, 530) -> LeagueInfo(140, "La Liga", "Spain", 2024)
            in listOf(157, 165) -> LeagueInfo(78, "Bundesliga", "Germany", 2024)
            in listOf(496, 505, 489) -> LeagueInfo(135, "Serie A", "Italy", 2024)
            85 -> LeagueInfo(61, "Ligue 1", "France", 2024)
            in listOf(559, 562, 558, 564) -> LeagueInfo(203, "Super Lig", "Turkey", 2024)
            else -> {
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
                null
            }
        }
    }

    suspend fun getFavoriteTeamsMatches(favoriteTeamIds: Set<Long>): Result<List<Match>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FootballRepository", "Getting matches for ${favoriteTeamIds.size} favorite teams")

                if (favoriteTeamIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val allMatches = mutableListOf<Match>()

                for (teamId in favoriteTeamIds) {
                    try {
                        val teamLeague = findTeamLeagueById(teamId)

                        if (teamLeague != null) {
                            val leagueMatches = getMatchesByLeague(teamLeague.id, teamLeague.season)

                            leagueMatches.onSuccess { matches ->
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

    private suspend fun findTeamLeagueById(teamId: Long): LeagueInfo? {
        return when (teamId.toInt()) {
            in listOf(42, 49, 33, 50, 40, 47) -> LeagueInfo(39, "Premier League", "England", 2024)
            in listOf(529, 541, 530) -> LeagueInfo(140, "La Liga", "Spain", 2024)
            in listOf(157, 165) -> LeagueInfo(78, "Bundesliga", "Germany", 2024)
            in listOf(496, 505, 489) -> LeagueInfo(135, "Serie A", "Italy", 2024)
            85 -> LeagueInfo(61, "Ligue 1", "France", 2024)
            in listOf(559, 562, 558, 564) -> LeagueInfo(203, "Super Lig", "Turkey", 2024)
            else -> {
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

    suspend fun searchTeams(query: String): Result<List<Team>> {
        return searchTeamsAdvanced(query).map { results ->
            results.map { it.team }
        }
    }

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

    fun getTeamsCacheSize(): Int = teamsCache.size

    fun getSearchCacheSize(): Int = searchCache.size

    fun clearAllCaches() {
        teamsCache.clear()
        leagueTeamsCache.clear()
        searchCache.clear()
        Log.d("FootballRepository", "All caches cleared")
    }

    fun getPopularLeagues(): List<LeagueInfo> = popularLeagues.toList()

    fun isTeamCached(teamId: Long): Boolean = teamsCache.containsKey(teamId)
}