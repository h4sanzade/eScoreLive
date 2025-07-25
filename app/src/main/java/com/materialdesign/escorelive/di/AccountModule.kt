// AccountModule.kt
package com.materialdesign.escorelive.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.materialdesign.escorelive.data.local.AccountDataStore
import com.materialdesign.escorelive.data.remote.repository.LeaguesApiService
import com.materialdesign.escorelive.data.remote.repository.LeaguesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

private val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore(name = "account_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AccountModule {

    @Provides
    @Singleton
    @Named("account")
    fun provideAccountDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.accountDataStore
    }

    @Provides
    @Singleton
    fun provideAccountDataStore(@Named("account") dataStore: DataStore<Preferences>): AccountDataStore {
        return AccountDataStore(dataStore)
    }

    @Provides
    @Singleton
    @Named("leagues")
    fun provideLeaguesRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.football-data.org/v4/") // Example API
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideLeaguesApiService(@Named("leagues") retrofit: Retrofit): LeaguesApiService {
        return retrofit.create(LeaguesApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLeaguesRepository(apiService: LeaguesApiService): LeaguesRepository {
        return LeaguesRepository(apiService)
    }
}