package com.materialdesign.escorelive.di

import com.materialdesign.escorelive.data.remote.CompetitionApiService
import com.materialdesign.escorelive.data.remote.FootballApiService
import com.materialdesign.escorelive.data.remote.NewsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val FOOTBALL_BASE_URL = "https://v3.football.api-sports.io/"
    private const val NEWS_BASE_URL = "https://newsapi.org/v2/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("football")
    fun provideFootballRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(FOOTBALL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("news")
    fun provideNewsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NEWS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideFootballApiService(@Named("football") retrofit: Retrofit): FootballApiService {
        return retrofit.create(FootballApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNewsApiService(@Named("news") retrofit: Retrofit): NewsApiService {
        return retrofit.create(NewsApiService::class.java)
    }
    @Provides
    @Singleton
    fun provideCompetitionApiService(@Named("football") retrofit: Retrofit): CompetitionApiService {
        return retrofit.create(CompetitionApiService::class.java)
    }
}