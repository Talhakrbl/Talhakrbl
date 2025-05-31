package com.example.quizduellosu.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizduellosu.ui.theme.QuizDuellosuTheme
import com.example.quizduellosu.ui.viewmodels.RoomViewModel
import com.example.quizduellosu.data.models.Player // Player modelini import et

@Composable
fun RoomLobbyScreen(
    roomViewModel: RoomViewModel = viewModel(),
    roomCode: String, // Oda kodu null olmamalı, navigasyondan zorunlu gelmeli
    currentPlayerName: String, // Login'den taşınan oyuncu adı
    onNavigateBackToLogin: () -> Unit,
    onNavigateToGame: (roomCode: String) -> Unit // Oyun ekranına gitmek için
) {
    val roomState by roomViewModel.roomData.collectAsState()
    val isLoading by roomViewModel.isLoading.collectAsState()
    val errorMessage by roomViewModel.errorMessage.collectAsState()
    val playerLeft by roomViewModel.playerLeftRoom.collectAsState()
    val navigateToGameSignal by roomViewModel.navigateToGame.collectAsState() // Yeni state
    val context = LocalContext.current

    // Oda güncellemelerini dinle
    LaunchedEffect(key1 = roomCode) {
        if (roomCode.isNotBlank()) {
            // setCurrentPlayer çağrısı burada da yapılabilir, eğer LoginScreen'den sonra
            // MainActivity üzerinden state kayboluyorsa. Ancak ideal olanı AppViewModel'da tutmak.
            // Şimdilik, currentPlayerName'in ViewModel'da doğru set edildiğini varsayıyoruz.
            // roomViewModel.setCurrentPlayer(currentPlayerName, currentPlayerName) // Gerekirse tekrar set et
            roomViewModel.listenToRoomUpdates(roomCode)
        }
    }

    // Hata mesajlarını göster
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            roomViewModel.clearNavigationTriggers()
        }
    }

    // Odadan ayrılma durumunu dinle
    LaunchedEffect(playerLeft) {
        if (playerLeft) {
            Toast.makeText(context, "Odadan ayrıldınız veya oda kapatıldı.", Toast.LENGTH_SHORT).show()
            onNavigateBackToLogin()
            roomViewModel.clearNavigationTriggers()
            // roomViewModel.stopListeningToRoomUpdates() // Zaten leaveRoom içinde çağrılıyor
        }
    }

    // Oda verisi null ise (örn. oda silindi, oyuncu atıldı), Login'e geri dön
    LaunchedEffect(roomState){
        if(roomCode.isNotBlank() && roomState == null && !playerLeft && !isLoading && navigateToGameSignal == null){
             // Eğer aktif olarak dinliyorsak ve oda null olduysa (ve playerLeft tetiklenmediyse ve oyuna gitmiyorsak)
             // Bu durum, listenToRoomUpdates içinde _roomData.value = null yapıldığında oluşur.
            Toast.makeText(context, "Oda bilgisi alınamadı veya oda kapatıldı.", Toast.LENGTH_SHORT).show()
            roomViewModel.clearNavigationTriggers() // Hata sonrası triggerları temizle
            onNavigateBackToLogin()
        }
    }

    // Oyuna navigasyon sinyalini dinle
    LaunchedEffect(navigateToGameSignal) {
        if (navigateToGameSignal != null) {
            onNavigateToGame(navigateToGameSignal!!) // Oyun ekranına navigate et
            roomViewModel.clearNavigationTriggers() // Navigasyon sonrası trigger'ı temizle
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && roomState == null) { // Sadece ilk yüklemede tam ekran loading
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (roomState != null) {
            val currentRoom = roomState!!
            // ViewModel'den host adını alıp kontrol et. Oyuncu adı ViewModel'de set edilmiş olmalı.
            val actualCurrentPlayerName = roomViewModel.getCurrentPlayerNameForHostCheck() ?: currentPlayerName
            val isCurrentUserHost = currentRoom.hostName == actualCurrentPlayerName || currentRoom.players[actualCurrentPlayerName]?.isHost == true

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Oda Bekleme Ekranı",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Oda Kodu",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = currentRoom.roomId,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Katılımcılar (${currentRoom.players.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val participantsList = currentRoom.players.values.toList()
                if (participantsList.isEmpty()) {
                    Text("Henüz katılan kimse yok.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(participantsList, key = { it.name }) { player -> // Key ekledik
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = player.name + if (player.isHost) " (Host)" else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                // İleride oyuncu skorları da eklenebilir.
                            }
                            Divider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isCurrentUserHost) {
                    Button(
                        onClick = {
                            if (currentRoom.selectedThemes.isNotEmpty()) {
                                roomViewModel.startGame(currentRoom.roomId, currentRoom.selectedThemes)
                            } else {
                                Toast.makeText(context, "Oyun başlatmak için en az bir tema seçilmiş olmalı. (Bu hata normalde olmamalı)", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        enabled = participantsList.size >= 1 && !isLoading // En az 1 oyuncu (host) olmalı
                    ) {
                        Text("Oyunu Başlat")
                    }
                } else {
                    Text(
                        text = "Host'un (${currentRoom.hostName}) oyunu başlatması bekleniyor...",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Button(
                    onClick = { roomViewModel.leaveRoom(currentRoom.roomId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isLoading
                ) {
                    Text("Odadan Ayrıl")
                }
            }
        } else if (!isLoading) { // Oda null ve yüklenmiyorsa (hata veya oda yok durumu)
             Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Oda bilgileri yüklenemedi veya oda mevcut değil.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateBackToLogin) {
                    Text("Giriş Ekranına Dön")
                }
            }
        }
        // Yükleme devam ediyorsa ve roomState null değilse (örn. leaveRoom sırasında veya startGame sırasında), küçük bir indicator göster
        if (isLoading && roomState != null) { // roomState null ise zaten tam ekran loading var
             CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}


@Preview(showBackground = true, name = "Room Lobby Screen (Host, With Participants)")
@Composable
fun RoomLobbyScreenHostPreview() {
    QuizDuellosuTheme {
        // Preview için sahte RoomViewModel ve data sağlamak zor olabilir.
        // Bu nedenle Composable'ın aldığı parametrelerle direkt preview yapalım.
        // Gerçek ViewModel etkileşimini test etmek için uygulamayı çalıştırmak daha iyi.
        val room = Room(
            roomId = "XYZ123",
            hostName = "Oyuncu Ayşe",
            players = mapOf(
                "ayse" to Player("Oyuncu Ayşe", 0, true),
                "ali" to Player("Oyuncu Ali", 0, false),
                "zeynep" to Player("Oyuncu Zeynep", 0, false)
            ),
            status = "WAITING"
        )
        // Bu preview tam olarak ViewModel ile aynı davranışı göstermeyebilir.
        // Sadece UI'ın nasıl göründüğünü test eder.
         RoomLobbyContentForPreview(room = room, currentPlayerName = "Oyuncu Ayşe", isLoading = false)
    }
}

@Preview(showBackground = true, name = "Room Lobby Screen (Guest, With Participants)")
@Composable
fun RoomLobbyScreenGuestPreview() {
    QuizDuellosuTheme {
        val room = Room(
            roomId = "ABC456",
            hostName = "Oyuncu Ayşe",
            players = mapOf(
                "ayse" to Player("Oyuncu Ayşe", 0, true),
                "veli" to Player("Oyuncu Veli", 0, false),
                "fatma" to Player("Oyuncu Fatma", 0, false)
            )
        )
        RoomLobbyContentForPreview(room = room, currentPlayerName = "Oyuncu Veli", isLoading = false)
    }
}

// Preview için yardımcı Composable
@Composable
private fun RoomLobbyContentForPreview(room: Room, currentPlayerName: String, isLoading: Boolean) {
    val isCurrentUserHost = room.hostName == currentPlayerName
    val participantsList = room.players.values.toList()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Oda Bekleme Ekranı", style = MaterialTheme.typography.headlineLarge)
        Text("Oda Kodu: ${room.roomId}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Katılımcılar (${participantsList.size})", style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.weight(1f)) { items(participantsList) { Text(it.name + if (it.isHost) " (Host)" else "") } }
        if (isCurrentUserHost) Button(onClick = {}, enabled = !isLoading) { Text("Oyunu Başlat") }
        else Text("Host'un (${room.hostName}) oyunu başlatması bekleniyor...")
        Button(onClick = {}, enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Odadan Ayrıl") }
        if(isLoading) CircularProgressIndicator()
    }
}
