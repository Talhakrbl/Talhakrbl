package com.example.quizduellosu.data.models

import com.google.firebase.database.IgnoreExtraProperties

enum class QuestionActivityStatus {
    OPEN_FOR_CLAIMS, // Hız soruları için "Hemen Bas"a açık
    CLAIMED,         // Birisi "Hemen Bas" ile hak kazandı, cevap bekleniyor
    ANSWERING,       // (CLAIMED ile birleştirilebilir veya çoktan seçmeli için kullanılabilir)
    LOCKED,          // Cevaplar kilitlendi, süre doldu veya herkes cevapladı
    REVEALED         // Cevaplar gösteriliyor
}

@IgnoreExtraProperties
data class LiveQuestionState(
    val questionId: String = "", // Aktif sorunun ID'si (Room.activeQuestionIds[currentQuestionIndex] ile aynı olmalı)
    val status: String = QuestionActivityStatus.OPEN_FOR_CLAIMS.name,
    val claimerPlayerId: String? = null, // Hız sorusunda cevap hakkını ilk alan oyuncunun ID'si
    val claimerPlayerName: String? = null, // Kolay erişim için claimer'ın adı
    // Key: playerId, Value: selectedOptionId (örn: "a", "b", "c", "d")
    val answers: Map<String, String> = emptyMap(),
    // Key: playerId, Value: timestamp (Long) - Cevap verme zamanı, hız için önemli olabilir
    val answerTimestamps: Map<String, Long> = emptyMap()
) {
    // Firebase için boş constructor (default argümanlar sayesinde sağlanır)
}
