package cz.mares.tetrisek.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cz.mares.tetrisek.engine.Difficulty

@Composable
actual fun rememberScoreStorage(): ScoreStorage {
    val context = LocalContext.current
    return remember(context) {
        val prefs = context.getSharedPreferences("tetrisek_scores", Context.MODE_PRIVATE)
        object : ScoreStorage {
            override fun getHighScore(difficulty: Difficulty): Int {
                return prefs.getInt("high_score_${difficulty.name}", 0)
            }

            override fun saveHighScore(difficulty: Difficulty, score: Int) {
                prefs.edit().putInt("high_score_${difficulty.name}", score).apply()
            }
        }
    }
}
