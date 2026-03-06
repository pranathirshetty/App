sealed class Screen(val route: String) {
    data class Info(val animeId: String) : Screen("info/$animeId") {
        companion object {
            const val route = "info/{animeId}"
        }
    }
}

fun main() {
    println(Screen.Info("123").route)
}
