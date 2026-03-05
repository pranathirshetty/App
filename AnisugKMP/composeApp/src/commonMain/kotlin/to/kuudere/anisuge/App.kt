package to.kuudere.anisuge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import to.kuudere.anisuge.navigation.Screen
import to.kuudere.anisuge.screens.auth.AuthScreen
import to.kuudere.anisuge.screens.auth.AuthViewModel
import to.kuudere.anisuge.screens.splash.SplashScreen
import to.kuudere.anisuge.screens.splash.SplashViewModel
import to.kuudere.anisuge.theme.AnisugTheme

@Composable
fun App() {
    AnisugTheme {
        val navController = rememberNavController()
        val splashVm = remember { SplashViewModel(AppComponent.authService) }
        val authVm   = remember { AuthViewModel(AppComponent.authService) }

        NavHost(
            navController    = navController,
            startDestination = Screen.Splash.route,
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
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "🏠 Home — coming soon",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }
    }
}
