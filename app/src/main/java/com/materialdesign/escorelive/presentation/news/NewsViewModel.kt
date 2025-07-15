package com.materialdesign.escorelive.presentation.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

@HiltViewModel
class NewsViewModel @Inject constructor(
    // private val repository: NewsRepository
) : ViewModel() {

    private val _news = MutableLiveData<List<NewsItem>>()
    val news: LiveData<List<NewsItem>> = _news

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadNews()
    }

    private fun loadNews() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Mock data for now - daha gerçekçi haberler
                val mockNews = listOf(
                    NewsItem(
                        1,
                        "Breaking: Mbappe completes Real Madrid transfer",
                        "Kylian Mbappe has officially signed for Real Madrid in a record-breaking deal worth €200 million. The French superstar will join Los Blancos this summer after his contract with PSG expires...",
                        "https://example.com/mbappe.jpg",
                        getCurrentDateWithTime(-2), // 2 hours ago
                        "Transfer News"
                    ),
                    NewsItem(
                        2,
                        "Champions League Final: Manchester City vs Inter Milan Preview",
                        "The stage is set for an epic Champions League final as Pep Guardiola's Manchester City face Simone Inzaghi's Inter Milan. Both teams have shown incredible form throughout the tournament...",
                        "https://example.com/ucl-final.jpg",
                        getCurrentDateWithTime(-5), // 5 hours ago
                        "Match Reports"
                    ),
                    NewsItem(
                        3,
                        "Exclusive: Arsenal close to signing Declan Rice",
                        "Arsenal are reportedly in advanced negotiations with West Ham United for midfielder Declan Rice. The England international is valued at £100 million and could become Arsenal's record signing...",
                        "https://example.com/rice-arsenal.jpg",
                        getCurrentDateWithTime(-8), // 8 hours ago
                        "Transfer News"
                    ),
                    NewsItem(
                        4,
                        "Injury Update: Haaland expected back next week",
                        "Manchester City striker Erling Haaland is making good progress in his recovery from a groin injury. The Norwegian is expected to return to training next week ahead of the crucial Premier League fixtures...",
                        "https://example.com/haaland-injury.jpg",
                        getCurrentDateWithTime(-12), // 12 hours ago
                        "Injury News"
                    ),
                    NewsItem(
                        5,
                        "Barcelona's Financial Recovery: La Liga spending approved",
                        "Barcelona have received approval from La Liga to increase their spending limit for the upcoming transfer window. The Catalan club's financial recovery has impressed league officials...",
                        "https://example.com/barca-finances.jpg",
                        getCurrentDateWithTime(-18), // 18 hours ago
                        "Club News"
                    ),
                    NewsItem(
                        6,
                        "World Cup 2026: New stadiums unveiled in USA",
                        "FIFA has officially unveiled the final list of stadiums that will host matches during the 2026 World Cup across the United States, Mexico, and Canada. The tournament will feature 48 teams...",
                        "https://example.com/worldcup-stadiums.jpg",
                        getCurrentDateWithTime(-24), // 1 day ago
                        "World Cup"
                    ),
                    NewsItem(
                        7,
                        "Premier League title race: Arsenal maintain top spot",
                        "Arsenal's victory over Chelsea keeps them at the top of the Premier League table with just five games remaining. The Gunners are two points clear of Manchester City with both teams in excellent form...",
                        "https://example.com/arsenal-title.jpg",
                        getCurrentDateWithTime(-30), // 30 hours ago
                        "Match Reports"
                    ),
                    NewsItem(
                        8,
                        "UEFA announces new Champions League format for 2024-25",
                        "UEFA has confirmed major changes to the Champions League format starting from the 2024-25 season. The competition will expand to 36 teams with a new 'Swiss model' league phase...",
                        "https://example.com/ucl-format.jpg",
                        getCurrentDateWithTime(-36), // 36 hours ago
                        "UEFA News"
                    )
                )

                _news.value = mockNews
            } catch (e: Exception) {
                _error.value = "Failed to load news: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getCurrentDateWithTime(hoursOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, hoursOffset)
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun refreshNews() {
        loadNews()
    }

    fun clearError() {
        _error.value = null
    }
}

// Data class for NewsItem
data class NewsItem(
    val id: Long,
    val title: String,
    val summary: String,
    val imageUrl: String,
    val publishDate: String,
    val category: String
)