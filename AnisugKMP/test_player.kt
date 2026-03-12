import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.ListenableFuture

class TestPlayer : SimpleBasePlayer(android.os.Looper.getMainLooper()) {
    override fun getState(): State = TODO()
    override fun handleSetPlayWhenReady(p0: Boolean): ListenableFuture<*> = TODO()
    override fun handlePlay(): ListenableFuture<*> = TODO()
}
