package com.example.quizduellosu.firebase

import com.google.firebase.database.FirebaseDatabase

object FirebaseRefs {
    private val database = FirebaseDatabase.getInstance("https://quizduellosuapp-default-rtdb.firebaseio.com") // Gerçek Firebase URL'nizle değiştirin

    val roomsRef = database.getReference("rooms")

    fun getRoomRef(roomId: String) = roomsRef.child(roomId)

    fun getPlayersRef(roomId: String) = roomsRef.child(roomId).child("players")

    fun getPlayerRef(roomId: String, playerId: String) = getPlayersRef(roomId).child(playerId)

    fun getRoomStatusRef(roomId: String) = roomsRef.child(roomId).child("status")

    fun getRoomSelectedThemesRef(roomId: String) = roomsRef.child(roomId).child("selectedThemes")

    // Diğer spesifik referanslar buraya eklenebilir
}
