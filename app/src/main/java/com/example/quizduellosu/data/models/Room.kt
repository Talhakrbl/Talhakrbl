package com.example.quizduellosu.data.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties // Firebase'den okurken modelde olmayan alanları yoksay
data class Room(
    val roomId: String = "", // Oda kodu, Firebase'de doküman/düğüm anahtarı olarak da kullanılır
    val hostId: String = "", // Odayı kuran oyuncunun benzersiz kimliği (Auth UID olabilir)
    val hostName: String = "", // Odayı kuran oyuncunun adı (kolay erişim için)
    val createdAt: Long = System.currentTimeMillis(),
    var status: String = RoomStatus.WAITING.name, // "WAITING", "IN_PROGRESS", "FINISHED"
    val selectedThemes: List<String> = emptyList(),
    // Key: Player ID (Auth UID veya benzersiz bir tanımlayıcı). Oyuncu adı key olmamalı çünkü değişebilir veya benzersiz olmayabilir.
    val players: Map<String, Player> = emptyMap(),
    val activeQuestionIds: List<String> = emptyList(), // Oyun için seçilen soruların ID listesi
    val currentQuestionIndex: Int = -1, // Mevcut soru indeksi (-1: oyun başlamadı, 0: ilk soru)
    val liveQuestionState: LiveQuestionState? = null, // Aktif sorunun anlık durumu
    // val questions: List<String> = emptyList() // Soru ID'leri veya direkt sorular (yapıya göre değişir)
) {
    // Firebase için boş constructor (default argümanlar sayesinde sağlanır)
}

// Oda durumları için bir enum tanımlamak iyi bir pratik olabilir.
enum class RoomStatus {
    WAITING,
    IN_PROGRESS,
    FINISHED
}
