package to.kuudere.anisuge.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/** Returns a platform-specific DataStore<Preferences> instance. */
expect fun createDataStore(): DataStore<Preferences>
