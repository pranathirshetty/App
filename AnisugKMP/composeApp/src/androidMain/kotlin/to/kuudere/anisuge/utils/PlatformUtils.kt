package to.kuudere.anisuge.utils

import android.os.Environment
import java.io.File

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import to.kuudere.anisuge.platform.androidAppContext

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
        // Fallback to generic picker if specific one fails
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
