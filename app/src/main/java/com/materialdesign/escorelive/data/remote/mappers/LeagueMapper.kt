package com.materialdesign.escorelive.data.remote.mappers

import com.materialdesign.escorelive.data.remote.dto.LeagueData
import com.materialdesign.escorelive.domain.model.League

object LeagueMapper {

    fun mapToLeague(leagueData: LeagueData): League {
        return League(
            id = leagueData.id,
            name = leagueData.name,
            logo = leagueData.logo,
            country = leagueData.country
        )
    }
}