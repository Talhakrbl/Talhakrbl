package com.example.quizduellosu

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quizduellosu.ui.activities.PlayerInputActivity 
import org.hamcrest.CoreMatchers.containsString 
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerInputToGameFlowTest {

    @get:Rule
    var activityRule = ActivityScenarioRule(PlayerInputActivity::class.java)

    @Test
    fun completePlayerInputAndTopicSelection_startsMainActivity() { // Test method name updated to reflect actual target
        // Oyuncu ismi gir (R.id.edit_text_player_name_1 PlayerInputActivity'deki ilk EditText'in ID'si)
        onView(withId(R.id.edit_text_player_name_1)).perform(typeText("OyuncuEspresso"), closeSoftKeyboard())
        
        // "Devam Et" butonuna tıkla (R.id.button_proceed_to_topic_selection PlayerInputActivity'deki butonun ID'si)
        onView(withId(R.id.button_proceed_to_topic_selection)).perform(click())

        // TopicSelectionActivity'de oyuncu bilgisini kontrol et 
        // (R.id.text_view_player_info TopicSelectionActivity'deki TextView ID'si)
        onView(withId(R.id.text_view_player_info)).check(matches(withText(containsString("OyuncuEspresso"))))

        // İki konu seç (R.id.checkbox_topic_history ve R.id.checkbox_topic_science TopicSelectionActivity'deki CheckBox ID'leri)
        onView(withId(R.id.checkbox_topic_history)).perform(click())
        onView(withId(R.id.checkbox_topic_science)).perform(click())

        // "Oyunu Başlat" butonuna tıkla (R.id.button_start_game TopicSelectionActivity'deki butonun ID'si)
        onView(withId(R.id.button_start_game)).perform(click())

        // GameActivity'nin açıldığını ve bir elemanın göründüğünü kontrol et
        // (R.id.text_view_question_text GameActivity'deki soru metnini gösteren TextView ID'si)
        onView(withId(R.id.text_view_question_text)).check(matches(isDisplayed()))
    }
}
