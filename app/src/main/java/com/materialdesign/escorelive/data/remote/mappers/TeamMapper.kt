package com.materialdesign.escorelive.data.remote.mappers


import com.materialdesign.escorelive.data.remote.dto.TeamData
import com.materialdesign.escorelive.domain.model.Team

object TeamMapper {

    fun mapToTeam(teamData: TeamData): Team {
        return Team(
            id = teamData.id,
            name = teamData.name,
            logo = teamData.logo,
            shortName = teamData.name.take(3).uppercase()
        )
    }
}