package com.example.quizduellosu.data.models

import com.google.firebase.database.PropertyName

data class Player(
    // Firebase'de field isimlerinin Kotlin property isimleriyle aynı olması beklenir.
    // Farklıysa @PropertyName("firebase_field_name") kullanılabilir.
    // Oyuncu adı, Firebase'de key olarak da kullanılabilir, bu durumda bu field gerekmeyebilir.
    // Ancak bir obje olarak saklamak daha esnek olabilir.
    val name: String = "",
    val score: Int = 0,
    val isHost: Boolean = false,
    // val playerId: String = "" // Eğer oyuncu adı key olarak kullanılmıyorsa, benzersiz bir ID.
    val lastAnswerResult: String? = null, // "CORRECT", "WRONG", "NO_ANSWER"
    val lastScoreChange: Int? = null      // +3, -1, +2
) {
    // Firebase'in data class'ları düzgün (de)serialize edebilmesi için boş constructor gereklidir.
    // Kotlin data class'ları için default argümanlar bunu sağlar.
}

object AnswerResultType {
    const val CORRECT = "CORRECT"
    const val WRONG = "WRONG"
    const val NO_ANSWER = "NO_ANSWER"
    const val CLAIM_FAILED = "CLAIM_FAILED" // Hız sorusunda claim edemedi
}
