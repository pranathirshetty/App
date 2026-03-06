package to.kuudere.anisuge.navigation

/** All navigation destinations in the app */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth   : Screen("auth")
    data object Home   : Screen("home")
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
        val lang: String? = null
    ) : Screen(
        "watch/$animeId/$episodeNumber" + buildString {
            if (server != null || lang != null) append("?")
            val params = mutableListOf<String>()
            if (server != null) params.add("server=$server")
            if (lang != null) params.add("lang=$lang")
            append(params.joinToString("&"))
        }
    ) {
        companion object {
            const val route = "watch/{animeId}/{episodeNumber}?server={server}&lang={lang}"
        }
    }
}
