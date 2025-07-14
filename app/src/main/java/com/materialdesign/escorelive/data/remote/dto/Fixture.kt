package com.materialdesign.escorelive.data.remote.dto

data class Fixture(
    val id: Long,
    val date: String,
    val status: Status,
    val elapsed: Int?,
    val timestamp: Long
)

data class Status(
    val long: String,
    val short: String,
    val elapsed: Int?
)
