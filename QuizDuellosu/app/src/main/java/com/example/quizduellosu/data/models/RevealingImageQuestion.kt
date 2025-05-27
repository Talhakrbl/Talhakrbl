package com.example.quizduellosu.data.models

data class RevealingImageQuestion(
    override val id: String,
    override val text: String, // e.g., "What is this gradually revealing image?"
    override val topic: String,
    val imageUrl: String,
    val options: List<String>,
    val correctOptionIndex: Int
) : Question
