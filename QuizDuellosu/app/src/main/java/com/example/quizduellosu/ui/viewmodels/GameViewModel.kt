package com.example.quizduellosu.ui.viewmodels

import android.app.Application
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.quizduellosu.data.models.AudioQuestion
import com.example.quizduellosu.data.models.FillInTheBlankQuestion
import com.example.quizduellosu.data.models.ImageQuestion
import com.example.quizduellosu.data.models.OrderingQuestion
import com.example.quizduellosu.data.models.PlayerState
import com.example.quizduellosu.data.models.Question
import com.example.quizduellosu.data.models.RevealingImageQuestion
import com.example.quizduellosu.data.models.SpeedQuestion
import com.example.quizduellosu.data.repositories.QuestionLoaderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Constructor updated to accept QuestionLoaderRepository
class GameViewModel(
    application: Application,
    private val questionLoaderRepository: QuestionLoaderRepository // Added repository as parameter
) : AndroidViewModel(application) {

    private val _questions = MutableLiveData<List<Question>>()
    val questions: LiveData<List<Question>> = _questions

    private val _currentQuestion = MutableLiveData<Question?>()
    val currentQuestion: LiveData<Question?> = _currentQuestion

    private val _playerStates = MutableLiveData<List<PlayerState>>()
    val playerStates: LiveData<List<PlayerState>> = _playerStates

    private val _activePlayerIndex = MutableLiveData<Int?>()
    val activePlayerIndex: LiveData<Int?> = _activePlayerIndex

    private val _showAnswerResult = MutableLiveData<String?>(null)
    val showAnswerResult: LiveData<String?> = _showAnswerResult

    private val _currentTime = MutableLiveData<Long>()
    val currentTime: LiveData<Long> = _currentTime
    private var questionTimer: CountDownTimer? = null

    private val _showPeriodicScoreSummary = MutableLiveData<String?>()
    val showPeriodicScoreSummary: LiveData<String?> = _showPeriodicScoreSummary
    private var questionsAnsweredThisSession: Int = 0

    private val _powerUpNotification = MutableLiveData<String?>()
    val powerUpNotification: LiveData<String?> = _powerUpNotification

    private val _showWheelEventNotification = MutableLiveData<String?>()
    val showWheelEventNotification: LiveData<String?> = _showWheelEventNotification
    private var wheelTimer: CountDownTimer? = null
    private var isWheelTimerStarted = false

    private val _soundEffectNotification = MutableLiveData<String?>()
    val soundEffectNotification: LiveData<String?> = _soundEffectNotification

    private val _finalScoreData = MutableLiveData<String?>()
    val finalScoreData: LiveData<String?> = _finalScoreData


    private val playersWhoAttemptedThisQuestion = mutableSetOf<String>()
    // Public getter for testing
    internal val playersWhoAttemptedThisQuestionPublicForTest: Set<String> get() = playersWhoAttemptedThisQuestion


    private var allPlayerNames: List<String> = emptyList()
    private var selectedTopics: List<String> = emptyList()
    private var questionIndex = 0
    private val POINTS_FOR_CORRECT_ANSWER = 10
    private val NEGATIVE_POINTS_FOR_SPEED_QUESTION = -5
    private val WHEEL_BONUS_POINTS = 5


    companion object {
        private const val TAG = "GameVM"
        private const val HARDWARE_SIM_TAG = "HardwareSim" 
        private const val NEXT_QUESTION_DELAY_MS = 2000L
        private const val QUESTION_TIME_LIMIT_MS: Long = 30000L
        private const val ORDERING_QUESTION_TIME_LIMIT_MS: Long = 15000L
        private const val TIMER_INTERVAL_MS: Long = 1000L
        private const val SCORE_SUMMARY_INTERVAL = 6 
        private const val WHEEL_TIMER_INTERVAL_MS: Long = 600000L 
        private const val WHEEL_TIMER_TICK_MS: Long = 1000L 
    }

    fun setGameParameters(playerNames: List<String>, topics: List<String>) {
        allPlayerNames = playerNames
        selectedTopics = topics
        val initialPlayerStates = playerNames.map { PlayerState(playerName = name, id = name, hasStealPowerUp = true) }
        _playerStates.value = initialPlayerStates
        _activePlayerIndex.value = null
        questionsAnsweredThisSession = 0
        _finalScoreData.value = null 
        initialLoadQuestions() 
        if (!isWheelTimerStarted) {
            startWheelTimer()
            isWheelTimerStarted = true
        }
    }

    // Renamed from loadSampleQuestions to initialLoadQuestions as per test
    internal fun initialLoadQuestions() { // Made internal for test access if needed, or keep private
        val loadedQuestions = questionLoaderRepository.loadQuestionsFromJson() // Removed context parameter
        if (loadedQuestions.isEmpty()) {
            Log.e(TAG, "JSON'dan hiç soru yüklenemedi! Oyun başlatılamıyor.")
            _showAnswerResult.value = "Hata: Sorular yüklenemedi! Lütfen uygulamayı kontrol edin."
            _currentQuestion.value = null 
            return
        }
        
        val filteredQuestions = if (selectedTopics.isNotEmpty() && !selectedTopics.contains("Genel")) {
            loadedQuestions.filter { question -> selectedTopics.contains(question.topic) }
        } else {
            loadedQuestions
        }

        if (filteredQuestions.isEmpty() && selectedTopics.isNotEmpty() && !selectedTopics.contains("Genel")) {
            Log.w(TAG, "Seçilen konulara (${selectedTopics.joinToString()}) uygun soru bulunamadı. Tüm sorular kullanılıyor.")
            _questions.value = loadedQuestions.shuffled()
            _showAnswerResult.value = "Uyarı: Seçilen konulara uygun soru bulunamadı. Tüm sorularla oynanıyor."
        } else if (filteredQuestions.isEmpty()) {
             Log.e(TAG, "Filtreleme sonrası hiç soru kalmadı. Oyun başlatılamıyor.")
            _showAnswerResult.value = "Hata: Oynanacak soru bulunamadı!"
            _currentQuestion.value = null
            return
        }
        else {
            _questions.value = filteredQuestions.shuffled()
        }

        Log.d(TAG, "${_questions.value?.size ?: 0} soru yüklendi ve karıştırıldı. Seçilen konular: ${selectedTopics.joinToString()}")


        questionIndex = 0
        if (_questions.value?.isNotEmpty() == true) {
            _currentQuestion.value = _questions.value!![questionIndex]
            triggerQuestionTimer(_currentQuestion.value)
        } else {
            _currentQuestion.value = null
            _showAnswerResult.value = "Oynanacak soru bulunamadı!"
            Log.e(TAG, "Hiç soru yüklenemedi veya filtrelenemedi, oyun başlatılamıyor.")
        }
        _activePlayerIndex.value = null
        _showPeriodicScoreSummary.value = null
        playersWhoAttemptedThisQuestion.clear()
    }


    private fun triggerQuestionTimer(question: Question?) {
        when (question) {
            is OrderingQuestion -> startQuestionTimer(ORDERING_QUESTION_TIME_LIMIT_MS)
            null -> questionTimer?.cancel()
            else -> startQuestionTimer()
        }
    }

    private fun startQuestionTimer(specificTimeLimitMs: Long? = null) {
        questionTimer?.cancel()
        val timeLimit = specificTimeLimitMs ?: QUESTION_TIME_LIMIT_MS
        _currentTime.value = timeLimit / TIMER_INTERVAL_MS
        questionTimer = object : CountDownTimer(timeLimit, TIMER_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                _currentTime.value = millisUntilFinished / TIMER_INTERVAL_MS
            }
            override fun onFinish() {
                _currentTime.value = 0
                _showAnswerResult.value = "Süre doldu! Cevap hakkı kalmadı."
                _activePlayerIndex.value = null
                Log.d(TAG, "Süre doldu!")
                moveToNextQuestion(cancelTimer = false)
            }
        }
        questionTimer?.start()
        Log.d(TAG, "Soru zamanlayıcısı başlatıldı (${timeLimit / 1000} s).")
    }

    private fun startWheelTimer() {
        wheelTimer?.cancel()
        wheelTimer = object : CountDownTimer(WHEEL_TIMER_INTERVAL_MS, WHEEL_TIMER_TICK_MS) {
            override fun onTick(millisUntilFinished: Long) {
            }
            override fun onFinish() {
                Log.d(TAG, "Wheel Timer Finished. Triggering event.")
                triggerWheelEvent()
            }
        }
        wheelTimer?.start()
        Log.d(TAG, "Şans Çarkı zamanlayıcısı başlatıldı (${WHEEL_TIMER_INTERVAL_MS / 1000 / 60} dakika).")
    }

    private fun triggerWheelEvent() {
        val currentPlayers = _playerStates.value
        if (!currentPlayers.isNullOrEmpty()) {
            val randomPlayer = currentPlayers.random()
            updateScoreForPlayerById(randomPlayer.id, WHEEL_BONUS_POINTS)
            _showWheelEventNotification.value = "Şans Çarkı! ${randomPlayer.playerName}, +${WHEEL_BONUS_POINTS} puan kazandı!"
            Log.d(TAG, "Şans Çarkı: ${randomPlayer.playerName} +${WHEEL_BONUS_POINTS} puan kazandı.")
        } else {
            Log.d(TAG, "Şans Çarkı: Oyuncu bulunamadı.")
        }
        startWheelTimer() 
    }

    private fun updateScoreForPlayerById(playerId: String, pointsToAdd: Int) {
        val currentStates = _playerStates.value?.toMutableList()
        if (currentStates != null) {
            val playerIndex = currentStates.indexOfFirst { it.id == playerId }
            if (playerIndex != -1) {
                val player = currentStates[playerIndex]
                currentStates[playerIndex] = player.copy(score = player.score + pointsToAdd)
                _playerStates.value = currentStates
                Log.d(TAG, "${player.playerName} için skor güncellendi (Olay): ${currentStates[playerIndex].score}")
            } else {
                Log.d(TAG, "Skor güncellenemedi (Olay): Oyuncu ID '$playerId' bulunamadı.")
            }
        }
    }


    private fun actuallyLoadNextConcreteQuestion() {
        val currentList = _questions.value ?: return
        questionIndex++
        playersWhoAttemptedThisQuestion.clear() 
        if (questionIndex < currentList.size) {
            _currentQuestion.value = currentList[questionIndex]
            questionsAnsweredThisSession++
            triggerQuestionTimer(_currentQuestion.value)
        } else {
            _currentQuestion.value = null 
            _showAnswerResult.value = null 
            _finalScoreData.value = generateScoreSummaryString(gameOver = true)
            Log.d(TAG, "Oyun bitti!")
            Log.d(HARDWARE_SIM_TAG, "TAVAN IŞIKLARI normal duruma döndü. (Oyun Sonu - Simülasyon)")
            questionsAnsweredThisSession = 0
            questionTimer?.cancel()
            wheelTimer?.cancel() 
            isWheelTimerStarted = false 
        }
    }

    fun proceedToNextQuestionAfterSummary() {
        _showPeriodicScoreSummary.value = null
        _activePlayerIndex.value = null
        _showAnswerResult.value = null
        Log.d(HARDWARE_SIM_TAG, "TAVAN IŞIKLARI normal duruma döndü. (Puan Özeti Sonrası - Simülasyon)")
        actuallyLoadNextConcreteQuestion()
    }

    private fun moveToNextQuestion(cancelTimer: Boolean = true) {
        if (cancelTimer) {
            questionTimer?.cancel()
            Log.d(TAG, "Cevap verildi, zamanlayıcı durduruldu.")
        }
        Log.d(HARDWARE_SIM_TAG, "TAVAN IŞIKLARI normal duruma döndü. (Soru Geçişi Başlangıcı - Simülasyon)")
        viewModelScope.launch {
            delay(NEXT_QUESTION_DELAY_MS)
            if (questionsAnsweredThisSession > 0 && questionsAnsweredThisSession % SCORE_SUMMARY_INTERVAL == 0 && _currentQuestion.value != null) {
                _showPeriodicScoreSummary.value = generateScoreSummaryString(periodic = true)
            } else {
                _activePlayerIndex.value = null
                _showAnswerResult.value = null
                actuallyLoadNextConcreteQuestion()
            }
        }
    }
    
    private fun generateScoreSummaryString(periodic: Boolean = false, gameOver: Boolean = false): String {
        val title = when {
            gameOver -> "Oyun Bitti!\n\nFinal Puanları:"
            periodic -> "Ara Puan Durumu:"
            else -> "Puan Durumu:" 
        }
        return _playerStates.value?.joinToString(
            separator = "\n",
            prefix = "$title\n"
        ) { "${it.playerName}: ${it.score} puan" } ?: "Puan bilgisi yok."
    }

    private fun processAnswer(isCorrect: Boolean, correctAnswerDetails: String = "", questionType: Question?) {
        val activePlayerIdx = _activePlayerIndex.value
        val activePlayer = activePlayerIdx?.let { _playerStates.value?.getOrNull(it) }

        if (activePlayer == null) {
            _showAnswerResult.value = if (isCorrect) "Doğru cevap, ancak aktif oyuncu yok." else "Yanlış cevap! $correctAnswerDetails. Aktif oyuncu yok."
            _soundEffectNotification.value = "YANLIŞ CEVAP SESİ ÇALIYOR! (Simülasyon - Aktif Oyuncu Yok)"
            Log.d(TAG, "Cevap işlendi, aktif oyuncu yok.")
            if (isCorrect) { 
                questionTimer?.cancel()
                moveToNextQuestion(cancelTimer = false)
            }
            return
        }

        val q = questionType ?: currentQuestion.value ?: return

        if (q is SpeedQuestion || q is RevealingImageQuestion || q is AudioQuestion) {
            if (isCorrect) {
                questionTimer?.cancel()
                updateScoreForActivePlayer(POINTS_FOR_CORRECT_ANSWER, activePlayerIdx)
                _showAnswerResult.value = "Doğru! ${activePlayer.playerName} ${POINTS_FOR_CORRECT_ANSWER} puan kazandı!"
                _soundEffectNotification.value = "DOĞRU CEVAP SESİ ÇALIYOR! (Simülasyon)"
                moveToNextQuestion(cancelTimer = false)
            } else {
                playersWhoAttemptedThisQuestion.add(activePlayer.id)
                if (q is SpeedQuestion) {
                    updateScoreForActivePlayer(NEGATIVE_POINTS_FOR_SPEED_QUESTION, activePlayerIdx)
                    _showAnswerResult.value = "Yanlış cevap, ${activePlayer.playerName}! ${NEGATIVE_POINTS_FOR_SPEED_QUESTION} puan. Cevap hakkı başkasına geçebilir."
                } else {
                    _showAnswerResult.value = "Yanlış cevap, ${activePlayer.playerName}! Cevap hakkı başkasına geçebilir."
                }
                _soundEffectNotification.value = "YANLIŞ CEVAP SESİ ÇALIYOR! (Simülasyon)"
                _activePlayerIndex.value = null 
            }
        } else { 
            questionTimer?.cancel()
            if (isCorrect) {
                updateScoreForActivePlayer(POINTS_FOR_CORRECT_ANSWER, activePlayerIdx)
                _showAnswerResult.value = "Doğru! ${activePlayer.playerName} ${POINTS_FOR_CORRECT_ANSWER} puan kazandı!"
                _soundEffectNotification.value = "DOĞRU CEVAP SESİ ÇALIYOR! (Simülasyon)"
            } else {
                _showAnswerResult.value = "Yanlış cevap, ${activePlayer.playerName}! $correctAnswerDetails"
                _soundEffectNotification.value = "YANLIŞ CEVAP SESİ ÇALIYOR! (Simülasyon)"
            }
            moveToNextQuestion(cancelTimer = false)
        }
    }

    fun submitAnswer(selectedIndex: Int) {
        val q = currentQuestion.value ?: return
        val correct = when (q) {
            is SpeedQuestion -> q.correctOptionIndex == selectedIndex
            is ImageQuestion -> q.correctOptionIndex == selectedIndex
            is RevealingImageQuestion -> q.correctOptionIndex == selectedIndex
            is AudioQuestion -> q.correctOptionIndex == selectedIndex
            else -> {
                Log.d(TAG, "Bu soru tipi için şıklı cevap beklenmiyor: ${q.javaClass.simpleName}")
                _showAnswerResult.value = "Hatalı Soru Tipi!"
                moveToNextQuestion()
                return
            }
        }
        val details = if (!correct && q is SpeedQuestion) "Doğru: ${q.options.getOrNull(q.correctOptionIndex)}"
                      else if (!correct && q is ImageQuestion) "Doğru: ${q.options.getOrNull(q.correctOptionIndex)}"
                      else if (!correct && q is RevealingImageQuestion) "Doğru: ${q.options.getOrNull(q.correctOptionIndex)}"
                      else if (!correct && q is AudioQuestion) "Doğru: ${q.options.getOrNull(q.correctOptionIndex)}"
                      else ""
        processAnswer(correct, details, q)
    }

    fun submitAnswer(answerText: String) {
        val q = currentQuestion.value ?: return
        val correct = when (q) {
            is FillInTheBlankQuestion -> q.correctAnswers.any { it.equals(answerText.trim(), ignoreCase = true) }
            else -> {
                Log.d(TAG, "Bu soru tipi için metin cevabı beklenmiyor: ${q.javaClass.simpleName}")
                _showAnswerResult.value = "Hatalı Soru Tipi!"
                moveToNextQuestion()
                return
            }
        }
        val details = if (!correct && q is FillInTheBlankQuestion) "Doğru: ${q.correctAnswers.joinToString()}" else ""
        processAnswer(correct, details, q)
    }

    fun submitAnswer(orderedItemsList: List<String>) {
        val q = currentQuestion.value ?: return
        val correct = when (q) {
            is OrderingQuestion -> orderedItemsList == q.correctOrder
            else -> {
                Log.d(TAG, "Bu soru tipi için sıralama cevabı beklenmiyor: ${q.javaClass.simpleName}")
                _showAnswerResult.value = "Hatalı Soru Tipi!"
                moveToNextQuestion()
                return
            }
        }
        val details = if (!correct && q is OrderingQuestion) "Doğru: ${q.correctOrder.joinToString(" -> ")}" else ""
        processAnswer(correct, details, q)
    }

    fun claimAnswerAttempt() {
        if (_activePlayerIndex.value != null) {
            val currentPlayerName = _playerStates.value?.getOrNull(_activePlayerIndex.value!!)?.playerName ?: "Bilinmeyen"
            _showAnswerResult.value = "Zaten $currentPlayerName cevaplama hakkına sahip."
            Log.d(TAG, "Zaten bir oyuncu ($currentPlayerName) cevaplama hakkına sahip.")
            return
        }

        val availablePlayers = _playerStates.value ?: return
        var claimed = false
        for (i in availablePlayers.indices) { 
            val player = availablePlayers[i]
            if (!playersWhoAttemptedThisQuestion.contains(player.id)) {
                _activePlayerIndex.value = i
                val activePlayerName = player.playerName
                _showAnswerResult.value = "${activePlayerName} cevaplama hakkı kazandı! Şimdi cevaplayın."
                Log.d(TAG, "${activePlayerName} cevaplama hakkı kazandı!")
                Log.d(HARDWARE_SIM_TAG, "TAVAN IŞIKLARI '$activePlayerName' adlı oyuncuya yönlendirildi. (Simülasyon)")
                claimed = true
                break
            }
        }

        if (!claimed) {
            _showAnswerResult.value = "Tüm oyuncular bu soru için yanlış cevap verdi veya oyuncu yok."
            Log.d(TAG, "Kimse cevaplama hakkı kazanamadı, herkes denedi veya oyuncu yok.")
        }
    }
    
    private fun updateScoreForActivePlayer(points: Int, playerIndex: Int?) { 
        val activeIdx = playerIndex ?: _activePlayerIndex.value 
        if (activeIdx == null) {
            Log.d(TAG, "Puan verilecek aktif oyuncu yok!")
            return
        }
        val currentStates = _playerStates.value?.toMutableList()
        if (currentStates != null && activeIdx >= 0 && activeIdx < currentStates.size) {
            currentStates[activeIdx].score += points
            _playerStates.value = currentStates
            Log.d(TAG, "${currentStates[activeIdx].playerName} için skor güncellendi: ${currentStates[activeIdx].score}")
        } else {
            Log.d(TAG, "Skor güncellenemedi. Geçersiz aktif oyuncu indexi: $activeIdx veya state listesi null.")
        }
    }

    fun useStealPowerUp() {
        val activeIdx = _activePlayerIndex.value
        if (activeIdx == null) {
            _powerUpNotification.value = "Joker kullanmak için aktif bir oyuncu olmalı!"
            return
        }

        val currentPlayers = _playerStates.value ?: return
        val activePlayerState = currentPlayers.getOrNull(activeIdx)
        if (activePlayerState == null) {
            _powerUpNotification.value = "Aktif oyuncu bulunamadı!"
            return
        }

        if (!activePlayerState.hasStealPowerUp) {
            _powerUpNotification.value = "${activePlayerState.playerName}, çalma joker hakkınız kalmadı!"
            return
        }

        val q = currentQuestion.value
        val correctOptionText: String? = when (q) {
            is SpeedQuestion -> q.options.getOrNull(q.correctOptionIndex)
            is ImageQuestion -> q.options.getOrNull(q.correctOptionIndex)
            is RevealingImageQuestion -> q.options.getOrNull(q.correctOptionIndex)
            is AudioQuestion -> q.options.getOrNull(q.correctOptionIndex)
            else -> null
        }

        if (correctOptionText != null) {
            val updatedPlayerStates = currentPlayers.toMutableList()
            updatedPlayerStates[activeIdx] = activePlayerState.copy(hasStealPowerUp = false)
            _playerStates.value = updatedPlayerStates

            _powerUpNotification.value = "Joker Kullanıldı! Doğru şık: $correctOptionText"
            Log.d(TAG, "${activePlayerState.playerName} joker kullandı. Doğru şık: $correctOptionText")
        } else {
            _powerUpNotification.value = "Bu soru tipinde joker kullanılamaz."
        }
    }

    fun clearPowerUpNotification() { 
        _powerUpNotification.value = null
    }

    fun clearWheelEventNotification() {
        _showWheelEventNotification.value = null
    }

    fun clearSoundEffectNotification() {
        _soundEffectNotification.value = null
    }

    fun clearFinalScoreData() {
        _finalScoreData.value = null
    }

    override fun onCleared() {
        super.onCleared()
        questionTimer?.cancel()
        wheelTimer?.cancel()
        Log.d(TAG, "ViewModel temizlendi, tüm zamanlayıcılar iptal edildi.")
    }
}
