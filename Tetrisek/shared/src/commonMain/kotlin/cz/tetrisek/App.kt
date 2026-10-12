package cz.tetrisek

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cz.tetrisek.engine.TetrisEngine
import cz.tetrisek.ui.TetrisScreen

@Composable
fun App() {
    MaterialTheme {
        // Inicializace herního enginu, která se provede pouze jednou při startu
        val engine = remember { TetrisEngine() }

        // Spuštění naší hlavní neonové herní obrazovky
        TetrisScreen(engine)
    }
}