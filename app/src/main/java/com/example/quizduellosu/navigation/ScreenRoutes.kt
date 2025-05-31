package com.example.quizduellosu.navigation

object ScreenRoutes {
    const val LOGIN_SCREEN = "login"
    const val CREATE_ROOM_SCREEN = "create_room"

    // Oda kodu argümanı alacak normal oda lobisi rotası
    const val ROOM_LOBBY_SCREEN_ROUTE = "room_lobby/{roomCode}"
    fun roomLobbyScreen(roomCode: String) = "room_lobby/$roomCode"

    // CreateRoomScreen'den RoomLobbyScreen'e geçerken kullanılacak rota.
    // Bu, farklı bir argüman adı veya farklı popUpTo davranışları için gerekebilir.
    // Şimdilik, CreateRoom'dan sonra Lobby'e yeni oluşturulmuş oda kodu ile gidilecek.
    // Temalar Firebase üzerinden odaya yazılacak ve Lobby'de oradan okunacak varsayımıyla ilerliyoruz.
    // Bu rotanın argüman adı farklı olabilir veya aynı Composable'ı farklı bir şekilde yapılandırmak için kullanılabilir.
    // Basitlik adına, roomCode argümanını kullanacağız ama farklı bir base route tanımlayacağız.
    const val ROOM_LOBBY_FROM_CREATE_ROUTE = "room_lobby_from_create/{newRoomCode}"
    fun roomLobbyFromCreateScreen(newRoomCode: String) = "room_lobby_from_create/$newRoomCode"

    // Soru Görüntüleme Ekranı Rotası
    const val QUESTION_DISPLAY_SCREEN_ROUTE = "question_display/{roomId}"
    fun questionDisplayScreen(roomId: String) = "question_display/$roomId"

    // Oyun ekranı için ileride eklenecek bir rota
    // const val GAME_SCREEN_ROUTE = "game_screen/{roomCode}" // Bu aslında QuestionDisplayScreen olabilir veya daha genel bir oyun yönetim ekranı
    // fun gameScreen(roomCode: String) = "game_screen/$roomCode"
}
