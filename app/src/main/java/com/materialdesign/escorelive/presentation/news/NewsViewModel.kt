package com.materialdesign.escorelive.presentation.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.materialdesign.escorelive.data.remote.repository.NewsRepository

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _news = MutableLiveData<List<NewsItem>>()
    val news: LiveData<List<NewsItem>> = _news

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedCategory = MutableLiveData<NewsCategory>()
    val selectedCategory: LiveData<NewsCategory> = _selectedCategory

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        _selectedCategory.value = NewsCategory.ALL
        loadNews(NewsCategory.ALL)
    }

    fun loadNews(category: NewsCategory, refresh: Boolean = false) {
        viewModelScope.launch {
            try {
                Log.d("NewsViewModel", "Loading news for category: $category")

                if (refresh) {
                    _isRefreshing.value = true
                } else {
                    _isLoading.value = true
                }
                _error.value = null

                val result = newsRepository.getNews(category, refresh)

                result.onSuccess { newsList ->
                    _news.value = newsList
                    _selectedCategory.value = category
                    Log.d("NewsViewModel", "Successfully loaded ${newsList.size} news articles")
                }.onFailure { exception ->
                    Log.e("NewsViewModel", "Failed to load news", exception)
                    _error.value = "Failed to load news: ${exception.message}"

                    // Show cached data if available
                    val cachedNews = newsRepository.getCachedNews(category)
                    if (cachedNews.isNotEmpty()) {
                        _news.value = cachedNews
                        Log.d("NewsViewModel", "Showing ${cachedNews.size} cached articles")
                    }
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Exception in loadNews", e)
                _error.value = "Error loading news: ${e.message}"
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun refreshNews() {
        val currentCategory = _selectedCategory.value ?: NewsCategory.ALL
        loadNews(currentCategory, refresh = true)
    }

    fun selectCategory(category: NewsCategory) {
        if (category != _selectedCategory.value) {
            loadNews(category, refresh = false)
        }
    }

    fun loadMoreNews() {
        val currentCategory = _selectedCategory.value ?: NewsCategory.ALL
        viewModelScope.launch {
            try {
                val result = newsRepository.loadMoreNews(currentCategory)
                result.onSuccess { newsList ->
                    _news.value = newsList
                }.onFailure { exception ->
                    _error.value = "Failed to load more news: ${exception.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error loading more news: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun getNewsById(newsId: Long): NewsItem? {
        return _news.value?.find { it.id == newsId }
    }

    fun getCurrentCategoryString(): String {
        return when (_selectedCategory.value) {
            NewsCategory.ALL -> "All News"
            NewsCategory.TRANSFERS -> "Transfers"
            NewsCategory.MATCHES -> "Matches"
            NewsCategory.INJURIES -> "Injuries"
            null -> "All News"
        }
    }

    fun getNewsCount(): Int {
        return _news.value?.size ?: 0
    }
}

// Data classes
data class NewsItem(
    val id: Long,
    val title: String,
    val summary: String,
    val imageUrl: String,
    val publishDate: String,
    val category: String,
    val url: String,
    val source: String,
    val author: String? = null,
    val content: String? = null
)

enum class NewsCategory {
    ALL, TRANSFERS, MATCHES, INJURIES
}