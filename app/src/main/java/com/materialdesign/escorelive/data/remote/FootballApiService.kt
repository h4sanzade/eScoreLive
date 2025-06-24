package com.materialdesign.escorelive.data.remote

import com.materialdesign.escorelive.FixturesResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.Response

interface FootballApiService {

    @GET("fixtures")
    suspend fun getLiveFixtures(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("live") live: String = "all"
    ): Response<FixturesResponse>

    @GET("fixtures")
    suspend fun getFixturesByDate(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("date") date: String,
        @Query("timezone") timezone: String = "Europe/London"
    ): Response<FixturesResponse>

    @GET("fixtures")
    suspend fun getFixturesByLeague(
        @Header("X-RapidAPI-Key") apiKey: String = "acc13599400653ba46e1defb6d242255",
        @Header("X-RapidAPI-Host") host: String = "v3.football.api-sports.io",
        @Query("league") leagueId: Int,
        @Query("season") season: Int,
        @Query("live") live: String = "all"
    ): Response<FixturesResponse>
}