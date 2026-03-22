package to.kuudere.anisuge.utils

import android.os.Environment
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import to.kuudere.anisuge.platform.androidAppContext
import io.microshow.rxffmpeg.RxFFmpegInvoke
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat

actual fun getDownloadsDirectory(): String {
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Anisug")
    if (!dir.exists()) {
        try {
            dir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return dir.absolutePath
}

actual fun getCacheDirectory(): String {
    return androidAppContext.filesDir.absolutePath
}

actual fun hasStoragePermission(): Boolean {
    // Android 11+ doesn't need WRITE_EXTERNAL_STORAGE for app-created files in Downloads
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return true
    
    // Android 10 with legacy storage support also might not need it for its own files
    // but better check if we have it requested in manifest.
    return ContextCompat.checkSelfPermission(
        androidAppContext,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
actual fun RequestStoragePermission(onResult: (Boolean) -> Unit) {
    if (hasStoragePermission()) {
        onResult(true)
        return
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }

    SideEffect {
        launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}

actual fun hasNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    
    return ContextCompat.checkSelfPermission(
        androidAppContext,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
actual fun RequestNotificationPermission(onResult: (Boolean) -> Unit) {
    if (hasNotificationPermission()) {
        onResult(true)
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onResult(isGranted)
        }

        SideEffect {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    } else {
        onResult(true)
    }
}

actual fun openDirectory(path: String) {
    try {
        val file = File(path)
        val dir = if (file.isDirectory) file else file.parentFile ?: file

        // Build the content URI relative to external storage root
        val extRoot = Environment.getExternalStorageDirectory().absolutePath
        val relativePath = dir.absolutePath.removePrefix(extRoot).removePrefix("/")
        val encodedPath = relativePath.replace("/", "%2F")
        val documentUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3A$encodedPath")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(documentUri, DocumentsContract.Document.MIME_TYPE_DIR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        androidAppContext.startActivity(intent)
    } catch (e: Exception) {
        try {
            // Fallback: open the root Anisug folder
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FAnisug")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            androidAppContext.startActivity(intent)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}

actual suspend fun muxToMkv(
    videoPath: String,
    audioPath: String?,
    subtitles: List<Pair<String, String>>,
    fonts: List<String>,
    metadataPath: String?,
    outputPath: String
): Boolean = withContext(Dispatchers.IO) {
    val cmdArray = mutableListOf("ffmpeg", "-y")
    cmdArray.add("-i"); cmdArray.add(videoPath)
    if (audioPath != null) {
        cmdArray.add("-i"); cmdArray.add(audioPath)
    }
    subtitles.forEach { (path, _) ->
        cmdArray.add("-i"); cmdArray.add(path)
    }
    
    val metadataIndex = if (metadataPath != null) {
        val index = 1 + (if (audioPath != null) 1 else 0) + subtitles.size
        cmdArray.add("-i"); cmdArray.add(metadataPath)
        index
    } else -1

    // Mapping
    cmdArray.add("-map"); cmdArray.add("0:v")
    if (audioPath != null) {
        cmdArray.add("-map"); cmdArray.add("1:a")
    } else {
        cmdArray.add("-map"); cmdArray.add("0:a?")
    }

    subtitles.forEachIndexed { i, _ ->
        val index = if (audioPath != null) i + 2 else i + 1
        cmdArray.add("-map"); cmdArray.add("$index:s")
    }

    if (metadataIndex != -1) {
        cmdArray.add("-map_metadata"); cmdArray.add("$metadataIndex")
    }

    // Attach fonts
    fonts.forEach { fontPath ->
        cmdArray.add("-attach"); cmdArray.add(fontPath)
    }
    cmdArray.add("-metadata:s:t"); cmdArray.add("mimetype=application/x-truetype-font")

    subtitles.forEachIndexed { i, (_, label) ->
        cmdArray.add("-metadata:s:s:$i")
        cmdArray.add("title=$label")
    }

    cmdArray.add("-c"); cmdArray.add("copy")
    cmdArray.add(outputPath)

    // Using runCommand with null subscriber
    try {
        val response = RxFFmpegInvoke.getInstance().runCommand(cmdArray.toTypedArray(), null)
        response == 0
    } catch (e: Exception) {
        false
    }
}
