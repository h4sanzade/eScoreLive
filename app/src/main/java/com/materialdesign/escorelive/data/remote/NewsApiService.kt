
package com.materialdesign.escorelive.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {

    companion object {
        const val NEWS_API_KEY = "c6b0002029fa44f69b5bb30db90160de"
        const val BASE_URL = "https://newsapi.org/v2/"
    }

    @GET("everything")
    suspend fun getFootballNews(
        @Query("q") query: String = "football OR soccer OR premier league OR la liga OR champions league",
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 50,
        @Query("page") page: Int = 1,
        @Query("apiKey") apiKey: String = NEWS_API_KEY
    ): Response<NewsResponse>

    @GET("everything")
    suspend fun getTransferNews(
        @Query("q") query: String = "football transfer OR soccer transfer OR player signing",
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 30,
        @Query("page") page: Int = 1,
        @Query("apiKey") apiKey: String = NEWS_API_KEY
    ): Response<NewsResponse>

    @GET("everything")
    suspend fun getMatchNews(
        @Query("q") query: String = "football match OR soccer game OR premier league results",
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 30,
        @Query("page") page: Int = 1,
        @Query("apiKey") apiKey: String = NEWS_API_KEY
    ): Response<NewsResponse>

    @GET("everything")
    suspend fun getInjuryNews(
        @Query("q") query: String = "football injury OR soccer player injured OR football fitness",
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 30,
        @Query("page") page: Int = 1,
        @Query("apiKey") apiKey: String = NEWS_API_KEY
    ): Response<NewsResponse>
}

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<NewsArticle>
)

data class NewsArticle(
    val source: NewsSource,
    val author: String?,
    val title: String,
    val description: String?,
    val url: String,
    val urlToImage: String?,
    val publishedAt: String,
    val content: String?
)

data class NewsSource(
    val id: String?,
    val name: String
)