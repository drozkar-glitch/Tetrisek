package cz.tetrisek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }

    override fun onPause() {
        super.onPause()
        pauseGame()
    }

    override fun onStop() {
        super.onStop()
        pauseGame()
    }
}
