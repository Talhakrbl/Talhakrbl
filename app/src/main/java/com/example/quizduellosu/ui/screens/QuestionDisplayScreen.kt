package com.example.quizduellosu.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.quizduellosu.data.models.LiveQuestionState // LiveQuestionState import
import com.example.quizduellosu.data.models.QuestionActivityStatus // Enum import
import com.example.quizduellosu.data.models.AnswerResultType // AnswerResultType import
import com.example.quizduellosu.ui.theme.QuizDuellosuTheme
import com.example.quizduellosu.ui.viewmodels.RoomViewModel

// --- Data Classes ---
data class AnswerOption(val id: String, val text: String)

data class Question(
    val id: String,
    val text: String,
    val imageUrl: String? = null,
    val options: List<AnswerOption>,
    val questionType: QuestionType = QuestionType.SPEED,
    val correctAnswerId: String? = null // ViewModel'dan gelen soru objesinde bu alan dolu olmalı
)

enum class QuestionType {
    SPEED,
    FILL_IN_THE_BLANK,
    ORDERING,
    IMAGE_QUESTION,
    REVEALING_IMAGE
}
// --- End Data Classes ---

@Composable
fun QuestionDisplayScreen(
    roomId: String,
    currentPlayerName: String,
    roomViewModel: RoomViewModel = viewModel(),
    onNavigateBackToLobby: () -> Unit,
    onFinishGame: () -> Unit
) {
    val roomState by roomViewModel.roomData.collectAsState()
    val isLoading by roomViewModel.isLoading.collectAsState()
    val errorMessage by roomViewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    var localSelectedAnswer by remember { mutableStateOf<AnswerOption?>(null) }

    LaunchedEffect(key1 = roomId) {
        if (roomId.isNotBlank()) {
            roomViewModel.setCurrentPlayer(currentPlayerName, currentPlayerName)
            roomViewModel.listenToRoomUpdates(roomId)
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            roomViewModel.clearNavigationTriggers()
            // Ciddi bir hata varsa lobiye yönlendir (opsiyonel, ViewModel'daki mesaja göre karar verilebilir)
            // onNavigateBackToLobby()
        }
    }

    val currentRoom = roomState
    val liveState = currentRoom?.liveQuestionState

    LaunchedEffect(currentRoom?.status) {
        if (currentRoom?.status == com.example.quizduellosu.data.models.RoomStatus.FINISHED.name) {
            Toast.makeText(context, "Oyun Bitti!", Toast.LENGTH_LONG).show()
            onFinishGame() // Veya bir skor ekranına yönlendir
        }
    }

    LaunchedEffect(liveState?.status) {
        if (liveState?.status == QuestionActivityStatus.REVEALED.name) {
            // Cevaplar açıklandıktan sonra ViewModel'deki processEndOfQuestion çağrılacak (içinde delay var)
            roomViewModel.processEndOfQuestion(roomId)
        }
    }

    LaunchedEffect(currentRoom?.currentQuestionIndex) {
        localSelectedAnswer = null // Yeni soruya geçildiğinde seçimi sıfırla
        // Yeni soru için LiveState'in ViewModel'da OPEN_FOR_CLAIMS olarak ayarlandığını varsayıyoruz
        // (prepareLiveStateForNewQuestion çağrısıyla).
    }

    if (isLoading && currentRoom == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (currentRoom != null) {
        val questionToDisplay = aktuellSoru(currentRoom) // Örnek soru alma fonksiyonu

        if (questionToDisplay != null) {
            ActualQuestionUI(
                question = questionToDisplay,
                timeLeftInSeconds = 15, // TODO: Gerçek zamanlayıcı
                currentQuestionNumber = currentRoom.currentQuestionIndex + 1,
                totalQuestions = currentRoom.activeQuestionIds.size,
                selectedAnswer = localSelectedAnswer,
                onAnswerSelected = { answer ->
                    val currentLiveStatus = liveState?.status
                    val canSelect = when (questionToDisplay.questionType) {
                        QuestionType.SPEED -> currentLiveStatus == QuestionActivityStatus.OPEN_FOR_CLAIMS.name ||
                                (currentLiveStatus == QuestionActivityStatus.CLAIMED.name && liveState.claimerPlayerId == currentPlayerName)
                        else -> currentLiveStatus != QuestionActivityStatus.LOCKED.name && currentLiveStatus != QuestionActivityStatus.REVEALED.name
                    }
                    if (canSelect && liveState?.answers?.containsKey(currentPlayerName) != true) { // Henüz cevap vermemişse
                        localSelectedAnswer = answer
                    }
                },
                onConfirmAnswerClicked = {
                    if (localSelectedAnswer != null) {
                        when (questionToDisplay.questionType) {
                            QuestionType.SPEED -> {
                                if (liveState?.status == QuestionActivityStatus.OPEN_FOR_CLAIMS.name) {
                                    roomViewModel.claimSpeedQuestionAttempt(currentRoom.roomId, questionToDisplay.id)
                                } else if (liveState?.status == QuestionActivityStatus.CLAIMED.name && liveState.claimerPlayerId == currentPlayerName) {
                                    roomViewModel.submitPlayerAnswer(currentRoom.roomId, questionToDisplay.id, localSelectedAnswer!!.id)
                                }
                            }
                            else -> { // Diğer soru tipleri için direkt cevap gönder
                                if (liveState?.answers?.containsKey(currentPlayerName) != true && liveState?.status != QuestionActivityStatus.LOCKED.name && liveState?.status != QuestionActivityStatus.REVEALED.name) {
                                    roomViewModel.submitPlayerAnswer(currentRoom.roomId, questionToDisplay.id, localSelectedAnswer!!.id)
                                }
                            }
                        }
                    }
                },
                liveQuestionState = liveState,
                currentPlayerName = currentPlayerName,
                questionType = questionToDisplay.questionType,
                isLoading = isLoading,
                playerSpecificState = currentRoom.players[currentPlayerName]
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                if(currentRoom.status == com.example.quizduellosu.data.models.RoomStatus.FINISHED.name) {
                    Text("Oyun Bitti!", style = MaterialTheme.typography.headlineMedium)
                } else if (currentRoom.currentQuestionIndex >= currentRoom.activeQuestionIds.size) {
                     Text("Tüm sorular tamamlandı!", style = MaterialTheme.typography.headlineMedium)
                } else {
                    Text("Soru yükleniyor veya beklenmedik bir durum oluştu...")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateBackToLobby) { Text("Lobiye Dön") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onFinishGame) { Text("Ana Ekrana Dön") }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Text("Oda bilgileri bekleniyor...")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateBackToLobby) { Text("Lobiye Dön") }
        }
    }
}

@Composable
fun ActualQuestionUI(
    question: Question,
    timeLeftInSeconds: Int,
    currentQuestionNumber: Int,
    totalQuestions: Int,
    selectedAnswer: AnswerOption?,
    onAnswerSelected: (AnswerOption) -> Unit,
    onConfirmAnswerClicked: () -> Unit,
    liveQuestionState: LiveQuestionState?,
    currentPlayerName: String,
    questionType: QuestionType,
    isLoading: Boolean,
    playerSpecificState: com.example.quizduellosu.data.models.Player?
) {
    val isSpeedQuestion = questionType == QuestionType.SPEED
    val currentLiveStatus = liveQuestionState?.status
    val claimerPlayerId = liveQuestionState?.claimerPlayerId

    val canClaim = isSpeedQuestion && claimerPlayerId == null && currentLiveStatus == QuestionActivityStatus.OPEN_FOR_CLAIMS.name
    val isClaimerAndCanAnswer = isSpeedQuestion && claimerPlayerId == currentPlayerName && currentLiveStatus == QuestionActivityStatus.CLAIMED.name

    // Oyuncunun bu soruya daha önce cevap verip vermediği (çoktan seçmeli için)
    val answerSubmittedByMe = liveQuestionState?.answers?.containsKey(currentPlayerName) == true
    val isRevealed = currentLiveStatus == QuestionActivityStatus.REVEALED.name

    val answerButtonsEnabled = !isLoading && !isRevealed &&
        (!isSpeedQuestion || // Hız sorusu değilse VEYA
         currentLiveStatus == QuestionActivityStatus.OPEN_FOR_CLAIMS.name || // Henüz kimse claim etmediyse VEYA
         isClaimerAndCanAnswer) && // Claim eden biz isek ve cevaplama sırası bizdeyse
         !answerSubmittedByMe // Eğer bu oyuncu zaten cevap verdiyse (çoktan seçmeli için önemli)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Soru: $currentQuestionNumber / $totalQuestions", style = MaterialTheme.typography.titleMedium)
            Text("Süre: $timeLeftInSeconds sn", style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (question.imageUrl != null) {
            AsyncImage(
                model = question.imageUrl,
                contentDescription = "Soru Resmi",
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )
        } else {
             Spacer(modifier = Modifier.height(32.dp))
        }

        Text(
            text = question.text,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        if (isRevealed && playerSpecificState?.lastAnswerResult != null) {
            val resultText = when (playerSpecificState.lastAnswerResult) {
                AnswerResultType.CORRECT -> "Doğru! +${playerSpecificState.lastScoreChange ?: 0}"
                AnswerResultType.WRONG -> "Yanlış. ${playerSpecificState.lastScoreChange ?: 0}"
                AnswerResultType.NO_ANSWER -> "Cevaplanmadı."
                AnswerResultType.CLAIM_FAILED -> "Hızlı değildin!"
                else -> ""
            }
            val resultColor = when (playerSpecificState.lastAnswerResult) {
                AnswerResultType.CORRECT -> Color.Green.copy(alpha = 0.7f)
                AnswerResultType.WRONG -> Color.Red.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }
            Text(resultText, color = resultColor, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        } else if (isSpeedQuestion && claimerPlayerId != null && !isRevealed) {
             Text(
                text = if (claimerPlayerId == currentPlayerName) "Cevap hakkı sende!" else "${liveQuestionState?.claimerPlayerName ?: "Rakip"} cevaplıyor...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp)) // Yeterli boşluk için
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(question.options, key = { it.id }) { option ->
                AnswerButton(
                    option = option,
                    isSelected = selectedAnswer?.id == option.id,
                    onClick = { onAnswerSelected(option) },
                    enabled = answerButtonsEnabled,
                    isRevealed = isRevealed,
                    isCorrect = question.correctAnswerId == option.id,
                    isSelectedByPlayer = selectedAnswer?.id == option.id // Bu, seçili olanı vurgulamak için
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (!isRevealed) {
            val confirmButtonText = when {
                canClaim -> "Hemen Bas!"
                isClaimerAndCanAnswer -> "Cevabı Onayla!"
                isSpeedQuestion && claimerPlayerId != null && claimerPlayerId != currentPlayerName -> "Bekle..."
                else -> "Cevabı Onayla"
            }
            val confirmButtonEnabled = !isLoading && !answerSubmittedByMe &&
                    (canClaim ||
                     (isClaimerAndCanAnswer && selectedAnswer != null) ||
                     (!isSpeedQuestion && selectedAnswer != null && currentLiveStatus != QuestionActivityStatus.LOCKED.name))

            Button(
                onClick = onConfirmAnswerClicked,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = confirmButtonEnabled
            ) {
                Text(confirmButtonText, style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            }
        } else {
            Text("Cevaplar açıklandı...", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Joker Alanı (Yer Tutucu)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { /* TODO: Çalma Jokeri */ }, enabled = false) {
                Text("Çalma Jokeri")
            }
        }
    }
}

@Composable
fun AnswerButton(
    option: AnswerOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isRevealed: Boolean = false,
    isCorrect: Boolean = false,
    isSelectedByPlayer: Boolean = false // Oyuncunun bu cevabı seçip seçmediği
) {
    val buttonColors = when {
        isRevealed && isCorrect -> ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.6f))
        isRevealed && isSelectedByPlayer && !isCorrect -> ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f))
        isSelected -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        else -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    }
    val contentColor = when {
        isRevealed && isCorrect -> Color.Black
        isRevealed && isSelectedByPlayer && !isCorrect -> Color.White
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        colors = buttonColors,
        shape = MaterialTheme.shapes.medium,
        enabled = enabled && !isRevealed // Cevaplar açıklandıysa butonlar pasif
    ) {
        Text(
            text = "${option.id.uppercase()}) ${option.text}",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp),
            color = contentColor
        )
    }
}

// Örnek soru döndüren yardımcı fonksiyon
fun aktuellSoru(room: com.example.quizduellosu.data.models.Room): Question? {
    if (room.activeQuestionIds.isNotEmpty() && room.currentQuestionIndex >= 0 && room.currentQuestionIndex < room.activeQuestionIds.size) {
        val currentQuestionIdWithTheme = room.activeQuestionIds[room.currentQuestionIndex]
        val parts = currentQuestionIdWithTheme.split("/")
        val questionId = if (parts.size > 1) parts[1] else parts[0] // theme/id veya sadece id formatı

        // ViewModel'dan gerçek soru tipi ve doğru cevap alınacak.
        // Bu placeholder fonksiyon, ViewModel'daki getQuestionFromFirebase'in yerini tutuyor.
        // Preview'larda bu fonksiyon kullanılmaya devam edilebilir ama canlıda ViewModel'dan gelen veri kullanılmalı.
        val questionType = if (room.currentQuestionIndex % 2 == 0) QuestionType.SPEED else QuestionType.IMAGE_QUESTION
        return Question(
            id = questionId,
            text = "Bu bir ${questionType.name} sorusudur ($questionId). Cevap nedir?",
            options = listOf(
                AnswerOption("a", "Cevap A"), AnswerOption("b", "Cevap B"),
                AnswerOption("c", "Cevap C"), AnswerOption("d", "Cevap D")
            ),
            imageUrl = if(questionType == QuestionType.IMAGE_QUESTION) "https://via.placeholder.com/600x400.png?text=Soru+Resmi" else null,
            questionType = questionType,
            correctAnswerId = "a" // Örnek doğru cevap
        )
    }
    return null
}

@Preview(showBackground = true, widthDp = 380, heightDp = 720)
@Composable
fun QuestionDisplayScreenPreview_TextQuestion() {
    val sampleQuestion = Question("1","Türkiye'nin başkenti neresidir?", null,
        listOf(AnswerOption("a", "İstanbul"), AnswerOption("b", "Ankara"), AnswerOption("c", "İzmir"), AnswerOption("d", "Bursa")),
        QuestionType.SPEED, "b"
    )
    QuizDuellosuTheme {
        ActualQuestionUI(sampleQuestion, 12, 3, 20, null, {}, {},
            com.example.quizduellosu.data.models.LiveQuestionState(status = QuestionActivityStatus.OPEN_FOR_CLAIMS.name),
            "TestOyuncu", sampleQuestion.questionType, false, null)
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 720)
@Composable
fun QuestionDisplayScreenPreview_ImageQuestion_ClaimedByOther_Revealed() {
    val sampleQuestion = Question("2", "Bu görseldeki ünlü yapı hangisidir?", "https://via.placeholder.com/600x400.png?text=Örnek+Resim",
        listOf(AnswerOption("a", "Eyfel Kulesi"), AnswerOption("b", "Pisa Kulesi"), AnswerOption("c", "Tac Mahal"), AnswerOption("d", "Kolezyum")),
        QuestionType.SPEED, "a"
    )
    QuizDuellosuTheme {
        ActualQuestionUI(sampleQuestion, 0, 1, 10, null, {}, {},
            com.example.quizduellosu.data.models.LiveQuestionState(
                questionId = "2", status = QuestionActivityStatus.REVEALED.name,
                claimerPlayerId = "otherPlayer", claimerPlayerName = "RakipOyuncu",
                answers = mapOf("otherPlayer" to "a", "TestOyuncu" to "b")
            ),
            "TestOyuncu", sampleQuestion.questionType, false,
            com.example.quizduellosu.data.models.Player(name="TestOyuncu", lastAnswerResult = AnswerResultType.WRONG, lastScoreChange = -1)
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 720)
@Composable
fun QuestionDisplayScreenPreview_SpeedQuestion_ClaimedByMe_Correct_Revealed() {
    val sampleQuestion = Question("1", "Hangisi bir programlama dili değildir?", null,
        listOf(AnswerOption("a", "Python"), AnswerOption("b", "HTML"), AnswerOption("c", "Java"), AnswerOption("d", "C++")),
        QuestionType.SPEED, "b"
    )
    val currentPlayer = "TestOyuncuBen"
    QuizDuellosuTheme {
        ActualQuestionUI(sampleQuestion, 0, 5, 15, sampleQuestion.options[1], {}, {},
            com.example.quizduellosu.data.models.LiveQuestionState(
                questionId = "1", status = QuestionActivityStatus.REVEALED.name,
                claimerPlayerId = currentPlayer, claimerPlayerName = currentPlayer,
                answers = mapOf(currentPlayer to "b")
            ),
            currentPlayer, sampleQuestion.questionType, false,
            com.example.quizduellosu.data.models.Player(name=currentPlayer, lastAnswerResult = AnswerResultType.CORRECT, lastScoreChange = 3)
        )
    }
}
[end of app/src/main/java/com/example/quizduellosu/ui/screens/QuestionDisplayScreen.kt]
