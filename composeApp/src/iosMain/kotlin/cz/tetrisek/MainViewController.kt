package cz.tetrisek

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

fun MainViewController() = ComposeUIViewController {
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val resignObserver = center.addObserverForName(
            name = UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            pauseGame()
        }
        val backgroundObserver = center.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            pauseGame()
        }
        onDispose {
            center.removeObserver(resignObserver)
            center.removeObserver(backgroundObserver)
        }
    }
    App()
}
