package to.kuudere.anisuge.platform

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberDeviceControls(): DeviceControls {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    return remember(activity) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        object : DeviceControls {
            override val currentVolume: Float
                get() {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    return if (max > 0) current / max else 0f
                }

            override val currentBrightness: Float
                get() {
                    val act = activity ?: return 0.5f
                    val window = act.window
                    val params = window.attributes
                    val windowBri = params.screenBrightness
                    if (windowBri >= 0f) return windowBri
                    val sysBri = try {
                        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                    } catch (e: Exception) { 128 }
                    return sysBri / 255f
                }

            override fun setVolume(volume: Float) {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    (volume * max).toInt(),
                    0
                )
            }

            override fun setBrightness(brightness: Float) {
                val act = activity ?: return
                val window = act.window
                val layoutParams = window.attributes
                layoutParams.screenBrightness = brightness.coerceIn(0.01f, 1f)
                window.attributes = layoutParams
            }
        }
    }
}
