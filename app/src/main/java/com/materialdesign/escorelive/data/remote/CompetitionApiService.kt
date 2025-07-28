package com.materialdesign.escorelive.data.remote

import com.materialdesign.escorelive.data.remote.dto.LeaguesApiResponse
import com.materialdesign.escorelive.data.remote.dto.CountriesApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query


interface CompetitionApiService {

    companion object {
        const val API_KEY = "8767b67b14200a87603b7211cf4239dd"
        const val API_HOST = "v3.football.api-sports.io"
    }


    @GET("leagues")
    suspend fun getAllLeagues(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST
    ): Response<LeaguesApiResponse>


    @GET("leagues")
    suspend fun getLeaguesByCountry(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("country") country: String
    ): Response<LeaguesApiResponse>

    @GET("leagues")
    suspend fun searchLeagues(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("search") searchTerm: String
    ): Response<LeaguesApiResponse>


    @GET("leagues")
    suspend fun getLeagueById(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("id") leagueId: Int
    ): Response<LeaguesApiResponse>

    @GET("countries")
    suspend fun getCountries(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST
    ): Response<CountriesApiResponse>


    @GET("leagues")
    suspend fun getCurrentSeasonLeagues(
        @Header("X-RapidAPI-Key") apiKey: String = API_KEY,
        @Header("X-RapidAPI-Host") host: String = API_HOST,
        @Query("current") current: Boolean = true
    ): Response<LeaguesApiResponse>
}