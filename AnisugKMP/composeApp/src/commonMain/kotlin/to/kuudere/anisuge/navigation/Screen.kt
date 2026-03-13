package to.kuudere.anisuge.navigation

/** All navigation destinations in the app */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth   : Screen("auth")
    data class Home(val startOnDownloads: Boolean = false) : Screen(
        if (startOnDownloads) "home?downloads=true" else "home"
    ) {
        companion object {
            const val route = "home?downloads={downloads}"
        }
    }
    data object Search : Screen("search")
    data class Info(val animeId: String) : Screen("info/$animeId") {
        companion object {
            const val route = "info/{animeId}"
        }
    }
    data class Watch(
        val animeId: String,
        val episodeNumber: Int,
        val server: String? = null,
        val lang: String? = null,
        val offlinePath: String? = null,
        val offlineTitle: String? = null
    ) : Screen(
        "watch/$animeId/$episodeNumber" + buildString {
            if (server != null || lang != null || offlinePath != null || offlineTitle != null) append("?")
            val params = mutableListOf<String>()
            if (server != null) params.add("server=$server")
            if (lang != null) params.add("lang=$lang")
            if (offlinePath != null) params.add("offlinePath=$offlinePath")
            if (offlineTitle != null) params.add("offlineTitle=$offlineTitle")
            append(params.joinToString("&"))
        }
    ) {
        companion object {
            const val route = "watch/{animeId}/{episodeNumber}?server={server}&lang={lang}&offlinePath={offlinePath}&offlineTitle={offlineTitle}"
        }
    }

    data object Settings : Screen("settings")
    data object Latest : Screen("latest")
}
