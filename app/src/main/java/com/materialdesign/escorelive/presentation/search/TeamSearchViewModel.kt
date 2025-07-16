package com.materialdesign.escorelive.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.materialdesign.escorelive.domain.model.Team
import com.materialdesign.escorelive.domain.model.Match
import com.materialdesign.escorelive.data.remote.TeamStanding
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import com.materialdesign.escorelive.data.remote.repository.FootballRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@HiltViewModel
class TeamSearchViewModel @Inject constructor(
    private val repository: FootballRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<TeamSearchResult>>()
    val searchResults: LiveData<List<TeamSearchResult>> = _searchResults

    private val _suggestions = MutableLiveData<List<String>>()
    val suggestions: LiveData<List<String>> = _suggestions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedTeamStandings = MutableLiveData<List<TeamStanding>>()
    val selectedTeamStandings: LiveData<List<TeamStanding>> = _selectedTeamStandings

    private val _favoriteMatches = MutableLiveData<List<Match>>()
    val favoriteMatches: LiveData<List<Match>> = _favoriteMatches

    private val favoriteTeamIds = mutableSetOf<Long>()
    private var currentSearchQuery = ""
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null

    // Debugging için arama geçmişi
    private val searchHistory = mutableListOf<String>()

    init {
        loadFavoriteTeamIds()
        Log.d("TeamSearchViewModel", "Enhanced ViewModel initialized")
    }

    private fun loadFavoriteTeamIds() {
        try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            val favoriteIds = prefs.getStringSet("favorite_team_ids", emptySet()) ?: emptySet()
            favoriteTeamIds.clear()
            favoriteTeamIds.addAll(favoriteIds.map { it.toLong() })
            Log.d("TeamSearchViewModel", "Loaded ${favoriteTeamIds.size} favorite teams")
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error loading favorite teams", e)
        }
    }

    // GELİŞTİRİLMİŞ API TABANLI ARAMA FONKSİYONU
    fun searchTeams(query: String) {
        Log.d("TeamSearchViewModel", "Enhanced searchTeams called with: '$query'")

        if (query.length < 2) {
            Log.d("TeamSearchViewModel", "Query too short, clearing results")
            _searchResults.value = emptyList()
            _suggestions.value = emptyList()
            currentSearchQuery = ""
            return
        }

        currentSearchQuery = query

        // Önceki işi iptal et
        searchJob?.cancel()

        searchJob = viewModelScope.launch(Dispatchers.Main) {
            try {
                Log.d("TeamSearchViewModel", "Starting enhanced search job for: '$query'")
                _isLoading.value = true
                _error.value = null

                // Debounce
                delay(300) // Daha hızlı response için kısaltıldı

                if (!isActive) {
                    Log.d("TeamSearchViewModel", "Job cancelled during delay")
                    return@launch
                }

                Log.d("TeamSearchViewModel", "Calling enhanced repository search...")

                val result = repository.searchTeamsAdvanced(query)

                if (!isActive) {
                    Log.d("TeamSearchViewModel", "Job cancelled after repository call")
                    return@launch
                }

                result.fold(
                    onSuccess = { results ->
                        Log.d("TeamSearchViewModel", "Enhanced search successful: ${results.size} results")
                        _searchResults.value = results

                        // Debug: İlk birkaç sonucu logla
                        results.take(5).forEach { result ->
                            Log.d("TeamSearchViewModel", "Result: ${result.team.name} (${result.leagueName})")
                        }

                        // Search history'e ekle
                        addToSearchHistory(query)
                    },
                    onFailure = { exception ->
                        Log.e("TeamSearchViewModel", "Enhanced search failed for '$query'", exception)
                        _error.value = "Search failed: ${exception.message}"
                        _searchResults.value = emptyList()
                    }
                )

            } catch (e: Exception) {
                if (isActive) {
                    Log.e("TeamSearchViewModel", "Exception in enhanced searchTeams", e)
                    _error.value = "Search error: ${e.message}"
                    _searchResults.value = emptyList()
                } else {
                    Log.d("TeamSearchViewModel", "Job was cancelled, ignoring exception")
                }
            } finally {
                if (isActive) {
                    _isLoading.value = false
                }
            }
        }
    }

    // GELİŞTİRİLMİŞ API TABANLI ÖNERİ FONKSİYONU
    fun getSuggestions(query: String) {
        Log.d("TeamSearchViewModel", "Enhanced getSuggestions called with: '$query'")

        if (query.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }

        // Önceki işi iptal et
        suggestionJob?.cancel()

        suggestionJob = viewModelScope.launch(Dispatchers.Main) {
            try {
                Log.d("TeamSearchViewModel", "Starting enhanced API-based suggestion job for: '$query'")

                // Kısa debounce
                delay(200)

                if (!isActive) {
                    Log.d("TeamSearchViewModel", "Suggestion job cancelled during delay")
                    return@launch
                }

                // API'den gerçek öneriler al
                val result = repository.getTeamSuggestions(query)

                if (!isActive) {
                    Log.d("TeamSearchViewModel", "Suggestion job cancelled after repository call")
                    return@launch
                }

                result.fold(
                    onSuccess = { suggestions ->
                        Log.d("TeamSearchViewModel", "Got ${suggestions.size} enhanced API-based suggestions: $suggestions")
                        _suggestions.value = suggestions
                    },
                    onFailure = { exception ->
                        Log.w("TeamSearchViewModel", "Failed to get enhanced API suggestions for '$query'", exception)

                        // API başarısız olursa fallback öneriler
                        val fallbackSuggestions = getFallbackSuggestions(query)
                        _suggestions.value = fallbackSuggestions
                        Log.d("TeamSearchViewModel", "Using fallback suggestions: $fallbackSuggestions")
                    }
                )

            } catch (e: Exception) {
                if (isActive) {
                    Log.w("TeamSearchViewModel", "Exception in enhanced getSuggestions", e)
                    val fallbackSuggestions = getFallbackSuggestions(query)
                    _suggestions.value = fallbackSuggestions
                } else {
                    Log.d("TeamSearchViewModel", "Enhanced suggestion job was cancelled, ignoring exception")
                }
            }
        }
    }

    // Fallback öneriler (API başarısız olursa)
    private fun getFallbackSuggestions(query: String): List<String> {
        return getPopularTeams()
            .filter { it.contains(query, ignoreCase = true) }
            .sortedWith(compareBy<String> { suggestion ->
                when {
                    suggestion.equals(query, ignoreCase = true) -> 0
                    suggestion.startsWith(query, ignoreCase = true) -> 1
                    else -> 2
                }
            }.thenBy { it.length })
            .take(6)
    }

    // HIZLI ARAMA - Suggestion'a tıklandığında
    fun searchTeamByExactName(teamName: String) {
        Log.d("TeamSearchViewModel", "Enhanced searchTeamByExactName called with: '$teamName'")

        viewModelScope.launch(Dispatchers.Main) {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d("TeamSearchViewModel", "Enhanced searching exact name: '$teamName'")

                val result = repository.searchTeamByExactName(teamName)

                result.fold(
                    onSuccess = { results ->
                        Log.d("TeamSearchViewModel", "Enhanced exact search successful: ${results.size} results")
                        _searchResults.value = results
                        currentSearchQuery = teamName
                        addToSearchHistory(teamName)
                    },
                    onFailure = { exception ->
                        Log.e("TeamSearchViewModel", "Enhanced exact search failed for '$teamName'", exception)
                        _error.value = "Team not found: ${exception.message}"
                        _searchResults.value = emptyList()
                    }
                )

            } catch (e: Exception) {
                Log.e("TeamSearchViewModel", "Exception in enhanced searchTeamByExactName", e)
                _error.value = "Search error: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTeamStandings(teamSearchResult: TeamSearchResult) {
        Log.d("TeamSearchViewModel", "Enhanced loadTeamStandings called for: ${teamSearchResult.team.name}")

        viewModelScope.launch(Dispatchers.Main) {
            try {
                _isLoading.value = true
                _error.value = null

                val result = repository.getStandings(teamSearchResult.leagueId, teamSearchResult.season)

                result.fold(
                    onSuccess = { standings ->
                        Log.d("TeamSearchViewModel", "Enhanced loaded standings: ${standings.size} teams")
                        _selectedTeamStandings.value = standings
                    },
                    onFailure = { exception ->
                        // Try previous season if current season fails
                        Log.w("TeamSearchViewModel", "Current season failed, trying previous season")

                        val prevResult = repository.getStandings(teamSearchResult.leagueId, teamSearchResult.season - 1)
                        prevResult.fold(
                            onSuccess = { standings ->
                                Log.d("TeamSearchViewModel", "Enhanced loaded standings from previous season: ${standings.size} teams")
                                _selectedTeamStandings.value = standings
                            },
                            onFailure = {
                                Log.e("TeamSearchViewModel", "Enhanced failed to load standings", exception)
                                _error.value = "Failed to load standings for ${teamSearchResult.team.name}"
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("TeamSearchViewModel", "Exception in enhanced loadTeamStandings", e)
                _error.value = "Failed to load standings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFavoriteTeamMatches() {
        if (favoriteTeamIds.isEmpty()) {
            _favoriteMatches.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("TeamSearchViewModel", "Enhanced loading matches for ${favoriteTeamIds.size} favorite teams")

                repository.getFavoriteTeamsMatches(favoriteTeamIds)
                    .onSuccess { matches ->
                        val sortedMatches = sortMatchesByStatus(matches)
                        _favoriteMatches.value = sortedMatches
                        Log.d("TeamSearchViewModel", "Enhanced loaded ${sortedMatches.size} favorite team matches")
                    }
                    .onFailure { exception ->
                        Log.e("TeamSearchViewModel", "Enhanced failed to load favorite team matches", exception)
                        _error.value = "Failed to load favorite team matches: ${exception.message}"
                        _favoriteMatches.value = emptyList()
                    }
            } catch (e: Exception) {
                Log.e("TeamSearchViewModel", "Exception in enhanced loadFavoriteTeamMatches", e)
                _error.value = "Failed to load matches: ${e.message}"
                _favoriteMatches.value = emptyList()
            }

            _isLoading.value = false
        }
    }

    private fun sortMatchesByStatus(matches: List<Match>): List<Match> {
        return matches.sortedWith(compareBy<Match> { match ->
            when {
                match.isLive -> 0
                match.isUpcoming -> 1
                match.isFinished -> 2
                else -> 3
            }
        }.thenBy { match ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                match.kickoffTime?.let { inputFormat.parse(it)?.time } ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        })
    }

    fun getTeamPositionInStandings(teamId: Long): TeamStanding? {
        return _selectedTeamStandings.value?.find { it.team.id == teamId }
    }

    fun addToFavorites(teamId: Long) {
        try {
            favoriteTeamIds.add(teamId)
            saveFavoriteTeamIds()
            Log.d("TeamSearchViewModel", "Enhanced added team $teamId to favorites")
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error adding to favorites", e)
        }
    }

    fun removeFromFavorites(teamId: Long) {
        try {
            favoriteTeamIds.remove(teamId)
            saveFavoriteTeamIds()
            Log.d("TeamSearchViewModel", "Enhanced removed team $teamId from favorites")
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error removing from favorites", e)
        }
    }

    fun isTeamFavorite(teamId: Long): Boolean {
        return favoriteTeamIds.contains(teamId)
    }

    private fun saveFavoriteTeamIds() {
        try {
            val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            prefs.edit()
                .putStringSet("favorite_team_ids", favoriteTeamIds.map { it.toString() }.toSet())
                .apply()
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error saving favorite teams", e)
        }
    }

    fun clearSearch() {
        Log.d("TeamSearchViewModel", "Enhanced clearSearch called")

        currentSearchQuery = ""
        _searchResults.value = emptyList()
        _selectedTeamStandings.value = emptyList()
        _suggestions.value = emptyList()

        // Cancel ongoing jobs
        searchJob?.cancel()
        suggestionJob?.cancel()
        searchJob = null
        suggestionJob = null

        // Clear repository cache
        repository.clearSearchCache()
    }

    fun hasSearchQuery(): Boolean {
        return currentSearchQuery.isNotEmpty()
    }

    fun clearError() {
        _error.value = null
    }

    fun getFavoriteTeamsCount(): Int {
        return favoriteTeamIds.size
    }

    // GELİŞTİRİLMİŞ POPÜLER TAKIMLAR - Daha fazla ülke ve takım
    fun getPopularTeams(): List<String> {
        return listOf(
            // İngilizce takımlar
            "Arsenal", "Chelsea", "Manchester United", "Manchester City", "Liverpool", "Tottenham",
            "Leicester City", "West Ham", "Newcastle", "Brighton", "Aston Villa", "Crystal Palace",
            "Everton", "Leeds United", "Wolverhampton", "Southampton", "Burnley", "Norwich City",

            // İspanyol takımlar
            "Barcelona", "Real Madrid", "Atletico Madrid", "Sevilla", "Valencia", "Villarreal",
            "Athletic Bilbao", "Real Sociedad", "Betis", "Espanyol", "Celta Vigo", "Granada",
            "Getafe", "Cadiz", "Osasuna", "Mallorca", "Alaves", "Elche",

            // Alman takımlar
            "Bayern Munich", "Borussia Dortmund", "RB Leipzig", "Bayer Leverkusen", "Borussia Monchengladbach",
            "Eintracht Frankfurt", "Wolfsburg", "Schalke", "Werder Bremen", "Hamburg", "Stuttgart",
            "Hertha Berlin", "Hoffenheim", "Mainz", "Augsburg", "Freiburg", "Cologne",

            // İtalyan takımlar
            "Juventus", "Inter Milan", "AC Milan", "Roma", "Napoli", "Lazio", "Atalanta", "Fiorentina",
            "Torino", "Genoa", "Sampdoria", "Bologna", "Sassuolo", "Udinese", "Cagliari", "Verona",
            "Spezia", "Empoli", "Salernitana", "Venezia",

            // Fransız takımlar
            "Paris Saint-Germain", "Marseille", "Lyon", "Monaco", "Lille", "Nice", "Rennes", "Nantes",
            "Bordeaux", "Saint-Etienne", "Strasbourg", "Montpellier", "Angers", "Lens", "Reims",
            "Brest", "Troyes", "Clermont", "Lorient", "Metz",

            // Türk takımlar
            "Galatasaray", "Fenerbahce", "Besiktas", "Trabzonspor", "Basaksehir", "Antalyaspor",
            "Konyaspor", "Sivasspor", "Alanyaspor", "Rizespor", "Kayserispor", "Samsunspor",
            "Gaziantepspor", "Denizlispor", "Goztepe", "Kasimpasa", "Yeni Malatyaspor", "Hatayspor",
            "Adana Demirspor", "Giresunspor", "Altay", "Bandirmaspor", "Umraniyespor", "Istanbulspor",

            // Azerbaycan takımlar
            "Qarabag", "Neftchi", "Sabah", "Zira", "Sumqayit", "Kapaz", "Sabail", "Turan Tovuz",
            "Shamakhi", "Gabala", "Keşla", "Mil-Muğan",

            // Hollandalı takımlar
            "Ajax", "PSV", "Feyenoord", "AZ Alkmaar", "Vitesse", "Utrecht", "Twente", "Groningen",
            "Heerenveen", "Willem II", "Sparta Rotterdam", "Heracles", "Go Ahead Eagles", "Cambuur",

            // Portekizli takımlar
            "Benfica", "Porto", "Sporting", "Braga", "Vitoria Guimaraes", "Boavista", "Pacos Ferreira",
            "Santa Clara", "Maritimo", "Moreirense", "Tondela", "Famalicao", "Gil Vicente", "Arouca",

            // Belçika takımlar
            "Anderlecht", "Club Brugge", "Genk", "Standard Liege", "Gent", "Antwerp", "Mechelen",
            "Oostende", "Charleroi", "Kortrijk", "Eupen", "Cercle Brugge", "Sint-Truiden", "Seraing",

            // İsviçre takımlar
            "Young Boys", "Basel", "Zurich", "St. Gallen", "Servette", "Lugano", "Lucerne", "Sion",
            "Grasshoppers", "Lausanne", "Vaduz",

            // Avusturya takımlar
            "Red Bull Salzburg", "Rapid Wien", "Austria Wien", "Sturm Graz", "LASK", "Wolfsberg",
            "Altach", "Hartberg", "Admira", "Ried", "St. Polten", "Tirol",

            // Çek takımlar
            "Sparta Prague", "Slavia Prague", "Viktoria Plzen", "Banik Ostrava", "Jablonec", "Liberec",
            "Slovacko", "Bohemians", "Hradec Kralove", "Pardubice", "Teplice", "Ceske Budejovice",

            // Rus takımlar
            "Zenit", "Spartak Moscow", "CSKA Moscow", "Dynamo Moscow", "Lokomotiv Moscow", "Krasnodar",
            "Rostov", "Akhmat Grozny", "Ufa", "Sochi", "Arsenal Tula", "Rubin Kazan",

            // Ukrayna takımlar
            "Shakhtar Donetsk", "Dynamo Kiev", "Zorya", "Desna", "Kolos", "Vorskla", "Olimpik",
            "Rukh Lviv", "Mynai", "Chornomorets", "Lviv", "Mariupol",

            // Yunan takımlar
            "Olympiacos", "Panathinaikos", "AEK Athens", "PAOK", "Aris", "Atromitos", "Volos",
            "Lamia", "Apollon Smyrnis", "Ionikos", "Asteras", "PAS Giannina",

            // Sırp takımlar
            "Red Star Belgrade", "Partizan", "Vojvodina", "Cukaricki", "Radnicki Nis", "Spartak Subotica",
            "Kolubara", "Novi Pazar", "Metalac", "Vozdovac", "Javor", "Radnik Surdulica",

            // Hırvat takımlar
            "Dinamo Zagreb", "Hajduk Split", "Rijeka", "Osijek", "Lokomotiva", "Varazdin", "Istra",
            "Gorica", "Slaven Belupo", "Sibenik",

            // Slovakya takımlar
            "Slovan Bratislava", "Spartak Trnava", "Zilina", "Dunajska Streda", "Ruzomberok", "Senica",
            "Michalovce", "Nitra", "Skalica", "Podbrezova",

            // Romanya takımlar
            "CFR Cluj", "FCSB", "Craiova", "Sepsi", "Rapid Bucharest", "Botosani", "Chindia",
            "Farul Constanta", "Arges", "Voluntari", "Mioveni", "Clinceni",

            // Bulgar takımlar
            "Ludogorets", "CSKA Sofia", "Levski Sofia", "Cherno More", "Beroe", "Arda", "Botev Plovdiv",
            "Lokomotiv Plovdiv", "Botev Vratsa", "Pirin", "Tsarsko Selo", "Slavija Sofia",

            // Diğer popüler takımlar
            "Celtic", "Rangers", "Olympiacos", "Panathinaikos", "Rosenborg", "Molde", "Brann",
            "Stromsgodset", "Bodo/Glimt", "Valerenga", "Lillestrom", "Haugesund"
        )
    }

    // TEST FONKSİYONU - Debug için
    fun testSearch() {
        Log.d("TeamSearchViewModel", "Enhanced testSearch called - using API search")
        searchTeams("Arsenal")
    }

    fun addToSearchHistory(query: String) {
        try {
            if (query.isNotEmpty() && !searchHistory.contains(query)) {
                searchHistory.add(0, query)
                if (searchHistory.size > 15) { // Daha fazla history
                    searchHistory.removeAt(15)
                }
                Log.d("TeamSearchViewModel", "Enhanced added '$query' to search history")
            }
        } catch (e: Exception) {
            Log.e("TeamSearchViewModel", "Error adding to search history", e)
        }
    }

    fun getSearchHistory(): List<String> {
        return searchHistory.toList()
    }

    fun getFavoriteTeamIds(): Set<Long> {
        return favoriteTeamIds.toSet()
    }

    fun hasAnyFavoriteTeams(): Boolean {
        return favoriteTeamIds.isNotEmpty()
    }

    override fun onCleared() {
        Log.d("TeamSearchViewModel", "Enhanced onCleared called")
        super.onCleared()
        searchJob?.cancel()
        suggestionJob?.cancel()
    }
}

// Data class for search results with league information
data class TeamSearchResult(
    val team: Team,
    val leagueId: Int,
    val leagueName: String,
    val season: Int
)