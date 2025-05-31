package com.example.quizduellosu.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizduellosu.ui.theme.QuizDuellosuTheme
import com.example.quizduellosu.ui.viewmodels.RoomViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    roomViewModel: RoomViewModel = viewModel(),
    onNavigateToCreateRoom: (playerName: String) -> Unit, // Oyuncu adını CreateRoom'a taşıyacak
    onNavigateToLobby: (roomCode: String, playerName: String) -> Unit // Oyuncu adını Lobby'ye taşıyacak
) {
    var playerNameState by remember { mutableStateOf("") }
    var roomCodeState by remember { mutableStateOf("") }
    var showRoomCodeInput by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isLoading by roomViewModel.isLoading.collectAsState()
    val errorMessage by roomViewModel.errorMessage.collectAsState()
    val navigateToLobbyWithCode by roomViewModel.navigateToRoomLobbyWithCode.collectAsState()

    LaunchedEffect(navigateToLobbyWithCode) {
        if (navigateToLobbyWithCode != null) {
            onNavigateToLobby(navigateToLobbyWithCode!!, playerNameState)
            roomViewModel.clearNavigationTriggers()
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            roomViewModel.clearNavigationTriggers() // Hata mesajını da temizleyebiliriz
        }
    }

    Box(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quiz Düellosu",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = playerNameState,
                onValueChange = { playerNameState = it },
                label = { Text("Oyuncu Adınız") },
                modifier = Modifier.fillMaxWidth(),
                isError = playerNameState.isBlank() && (showRoomCodeInput || !showRoomCodeInput) // Butonlara basıldığında kontrol edilebilir
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (playerNameState.isNotBlank()) {
                        // Basitlik adına oyuncu adını ID olarak kullanıyoruz.
                        // Gerçek uygulamada benzersiz bir ID (örn: UUID.randomUUID().toString() veya Firebase Auth UID) kullanılmalı.
                        // Şimdilik ViewModel'a hem ID hem de isim olarak aynı şeyi geçelim.
                        // val playerId = UUID.randomUUID().toString() // Daha iyi bir yaklaşım
                        roomViewModel.setCurrentPlayer(playerNameState, playerNameState)
                        onNavigateToCreateRoom(playerNameState)
                    } else {
                        Toast.makeText(context, "Lütfen oyuncu adınızı girin.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = playerNameState.isNotBlank() && !isLoading
            ) {
                Text("Oda Kur")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (playerNameState.isNotBlank()) {
                        // val playerId = UUID.randomUUID().toString()
                        roomViewModel.setCurrentPlayer(playerNameState, playerNameState)
                        if (showRoomCodeInput) {
                            if (roomCodeState.isNotBlank()) {
                                roomViewModel.joinRoom(playerNameState, roomCodeState)
                            } else {
                                Toast.makeText(context, "Lütfen oda kodunu girin.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showRoomCodeInput = true
                        }
                    } else {
                        Toast.makeText(context, "Lütfen önce oyuncu adınızı girin.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = playerNameState.isNotBlank() && !isLoading
            ) {
                Text(if (showRoomCodeInput) "Odaya Katıl!" else "Odaya Katıl")
            }

            if (showRoomCodeInput) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = roomCodeState,
                    onValueChange = { roomCodeState = it },
                    label = { Text("Oda Kodu") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = roomCodeState.isBlank()
                )
            }
        }
        if(isLoading){
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    QuizDuellosuTheme {
        LoginScreen(onNavigateToCreateRoom = {}, onNavigateToLobby = { _, _ ->})
    }
}
