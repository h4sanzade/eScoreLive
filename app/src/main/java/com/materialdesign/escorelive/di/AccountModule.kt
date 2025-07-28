package com.materialdesign.escorelive.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.materialdesign.escorelive.presentation.account.AccountDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

private val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore(name = "account_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AccountModule {

    @Provides
    @Singleton
    @Named("account")
    fun provideAccountPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.accountDataStore
    }

    @Provides
    @Singleton
    fun provideAccountDataStoreWrapper(@Named("account") dataStore: DataStore<Preferences>): AccountDataStore {
        return AccountDataStore(dataStore)
    }
}