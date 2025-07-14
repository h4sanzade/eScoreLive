package com.materialdesign.escorelive.data.remote.dto

data class Score(
    val halftime: ScoreDetail,
    val fulltime: ScoreDetail,
    val extratime: ScoreDetail?,
    val penalty: ScoreDetail?
)

data class ScoreDetail(
    val home: Int?,
    val away: Int?
)