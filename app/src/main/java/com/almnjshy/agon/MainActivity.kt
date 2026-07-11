package com.almnjshy.agon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almnjshy.agon.engine.AIDifficulty
import com.almnjshy.agon.network.NetworkGameViewModel
import com.almnjshy.agon.ui.GameScreen
import com.almnjshy.agon.ui.LobbyScreen
import com.almnjshy.agon.ui.MainMenuScreen
import com.almnjshy.agon.ui.NetworkGameScreen
import com.almnjshy.agon.ui.theme.AgonTheme

private sealed interface Screen {
    data object Menu : Screen
    data class InGame(val playerCount: Int, val difficulty: AIDifficulty) : Screen  // ← أضفنا val
    data object Lobby : Screen
    data object NetworkGame : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgonTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Menu) }
                val networkVm: NetworkGameViewModel = viewModel()

                when (val s = screen) {
                    is Screen.Menu -> MainMenuScreen(
                        onStart = { count, diff -> screen = Screen.InGame(count, diff) },
                        onPlayWithFriends = { screen = Screen.Lobby }
                    )
                    is Screen.InGame -> GameScreen(
                        playerCount = s.playerCount,
                        difficulty = s.difficulty,
                        onExitToMenu = { screen = Screen.Menu }
                    )
                    is Screen.Lobby -> LobbyScreen(
                        vm = networkVm,
                        onGameStarting = { screen = Screen.NetworkGame },
                        onCancel = { networkVm.reset(); screen = Screen.Menu }
                    )
                    is Screen.NetworkGame -> NetworkGameScreen(
                        vm = networkVm,
                        onExitToMenu = { networkVm.reset(); screen = Screen.Menu }
                    )
                }
            }
        }
    }
}
