package cz.mares.tetrisek

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cz.mares.tetrisek.engine.TetrisEngine
import cz.mares.tetrisek.ui.TetrisScreen

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
