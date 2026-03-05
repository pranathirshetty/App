package to.kuudere.anisuge

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import to.kuudere.anisuge.data.services.AuthService
import to.kuudere.anisuge.data.services.SessionStore
import to.kuudere.anisuge.platform.createDataStore

/**
 * Manual dependency graph — keeps it simple for now, easy to swap out for Koin later.
 * Everything is lazily initialised and kept as a singleton for the app lifetime.
 */
object AppComponent {
    val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient         = true
                })
            }
            install(Logging) { level = LogLevel.HEADERS }
        }
    }

    val sessionStore: SessionStore by lazy {
        SessionStore(createDataStore())
    }

    val authService: AuthService by lazy {
        AuthService(sessionStore, httpClient)
    }
}
