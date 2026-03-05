package to.kuudere.anisuge.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

actual fun createDataStore(): DataStore<Preferences> {
    val homeDir = System.getProperty("user.home") ?: "."
    val path = "$homeDir/.local/share/anisuge/session.preferences_pb"
    // Ensure directory exists
    java.io.File(path).parentFile?.mkdirs()
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { path.toPath() }
    )
}
