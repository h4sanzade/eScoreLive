package com.materialdesign.escorelive.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.materialdesign.escorelive.data.model.FavoriteTeam
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("favorites")

@Singleton
class FavoritesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    private val FAVORITES_KEY = stringPreferencesKey("favorite_teams")

    suspend fun getFavoriteTeams(): List<FavoriteTeam> {
        val favoritesJson = context.dataStore.data
            .map { preferences ->
                preferences[FAVORITES_KEY] ?: "[]"
            }
            .first()

        return try {
            val type = object : TypeToken<List<FavoriteTeam>>() {}.type
            gson.fromJson(favoritesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addFavoriteTeam(team: FavoriteTeam) {
        val currentFavorites = getFavoriteTeams().toMutableList()

        // Check if team is already in favorites
        if (currentFavorites.none { it.id == team.id }) {
            currentFavorites.add(team)
            saveFavorites(currentFavorites)
        }
    }

    suspend fun removeFavoriteTeam(teamId: Long) {
        val currentFavorites = getFavoriteTeams().toMutableList()
        currentFavorites.removeAll { it.id == teamId }
        saveFavorites(currentFavorites)
    }

    suspend fun isFavorite(teamId: Long): Boolean {
        val favorites = getFavoriteTeams()
        return favorites.any { it.id == teamId }
    }

    suspend fun toggleFavorite(team: FavoriteTeam): Boolean {
        return if (isFavorite(team.id)) {
            removeFavoriteTeam(team.id)
            false
        } else {
            addFavoriteTeam(team)
            true
        }
    }

    private suspend fun saveFavorites(favorites: List<FavoriteTeam>) {
        val favoritesJson = gson.toJson(favorites)
        context.dataStore.edit { preferences ->
            preferences[FAVORITES_KEY] = favoritesJson
        }
    }
}