package cz.tetrisek

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cz.tetrisek.engine.TetrisEngine
import cz.tetrisek.ui.TetrisScreen

val appTetrisEngine = TetrisEngine()

fun pauseGame() {
    appTetrisEngine.pause()
}

@Composable
fun App() {
    MaterialTheme {
        TetrisScreen(appTetrisEngine)
    }
}
