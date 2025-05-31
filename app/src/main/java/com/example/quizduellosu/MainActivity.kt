package com.example.quizduellosu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel // viewModel importu
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.quizduellosu.navigation.ScreenRoutes
import com.example.quizduellosu.ui.screens.CreateRoomScreen
import com.example.quizduellosu.ui.screens.LoginScreen
import com.example.quizduellosu.ui.screens.RoomLobbyScreen
import com.example.quizduellosu.ui.theme.QuizDuellosuTheme
import com.example.quizduellosu.ui.viewmodels.RoomViewModel // ViewModel importu
// import android.util.Log

// Oyuncu adını global olarak tutmak için (idealde AppViewModel veya benzeri bir yapıda olmalı)
// Bu sadece basit bir örnekleme, gerçek uygulamada daha iyi bir state yönetimi gerekir.
var appGlobalPlayerName: String = "Oyuncu"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizDuellosuTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    // RoomViewModel'ı burada oluşturup tüm ekranlara aynı instance'ı vermek
                    // veya her ekranın kendi viewModel() ile alması arasında bir tercih yapılabilir.
                    // NavGraph seviyesinde scoped ViewModel'lar için Hilt kullanımı daha yaygındır.
                    // Şimdilik her ekran kendi viewModel() ile alacak, bu da her seferinde yeni instance demek olabilir
                    // eğer composable recompose olursa. Daha stabil state için NavGraph scoped ViewModel veya
                    // bu ViewModel'ı Activity seviyesinde tutup NavHost'a parametre olarak vermek düşünülebilir.
                    // Biz basitlik adına her ekranın kendi `viewModel()` ile almasını sağlayacağız.
                    // setCurrentPlayerName çağrısı LoginScreen'de yapıldığı için diğer ekranlar bu bilgiyi kullanabilir.

                    AppNavigationHost(navController = navController)
                }
            }
        }
    }
}

@Composable
fun AppNavigationHost(navController: NavHostController) {
    // val roomViewModel: RoomViewModel = viewModel() // Eğer NavHost seviyesinde tek instance istenirse

    NavHost(navController = navController, startDestination = ScreenRoutes.LOGIN_SCREEN) {
        composable(ScreenRoutes.LOGIN_SCREEN) {
            LoginScreen(
                // roomViewModel = roomViewModel, // Eğer yukarıda oluşturulduysa
                onNavigateToCreateRoom = { playerName ->
                    appGlobalPlayerName = playerName // Global değişkene ata
                    navController.navigate(ScreenRoutes.CREATE_ROOM_SCREEN)
                },
                onNavigateToLobby = { roomCode, playerName ->
                    appGlobalPlayerName = playerName // Global değişkene ata
                    // Oda kodunu ve oyuncu adını lobiye yolla
                    navController.navigate(ScreenRoutes.roomLobbyScreen(roomCode))
                }
            )
        }
        composable(ScreenRoutes.CREATE_ROOM_SCREEN) {
            CreateRoomScreen(
                // roomViewModel = roomViewModel,
                currentPlayerName = appGlobalPlayerName, // Global değişkenden al
                onNavigateToLobby = { roomCode, playerName -> // playerName zaten appGlobalPlayerName
                    // Oda oluşturulduktan sonra lobiye oyuncu adıyla git
                    navController.navigate(ScreenRoutes.roomLobbyFromCreateScreen(roomCode)) {
                        popUpTo(ScreenRoutes.LOGIN_SCREEN)
                    }
                }
            )
        }
        composable(
            route = ScreenRoutes.ROOM_LOBBY_SCREEN_ROUTE,
            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode")
            requireNotNull(roomCode) { "Room code cannot be null for RoomLobbyScreen" }
            RoomLobbyScreen(
                // roomViewModel = roomViewModel,
                roomCode = roomCode,
                currentPlayerName = appGlobalPlayerName, // Global değişkenden al
                onNavigateBackToLogin = {
                    navController.popBackStack(ScreenRoutes.LOGIN_SCREEN, inclusive = false)
                },
                onNavigateToGame = { navigatedRoomId ->
                    navController.navigate(ScreenRoutes.questionDisplayScreen(navigatedRoomId))
                }
            )
        }
        composable(
            route = ScreenRoutes.ROOM_LOBBY_FROM_CREATE_ROUTE,
            arguments = listOf(navArgument("newRoomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("newRoomCode")
            requireNotNull(roomCode) { "New room code cannot be null for RoomLobbyScreen" }
            RoomLobbyScreen(
                // roomViewModel = roomViewModel,
                roomCode = roomCode,
                currentPlayerName = appGlobalPlayerName, // Global değişkenden al
                onNavigateBackToLogin = {
                    navController.popBackStack(ScreenRoutes.LOGIN_SCREEN, inclusive = false)
                },
                onNavigateToGame = { navigatedRoomId ->
                     navController.navigate(ScreenRoutes.questionDisplayScreen(navigatedRoomId))
                }
            )
        }
        composable(
            route = ScreenRoutes.QUESTION_DISPLAY_SCREEN_ROUTE,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId")
            requireNotNull(roomId) { "Room ID cannot be null for QuestionDisplayScreen" }

            // QuestionDisplayScreen kendi ViewModel'ını veya RoomViewModel'ı kullanabilir.
            // Şimdilik RoomViewModel'ı kullanmaya devam edelim, çünkü oyun state'i (hangi soru vb.)
            // zaten Room objesinde tutuluyor.
            // val roomViewModel: RoomViewModel = viewModel() // Her ekran kendi instance'ını alırsa
                                                            // veya NavHost seviyesinde sağlanırsa.

            QuestionDisplayScreen(
                // question ve diğer UI ile ilgili parametreler QuestionDisplayScreen içinde
                // RoomViewModel'dan alınan roomState'e göre set edilecek.
                // Bu subtask sadece navigasyonu ve temel veri geçişini hedefliyor.
                // Bu yüzden burada direkt question objesi oluşturmuyoruz.
                // Gerekli parametreleri QuestionDisplayScreen kendi içinde ViewModel'dan alacak.
                roomId = roomId, // QuestionDisplayScreen'in hangi odayı dinleyeceğini bilmesi için
                currentPlayerName = appGlobalPlayerName, // Gerekirse oyuncu adını da verelim
                // roomViewModel = roomViewModel, // Eğer NavHost seviyesinde oluşturulmuşsa
                onNavigateBackToLobby = { // Örneğin soru bittiğinde veya hata olduğunda lobiye dönmek için
                    navController.popBackStack(ScreenRoutes.roomLobbyScreen(roomId), inclusive = false)
                },
                onFinishGame = { // Oyun bittiğinde belki Login'e veya bir sonuç ekranına
                    navController.popBackStack(ScreenRoutes.LOGIN_SCREEN, inclusive = false)
                }
            )
                        }
                    }
                }
            }
        }
    }
}
// Eski Greeting Preview'ları kaldırıldı. Ekranların kendi Preview'ları var.
