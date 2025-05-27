package com.example.quizduellosu.data.models

data class OrderingQuestion(
    override val id: String,
    override val text: String, // e.g., "Sort these events chronologically"
    override val topic: String,
    val itemsToOrder: List<String>,
    val correctOrder: List<String> // This should contain the same items as itemsToOrder but in correct sequence
) : Question
