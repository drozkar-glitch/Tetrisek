package cz.tetrisek.ui

import androidx.compose.runtime.Composable
import cz.tetrisek.engine.Difficulty

interface ScoreStorage {
    fun getHighScore(difficulty: Difficulty): Int
    fun saveHighScore(difficulty: Difficulty, score: Int)
}

@Composable
expect fun rememberScoreStorage(): ScoreStorage
