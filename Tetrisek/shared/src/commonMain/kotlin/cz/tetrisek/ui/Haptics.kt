package cz.tetrisek.ui

import androidx.compose.runtime.Composable

interface HapticFeedback {
    fun light()
    fun heavy()
}

@Composable
expect fun rememberHaptics(): HapticFeedback