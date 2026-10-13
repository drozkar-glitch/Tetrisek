package cz.tetrisek.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

@Composable
actual fun rememberHaptics(): HapticFeedback {
    return remember {
        val lightGenerator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
        val heavyGenerator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
        object : HapticFeedback {
            override fun light() {
                lightGenerator.prepare()
                lightGenerator.impactOccurred()
            }

            override fun heavy() {
                heavyGenerator.prepare()
                heavyGenerator.impactOccurred()
            }
        }
    }
}