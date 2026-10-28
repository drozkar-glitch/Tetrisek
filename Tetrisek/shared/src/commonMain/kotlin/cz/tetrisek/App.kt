package cz.tetrisek

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.tetrisek.engine.TetrisEngine
import cz.tetrisek.ui.TetrisScreen

@Composable
fun App() {
    MaterialTheme {
        val engine = remember { TetrisEngine() }
        val lifecycleOwner = LocalLifecycleOwner.current

        // Automatická pauza při opuštění obrazovky (hovor, minimalizace)
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                    engine.pause()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        TetrisScreen(engine)
    }
}