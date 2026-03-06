package to.kuudere.anisuge.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

lateinit var androidAppContext: Context

actual fun createDataStore(): DataStore<Preferences> {
    val file = androidAppContext.filesDir.resolve("session.preferences_pb")
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { file.absolutePath.toPath() }
    )
}
