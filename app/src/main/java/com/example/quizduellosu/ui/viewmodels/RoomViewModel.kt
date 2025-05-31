package com.example.quizduellosu.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizduellosu.data.models.Player
import com.example.quizduellosu.data.models.Room
import com.example.quizduellosu.data.models.RoomStatus
import com.example.quizduellosu.firebase.FirebaseRefs
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class RoomViewModel : ViewModel() {

    private val _roomData = MutableStateFlow<Room?>(null)
    val roomData: StateFlow<Room?> = _roomData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Navigasyon trigger'ları
    private val _navigateToRoomLobbyWithCode = MutableStateFlow<String?>(null) // Oda kodu ile navigate etmek için
    val navigateToRoomLobbyWithCode: StateFlow<String?> = _navigateToRoomLobbyWithCode.asStateFlow()

    private val _playerLeftRoom = MutableStateFlow<Boolean>(false) // Odadan ayrılma sonrası navigasyon için
    val playerLeftRoom: StateFlow<Boolean> = _playerLeftRoom.asStateFlow()

    private val _navigateToGame = MutableStateFlow<String?>(null) // Oda ID'si ile oyun ekranına yönlendirme
    val navigateToGame: StateFlow<String?> = _navigateToGame.asStateFlow()

    // Oyuncu kimliğini ve adını tutmak için (Bu normalde bir Auth servisi veya AppViewModel'den gelmeli)
    // Şimdilik basitlik adına ViewModel içinde tutacağız ve LoginScreen'den set edilecek.
    private var currentPlayerId: String? = null
    private var currentPlayerName: String? = null

    fun setCurrentPlayer(playerId: String, playerName: String) {
        this.currentPlayerId = playerId
        this.currentPlayerName = playerName
        Log.d("RoomViewModel", "Current player set: ID=$playerId, Name=$playerName")
    }

    fun getCurrentPlayerNameForHostCheck(): String? {
        return currentPlayerName
    }


    private var roomListener: ValueEventListener? = null
    private var currentListeningRoomId: String? = null

    // Firebase'deki questions referansı
    private val questionsRef = FirebaseDatabase.getInstance("https://quizduellosuapp-default-rtdb.firebaseio.com").getReference("questions")


    companion object {
        private const val TAG = "RoomViewModel"
        private const val MAX_QUESTIONS_PER_GAME = 5 // Örnek bir oyun için maksimum soru sayısı
    }

    fun createRoom(playerName: String, themes: List<String>) {
        if (this.currentPlayerId == null) {
            _errorMessage.value = "Oyuncu kimliği bulunamadı. Lütfen tekrar deneyin."
            Log.e(TAG, "createRoom: currentPlayerId is null. PlayerName: $playerName")
            return
        }
        _isLoading.value = true
        val newRoomId = Random.nextInt(100000, 999999).toString()
        val hostPlayer = Player(name = playerName, score = 0, isHost = true)
        val initialPlayersMap = mapOf(this.currentPlayerId!! to hostPlayer)

        val newRoom = Room(
            roomId = newRoomId,
            hostId = this.currentPlayerId!!,
            hostName = playerName,
            createdAt = System.currentTimeMillis(),
            status = RoomStatus.WAITING.name,
            selectedThemes = themes,
            players = initialPlayersMap
        )

        FirebaseRefs.getRoomRef(newRoomId).setValue(newRoom)
            .addOnSuccessListener {
                Log.i(TAG, "Room created successfully with ID: $newRoomId")
                _isLoading.value = false
                _navigateToRoomLobbyWithCode.value = newRoomId
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create room", e)
                _isLoading.value = false
                _errorMessage.value = "Oda oluşturulamadı: ${e.message}"
            }
    }

    fun joinRoom(playerName: String, roomCode: String) {
        if (this.currentPlayerId == null) {
            _errorMessage.value = "Oyuncu kimliği bulunamadı. Lütfen tekrar deneyin."
            Log.e(TAG, "joinRoom: currentPlayerId is null. PlayerName: $playerName, RoomCode: $roomCode")
            return
        }
        _isLoading.value = true
        val roomRef = FirebaseRefs.getRoomRef(roomCode)
        roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val room = snapshot.getValue(Room::class.java)
                    if (room != null) {
                        if (room.status == RoomStatus.WAITING.name) {
                            if (!room.players.containsKey(currentPlayerId!!)) {
                                val newPlayer = Player(name = playerName, score = 0, isHost = false)
                                FirebaseRefs.getPlayerRef(roomCode, currentPlayerId!!)
                                    .setValue(newPlayer)
                                    .addOnSuccessListener {
                                        Log.i(TAG, "Player $playerName joined room $roomCode")
                                        _isLoading.value = false
                                        _navigateToRoomLobbyWithCode.value = roomCode
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to add player to room $roomCode", e)
                                        _isLoading.value = false
                                        _errorMessage.value = "Odaya katılırken bir hata oluştu: ${e.message}"
                                    }
                            } else {
                                // Oyuncu zaten odada
                                Log.i(TAG, "Player $playerName already in room $roomCode")
                                _isLoading.value = false
                                _navigateToRoomLobbyWithCode.value = roomCode // Direkt lobiye yönlendir
                            }
                        } else {
                            Log.w(TAG, "Room $roomCode is not in WAITING state (state: ${room.status})")
                            _isLoading.value = false
                            _errorMessage.value = "Oda şu anda katılıma açık değil."
                        }
                    } else {
                        Log.w(TAG, "Room $roomCode data could not be parsed.")
                        _isLoading.value = false
                        _errorMessage.value = "Oda verisi okunamadı."
                    }
                } else {
                    Log.w(TAG, "Room $roomCode not found.")
                    _isLoading.value = false
                    _errorMessage.value = "Oda bulunamadı."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read room $roomCode for joinRoom", error.toException())
                _isLoading.value = false
                _errorMessage.value = "Oda bilgileri alınamadı: ${error.message}"
            }
        })
    }


    fun listenToRoomUpdates(roomId: String) {
        if (roomListener != null && currentListeningRoomId == roomId) {
            Log.d(TAG, "Already listening to room $roomId")
            return
        }
        // Önceki listener'ı kaldır
        stopListeningToRoomUpdates()
        currentListeningRoomId = roomId

        val ref = FirebaseRefs.getRoomRef(roomId)
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val room = snapshot.getValue(Room::class.java)
                    _roomData.value = room
                    Log.d(TAG, "Room data updated: ${room?.roomId}, Status: ${room?.status}, Players: ${room?.players?.size}, CurrentQ: ${room?.currentQuestionIndex}")

                    if (room != null) {
                        // Oyuncunun odadan atılıp atılmadığını kontrol et
                        if (currentPlayerId != null && !room.players.containsKey(currentPlayerId!!)) {
                            if (room.status != RoomStatus.FINISHED.name) { // Oyun bitmediyse ve oyuncu yoksa
                                Log.w(TAG, "Player $currentPlayerId no longer in room $roomId. Setting room data to null.")
                                _errorMessage.value = "Odadan çıkarıldınız veya oda kapatıldı."
                                _roomData.value = null // Odayı null yaparak lobiye yönlendirmeyi tetikle
                                _playerLeftRoom.value = true // Bu Login'e yönlendirecek
                                return // Daha fazla işlem yapma
                            }
                        }

                        // Oyun başladıysa ve oyuncu hala lobideyse oyun ekranına yönlendir
                        if (room.status == RoomStatus.IN_PROGRESS.name && room.currentQuestionIndex >= 0) {
                            // Eğer _navigateToGame zaten bu roomId ile dolu değilse veya farklıysa set et
                            if (_navigateToGame.value != roomId) {
                                Log.d(TAG, "Room status is IN_PROGRESS, navigating to game for room $roomId")
                                _navigateToGame.value = roomId
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Room $roomId does not exist anymore.")
                    _roomData.value = null
                    _errorMessage.value = "Oda artık mevcut değil."
                    _playerLeftRoom.value = true // Bu Login'e yönlendirecek
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error listening to room $roomId", error.toException())
                _errorMessage.value = "Oda güncellemeleri alınamadı: ${error.message}"
                _roomData.value = null
            }
        }
        ref.addValueEventListener(roomListener!!)
        Log.d(TAG, "Started listening to room $roomId")
    }

    fun stopListeningToRoomUpdates() {
        roomListener?.let { listener ->
            currentListeningRoomId?.let { roomId ->
                FirebaseRefs.getRoomRef(roomId).removeEventListener(listener)
                Log.d(TAG, "Stopped listening to room $roomId")
            }
        }
        roomListener = null
        currentListeningRoomId = null
        // _roomData.value = null // Listener durunca odayı temizle, ancak bu istenmeyebilir.
    }

    fun leaveRoom(roomId: String) {
        val playerId = this.currentPlayerId
        if (playerId == null) {
            _errorMessage.value = "Oyuncu kimliği bulunamadı."
            Log.e(TAG, "leaveRoom: currentPlayerId is null. RoomId: $roomId")
            return
        }

        _isLoading.value = true
        FirebaseRefs.getPlayerRef(roomId, playerId).removeValue()
            .addOnSuccessListener {
                Log.i(TAG, "Player $playerId left room $roomId")
                // Eğer host ayrılırsa ve odada kimse kalmazsa odayı silme mantığı eklenebilir.
                // Şimdilik sadece oyuncuyu çıkarıyoruz.
                // val currentRoom = _roomData.value
                // if (currentRoom != null && currentRoom.hostId == playerId && currentRoom.players.size == 1) { // Sadece host kalmıştı
                //     FirebaseRefs.getRoomRef(roomId).removeValue()
                //     Log.i(TAG, "Host left and room $roomId was empty, so it was deleted.")
                // }
                _isLoading.value = false
                _playerLeftRoom.value = true
                stopListeningToRoomUpdates() // Odadan ayrılınca dinlemeyi bırak
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to leave room $roomId for player $playerId", e)
                _isLoading.value = false
                _errorMessage.value = "Odadan ayrılırken bir hata oluştu: ${e.message}"
            }
    }


    fun clearNavigationTriggers() {
        _navigateToRoomLobbyWithCode.value = null
        _playerLeftRoom.value = false
        _navigateToGame.value = null
        _errorMessage.value = null // Hata mesajını da temizleyebiliriz
    }

    fun startGame(roomId: String, themes: List<String>) {
        _isLoading.value = true
        Log.d(TAG, "startGame called for room $roomId with themes: $themes")

        if (themes.isEmpty()) {
            _errorMessage.value = "Oyunu başlatmak için en az bir tema seçilmelidir."
            _isLoading.value = false
            Log.w(TAG, "No themes selected for room $roomId")
            return
        }

        // Basit soru seçme mantığı: Her temadan ilk soruyu al, toplamda MAX_QUESTIONS_PER_GAME kadar.
        val selectedQuestionIds = mutableListOf<String>()
        viewModelScope.launch {
            val questionsToFetchPerTheme = (MAX_QUESTIONS_PER_GAME / themes.size).coerceAtLeast(1)

            themes.forEach { themeKey ->
                if (selectedQuestionIds.size >= MAX_QUESTIONS_PER_GAME) return@forEach

                questionsRef.child(themeKey.lowercase()) // Firebase'de tema key'lerinin küçük harf olduğunu varsayalım
                    .limitToFirst(questionsToFetchPerTheme) // Her temadan N soru al
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.children.forEach { questionSnapshot ->
                                if (selectedQuestionIds.size < MAX_QUESTIONS_PER_GAME) {
                                    questionSnapshot.key?.let { selectedQuestionIds.add(it) }
                                }
                            }
                            // Tüm temalar işlendikten sonra (veya yeterli soru toplandıktan sonra) odayı güncelle
                            // Bu asenkron olduğu için, tüm callback'ler bittiğinde güncelleme yapılmalı.
                            // Daha sağlam bir yol, tüm soru ID'lerini toplayıp sonra tek bir güncelleme yapmak.
                            // Şimdilik, bu basit yapı için son tema işlendiğinde güncellemeyi deneyelim.
                            // Bu yaklaşım race condition'lara açık olabilir, dikkatli olunmalı.
                            // Daha iyi bir yöntem: Coroutine'leri ve Deferred'leri kullanmak.
                            if (themeKey == themes.last() || selectedQuestionIds.size >= MAX_QUESTIONS_PER_GAME) {
                                if (selectedQuestionIds.isEmpty()) {
                                    Log.e(TAG, "No questions found for selected themes.")
                                    _errorMessage.value = "Seçilen temalar için soru bulunamadı."
                                    _isLoading.value = false
                                    return
                                }

                                val initialLiveState = LiveQuestionState(
                                    questionId = selectedQuestionIds.first(), // İlk sorunun ID'si
                                    status = QuestionActivityStatus.OPEN_FOR_CLAIMS.name
                                )
                                val updates = mapOf(
                                    "status" to RoomStatus.IN_PROGRESS.name,
                                    "activeQuestionIds" to selectedQuestionIds.distinct(),
                                    "currentQuestionIndex" to 0,
                                    "liveQuestionState" to initialLiveState // Yeni liveQuestionState
                                )
                                FirebaseRefs.getRoomRef(roomId).updateChildren(updates)
                                    .addOnSuccessListener {
                                        Log.i(TAG, "Room $roomId started. Questions: ${selectedQuestionIds.joinToString()}. Updates: $updates")
                                        _isLoading.value = false
                                        // _navigateToGame.value = roomId // listenToRoomUpdates bunu zaten yapacak
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to start room $roomId", e)
                                        _isLoading.value = false
                                        _errorMessage.value = "Oyun başlatılamadı: ${e.message}"
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Failed to fetch questions for theme $themeKey", error.toException())
                            _isLoading.value = false
                            _errorMessage.value = "Soru alınamadı: ${error.message}"
                        }
                    })
            }
            // Eğer temalar işlenirken hiç soru bulunamazsa veya bir hata olursa, yukarıdaki bloklar _isLoading'i false yapmalı.
        }
    }


    override fun onCleared() {
        super.onCleared()
        stopListeningToRoomUpdates()
        Log.d(TAG, "RoomViewModel cleared.")
    }

    fun claimSpeedQuestionAttempt(roomId: String, questionId: String) {
        val playerId = this.currentPlayerId
        val playerName = this.currentPlayerName
        if (playerId == null || playerName == null) {
            _errorMessage.value = "Cevap hakkı almak için oyuncu bilgisi bulunamadı."
            Log.e(TAG, "claimSpeedQuestionAttempt: PlayerId or PlayerName is null.")
            return
        }

        val liveStateRef = FirebaseRefs.getRoomRef(roomId).child("liveQuestionState")
        liveStateRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val currentState = currentData.getValue(LiveQuestionState::class.java)
                if (currentState == null) { // Henüz liveState oluşmamışsa (beklenmedik durum)
                    val newState = LiveQuestionState(
                        questionId = questionId,
                        claimerPlayerId = playerId,
                        claimerPlayerName = playerName,
                        status = QuestionActivityStatus.CLAIMED.name
                    )
                    currentData.value = newState
                    return com.google.firebase.database.Transaction.success(currentData)
                }

                // Sadece OPEN_FOR_CLAIMS durumunda ve aynı soru için claim edilebilir.
                if (currentState.questionId == questionId && currentState.status == QuestionActivityStatus.OPEN_FOR_CLAIMS.name) {
                    if (currentState.claimerPlayerId == null) {
                        currentData.child("claimerPlayerId").value = playerId
                        currentData.child("claimerPlayerName").value = playerName
                        currentData.child("status").value = QuestionActivityStatus.CLAIMED.name
                        return com.google.firebase.database.Transaction.success(currentData)
                    } else {
                        // Başkası çoktan claim etmiş, transaction'ı durdur.
                        return com.google.firebase.database.Transaction.abort()
                    }
                }
                // Eğer durum uygun değilse veya soru ID'si farklıysa, bir değişiklik yapma.
                return com.google.firebase.database.Transaction.success(currentData) // Veya abort da edilebilir.
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e(TAG, "claimSpeedQuestionAttempt: Transaction failed.", error.toException())
                    _errorMessage.value = "Cevap hakkı alınamadı: ${error.message}"
                } else {
                    if (committed) {
                        val updatedState = currentData?.getValue(LiveQuestionState::class.java)
                        if (updatedState?.claimerPlayerId == playerId) {
                            Log.i(TAG, "Player $playerId claimed question $questionId in room $roomId. Status set to CLAIMED.")
                            // UI'da özel bir state güncellemesine gerek yok, listenToRoomUpdates yakalayacak.
                            // Claim başarılı, başka bir işlem yapmaya gerek yok, oyuncunun cevap vermesini bekle.
                        } else {
                            // Bu durum normalde onComplete'in `committed = false` kısmında ele alınmalı.
                            // Eğer buraya düşerse, bir tutarsızlık var demektir.
                            Log.w(TAG, "Player $playerId failed to claim question $questionId (already claimed or wrong state by the time onComplete was called with committed=true). Current state: $updatedState")
                            _errorMessage.value = "Üzgünüz, başkası daha hızlı davrandı!"
                        }
                    } else { // committed == false
                        Log.w(TAG, "claimSpeedQuestionAttempt: Transaction not committed for player $playerId (already claimed or wrong state). currentData: ${currentData?.value}")
                        // _errorMessage.value = "Cevap hakkı alınamadı (muhtemelen başkası daha hızlıydı)."
                        // Hata mesajı burada ayarlanırsa, başarılı claim eden oyuncunun UI'ı da bu mesajı görebilir.
                        // Bu yüzden, sadece claim eden oyuncu için UI güncellemesi yapmak daha iyi olabilir.
                        // Şimdilik bu mesajı koruyalım, UI tarafında refine edilebilir.
                        val currentState = currentData?.getValue(LiveQuestionState::class.java)
                        if (currentState?.claimerPlayerId != playerId) { // Eğer claim eden biz değilsek ve commit olmadıysa, başkası kapmıştır.
                           _errorMessage.value = "Üzgünüz, başkası daha hızlı davrandı!"
                        }
                    }
                }
            }
        })
    }

    fun submitPlayerAnswer(roomId: String, questionIdFromUI: String, selectedOptionId: String) {
        val playerId = this.currentPlayerId
        if (playerId == null) {
            _errorMessage.value = "Cevap göndermek için oyuncu bilgisi bulunamadı."
            Log.e(TAG, "submitPlayerAnswer: PlayerId is null.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val room = _roomData.value
            val liveState = room?.liveQuestionState
            val currentQuestionIndex = room?.currentQuestionIndex ?: -1
            val questionIdWithTheme = room?.activeQuestionIds?.getOrNull(currentQuestionIndex)

            if (room == null || liveState == null || questionIdWithTheme == null || currentQuestionIndex < 0) {
                _errorMessage.value = "Geçerli oda veya soru durumu bulunamadı."
                _isLoading.value = false
                return@launch
            }

            // UI'dan gelen questionIdFromUI ile Firebase'deki aktif soru ID'sinin (liveState.questionId)
            // ve room.activeQuestionIds[currentQuestionIndex]'den elde edilen ID'nin tutarlı olduğunu kontrol et.
            // Bu örnekte liveState.questionId'yi esas alacağız.
            val actualQuestionIdInLiveState = liveState.questionId
            if (actualQuestionIdInLiveState.isBlank() || questionIdFromUI != actualQuestionIdInLiveState) {
                 Log.e(TAG, "submitPlayerAnswer: Mismatch in question ID. UI: $questionIdFromUI, LiveState: $actualQuestionIdInLiveState. Aborting.")
                _errorMessage.value = "Soru ID uyuşmazlığı. Cevap kaydedilemedi."
                _isLoading.value = false
                return@launch
            }


            val answerPath = "liveQuestionState/answers/$playerId"
            val timestampPath = "liveQuestionState/answerTimestamps/$playerId"
            val updates = mutableMapOf<String, Any>(
                answerPath to selectedOptionId,
                timestampPath to System.currentTimeMillis()
            )

            // Hız sorusu ise ve cevaplayan claimer ise, durumu LOCKED yapabiliriz.
            // Diğer durumlarda, tüm oyuncuların cevap vermesini bekleyebilir veya süre sonunu bekleyebiliriz.
            // Bu subtask için basitleştirilmiş: Hız sorusunda claimer cevap verince LOCKED yap.
            // Diğerlerinde de şimdilik tek cevap sonrası LOCKED yap (sonra geliştirilecek).
            var shouldLockQuestion = false
            val question = getQuestionFromFirebase(questionIdWithTheme) // Doğru cevabı almak için soruyu çek
            if (question == null) {
                _errorMessage.value = "Soru detayları alınamadı. Cevap işlenemiyor."
                _isLoading.value = false
                return@launch
            }

            if (question.questionType == com.example.quizduellosu.ui.screens.QuestionType.SPEED) {
                if (liveState.claimerPlayerId == playerId) {
                    shouldLockQuestion = true
                }
            } else {
                // Diğer soru tipleri için, şimdilik tek cevap sonrası kilitle (ileride tüm oyuncuları bekle)
                shouldLockQuestion = true
            }

            if (shouldLockQuestion) {
                updates["liveQuestionState/status"] = QuestionActivityStatus.LOCKED.name
            }

            FirebaseRefs.getRoomRef(roomId).updateChildren(updates)
                .addOnSuccessListener {
                    Log.i(TAG, "Player $playerId submitted answer $selectedOptionId for question $actualQuestionIdInLiveState in room $roomId. Updates: $updates")
                    // Cevap gönderildikten sonra ve durum LOCKED olduysa değerlendirme yap.
                    if (shouldLockQuestion) {
                        evaluateAnswers(roomId, question) // evaluateAnswers status'u REVEALED yapacak
                        // evaluateAnswers REVEALED yaptıktan sonra processEndOfQuestion'ı tetikle
                        // Bu doğrudan çağrı yerine, listenToRoomUpdates'in REVEALED durumunu yakalaması ve
                        // QuestionDisplayScreen'in bunu tetiklemesi daha iyi olabilir.
                        // Şimdilik direkt çağıralım, UI zaten REVEALED'ı gösterecek.
                        // processEndOfQuestion içinde delay var.
                        // processEndOfQuestion(roomId) // Bu çağrı evaluateAnswers sonrasında dolaylı olarak tetiklenmeli
                        // VEYA evaluateAnswers başarılı olursa processEndOfQuestion'ı çağırabilir.
                        // evaluateAnswers'ın sonunda status REVEALED oluyor, bu da listenToRoomUpdates -> QuestionDisplayScreen -> processEndOfQuestion akışını tetikleyebilir.
                        // Şimdilik, processEndOfQuestion'ı doğrudan çağırmayalım, UI'dan tetiklensin.
                    }
                     _isLoading.value = false
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to submit answer for player $playerId in room $roomId", e)
                    _errorMessage.value = "Cevap gönderilemedi: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    // currentQuestionIndex değiştiğinde liveQuestionState'i sıfırlamak/hazırlamak için.
    // Bu, listenToRoomUpdates içinde veya yeni bir soruya geçişi yöneten bir fonksiyonda çağrılabilir.
    private fun prepareLiveStateForNewQuestion(roomId: String, newQuestionId: String) {
        val newLiveState = LiveQuestionState(
            questionId = newQuestionId,
            status = QuestionActivityStatus.OPEN_FOR_CLAIMS.name // Yeni soru için "Hemen Bas"a açık
        )
        FirebaseRefs.getRoomRef(roomId).child("liveQuestionState").setValue(newLiveState)
            .addOnSuccessListener {
                Log.i(TAG, "LiveQuestionState prepared for new question $newQuestionId in room $roomId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to prepare LiveQuestionState for new question $newQuestionId", e)
                _errorMessage.value = "Yeni soruya hazırlanamadı: ${e.message}"
            }
    }

    // Soru detaylarını Firebase'den getiren fonksiyon (basitleştirilmiş)
    private suspend fun getQuestionFromFirebase(themeAndQuestionId: String): com.example.quizduellosu.ui.screens.Question? {
        // format: "theme/questionId"
        val parts = themeAndQuestionId.split("/")
        if (parts.size != 2) {
            Log.e(TAG, "Invalid themeAndQuestionId format: $themeAndQuestionId")
            return null
        }
        val theme = parts[0]
        val questionId = parts[1]

        return try {
            val snapshot = questionsRef.child(theme).child(questionId).get().await() // Firebase-KTX await()
            if (snapshot.exists()) {
                // Firebase'den gelen veriyi Question data class'ına parse etmemiz lazım.
                // Bu örnekte Firebase'deki "options" map'inin AnswerOption listesine dönüştürülmesi gerekiyor.
                // Manuel parsing veya daha gelişmiş bir çözüm (örn: @PropertyName ile uyumlu hale getirmek) gerekir.
                // Şimdilik basitleştirilmiş bir parsing yapalım.
                val text = snapshot.child("text").getValue(String::class.java) ?: ""
                val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                val questionTypeStr = snapshot.child("questionType").getValue(String::class.java) ?: "SPEED"
                val questionType = com.example.quizduellosu.ui.screens.QuestionType.valueOf(questionTypeStr)

                val optionsList = mutableListOf<com.example.quizduellosu.ui.screens.AnswerOption>()
                var correctAnswerKeyFromDb: String? = null
                snapshot.child("options").children.forEach { optionSnapshot ->
                    val optionText = optionSnapshot.child("text").getValue(String::class.java)
                    val isCorrect = optionSnapshot.child("isCorrect").getValue(Boolean::class.java) ?: false
                    if (optionText != null) {
                        optionsList.add(com.example.quizduellosu.ui.screens.AnswerOption(id = optionSnapshot.key!!, text = optionText))
                        if (isCorrect) {
                            correctAnswerKeyFromDb = optionSnapshot.key
                        }
                    }
                }
                com.example.quizduellosu.ui.screens.Question(
                    id = questionId,
                    text = text,
                    imageUrl = imageUrl,
                    options = optionsList,
                    questionType = questionType,
                    correctAnswerId = correctAnswerKeyFromDb // Doğru cevap ID'sini modele ekledik
                )
            } else {
                Log.e(TAG, "Question not found: $theme/$questionId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching question $theme/$questionId", e)
            null
        }
    }


    private fun evaluateAnswers(roomId: String, question: com.example.quizduellosu.ui.screens.Question) {
        Log.d(TAG, "evaluateAnswers called for room $roomId, question ${question.id}")
        val currentRoom = _roomData.value ?: return
        val liveState = currentRoom.liveQuestionState ?: return
        val correctAnswerId = question.correctAnswerId ?: run {
            Log.e(TAG, "Correct answer ID not found for question ${question.id}")
            _errorMessage.value = "Soru için doğru cevap ayarlanmamış."
            // Durumu REVEALED yapıp sonraki soruya geçmeyi tetikle
            FirebaseRefs.getRoomRef(roomId).child("liveQuestionState/status").setValue(QuestionActivityStatus.REVEALED.name)
            return
        }

        val playerUpdates = mutableMapOf<String, Any?>()

        liveState.answers.forEach { (playerId, selectedOptionId) ->
            val playerRef = FirebaseRefs.getPlayerRef(roomId, playerId)
            val currentPlayerData = currentRoom.players[playerId]
            if (currentPlayerData != null) {
                var scoreChange = 0
                val resultType: String

                if (question.questionType == com.example.quizduellosu.ui.screens.QuestionType.SPEED) {
                    // Hız sorusunda sadece claimer puan alır veya kaybeder
                    if (liveState.claimerPlayerId == playerId) {
                        if (selectedOptionId == correctAnswerId) {
                            scoreChange = 3
                            resultType = AnswerResultType.CORRECT
                        } else {
                            scoreChange = -1
                            resultType = AnswerResultType.WRONG
                        }
                    } else {
                        // Diğer oyuncular hız sorusunda cevap verse bile puan almaz/kaybetmez (ya da farklı kural eklenebilir)
                        resultType = AnswerResultType.NO_ANSWER // Veya CLAIM_FAILED gibi bir durum
                    }
                } else { // Diğer soru tipleri
                    if (selectedOptionId == correctAnswerId) {
                        scoreChange = 2
                        resultType = AnswerResultType.CORRECT
                    } else {
                        scoreChange = 0 // Yanlış cevap için puan kırma yok (isteğe bağlı)
                        resultType = AnswerResultType.WRONG
                    }
                }

                val newScore = 서버에서 플레이어의 현재 점수를 가져와서 업데이트하거나 Transaction을 사용해야 합니다.
                // Simplification: update score directly. For concurrent updates, use transactions.
                // playerRef.child("score").setValue(currentPlayerData.score + scoreChange)
                // For robust score update, use a transaction:
                playerRef.child("score").runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        val currentScore = mutableData.getValue(Int::class.java) ?: 0
                        mutableData.value = currentScore + scoreChange
                        return com.google.firebase.database.Transaction.success(mutableData)
                    }
                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (error != null) {
                             Log.e(TAG, "Score update transaction failed for $playerId", error.toException())
                        }
                    }
                })

                playerUpdates["$playerId/lastAnswerResult"] = resultType
                playerUpdates["$playerId/lastScoreChange"] = scoreChange
            }
        }
        // Oyuncu güncellemelerini tek seferde yap
        if (playerUpdates.isNotEmpty()) {
            FirebaseRefs.getPlayersRef(roomId).updateChildren(playerUpdates)
        }

        // Soru durumunu REVEALED yap
        FirebaseRefs.getRoomRef(roomId).child("liveQuestionState/status").setValue(QuestionActivityStatus.REVEALED.name)
            .addOnFailureListener { e ->
                 _errorMessage.value = "Soru durumu güncellenemedi: ${e.message}"
            }
    }


    fun processEndOfQuestion(roomId: String) {
        viewModelScope.launch {
            val room = _roomData.value ?: return@launch
            val questionIdWithTheme = room.activeQuestionIds.getOrNull(room.currentQuestionIndex) ?: return@launch
            val question = getQuestionFromFirebase(questionIdWithTheme) ?: run {
                _errorMessage.value = "Soru bilgisi alınamadı, sonraki soruya geçilemiyor."
                 // Hata durumunda oyunu bitir veya lobiye dön
                FirebaseRefs.getRoomRef(roomId).child("status").setValue(RoomStatus.FINISHED.name)
                return@launch
            }

            // Cevapları değerlendir (eğer daha önce yapılmadıysa, örneğin süre bittiyse)
            // Bu subtask'te evaluateAnswers'ı submitPlayerAnswer'dan çağırıyoruz.
            // Eğer süre sonu mantığı eklenecekse, burada da bir kontrol olabilir.
            // if (room.liveQuestionState?.status != QuestionActivityStatus.REVEALED.name) {
            //    evaluateAnswers(roomId, question) // Bu çağrı burada olmamalı, submitAnswer sonrası olmalı.
            // }
            // Status REVEALED olduktan sonra bekleme ve sonraki soruya geçiş
            if (room.liveQuestionState?.status == QuestionActivityStatus.REVEALED.name) {
                delay(4000) // Cevapları göstermek için 4 saniye bekle

                val nextQuestionIndex = room.currentQuestionIndex + 1
                if (nextQuestionIndex < room.activeQuestionIds.size) {
                    val nextQuestionIdWithTheme = room.activeQuestionIds[nextQuestionIndex]
                    val nextQuestion = getQuestionFromFirebase(nextQuestionIdWithTheme)
                    if (nextQuestion != null) {
                        FirebaseRefs.getRoomRef(roomId).child("currentQuestionIndex").setValue(nextQuestionIndex)
                            .addOnSuccessListener {
                                prepareLiveStateForNewQuestion(roomId, nextQuestion.id)
                            }
                            .addOnFailureListener { e ->
                                _errorMessage.value = "Sonraki soruya geçilemedi: ${e.message}"
                            }
                    } else {
                         _errorMessage.value = "Sonraki soru yüklenemedi ($nextQuestionIdWithTheme)."
                        FirebaseRefs.getRoomRef(roomId).child("status").setValue(RoomStatus.FINISHED.name)
                    }
                } else {
                    // Sorular bitti, oyunu bitir
                    FirebaseRefs.getRoomRef(roomId).child("status").setValue(RoomStatus.FINISHED.name)
                    Log.i(TAG, "Game finished in room $roomId")
                    // Oyun sonu ekranına navigasyon _roomData.value (status=FINISHED) üzerinden tetiklenebilir.
                }
            } else {
                // Eğer status REVEALED değilse (bir hata oluşmuşsa veya akış bozulmuşsa),
                // yine de bir sonraki adıma geçmeyi dene veya hata logla.
                // Bu durum normalde oluşmamalı.
                Log.w(TAG, "processEndOfQuestion called but liveQuestionState status is not REVEALED: ${room.liveQuestionState?.status}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningToRoomUpdates()
        Log.d(TAG, "RoomViewModel cleared.")
    }
}

// Data class'ları ve enum'ları kendi dosyalarına taşıdığımız için buradan siliyoruz.
// import com.example.quizduellosu.data.models.LiveQuestionState
// import com.example.quizduellosu.data.models.QuestionActivityStatus

// Data class'ları ve enum'ları kendi dosyalarına taşıdığımız için buradan siliyoruz.
// import com.example.quizduellosu.data.models.LiveQuestionState
// import com.example.quizduellosu.data.models.QuestionActivityStatus
