package com.example.quizduellosu.data.models

data class FillInTheBlankQuestion(
    override val id: String,
    override val text: String, // Should contain a placeholder like "____"
    override val topic: String,
    val correctAnswers: List<String> // List of possible correct answers for the blank
) : Question
