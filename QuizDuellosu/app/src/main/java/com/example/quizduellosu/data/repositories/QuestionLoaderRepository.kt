package com.example.quizduellosu.data.repositories

import android.content.Context
import android.util.Log
import com.example.quizduellosu.data.models.AudioQuestion
import com.example.quizduellosu.data.models.FillInTheBlankQuestion
import com.example.quizduellosu.data.models.ImageQuestion
import com.example.quizduellosu.data.models.OrderingQuestion
import com.example.quizduellosu.data.models.Question
import com.example.quizduellosu.data.models.RevealingImageQuestion
import com.example.quizduellosu.data.models.SpeedQuestion
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import java.io.IOException

class QuestionLoaderRepository(private val context: Context) {

    companion object {
        private const val TAG = "QuestionLoaderRepo"
        private const val QUESTIONS_FILE_NAME = "questions.json"
    }

    fun loadQuestionsFromJson(): List<Question> {
        val questions = mutableListOf<Question>()
        val gson = Gson()

        try {
            val jsonString = context.assets.open(QUESTIONS_FILE_NAME).bufferedReader().use { it.readText() }
            val jsonArray = gson.fromJson(jsonString, JsonArray::class.java)

            for (jsonElement in jsonArray) {
                if (jsonElement.isJsonObject) {
                    val jsonObject = jsonElement.asJsonObject
                    val questionTypeString = jsonObject.get("questionType")?.asString

                    val question: Question? = when (questionTypeString) {
                        "SPEED" -> gson.fromJson(jsonObject, SpeedQuestion::class.java)
                        "FILL_IN_THE_BLANK" -> gson.fromJson(jsonObject, FillInTheBlankQuestion::class.java)
                        "ORDERING" -> gson.fromJson(jsonObject, OrderingQuestion::class.java)
                        "IMAGE" -> gson.fromJson(jsonObject, ImageQuestion::class.java)
                        "AUDIO" -> gson.fromJson(jsonObject, AudioQuestion::class.java)
                        "REVEALING_IMAGE" -> gson.fromJson(jsonObject, RevealingImageQuestion::class.java)
                        else -> {
                            Log.w(TAG, "Unknown question type: $questionTypeString in JSON object: $jsonObject")
                            null
                        }
                    }
                    question?.let { questions.add(it) }
                } else {
                    Log.w(TAG, "Skipping non-object element in JSON array: $jsonElement")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading questions.json from assets", e)
            // Return empty list or throw a custom exception
            return emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing JSON from questions.json", e)
            // Return empty list or throw a custom exception
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred during question loading", e)
            return emptyList()
        }

        Log.d(TAG, "${questions.size} questions loaded successfully from JSON.")
        return questions
    }
}
