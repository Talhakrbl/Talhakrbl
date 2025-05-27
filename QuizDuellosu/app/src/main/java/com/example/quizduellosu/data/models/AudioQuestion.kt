package com.example.quizduellosu.data.models

data class AudioQuestion(
    override val id: String,
    override val text: String, // e.g., "Which song is this?" or "Who is speaking?"
    override val topic: String,
    val audioUrl: String,
    val options: List<String>,
    val correctOptionIndex: Int
) : Question
