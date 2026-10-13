package cz.tetrisek.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cz.tetrisek.engine.Difficulty
import platform.Foundation.NSUserDefaults

@Composable
actual fun rememberScoreStorage(): ScoreStorage {
    return remember {
        val defaults = NSUserDefaults.standardUserDefaults
        object : ScoreStorage {
            override fun getHighScore(difficulty: Difficulty): Int {
                return defaults.integerForKey("high_score_${difficulty.name}").toInt()
            }

            override fun saveHighScore(difficulty: Difficulty, score: Int) {
                defaults.setInteger(score.toLong(), forKey = "high_score_${difficulty.name}")
            }
        }
    }
}