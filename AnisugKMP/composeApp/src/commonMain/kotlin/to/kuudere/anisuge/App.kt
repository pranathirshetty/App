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
import to.kuudere.anisuge.navigation.Screen
import to.kuudere.anisuge.screens.auth.AuthScreen
import to.kuudere.anisuge.screens.auth.AuthViewModel
import to.kuudere.anisuge.screens.splash.SplashScreen
import to.kuudere.anisuge.screens.splash.SplashViewModel
import to.kuudere.anisuge.screens.home.HomeScreen
import to.kuudere.anisuge.screens.home.HomeViewModel
import to.kuudere.anisuge.screens.search.SearchScreen
import to.kuudere.anisuge.screens.search.SearchViewModel
import to.kuudere.anisuge.screens.search.KUUDERE_GENRES
import to.kuudere.anisuge.screens.info.AnimeInfoScreen
import to.kuudere.anisuge.screens.info.AnimeInfoViewModel
import to.kuudere.anisuge.theme.AnisugTheme
import androidx.navigation.NamedNavArgument

@Composable
fun App(onAppExit: () -> Unit = {}) {
    AnisugTheme {
        val navController = rememberNavController()
        val splashVm = remember { SplashViewModel(AppComponent.authService) }
        val authVm   = remember { AuthViewModel(AppComponent.authService) }
        val homeVm   = remember { HomeViewModel(AppComponent.homeService, AppComponent.authService, AppComponent.infoService) }
        val searchVm = remember { SearchViewModel(AppComponent.searchService) }
        val infoVm   = remember { AnimeInfoViewModel(AppComponent.infoService) }


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
                        onAnimeClick = { animeId -> navController.navigate(Screen.Info(animeId).route) },
                        onWatchClick = { _, _, _ -> /* TODO: navigate to watch */ },
                        onExit       = onAppExit,
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
                        onWatchEpisode = { id, lang, ep -> /* TODO: navigate to watch */ },
                        onGenreClick = { genre ->
                            searchVm.clearFilters()
                            searchVm.onGenreToggle(genre)
                            searchVm.search()
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }
            }
        }
    }
}
