package cz.tetrisek.ui

import androidx.compose.runtime.Composable

interface Haptics {
    fun light()
    fun heavy()
}

@Composable
expect fun rememberHaptics(): Haptics
