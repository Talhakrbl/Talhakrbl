package com.example.quizduellosu.data.models

data class PlayerState(
    val playerName: String,
    var score: Int = 0,
    val id: String, // Using playerName as ID for now
    var hasStealPowerUp: Boolean = true // Added steal power-up flag
)
