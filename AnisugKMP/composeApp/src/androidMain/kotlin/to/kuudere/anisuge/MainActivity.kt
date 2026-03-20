package to.kuudere.anisuge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import to.kuudere.anisuge.platform.androidAppContext

import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

class MainActivity : ComponentActivity() {
    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            val path = to.kuudere.anisuge.platform.getPathFromUri(it)
            to.kuudere.anisuge.platform.onFolderPickedCallback?.invoke(path ?: it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidAppContext = applicationContext
        to.kuudere.anisuge.platform.currentMainActivity = this
        enableEdgeToEdge()
        setContent {
            App(onAppExit = { finishAffinity() })
        }
    }

    fun openFolderPicker() {
        folderPickerLauncher.launch(null)
    }
}
