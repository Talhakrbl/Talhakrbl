package com.example.quizduellosu.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.example.quizduellosu.databinding.ActivityPlayerInputBinding
import android.content.Intent // Added for Intent

class PlayerInputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerInputBinding
    private val playerEditTexts = mutableListOf<EditText>()
    private val MAX_PLAYERS = 6
    private var playerCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add the initial EditText to our list
        playerEditTexts.add(binding.editTextPlayerName1)
        binding.editTextPlayerName1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateProceedButtonState()
            }
        })

        binding.buttonAddPlayer.setOnClickListener {
            if (playerCount < MAX_PLAYERS) {
                playerCount++
                val newEditText = EditText(this).apply {
                    hint = "Oyuncu $playerCount Adı"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        val marginInPixels = (16 * resources.displayMetrics.density).toInt() // 16dp to pixels
                        it.setMargins(0, marginInPixels, 0, 0)
                    }
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                }

                // Add the new EditText to the LinearLayout
                // The first EditText is binding.editTextPlayerName1
                // Subsequent EditTexts are added to binding.linearLayoutPlayerNames
                // The parent of binding.editTextPlayerName1 is binding.linearLayoutPlayerNames
                binding.linearLayoutPlayerNames.addView(newEditText, binding.linearLayoutPlayerNames.indexOfChild(binding.buttonAddPlayer))


                playerEditTexts.add(newEditText)

                newEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        updateProceedButtonState() // Though not strictly required by the current logic, good for future proofing
                    }
                })

                if (playerCount == MAX_PLAYERS) {
                    binding.buttonAddPlayer.isEnabled = false
                    Toast.makeText(this, "Maksimum oyuncu sayısına ulaşıldı.", Toast.LENGTH_SHORT).show()
                }
            }
            updateProceedButtonState() // Update button state after adding a player (in case the first player's name was empty)
        }

        binding.buttonProceedToTopicSelection.setOnClickListener {
            val playerNames = mutableListOf<String>()
            for (editText in playerEditTexts) {
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    playerNames.add(name)
                }
            }

            if (playerNames.isNotEmpty()) {
                // Toast.makeText(this, "Oyuncular: $playerNames", Toast.LENGTH_LONG).show()
                val intent = Intent(this, TopicSelectionActivity::class.java)
                intent.putStringArrayListExtra("EXTRA_PLAYER_NAMES", ArrayList(playerNames))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Lütfen en az bir oyuncu adı girin.", Toast.LENGTH_SHORT).show()
            }
        }

        // Initial state for the proceed button
        updateProceedButtonState()
    }

    private fun updateProceedButtonState() {
        // Proceed button is enabled if the first player's name (editTextPlayerName1) is not empty.
        // Other players are optional.
        binding.buttonProceedToTopicSelection.isEnabled = binding.editTextPlayerName1.text.toString().trim().isNotEmpty()
    }
}
