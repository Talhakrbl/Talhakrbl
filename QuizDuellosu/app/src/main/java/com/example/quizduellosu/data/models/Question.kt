package com.example.quizduellosu.data.models

sealed interface Question {
    val id: String
    val text: String
    val topic: String
}
