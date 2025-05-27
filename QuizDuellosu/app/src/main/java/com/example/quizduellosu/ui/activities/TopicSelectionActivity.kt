package com.example.quizduellosu.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast
import com.example.quizduellosu.databinding.ActivityTopicSelectionBinding
import android.content.Intent // Added for Intent

class TopicSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopicSelectionBinding
    private lateinit var playerNames: ArrayList<String>
    private val selectedTopics = mutableListOf<CheckBox>()
    private val allCheckBoxes by lazy {
        listOf(
            binding.checkboxTopicHistory,
            binding.checkboxTopicScience,
            binding.checkboxTopicGeography,
            binding.checkboxTopicArt,
            binding.checkboxTopicSports,
            binding.checkboxTopicGeneralCulture
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerNames = intent.getStringArrayListExtra("EXTRA_PLAYER_NAMES") ?: ArrayList()

        if (playerNames.isNotEmpty()) {
            binding.textViewPlayerInfo.text = "Oyuncular: ${playerNames.joinToString(", ")}"
        } else {
            binding.textViewPlayerInfo.text = "Oyuncu bilgisi bulunamadı."
        }

        allCheckBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    if (selectedTopics.size < 2) {
                        selectedTopics.add(buttonView as CheckBox)
                    } else {
                        // More than 2 selected, uncheck the current one and show a toast
                        buttonView.isChecked = false
                        Toast.makeText(this, "En fazla 2 konu seçebilirsiniz.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    selectedTopics.remove(buttonView as CheckBox)
                }
                updateStartButtonState()
            }
        }

        binding.buttonStartGame.setOnClickListener {
            if (selectedTopics.size == 2) {
                val topicNames = selectedTopics.map { it.text.toString() }
                // Toast.makeText(this, "Seçilen Temalar: ${topicNames.joinToString(", ")}", Toast.LENGTH_LONG).show()
                
                val intent = Intent(this, GameActivity::class.java)
                intent.putStringArrayListExtra("EXTRA_PLAYER_NAMES", playerNames)
                intent.putStringArrayListExtra("EXTRA_SELECTED_TOPICS", ArrayList(topicNames))
                startActivity(intent)
            }
        }

        updateStartButtonState() // Initial state
    }

    private fun updateStartButtonState() {
        binding.buttonStartGame.isEnabled = selectedTopics.size == 2

        // Optional: Disable other checkboxes if 2 are already selected
        if (selectedTopics.size == 2) {
            allCheckBoxes.forEach { cb ->
                if (!selectedTopics.contains(cb)) {
                    cb.isEnabled = false
                }
            }
        } else {
            // Re-enable all checkboxes if selection is less than 2
            allCheckBoxes.forEach { cb ->
                cb.isEnabled = true
            }
        }
    }
}
