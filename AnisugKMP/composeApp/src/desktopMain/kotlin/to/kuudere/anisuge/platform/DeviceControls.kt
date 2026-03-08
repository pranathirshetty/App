package to.kuudere.anisuge.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberDeviceControls(): DeviceControls = remember {
    object : DeviceControls {
        override val currentVolume: Float = 1f
        override val currentBrightness: Float = 1f
        override fun setVolume(volume: Float) {}
        override fun setBrightness(brightness: Float) {}
    }
}
