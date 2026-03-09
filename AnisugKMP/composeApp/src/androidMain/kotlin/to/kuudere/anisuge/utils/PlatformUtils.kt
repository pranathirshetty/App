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

actual fun getDownloadsDirectory(): String {
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Anisug")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath
}

actual fun openDirectory(path: String) {
    try {
        val file = File(path)
        val dir = if (file.isDirectory) file else file.parentFile ?: file
        val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FAnisug")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        androidAppContext.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(getDownloadsDirectory()), DocumentsContract.Document.MIME_TYPE_DIR)
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

    fonts.forEach { fontPath ->
        cmdArray.add("-attach"); cmdArray.add(fontPath)
    }
    cmdArray.add("-metadata:s:t"); cmdArray.add("mimetype=application/x-truetype-font")

    subtitles.forEachIndexed { i, (_, label) ->
        cmdArray.add("-metadata:s:s:$i"); cmdArray.add("title=$label")
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