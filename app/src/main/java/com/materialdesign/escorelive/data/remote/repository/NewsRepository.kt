package com.materialdesign.escorelive.data.remote.repository

import com.materialdesign.escorelive.data.remote.NewsApiService
import com.materialdesign.escorelive.data.remote.mappers.NewsMapper
import com.materialdesign.escorelive.presentation.ui.news.NewsItem
import com.materialdesign.escorelive.presentation.ui.news.NewsCategory
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class NewsRepository @Inject constructor(
    private val newsApiService: NewsApiService
) {

    private val newsCache = mutableMapOf<NewsCategory, List<NewsItem>>()
    private var currentPage = 1
    private var isLoading = false

    suspend fun getNews(category: NewsCategory = NewsCategory.ALL, refresh: Boolean = false): Result<List<NewsItem>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("NewsRepository", "Getting news for category: $category, refresh: $refresh")

                // Return cached data if available and not refreshing
                if (!refresh && newsCache.containsKey(category)) {
                    val cachedNews = newsCache[category] ?: emptyList()
                    if (cachedNews.isNotEmpty()) {
                        Log.d("NewsRepository", "Returning cached news: ${cachedNews.size} articles")
                        return@withContext Result.success(cachedNews)
                    }
                }

                if (isLoading) {
                    Log.d("NewsRepository", "Already loading, returning cached data")
                    return@withContext Result.success(newsCache[category] ?: emptyList())
                }

                isLoading = true

                if (refresh) {
                    currentPage = 1
                }

                val response = when (category) {
                    NewsCategory.ALL -> newsApiService.getFootballNews(page = currentPage)
                    NewsCategory.TRANSFERS -> newsApiService.getTransferNews(page = currentPage)
                    NewsCategory.MATCHES -> newsApiService.getMatchNews(page = currentPage)
                    NewsCategory.INJURIES -> newsApiService.getInjuryNews(page = currentPage)
                }

                if (response.isSuccessful) {
                    val newsResponse = response.body()
                    if (newsResponse != null && newsResponse.status == "ok") {
                        val newsItems = newsResponse.articles.mapNotNull { article ->
                            try {
                                NewsMapper.mapToNewsItem(article, category)
                            } catch (e: Exception) {
                                Log.w("NewsRepository", "Failed to map article: ${article.title}", e)
                                null
                            }
                        }

                        // Filter out articles without images or with poor quality
                        val filteredNews = newsItems.filter { newsItem ->
                            newsItem.imageUrl.isNotEmpty() &&
                                    newsItem.title.length > 10 &&
                                    !newsItem.title.contains("[Removed]", ignoreCase = true)
                        }

                        // Cache the results
                        if (refresh || currentPage == 1) {
                            newsCache[category] = filteredNews
                        } else {
                            val existingNews = newsCache[category] ?: emptyList()
                            newsCache[category] = existingNews + filteredNews
                        }

                        Log.d("NewsRepository", "Successfully loaded ${filteredNews.size} news articles for $category")
                        Result.success(newsCache[category] ?: emptyList())
                    } else {
                        Log.e("NewsRepository", "API response not OK: ${newsResponse?.status}")
                        Result.failure(Exception("Failed to load news: ${newsResponse?.status}"))
                    }
                } else {
                    Log.e("NewsRepository", "API call failed: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("NewsRepository", "Exception in getNews", e)
                Result.failure(e)
            } finally {
                isLoading = false
            }
        }
    }

    suspend fun loadMoreNews(category: NewsCategory): Result<List<NewsItem>> {
        currentPage++
        return getNews(category, refresh = false)
    }

    suspend fun refreshNews(category: NewsCategory): Result<List<NewsItem>> {
        currentPage = 1
        return getNews(category, refresh = true)
    }

    fun getCachedNews(category: NewsCategory): List<NewsItem> {
        return newsCache[category] ?: emptyList()
    }

    fun clearCache() {
        newsCache.clear()
        currentPage = 1
        Log.d("NewsRepository", "News cache cleared")
    }

    fun getCacheSize(): Int {
        return newsCache.values.sumOf { it.size }
    }
}