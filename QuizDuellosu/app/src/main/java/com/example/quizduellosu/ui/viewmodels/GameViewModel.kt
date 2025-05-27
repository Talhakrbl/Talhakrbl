package com.example.quizduellosu.ui.viewmodels

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizduellosu.data.models.AudioQuestion
import com.example.quizduellosu.data.models.FillInTheBlankQuestion
import com.example.quizduellosu.data.models.ImageQuestion
import com.example.quizduellosu.data.models.OrderingQuestion
import com.example.quizduellosu.data.models.PlayerState
import com.example.quizduellosu.data.models.Question
import com.example.quizduellosu.data.models.RevealingImageQuestion
import com.example.quizduellosu.data.models.SpeedQuestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

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

    private var allPlayerNames: List<String> = emptyList()
    private var selectedTopics: List<String> = emptyList()
    private var questionIndex = 0
    private val POINTS_FOR_CORRECT_ANSWER = 10

    companion object {
        private const val TAG = "GameVM"
        private const val NEXT_QUESTION_DELAY_MS = 2000L
        private const val QUESTION_TIME_LIMIT_MS: Long = 30000L
        private const val TIMER_INTERVAL_MS: Long = 1000L
    }

    fun setGameParameters(playerNames: List<String>, topics: List<String>) {
        allPlayerNames = playerNames
        selectedTopics = topics
        val initialPlayerStates = playerNames.map { PlayerState(playerName = name, id = name) }
        _playerStates.value = initialPlayerStates
        _activePlayerIndex.value = null
        loadSampleQuestions()
    }

    private fun loadSampleQuestions() {
        val allSampleQuestions = listOf(
            SpeedQuestion("sq1", "Türkiye'nin başkenti neresidir?", "Coğrafya", listOf("İstanbul", "Ankara", "İzmir", "Bursa"), 1),
            ImageQuestion("iq1", "Bu logodaki hayvan hangisidir?", "Genel Kültür", "placeholder_firefox_logo.png", listOf("Tilki", "Panda", "Sincap", "Rakun"), 0),
            FillInTheBlankQuestion("fbq1", "Fatih Sultan Mehmet, İstanbul'u ____ yılında fethetti.", "Tarih", listOf("1453")),
            OrderingQuestion("oq1", "Bu gezegenleri Güneş'e yakınlıklarına göre sıralayın:", "Bilim", listOf("Mars", "Venüs", "Dünya", "Merkür"), listOf("Merkür", "Venüs", "Dünya", "Mars")),
            RevealingImageQuestion("riq1", "Bu yavaşça beliren resimdeki ünlü kimdir?", "Sanat", "placeholder_mona_lisa.png", listOf("Mona Lisa", "Van Gogh", "Picasso", "Dali"), 0),
            AudioQuestion("aq1", "Bu hangi enstrümanın sesidir?", "Müzik", "placeholder_piano_sound.mp3", listOf("Keman", "Piyano", "Gitar", "Davul"), 1),
            SpeedQuestion("sq2", "Hangi gezegen 'Kızıl Gezegen' olarak bilinir?", "Bilim", listOf("Venüs", "Mars", "Jüpiter", "Satürn"), 1)
        )
        _questions.value = allSampleQuestions.shuffled()
        questionIndex = 0
        if (_questions.value?.isNotEmpty() == true) {
            _currentQuestion.value = _questions.value!![questionIndex]
            startQuestionTimer() // Start timer for the first question
        } else {
            _currentQuestion.value = null
        }
        _activePlayerIndex.value = null
        _showAnswerResult.value = null
    }

    private fun startQuestionTimer() {
        questionTimer?.cancel()
        _currentTime.value = QUESTION_TIME_LIMIT_MS / TIMER_INTERVAL_MS
        questionTimer = object : CountDownTimer(QUESTION_TIME_LIMIT_MS, TIMER_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                _currentTime.value = millisUntilFinished / TIMER_INTERVAL_MS
            }

            override fun onFinish() {
                _currentTime.value = 0 // Ensure it shows 0 when finished
                _showAnswerResult.value = "Süre doldu!"
                _activePlayerIndex.value = null // No one can answer now
                Log.d(TAG, "Süre doldu!")
                moveToNextQuestion(cancelTimer = false) // Timer already finished, no need to cancel again
            }
        }
        questionTimer?.start()
        Log.d(TAG, "Soru zamanlayıcısı başlatıldı.")
    }

    private fun loadNextConcreteQuestion() {
        val currentList = _questions.value ?: return
        questionIndex++
        if (questionIndex < currentList.size) {
            _currentQuestion.value = currentList[questionIndex]
            if (_currentQuestion.value != null) {
                startQuestionTimer() // Start timer for the new question
            }
        } else {
            _currentQuestion.value = null
            _showAnswerResult.value = "Oyun bitti!"
            Log.d(TAG, "Oyun bitti!")
            questionTimer?.cancel() // Game ended, cancel timer
        }
    }

    private fun moveToNextQuestion(cancelTimer: Boolean = true) {
        if (cancelTimer) {
            questionTimer?.cancel()
            Log.d(TAG, "Cevap verildi, zamanlayıcı durduruldu.")
        }
        viewModelScope.launch {
            delay(NEXT_QUESTION_DELAY_MS)
            _activePlayerIndex.value = null
            _showAnswerResult.value = null
            loadNextConcreteQuestion()
        }
    }

    private fun processAnswer(correct: Boolean, correctAnswerDetails: String = "") {
        questionTimer?.cancel() // Stop timer as soon as an answer is processed
        Log.d(TAG, "Cevap işleniyor, zamanlayıcı durduruldu (processAnswer).")

        if (_activePlayerIndex.value == null) {
            if (correct) {
                _showAnswerResult.value = "Doğru cevap, ancak aktif oyuncu belirlenmediği için puan verilmedi. Lütfen 'Cevapla' butonuna basın."
                Log.d(TAG, "Doğru cevap, ancak aktif oyuncu yok, puan verilmedi.")
            } else {
                _showAnswerResult.value = "Yanlış cevap! $correctAnswerDetails"
                Log.d(TAG, "Yanlış cevap, aktif oyuncu yok.")
            }
        } else {
            if (correct) {
                updateScoreForActivePlayer(POINTS_FOR_CORRECT_ANSWER)
                _showAnswerResult.value = "Doğru! Puan kazandınız!"
            } else {
                _showAnswerResult.value = "Yanlış cevap! $correctAnswerDetails"
                Log.d(TAG, "Yanlış cevap, aktif oyuncu: ${_playerStates.value?.getOrNull(_activePlayerIndex.value!! )?.playerName}")
            }
        }
        moveToNextQuestion(cancelTimer = false) // Timer already cancelled in processAnswer or onFinish
    }

    fun submitAnswer(selectedIndex: Int) {
        val q = currentQuestion.value ?: return
        var correct = false
        var details = ""
        when (q) {
            is SpeedQuestion -> {
                correct = q.correctOptionIndex == selectedIndex
                if (!correct) details = "Doğru: ${q.options.getOrNull(q.correctOptionIndex)}"
            }
            is ImageQuestion -> {
                correct = q.correctOptionIndex == selectedIndex
                if (!correct) details = "Doğru: ${q.options.getOrNull(q.correctOptionIndex)}"
            }
            is RevealingImageQuestion -> {
                correct = q.correctOptionIndex == selectedIndex
                if (!correct) details = "Doğru: ${q.options.getOrNull(q.correctOptionIndex)}"
            }
            is AudioQuestion -> {
                correct = q.correctOptionIndex == selectedIndex
                if (!correct) details = "Doğru: ${q.options.getOrNull(q.correctOptionIndex)}"
            }
            else -> {
                Log.d(TAG, "Bu soru tipi için şıklı cevap beklenmiyor: ${q.javaClass.simpleName}")
                _showAnswerResult.value = "Hatalı Soru Tipi!"
                moveToNextQuestion()
                return
            }
        }
        processAnswer(correct, details)
    }

    fun submitAnswer(answerText: String) {
        val q = currentQuestion.value ?: return
        var correct = false
        var details = ""
        when (q) {
            is FillInTheBlankQuestion -> {
                correct = q.correctAnswers.any { it.equals(answerText.trim(), ignoreCase = true) }
                if (!correct) details = "Doğru: ${q.correctAnswers.joinToString()}"
            }
            else -> {
                Log.d(TAG, "Bu soru tipi için metin cevabı beklenmiyor: ${q.javaClass.simpleName}")
                _showAnswerResult.value = "Hatalı Soru Tipi!"
                moveToNextQuestion()
                return
            }
        }
        processAnswer(correct, details)
    }

    fun submitAnswer(orderedItemsList: List<String>) {
        val q = currentQuestion.value ?: return
        var correct = false
        var details = ""
        when (q) {
            is OrderingQuestion -> {
                correct = orderedItemsList == q.correctOrder
                if (!correct) details = "Doğru: ${q.correctOrder.joinToString(" -> ")}"
            }
            else -> {
                Log.d(TAG, "Bu soru tipi için sıralama cevabı beklenmiyor: ${q.javaClass.simpleName}")
                _showAnswerResult.value = "Hatalı Soru Tipi!"
                moveToNextQuestion()
                return
            }
        }
        processAnswer(correct, details)
    }

    fun claimAnswerAttempt() {
        if (_activePlayerIndex.value == null) {
            if (_playerStates.value?.isNotEmpty() == true) {
                _activePlayerIndex.value = 0 
                val activePlayerName = _playerStates.value?.get(0)?.playerName ?: "Bilinmeyen"
                Log.d(TAG, "$activePlayerName cevaplama hakkı kazandı!")
                _showAnswerResult.value = "$activePlayerName cevaplama hakkı kazandı! Şimdi cevaplayın."
            } else {
                Log.w(TAG, "Oyuncu listesi boş, cevaplama hakkı verilemiyor.")
                _showAnswerResult.value = "Oyuncu yok!"
            }
        } else {
            val currentPlayerName = _playerStates.value?.getOrNull(_activePlayerIndex.value!!)?.playerName ?: "Bilinmeyen"
            Log.d(TAG, "Zaten bir oyuncu ($currentPlayerName) cevaplama hakkına sahip.")
            _showAnswerResult.value = "Zaten $currentPlayerName cevaplama hakkına sahip."
        }
    }
    
    private fun updateScoreForActivePlayer(points: Int) {
        val activeIdx = _activePlayerIndex.value
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

    fun clearAnswerResult() { 
        _showAnswerResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        questionTimer?.cancel()
        Log.d(TAG, "ViewModel temizlendi, zamanlayıcı iptal edildi.")
    }
}
