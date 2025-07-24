package com.materialdesign.escorelive.di

import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.data.remote.AuthApiService
import com.materialdesign.escorelive.data.remote.FootballApiService
import com.materialdesign.escorelive.data.remote.NewsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val FOOTBALL_BASE_URL = "https://v3.football.api-sports.io/"
    private const val NEWS_BASE_URL = "https://newsapi.org/v2/"
    private const val AUTH_BASE_URL = "https://dummyjson.com/"

    @Provides
    @Singleton
    @Named("football")
    fun provideFootballRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(FOOTBALL_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("news")
    fun provideNewsRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NEWS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
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
    fun provideAuthApiService(@Named("auth") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object GlideModule {

        @Provides
        @Singleton
        fun provideGlideRequestOptions(): RequestOptions {
            return RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(10000)
        }
    }
}