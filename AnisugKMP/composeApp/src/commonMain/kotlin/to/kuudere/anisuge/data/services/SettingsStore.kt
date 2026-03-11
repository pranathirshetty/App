package to.kuudere.anisuge.data.services

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        val AUTO_PLAY_KEY = booleanPreferencesKey("auto_play")
        val AUTO_NEXT_KEY = booleanPreferencesKey("auto_next")
        val AUTO_SKIP_INTRO_KEY = booleanPreferencesKey("auto_skip_intro")
        val AUTO_SKIP_OUTRO_KEY = booleanPreferencesKey("auto_skip_outro")
        val DEFAULT_LANG_KEY = booleanPreferencesKey("default_lang")
        val DEFAULT_COMMENTS_KEY = booleanPreferencesKey("default_comments")
        val SYNC_PERCENTAGE_KEY = androidx.datastore.preferences.core.intPreferencesKey("sync_percentage")
    }

    val autoPlayFlow: Flow<Boolean> = dataStore.data.map { it[AUTO_PLAY_KEY] ?: false }
    val autoNextFlow: Flow<Boolean> = dataStore.data.map { it[AUTO_NEXT_KEY] ?: true }
    val autoSkipIntroFlow: Flow<Boolean> = dataStore.data.map { it[AUTO_SKIP_INTRO_KEY] ?: false }
    val autoSkipOutroFlow: Flow<Boolean> = dataStore.data.map { it[AUTO_SKIP_OUTRO_KEY] ?: false }
    val defaultLangFlow: Flow<Boolean> = dataStore.data.map { it[DEFAULT_LANG_KEY] ?: false }
    val defaultCommentsFlow: Flow<Boolean> = dataStore.data.map { it[DEFAULT_COMMENTS_KEY] ?: true }
    val syncPercentageFlow: Flow<Int> = dataStore.data.map { it[SYNC_PERCENTAGE_KEY] ?: 80 }

    suspend fun setAutoPlay(enabled: Boolean) {
        dataStore.edit { it[AUTO_PLAY_KEY] = enabled }
    }

    suspend fun setAutoNext(enabled: Boolean) {
        dataStore.edit { it[AUTO_NEXT_KEY] = enabled }
    }

    suspend fun setAutoSkipIntro(enabled: Boolean) {
        dataStore.edit { it[AUTO_SKIP_INTRO_KEY] = enabled }
    }

    suspend fun setAutoSkipOutro(enabled: Boolean) {
        dataStore.edit { it[AUTO_SKIP_OUTRO_KEY] = enabled }
    }

    suspend fun setDefaultLang(enabled: Boolean) {
        dataStore.edit { it[DEFAULT_LANG_KEY] = enabled }
    }

    suspend fun setDefaultComments(enabled: Boolean) {
        dataStore.edit { it[DEFAULT_COMMENTS_KEY] = enabled }
    }

    suspend fun setSyncPercentage(percentage: Int) {
        dataStore.edit { it[SYNC_PERCENTAGE_KEY] = percentage }
    }
}
