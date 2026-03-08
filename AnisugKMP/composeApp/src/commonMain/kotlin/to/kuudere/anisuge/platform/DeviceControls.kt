package to.kuudere.anisuge.platform

import androidx.compose.runtime.Composable

interface DeviceControls {
    val currentVolume: Float       // 0.0f to 1.0f
    val currentBrightness: Float   // 0.0f to 1.0f
    fun setVolume(volume: Float)
    fun setBrightness(brightness: Float)
}

@Composable
expect fun rememberDeviceControls(): DeviceControls
