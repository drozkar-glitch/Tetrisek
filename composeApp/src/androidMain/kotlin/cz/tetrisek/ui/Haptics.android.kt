package cz.tetrisek.ui

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) {
        object : Haptics {
            override fun light() {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }

            override fun heavy() {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }
}
