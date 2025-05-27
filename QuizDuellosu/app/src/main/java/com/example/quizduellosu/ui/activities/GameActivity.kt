package com.example.quizduellosu.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playerNamesFromIntent = intent.getStringArrayListExtra("EXTRA_PLAYER_NAMES") ?: arrayListOf("Bilinmeyen Oyuncu")
        val selectedTopicsFromIntent = intent.getStringArrayListExtra("EXTRA_SELECTED_TOPICS") ?: arrayListOf("Genel")

        Log.d("GameActivity", "Oyuncular (Intent'ten): $playerNamesFromIntent")
        Log.d("GameActivity", "Seçilen Konular (Intent'ten): $selectedTopicsFromIntent")

        gameViewModel.setGameParameters(playerNamesFromIntent, selectedTopicsFromIntent)

        gameViewModel.currentQuestion.observe(this) { question ->
            if (question != null) {
                updateUIForQuestion(question)
            } else {
                binding.textViewQuestionText.text = "Oyun bitti veya soru bulunamadı!"
                binding.textViewQuestionTopic.text = ""
                binding.frameLayoutQuestionContent.removeAllViews()
                hideAllAnswerButtons()
                Log.d("GameActivity", "Oyun bitti veya soru yok.")
            }
        }

        gameViewModel.playerStates.observe(this) { playerStates ->
            if (playerStates.isNullOrEmpty()) {
                binding.textViewScore.text = "Puanlar: Oyuncu bilgisi yok."
            } else {
                val scoreText = playerStates.joinToString(" - ") { "${it.playerName}: ${it.score}" }
                binding.textViewScore.text = "Puanlar: $scoreText"
            }
        }
        
        gameViewModel.activePlayerIndex.observe(this) { activePlayerIndex ->
            if (activePlayerIndex != null) {
                val activePlayerName = gameViewModel.playerStates.value?.getOrNull(activePlayerIndex)?.playerName
                Log.d("GameActivity", "Aktif Oyuncu: ${activePlayerName ?: "Bilinmiyor"} (Index: $activePlayerIndex)")
            } else {
                Log.d("GameActivity", "Aktif Oyuncu: Yok (Cevaplama hakkı talep edilebilir)")
            }
        }

        gameViewModel.showAnswerResult.observe(this) { resultMessage ->
            if (!resultMessage.isNullOrEmpty()) {
                Toast.makeText(this, resultMessage, Toast.LENGTH_SHORT).show()
                gameViewModel.clearAnswerResult() // Clear after showing to prevent re-toast on config change
            }
        }

        gameViewModel.currentTime.observe(this) { timeLeftInSeconds ->
            binding.textViewTimer.text = String.format(Locale.getDefault(), "Süre: %02d s", timeLeftInSeconds)
        }

        binding.textViewQuestionText.setOnClickListener { gameViewModel.nextQuestion() } // For easier testing
        binding.buttonClaimAnswer.setOnClickListener { gameViewModel.claimAnswerAttempt() }
    }

    private fun updateUIForQuestion(question: Question) {
        binding.textViewQuestionTopic.text = "Konu: ${question.topic}"
        binding.textViewQuestionText.text = question.text
        binding.frameLayoutQuestionContent.removeAllViews()
        hideAllAnswerButtons()

        when (question) {
            is SpeedQuestion -> {
                Log.d("GameActivity", "Soru Tipi: SpeedQuestion")
                showOptionButtons(question.options)
            }
            is ImageQuestion -> {
                Log.d("GameActivity", "Soru Tipi: ImageQuestion")
                showOptionButtons(question.options)
                val imageContentBinding = LayoutImageContentBinding.inflate(LayoutInflater.from(this), binding.frameLayoutQuestionContent, true)
                imageContentBinding.imageViewQuestionImage.setImageResource(R.mipmap.ic_launcher)
                Log.d("GameActivity", "Image URL: ${question.imageUrl}")
            }
            is RevealingImageQuestion -> {
                Log.d("GameActivity", "Soru Tipi: RevealingImageQuestion")
                showOptionButtons(question.options)
                val imageContentBinding = LayoutImageContentBinding.inflate(LayoutInflater.from(this), binding.frameLayoutQuestionContent, true)
                imageContentBinding.imageViewQuestionImage.setImageResource(R.mipmap.ic_launcher_round)
                Log.d("GameActivity", "Image URL: ${question.imageUrl}")
            }
            is AudioQuestion -> {
                Log.d("GameActivity", "Soru Tipi: AudioQuestion")
                showOptionButtons(question.options)
                val audioText = TextView(this).apply {
                    text = "Ses çalınıyor... (URL: ${question.audioUrl})"
                    textSize = 18f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                binding.frameLayoutQuestionContent.addView(audioText)
            }
            is FillInTheBlankQuestion -> {
                Log.d("GameActivity", "Soru Tipi: FillInTheBlankQuestion")
                val fillInBlankBinding = LayoutFillInBlankContentBinding.inflate(LayoutInflater.from(this), binding.frameLayoutQuestionContent, true)
                fillInBlankBinding.buttonSubmitFillBlankAnswer.setOnClickListener {
                    val answerText = fillInBlankBinding.editTextFillBlankAnswer.text.toString()
                    gameViewModel.submitAnswer(answerText)
                }
            }
            is OrderingQuestion -> {
                Log.d("GameActivity", "Soru Tipi: OrderingQuestion")
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
                Log.d("GameActivity", "Soru Tipi: Bilinmiyor veya henüz ele alınmadı.")
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
    }
}
