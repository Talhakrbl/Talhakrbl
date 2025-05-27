package com.example.quizduellosu.data.models

data class SpeedQuestion(
    override val id: String,
    override val text: String,
    override val topic: String,
    val options: List<String>,
    val correctOptionIndex: Int
) : Question
