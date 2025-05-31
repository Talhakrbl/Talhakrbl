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

@Composable
fun CreateRoomScreen(
    roomViewModel: RoomViewModel = viewModel(),
    currentPlayerName: String, // LoginScreen'den gelecek
    onNavigateToLobby: (roomCode: String, playerName: String) -> Unit
) {
    val themes = listOf("Tarih", "Bilim", "Coğrafya", "Sanat", "Spor", "Müzik")
    val selectedThemes = remember { mutableStateListOf<String>() }
    val context = LocalContext.current

    val isLoading by roomViewModel.isLoading.collectAsState()
    val errorMessage by roomViewModel.errorMessage.collectAsState()
    val navigateToLobbyWithCode by roomViewModel.navigateToRoomLobbyWithCode.collectAsState()

    LaunchedEffect(navigateToLobbyWithCode) {
        if (navigateToLobbyWithCode != null) {
            // Oda oluşturulduğunda hostName zaten ViewModel içinde set edilmişti (createRoom içinde).
            // Bu yüzden currentPlayerName'i tekrar yolluyoruz.
            onNavigateToLobby(navigateToLobbyWithCode!!, currentPlayerName)
            roomViewModel.clearNavigationTriggers()
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            roomViewModel.clearNavigationTriggers()
        }
    }

    Box(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Oda Kur ve Tema Seç",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Hoş geldin, $currentPlayerName!", // Oyuncu adını göster
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Lütfen oynamak istediğiniz temaları seçin:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            themes.chunked(2).forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowThemes.forEach { theme ->
                        ThemeChip(
                            themeName = theme,
                            isSelected = selectedThemes.contains(theme),
                            onThemeSelected = {
                                if (selectedThemes.contains(theme)) {
                                    selectedThemes.remove(theme)
                                } else {
                                    selectedThemes.add(theme)
                                }
                            },
                            enabled = !isLoading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (selectedThemes.isNotEmpty()) {
                        // currentPlayerName zaten LoginScreen'den geldi ve ViewModel'da setCurrentPlayer ile set edildi.
                        // createRoom fonksiyonu bu ismi kullanacak.
                        roomViewModel.createRoom(currentPlayerName, selectedThemes.toList())
                    } else {
                        Toast.makeText(context, "Lütfen en az bir tema seçin.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = selectedThemes.isNotEmpty() && !isLoading
            ) {
                Text("Odayı Oluştur")
            }
        }
        if(isLoading){
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeChip(
    themeName: String,
    isSelected: Boolean,
    onThemeSelected: () -> Unit,
    enabled: Boolean = true
) {
    FilterChip(
        selected = isSelected,
        onClick = onThemeSelected,
        label = { Text(themeName) },
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        enabled = enabled
    )
}

@Preview(showBackground = true)
@Composable
fun CreateRoomScreenPreview() {
    QuizDuellosuTheme {
        CreateRoomScreen(currentPlayerName = "Örnek Oyuncu", onNavigateToLobby = {_,_ ->})
    }
}

@Preview(showBackground = true)
@Composable
fun CreateRoomScreenWithThemesSelectedPreview() {
    QuizDuellosuTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Oda Kur ve Tema Seç",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            ThemeChip(themeName = "Tarih", isSelected = true, onThemeSelected = {}, enabled = true)
            ThemeChip(themeName = "Bilim", isSelected = true, onThemeSelected = {}, enabled = true)
            ThemeChip(themeName = "Coğrafya", isSelected = false, onThemeSelected = {}, enabled = true)
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Odayı Oluştur")
            }
        }
    }
}
