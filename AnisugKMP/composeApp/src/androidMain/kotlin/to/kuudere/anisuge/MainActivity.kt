package to.kuudere.anisuge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import to.kuudere.anisuge.platform.androidAppContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidAppContext = applicationContext
        enableEdgeToEdge()
        setContent {
            App(onAppExit = { finishAffinity() })
        }
    }
}
