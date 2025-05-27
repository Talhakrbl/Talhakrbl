package com.example.quizduellosu.ui.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.example.quizduellosu.R
import com.example.quizduellosu.data.models.AudioQuestion
import com.example.quizduellosu.data.models.FillInTheBlankQuestion
import com.example.quizduellosu.data.models.ImageQuestion
import com.example.quizduellosu.data.models.OrderingQuestion
import com.example.quizduellosu.data.models.Question
import com.example.quizduellosu.data.models.RevealingImageQuestion
import com.example.quizduellosu.data.models.SpeedQuestion
import com.example.quizduellosu.databinding.ActivityGameBinding
import com.example.quizduellosu.databinding.LayoutFillInBlankContentBinding
import com.example.quizduellosu.databinding.LayoutImageContentBinding
import com.example.quizduellosu.databinding.LayoutOrderingContentBinding
import com.example.quizduellosu.ui.viewmodels.GameViewModel
import java.util.Locale

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val gameViewModel: GameViewModel by viewModels()
    private var scoreSummaryDialog: AlertDialog? = null
    private var wheelEventDialog: AlertDialog? = null
    private var gameOverDialog: AlertDialog? = null
    private var currentImageAnimator: ObjectAnimator? = null

    private var mediaPlayer: MediaPlayer? = null
    private var audioStopHandler: Handler? = null
    private var audioStopRunnable: Runnable? = null

    companion object {
        private const val REVEALING_IMAGE_ANIMATION_DURATION = 10000L 
        private const val AUDIO_PLAY_DURATION_MS = 10000L 
        private const val TAG = "GameActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playerNamesFromIntent = intent.getStringArrayListExtra("EXTRA_PLAYER_NAMES") ?: arrayListOf("Bilinmeyen Oyuncu")
        val selectedTopicsFromIntent = intent.getStringArrayListExtra("EXTRA_SELECTED_TOPICS") ?: arrayListOf("Genel")

        Log.d(TAG, "Oyuncular (Intent'ten): $playerNamesFromIntent")
        Log.d(TAG, "Seçilen Konular (Intent'ten): $selectedTopicsFromIntent")

        gameViewModel.setGameParameters(playerNamesFromIntent, selectedTopicsFromIntent)

        gameViewModel.currentQuestion.observe(this) { question ->
            currentImageAnimator?.cancel() 
            stopAndReleaseMediaPlayer() 
            updateStealPowerUpButtonState() 
            if (question != null) {
                updateUIForQuestion(question)
            } else {
                // Game over logic is now primarily handled by finalScoreData observer
                // binding.frameLayoutQuestionContent.removeAllViews()
                // hideAllAnswerButtons()
                Log.d(TAG, "Mevcut soru null, oyun bitmiş olabilir.")
            }
        }

        gameViewModel.playerStates.observe(this) { playerStates ->
            if (playerStates.isNullOrEmpty()) {
                binding.textViewScore.text = "Puanlar: Oyuncu bilgisi yok."
            } else {
                val scoreText = playerStates.joinToString(" - ") { "${it.playerName}: ${it.score}" }
                binding.textViewScore.text = "Puanlar: $scoreText"
            }
            updateStealPowerUpButtonState() 
        }
        
        gameViewModel.activePlayerIndex.observe(this) { activePlayerIndex ->
            if (activePlayerIndex != null) {
                val activePlayerName = gameViewModel.playerStates.value?.getOrNull(activePlayerIndex)?.playerName
                Log.d(TAG, "Aktif Oyuncu: ${activePlayerName ?: "Bilinmiyor"} (Index: $activePlayerIndex)")
            } else {
                Log.d(TAG, "Aktif Oyuncu: Yok (Cevaplama hakkı talep edilebilir)")
            }
            updateStealPowerUpButtonState() 
        }

        gameViewModel.showAnswerResult.observe(this) { resultMessage ->
            if (!resultMessage.isNullOrEmpty()) {
                currentImageAnimator?.pause() 
                stopAndReleaseMediaPlayer() 
                Toast.makeText(this, resultMessage, Toast.LENGTH_SHORT).show()
                gameViewModel.clearAnswerResult() 
            }
        }

        gameViewModel.currentTime.observe(this) { timeLeftInSeconds ->
            binding.textViewTimer.text = String.format(Locale.getDefault(), "Süre: %02d s", timeLeftInSeconds)
            if (timeLeftInSeconds == 0L) { 
                currentImageAnimator?.cancel() 
                stopAndReleaseMediaPlayer() 
            }
        }

        gameViewModel.showPeriodicScoreSummary.observe(this) { summaryMessage ->
            if (!summaryMessage.isNullOrEmpty()) {
                currentImageAnimator?.pause() 
                stopAndReleaseMediaPlayer() 
                if (scoreSummaryDialog == null || !scoreSummaryDialog!!.isShowing) {
                    scoreSummaryDialog = AlertDialog.Builder(this)
                        .setTitle("Puan Durumu")
                        .setMessage(summaryMessage)
                        .setPositiveButton("Devam Et") { dialog, _ ->
                            gameViewModel.proceedToNextQuestionAfterSummary()
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }

        gameViewModel.powerUpNotification.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                gameViewModel.clearPowerUpNotification()
            }
        }

        gameViewModel.showWheelEventNotification.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                stopAndReleaseMediaPlayer() 
                currentImageAnimator?.pause() 
                if (wheelEventDialog == null || !wheelEventDialog!!.isShowing) {
                    wheelEventDialog = AlertDialog.Builder(this)
                        .setTitle("Şans Çarkı!")
                        .setMessage(message)
                        .setPositiveButton("Harika!") { dialog, _ ->
                            gameViewModel.clearWheelEventNotification()
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }

        gameViewModel.soundEffectNotification.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Sound Effect Sim: $message") 
                gameViewModel.clearSoundEffectNotification()
            }
        }

        gameViewModel.finalScoreData.observe(this) { finalMessage ->
            if (!finalMessage.isNullOrEmpty()) {
                stopAndReleaseMediaPlayer()
                currentImageAnimator?.cancel()
                hideAllAnswerButtons() // Hide all game buttons
                binding.frameLayoutQuestionContent.removeAllViews() // Clear question area
                binding.textViewQuestionText.text = "" // Clear question text
                binding.textViewQuestionTopic.text = "" // Clear topic text
                binding.textViewTimer.visibility = View.GONE // Hide timer

                if (gameOverDialog == null || !gameOverDialog!!.isShowing) {
                    gameOverDialog = AlertDialog.Builder(this)
                        .setTitle("Oyun Bitti!")
                        .setMessage(finalMessage)
                        .setPositiveButton("Yeni Oyun") { dialog, _ ->
                            val intent = Intent(this, PlayerInputActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                            gameViewModel.clearFinalScoreData() // Clear LiveData after use
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }


        binding.textViewQuestionText.setOnClickListener { gameViewModel.nextQuestion() } 
        binding.buttonClaimAnswer.setOnClickListener { gameViewModel.claimAnswerAttempt() }
        binding.buttonUseStealPowerUp.setOnClickListener { gameViewModel.useStealPowerUp() }
    }

    private fun updateStealPowerUpButtonState() {
        val activePlayerIdx = gameViewModel.activePlayerIndex.value
        val currentQuestion = gameViewModel.currentQuestion.value
        var canUsePowerUp = false

        if (activePlayerIdx != null && currentQuestion != null) {
            val activePlayerState = gameViewModel.playerStates.value?.getOrNull(activePlayerIdx)
            if (activePlayerState != null && activePlayerState.hasStealPowerUp) {
                when (currentQuestion) {
                    is SpeedQuestion, is ImageQuestion, is RevealingImageQuestion, is AudioQuestion -> {
                        canUsePowerUp = true
                    }
                    else -> { 
                        canUsePowerUp = false
                    }
                }
            }
        }

        binding.buttonUseStealPowerUp.isEnabled = canUsePowerUp
        binding.buttonUseStealPowerUp.visibility = if (canUsePowerUp) View.VISIBLE else View.GONE
    }


    private fun stopAndReleaseMediaPlayer() {
        audioStopHandler?.removeCallbacksAndMessages(null) 
        mediaPlayer?.takeIf { it.isPlaying }?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d(TAG, "MediaPlayer stopped and released.")
    }


    private fun updateUIForQuestion(question: Question) {
        binding.textViewQuestionTopic.text = "Konu: ${question.topic}"
        binding.textViewQuestionText.text = question.text
        binding.frameLayoutQuestionContent.removeAllViews()
        hideAllAnswerButtons()
        currentImageAnimator?.cancel() 
        stopAndReleaseMediaPlayer() 
        updateStealPowerUpButtonState() 
        binding.textViewTimer.visibility = View.VISIBLE // Ensure timer is visible for new question

        when (question) {
            is SpeedQuestion -> {
                Log.d(TAG, "Soru Tipi: SpeedQuestion")
                showOptionButtons(question.options)
            }
            is ImageQuestion -> {
                Log.d(TAG, "Soru Tipi: ImageQuestion")
                showOptionButtons(question.options)
                val imageContentBinding = LayoutImageContentBinding.inflate(LayoutInflater.from(this), binding.frameLayoutQuestionContent, true)
                imageContentBinding.imageViewQuestionImage.setImageResource(R.mipmap.ic_launcher) 
                Log.d(TAG, "Image URL: ${question.imageUrl}")
            }
            is RevealingImageQuestion -> {
                Log.d(TAG, "Soru Tipi: RevealingImageQuestion")
                showOptionButtons(question.options)
                val imageContentBinding = LayoutImageContentBinding.inflate(LayoutInflater.from(this), binding.frameLayoutQuestionContent, true)
                imageContentBinding.imageViewQuestionImage.setImageResource(R.mipmap.ic_launcher_round) 
                Log.d(TAG, "Image URL: ${question.imageUrl}")

                imageContentBinding.imageViewQuestionImage.alpha = 0f
                currentImageAnimator = ObjectAnimator.ofFloat(imageContentBinding.imageViewQuestionImage, View.ALPHA, 0f, 1f).apply {
                    duration = REVEALING_IMAGE_ANIMATION_DURATION
                    interpolator = LinearInterpolator()
                    start()
                }
            }
            is AudioQuestion -> {
                Log.d(TAG, "Soru Tipi: AudioQuestion")
                showOptionButtons(question.options)
                val audioText = TextView(this).apply {
                    text = "Ses çalınıyor... (URL: ${question.audioUrl})"
                    textSize = 18f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                binding.frameLayoutQuestionContent.addView(audioText)

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    try {
                        if (!question.audioUrl.startsWith("placeholder_")) {
                             setDataSource(question.audioUrl) 
                             prepareAsync() 
                        } else {
                            audioText.text = "Ses çalınıyor... (Placeholder URL: ${question.audioUrl})"
                            Log.w(TAG, "Placeholder audio URL, not playing: ${question.audioUrl}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting data source for MediaPlayer: ${e.message}", e)
                        audioText.text = "Ses dosyası yüklenemedi."
                        return@apply 
                    }
                    setOnPreparedListener { player ->
                        Log.d(TAG, "MediaPlayer prepared, starting playback.")
                        player.start()
                        audioStopHandler = Handler(Looper.getMainLooper())
                        audioStopRunnable = Runnable {
                            mediaPlayer?.takeIf { it.isPlaying }?.apply {
                                stop()
                                Log.d(TAG, "Audio stopped by Handler after 10 seconds.")
                            }
                        }
                        audioStopHandler?.postDelayed(audioStopRunnable!!, AUDIO_PLAY_DURATION_MS)
                    }
                    setOnCompletionListener { mp ->
                        Log.d(TAG, "MediaPlayer playback completed.")
                        audioStopHandler?.removeCallbacksAndMessages(null) 
                        mp.release()
                        mediaPlayer = null
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        audioStopHandler?.removeCallbacksAndMessages(null)
                        mp.release()
                        mediaPlayer = null
                        audioText.text = "Ses dosyası çalınırken hata."
                        true 
                    }
                }
            }
            is FillInTheBlankQuestion -> {
                Log.d(TAG, "Soru Tipi: FillInTheBlankQuestion")
                val fillInBlankBinding = LayoutFillInBlankContentBinding.inflate(LayoutInflater.from(this), binding.frameLayoutQuestionContent, true)
                fillInBlankBinding.buttonSubmitFillBlankAnswer.setOnClickListener {
                    val answerText = fillInBlankBinding.editTextFillBlankAnswer.text.toString()
                    gameViewModel.submitAnswer(answerText)
                }
            }
            is OrderingQuestion -> {
                Log.d(TAG, "Soru Tipi: OrderingQuestion")
                val orderingContentBinding = LayoutOrderingContentBinding.inflate(LayoutInflater.from(this), binding.frameLayoutQuestionContent, true)
                orderingContentBinding.linearLayoutOrderingItems.removeAllViews()
                val currentOrderedItemsViews = mutableListOf<TextView>()
                question.itemsToOrder.forEach { itemText ->
                    val textView = TextView(this).apply {
                        text = itemText
                        textSize = 18f
                        setPadding(8, 8, 8, 8)
                        setBackgroundResource(android.R.drawable.editbox_background)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            bottomMargin = 8
                        }
                    }
                    currentOrderedItemsViews.add(textView)
                    orderingContentBinding.linearLayoutOrderingItems.addView(textView)
                }
                orderingContentBinding.buttonSubmitOrderingAnswer.setOnClickListener {
                    val orderedTexts = currentOrderedItemsViews.map { it.text.toString() }
                    gameViewModel.submitAnswer(orderedTexts)
                }
            }
            else -> {
                Log.d(TAG, "Soru Tipi: Bilinmiyor veya henüz ele alınmadı.")
                binding.buttonClaimAnswer.visibility = View.VISIBLE
            }
        }
    }

    private fun showOptionButtons(options: List<String>) {
        val buttons = listOf(binding.buttonAnswerA, binding.buttonAnswerB, binding.buttonAnswerC, binding.buttonAnswerD)
        buttons.forEachIndexed { index, button ->
            if (index < options.size) {
                button.text = options[index]
                button.visibility = View.VISIBLE
                button.setOnClickListener {
                    currentImageAnimator?.cancel() 
                    stopAndReleaseMediaPlayer() 
                    gameViewModel.submitAnswer(index)
                }
            } else {
                button.visibility = View.GONE
            }
        }
    }

    private fun hideAllAnswerButtons() {
        binding.buttonAnswerA.visibility = View.GONE
        binding.buttonAnswerB.visibility = View.GONE
        binding.buttonAnswerC.visibility = View.GONE
        binding.buttonAnswerD.visibility = View.GONE
        binding.buttonClaimAnswer.visibility = View.GONE
        binding.buttonUseStealPowerUp.visibility = View.GONE 
    }

    override fun onStop() {
        super.onStop()
        scoreSummaryDialog?.dismiss()
        scoreSummaryDialog = null
        wheelEventDialog?.dismiss()
        wheelEventDialog = null
        gameOverDialog?.dismiss()
        gameOverDialog = null
        currentImageAnimator?.cancel() 
        stopAndReleaseMediaPlayer() 
    }

    override fun onDestroy() {
        super.onDestroy()
        currentImageAnimator?.cancel() 
        stopAndReleaseMediaPlayer() 
    }
}
