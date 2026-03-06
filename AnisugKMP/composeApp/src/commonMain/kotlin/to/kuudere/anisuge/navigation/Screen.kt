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
    data class Watch(val animeId: String, val episodeNumber: Int) : Screen("watch/$animeId/$episodeNumber") {
        companion object {
            const val route = "watch/{animeId}/{episodeNumber}"
        }
    }
}
