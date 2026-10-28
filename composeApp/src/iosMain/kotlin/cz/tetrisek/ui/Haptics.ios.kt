package cz.tetrisek.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

@Composable
actual fun rememberHaptics(): Haptics {
    return remember {
        val lightGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
        val heavyGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
        object : Haptics {
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
