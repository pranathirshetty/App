package to.kuudere.anisuge

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
                    val coroutineScope = rememberCoroutineScope()
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text  = "🏠 Home — coming soon",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    AppComponent.authService.logout()
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB71C1C),
                                contentColor   = Color.White,
                            )
                        ) {
                            Text(text = "Log Out", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
