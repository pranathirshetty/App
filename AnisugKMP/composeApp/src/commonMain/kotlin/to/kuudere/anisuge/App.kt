package to.kuudere.anisuge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import to.kuudere.anisuge.navigation.Screen
import to.kuudere.anisuge.screens.auth.AuthScreen
import to.kuudere.anisuge.screens.auth.AuthViewModel
import to.kuudere.anisuge.screens.splash.SplashScreen
import to.kuudere.anisuge.screens.splash.SplashViewModel
import to.kuudere.anisuge.screens.splash.SplashDestination
import to.kuudere.anisuge.screens.home.HomeScreen
import to.kuudere.anisuge.screens.home.HomeViewModel
import to.kuudere.anisuge.screens.search.SearchScreen
import to.kuudere.anisuge.screens.search.SearchViewModel
import to.kuudere.anisuge.screens.search.KUUDERE_GENRES
import to.kuudere.anisuge.screens.info.AnimeInfoScreen
import to.kuudere.anisuge.screens.info.AnimeInfoViewModel
import to.kuudere.anisuge.screens.watch.WatchScreen
import to.kuudere.anisuge.screens.watch.WatchViewModel
import to.kuudere.anisuge.screens.watchlist.WatchlistViewModel
import to.kuudere.anisuge.screens.schedule.ScheduleViewModel
import to.kuudere.anisuge.theme.AnisugTheme
import androidx.navigation.NamedNavArgument
import androidx.navigation.navArgument

@Composable
fun App(onAppExit: () -> Unit = {}) {
    AnisugTheme {
        val navController = rememberNavController()
        val splashVm = remember { SplashViewModel(AppComponent.authService) }
        val authVm   = remember { AuthViewModel(AppComponent.authService) }
        val homeVm   = remember { HomeViewModel(AppComponent.homeService, AppComponent.authService, AppComponent.infoService) }
        val searchVm = remember { SearchViewModel(AppComponent.searchService) }
        val infoVm   = remember { AnimeInfoViewModel(AppComponent.infoService) }
        val watchVm  = remember { WatchViewModel(AppComponent.infoService, AppComponent.settingsStore) }
        val watchlistVm = remember { WatchlistViewModel() }
        val scheduleVm = remember { ScheduleViewModel(AppComponent.scheduleService) }


        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            NavHost(
                navController    = navController,
                startDestination = Screen.Splash.route,
                // Splash exit: keep it visible while auth fades in on top
                enterTransition  = { fadeIn(animationSpec = tween(400)) },
                exitTransition   = { fadeOut(animationSpec = tween(400)) },
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        viewModel        = splashVm,
                        onNavigateToAuth = {
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Screen.Auth.route) {
                    AuthScreen(
                        viewModel      = authVm,
                        onLoginSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        homeViewModel = homeVm,
                        searchViewModel = searchVm,
                        watchlistViewModel = watchlistVm,
                        scheduleViewModel = scheduleVm,
                        onAnimeClick = { animeId -> navController.navigate(Screen.Info(animeId).route) },
                        onWatchClick = { id, lang, ep, server -> navController.navigate(Screen.Watch(id, ep, server, lang).route) },
                        onWatchOffline = { id, ep, path -> 
                            navController.navigate(Screen.Watch(id, ep, offlinePath = path).route) 
                        },
                        onLogout = {
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onExit = onAppExit,
                        startOnDownloads = splashVm.destination.value == SplashDestination.GoHomeOffline
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        viewModel = searchVm,
                        onAnimeClick = { animeId -> navController.navigate(Screen.Info(animeId).route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Info.route) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                    AnimeInfoScreen(
                        animeId = animeId,
                        viewModel = infoVm,
                        onBack = { navController.popBackStack() },
                        onWatchEpisode = { id, lang, ep -> navController.navigate(Screen.Watch(id, ep, null, lang).route) },
                        onGenreClick = { genre ->
                            searchVm.clearFilters()
                            searchVm.onGenreToggle(genre)
                            searchVm.search()
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }

                composable(
                    route = Screen.Watch.route,
                    enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) },
                    exitTransition = { fadeOut(animationSpec = tween(400)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                    popExitTransition = { fadeOut(animationSpec = tween(200)) },
                    arguments = listOf(
                        navArgument("animeId") { type = androidx.navigation.NavType.StringType },
                        navArgument("episodeNumber") { type = androidx.navigation.NavType.StringType },
                        navArgument("server") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("lang") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("offlinePath") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                    val episodeNumStr = backStackEntry.arguments?.getString("episodeNumber") ?: "1"
                    val episodeNum = episodeNumStr.toIntOrNull() ?: 1
                    val server = backStackEntry.arguments?.getString("server")
                    val lang = backStackEntry.arguments?.getString("lang")
                    val offlinePath = backStackEntry.arguments?.getString("offlinePath")

                    WatchScreen(
                        animeId = animeId,
                        episodeNumber = episodeNum,
                        server = server,
                        lang = lang,
                        offlinePath = offlinePath,
                        viewModel = watchVm,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
