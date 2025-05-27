package com.example.quizduellosu.ui.viewmodels

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.quizduellosu.data.models.* // All question models
import com.example.quizduellosu.data.repositories.QuestionLoaderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.* // For assertions

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class) // For Application context
class GameViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule() // For LiveData to execute instantly

    private lateinit var viewModel: GameViewModel
    private lateinit var application: Application

    @Mock
    private lateinit var mockQuestionLoaderRepository: QuestionLoaderRepository

    private val testDispatcher = StandardTestDispatcher() 

    // Test questions
    private val fakeSpeedQuestion = SpeedQuestion("s1", "Hız Sorusu 1", "Genel", listOf("A", "B", "C", "D"), 0)
    private val fakeFillInBlankQuestion = FillInTheBlankQuestion("fb1", "Boşluk ____.", "Genel", listOf("Doldurma"))
    private val fakeOrderingQuestion = OrderingQuestion("o1", "Sırala", "Genel", listOf("1", "2", "3"), listOf("1", "2", "3"))
    private val fakeImageQuestion = ImageQuestion("i1", "Resim Sorusu", "Genel", "url1", listOf("Opt1", "Opt2"), 0)
    private val fakeAudioQuestion = AudioQuestion("a1", "Ses Sorusu", "Genel", "url2", listOf("S1", "S2"), 0)
    private val fakeRevealingImageQuestion = RevealingImageQuestion("r1", "Görünür Resim", "Genel", "url3", listOf("R1", "R2"), 0)

    private fun createFakeQuestions(): List<Question> {
        return listOf(
            fakeSpeedQuestion,
            fakeFillInBlankQuestion,
            fakeOrderingQuestion,
            fakeImageQuestion,
            fakeAudioQuestion,
            fakeRevealingImageQuestion
        )
    }
     private fun createSingleFakeSpeedQuestionList(): List<Question> {
        return listOf(fakeSpeedQuestion)
    }


    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        application = RuntimeEnvironment.getApplication()
        Dispatchers.setMain(testDispatcher) 

        // Default mock behavior:
        `when`(mockQuestionLoaderRepository.loadQuestionsFromJson()).thenReturn(createFakeQuestions())
        
        viewModel = GameViewModel(application, mockQuestionLoaderRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() 
    }

    @Test
    fun `setGameParameters initializes players and scores correctly`() = runTest {
        val playerNames = listOf("Oyuncu1", "Oyuncu2")
        viewModel.setGameParameters(playerNames, listOf("Genel"))
        advanceUntilIdle() 

        val playerStates = viewModel.playerStates.value
        assertNotNull(playerStates)
        assertEquals(2, playerStates?.size)
        assertEquals("Oyuncu1", playerStates?.get(0)?.playerName)
        assertEquals(0, playerStates?.get(0)?.score)
        assertTrue(playerStates?.get(0)?.hasStealPowerUp == true)
        assertEquals("Oyuncu2", playerStates?.get(1)?.playerName)
        assertEquals(0, playerStates?.get(1)?.score)
        assertTrue(playerStates?.get(1)?.hasStealPowerUp == true)

        assertNotNull(viewModel.currentQuestion.value)
    }

    @Test
    fun `submitAnswer with correct option on SpeedQuestion updates score`() = runTest {
        `when`(mockQuestionLoaderRepository.loadQuestionsFromJson()).thenReturn(createSingleFakeSpeedQuestionList())
        // Re-initialize or use a method to reset and reload questions with new mock
        viewModel = GameViewModel(application, mockQuestionLoaderRepository) 

        viewModel.setGameParameters(listOf("Oyuncu1"), listOf("Genel"))
        advanceUntilIdle() 
        
        assertNotNull(viewModel.currentQuestion.value)
        assertTrue(viewModel.currentQuestion.value is SpeedQuestion)

        viewModel.claimAnswerAttempt() 
        advanceUntilIdle()
        assertEquals(0, viewModel.activePlayerIndex.value) 

        viewModel.submitAnswer(0) // Correct answer for fakeSpeedQuestion
        advanceUntilIdle() 

        assertEquals(10, viewModel.playerStates.value?.get(0)?.score) // POINTS_FOR_CORRECT_ANSWER
        assertNotNull(viewModel.showAnswerResult.value)
        assertTrue(viewModel.showAnswerResult.value?.contains("Doğru") == true)
    }

    @Test
    fun `submitAnswer with incorrect option on SpeedQuestion gives negative points`() = runTest {
        `when`(mockQuestionLoaderRepository.loadQuestionsFromJson()).thenReturn(createSingleFakeSpeedQuestionList())
        viewModel = GameViewModel(application, mockQuestionLoaderRepository)

        viewModel.setGameParameters(listOf("Oyuncu1"), listOf("Genel"))
        advanceUntilIdle()

        assertNotNull(viewModel.currentQuestion.value)
        assertTrue(viewModel.currentQuestion.value is SpeedQuestion)
        
        viewModel.claimAnswerAttempt() 
        advanceUntilIdle()
        assertEquals(0, viewModel.activePlayerIndex.value)

        viewModel.submitAnswer(1) // Incorrect answer (correct is 0)
        advanceUntilIdle()

        assertEquals(-5, viewModel.playerStates.value?.get(0)?.score) // NEGATIVE_POINTS_FOR_SPEED_QUESTION
        assertNotNull(viewModel.showAnswerResult.value)
        assertTrue(viewModel.showAnswerResult.value?.contains("Yanlış") == true)
        assertTrue(viewModel.playersWhoAttemptedThisQuestionPublicForTest.contains("Oyuncu1"))
        assertNull(viewModel.activePlayerIndex.value) 
    }

    @Test
    fun `useStealPowerUp reveals correct answer and updates player state`() = runTest {
        `when`(mockQuestionLoaderRepository.loadQuestionsFromJson()).thenReturn(createSingleFakeSpeedQuestionList())
        viewModel = GameViewModel(application, mockQuestionLoaderRepository)

        viewModel.setGameParameters(listOf("Oyuncu1"), listOf("Genel"))
        advanceUntilIdle()

        assertNotNull(viewModel.currentQuestion.value)
        assertTrue(viewModel.currentQuestion.value is SpeedQuestion)

        viewModel.claimAnswerAttempt() 
        advanceUntilIdle()
        assertEquals(0, viewModel.activePlayerIndex.value)

        viewModel.useStealPowerUp()
        advanceUntilIdle()

        assertFalse(viewModel.playerStates.value?.get(0)?.hasStealPowerUp ?: true)
        assertNotNull(viewModel.powerUpNotification.value)
        assertTrue(viewModel.powerUpNotification.value?.contains("Doğru şık: A") == true) 
    }
    
    @Test
    fun `initialLoadQuestions filters questions by topic`() = runTest {
        val topicSpecificQuestions = listOf(
            SpeedQuestion("s_topic1", "Soru Topic1", "Topic1", listOf("A", "B"), 0),
            FillInTheBlankQuestion("fb_topic2", "Soru Topic2", "Topic2", listOf("Cevap"))
        )
        `when`(mockQuestionLoaderRepository.loadQuestionsFromJson()).thenReturn(topicSpecificQuestions)
        viewModel = GameViewModel(application, mockQuestionLoaderRepository)

        viewModel.setGameParameters(listOf("Oyuncu1"), listOf("Topic1"))
        advanceUntilIdle()

        assertEquals(1, viewModel.questions.value?.size)
        assertEquals("s_topic1", viewModel.currentQuestion.value?.id)
    }

    @Test
    fun `initialLoadQuestions uses all questions if topic is Genel or empty`() = runTest {
        // The default mock in setUp already returns createFakeQuestions()
        // viewModel = GameViewModel(application, mockQuestionLoaderRepository) // Already done in setup

        viewModel.setGameParameters(listOf("Oyuncu1"), listOf("Genel")) 
        advanceUntilIdle()
        assertEquals(createFakeQuestions().size, viewModel.questions.value?.size)

        // Need to re-mock or ensure the state is reset if setGameParameters doesn't fully reset question list from repo
        // For simplicity, can re-initialize viewModel or ensure setGameParameters re-fetches and re-filters
        `when`(mockQuestionLoaderRepository.loadQuestionsFromJson()).thenReturn(createFakeQuestions()) // Re-affirm mock for clarity
        viewModel.setGameParameters(listOf("Oyuncu1"), emptyList()) 
        advanceUntilIdle()
        assertEquals(createFakeQuestions().size, viewModel.questions.value?.size)
    }
    
    @Test
    fun `claimAnswerAttempt assigns active player if none and not attempted`() = runTest {
        `when`(mockQuestionLoaderRepository.loadQuestionsFromJson()).thenReturn(createSingleFakeSpeedQuestionList())
        viewModel = GameViewModel(application, mockQuestionLoaderRepository)
        viewModel.setGameParameters(listOf("Oyuncu1", "Oyuncu2"), listOf("Genel"))
        advanceUntilIdle()

        assertNull(viewModel.activePlayerIndex.value)
        viewModel.claimAnswerAttempt()
        advanceUntilIdle()
        assertEquals(0, viewModel.activePlayerIndex.value) 
    }

    @Test
    fun `claimAnswerAttempt assigns to next available player if first already attempted`() = runTest {
        `when`(mockQuestionLoaderRepository.loadQuestionsFromJson()).thenReturn(createSingleFakeSpeedQuestionList())
        viewModel = GameViewModel(application, mockQuestionLoaderRepository)
        viewModel.setGameParameters(listOf("Oyuncu1", "Oyuncu2"), listOf("Genel"))
        advanceUntilIdle()

        viewModel.claimAnswerAttempt()
        advanceUntilIdle() 
        assertEquals(0, viewModel.activePlayerIndex.value) // Oyuncu1 claims
        viewModel.submitAnswer(1) // Oyuncu1 answers incorrectly
        advanceUntilIdle()
        
        assertTrue(viewModel.playersWhoAttemptedThisQuestionPublicForTest.contains("Oyuncu1"))
        assertNull(viewModel.activePlayerIndex.value)

        viewModel.claimAnswerAttempt() // Oyuncu2 claims
        advanceUntilIdle()
        assertEquals(1, viewModel.activePlayerIndex.value) 
    }
}
