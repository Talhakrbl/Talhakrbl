package com.example.quizduellosu.data.models

data class ImageQuestion(
    override val id: String,
    override val text: String, // e.g., "What is shown in this image?" or a specific question about the image
    override val topic: String,
    val imageUrl: String,
    val options: List<String>,
    val correctOptionIndex: Int
) : Question
