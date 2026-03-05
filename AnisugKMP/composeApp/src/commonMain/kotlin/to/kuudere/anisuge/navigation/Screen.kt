package to.kuudere.anisuge.navigation

/** All navigation destinations in the app */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth   : Screen("auth")
    data object Home   : Screen("home")
    data object Search : Screen("search")
}
