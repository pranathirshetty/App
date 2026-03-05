package to.kuudere.anisuge.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anisugkmp.composeapp.generated.resources.Res
import anisugkmp.composeapp.generated.resources.logo_txt
import org.jetbrains.compose.resources.painterResource
import to.kuudere.anisuge.theme.Border
import to.kuudere.anisuge.theme.Muted
import to.kuudere.anisuge.theme.Surface

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Navigate on success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onLoginSuccess()
    }

    // Show errors in snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Platform-specific video/gradient background
        AuthVideoBackground()

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        // Responsive layout
        BoxWithConstraints {
            if (maxWidth > 900.dp) {
                DesktopAuthLayout(state, viewModel)
            } else {
                MobileAuthLayout(state, viewModel)
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) { data ->
            Snackbar(
                snackbarData     = data,
                containerColor   = Color(0xFF2A0000),
                contentColor     = Color.White,
                shape            = RoundedCornerShape(8.dp),
            )
        }
    }
}

// ── Desktop layout (two columns) ─────────────────────────────────────────────
@Composable
private fun DesktopAuthLayout(state: AuthUiState, viewModel: AuthViewModel) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left — promo content + animated anime cards
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .padding(horizontal = 64.dp, vertical = 48.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter            = painterResource(Res.drawable.logo_txt),
                    contentDescription = "Anisuge",
                    modifier           = Modifier.height(56.dp),
                    contentScale       = ContentScale.Fit,
                )
                Spacer(Modifier.height(40.dp))
                Text(
                    text       = "Stream, Discover &\nDownload",
                    style      = MaterialTheme.typography.headlineLarge,
                    color      = Color.White,
                    lineHeight = 50.sp,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text  = "Find and download torrents, watch trailers, manage\nyour list, search, browse and discover anime,\nwatch together with friends and more — all in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
                Spacer(Modifier.height(48.dp))
                // Featured anime cards strip
                AnimeCardStrip()
            }
        }

        // Right panel — dark glass form
        Box(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF0A0A0A).copy(alpha = 0.98f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .padding(horizontal = 48.dp, vertical = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuthForm(state = state, viewModel = viewModel, centered = true)
            }
        }
    }
}

// ── Mobile layout (single column) ────────────────────────────────────────────
@Composable
private fun MobileAuthLayout(state: AuthUiState, viewModel: AuthViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Image(
                painter            = painterResource(Res.drawable.logo_txt),
                contentDescription = "Anisuge",
                modifier           = Modifier.height(48.dp),
                contentScale       = ContentScale.Fit,
            )
            Spacer(Modifier.height(32.dp))
            AuthForm(state = state, viewModel = viewModel, centered = false)
        }
    }
}

// ── Shared auth form ──────────────────────────────────────────────────────────
@Composable
private fun AuthForm(state: AuthUiState, viewModel: AuthViewModel, centered: Boolean) {
    val passwordFocus = remember { FocusRequester() }
    val nameFocus     = remember { FocusRequester() }

    // Title
    val titleAlign = if (centered) Alignment.CenterHorizontally else Alignment.Start
    Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        AnimatedContent(
            targetState  = state.isLogin,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label        = "auth_title",
        ) { isLogin ->
            Text(
                text  = if (isLogin) "Welcome back" else "Create account",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        AnimatedContent(
            targetState  = state.isLogin,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label        = "auth_subtitle",
        ) { isLogin ->
            Text(
                text  = if (isLogin) "Sign in to continue watching" else "Join our streaming platform",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
        }
    }

    Spacer(Modifier.height(28.dp))

    // Display name — only visible in register mode
    AnimatedVisibility(
        visible = !state.isLogin,
        enter   = expandVertically() + fadeIn(),
        exit    = shrinkVertically() + fadeOut(),
    ) {
        Column {
            AnisugTextField(
                value          = state.displayName,
                onValueChange  = viewModel::onDisplayNameChange,
                label          = "Full name",
                placeholder    = "Enter your full name",
                imeAction      = ImeAction.Next,
                onImeAction    = { passwordFocus.requestFocus() },
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    AnisugTextField(
        value         = state.email,
        onValueChange = viewModel::onEmailChange,
        label         = "Email",
        placeholder   = "Enter your email address",
        keyboardType  = KeyboardType.Email,
        imeAction     = ImeAction.Next,
        onImeAction   = { passwordFocus.requestFocus() },
    )

    Spacer(Modifier.height(16.dp))

    AnisugTextField(
        value           = state.password,
        onValueChange   = viewModel::onPasswordChange,
        label           = "Password",
        placeholder     = "Enter your password",
        isPassword      = true,
        keyboardType    = KeyboardType.Password,
        imeAction       = ImeAction.Done,
        onImeAction     = { viewModel.submit() },
        focusRequester  = passwordFocus,
    )

    Spacer(Modifier.height(24.dp))

    // Submit button
    Button(
        onClick  = viewModel::submit,
        enabled  = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape    = RoundedCornerShape(8.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Color.White,
            contentColor           = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.5f),
            disabledContentColor   = Color.Black,
        ),
    ) {
        AnimatedContent(
            targetState = state.isLoading,
            label       = "submit_content",
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(22.dp),
                    color     = Color.Black,
                    strokeWidth = 2.5.dp,
                )
            } else {
                AnimatedContent(
                    targetState  = state.isLogin,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label        = "button_label",
                ) { isLogin ->
                    Text(
                        text       = if (isLogin) "Sign in" else "Create account",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    // Toggle login / register
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState  = state.isLogin,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label        = "toggle_label",
        ) { isLogin ->
            Text(
                text  = if (isLogin) "Don't have an account? " else "Already have an account? ",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
        TextButton(
            onClick = viewModel::toggleMode,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
        ) {
            AnimatedContent(
                targetState  = state.isLogin,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label        = "toggle_btn",
            ) { isLogin ->
                Text(
                    text       = if (isLogin) "Sign up" else "Sign in",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
            }
        }
    }
}

// ── Text field ────────────────────────────────────────────────────────────────
@Composable
private fun AnisugTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean     = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction    = ImeAction.Next,
    onImeAction: () -> Unit = {},
    focusRequester: FocusRequester? = null,
) {
    var showPassword by remember { mutableStateOf(false) }

    Column {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
            modifier   = Modifier.padding(bottom = 6.dp),
        )

        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            placeholder   = {
                Text(
                    text  = placeholder,
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            singleLine            = true,
            visualTransformation  = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions       = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions       = KeyboardActions(onAny = { onImeAction() }),
            trailingIcon          = if (isPassword) {
                {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Text(
                            text  = if (showPassword) "👁" else "🔒",
                            fontSize = 16.sp,
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor         = Color.White,
                unfocusedTextColor       = Color.White,
                focusedContainerColor    = Color.Black.copy(alpha = 0.3f),
                unfocusedContainerColor  = Color.Black.copy(alpha = 0.3f),
                focusedBorderColor       = Color.White,
                unfocusedBorderColor     = Border,
                errorBorderColor         = Color(0xFFE53935),
                cursorColor              = Color.White,
            ),
            shape = RoundedCornerShape(6.dp),
        )
    }
}

// ── Anime card strip (desktop promo section) ──────────────────────────────────
private val animeList = listOf(
    "https://artworks.thetvdb.com/banners/v4/series/424536/posters/68d6d5b36aa2f.jpg",
    "https://artworks.thetvdb.com/banners/v4/series/377543/posters/655f6f3591801.jpg",
    "https://artworks.thetvdb.com/banners/v4/series/355480/posters/68aa38e36a087.jpg",
    "https://artworks.thetvdb.com/banners/v4/series/421069/posters/67026a480c6d1.jpg",
    "https://artworks.thetvdb.com/banners/v4/series/414221/posters/639d9b966b354.jpg",
)

@Composable
private fun AnimeCardStrip() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        animeList.take(4).forEach { url ->
            coil3.compose.AsyncImage(
                model             = url,
                contentDescription = null,
                modifier          = Modifier
                    .width(90.dp)
                    .height(132.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Border, RoundedCornerShape(8.dp)),
                contentScale      = ContentScale.Crop,
            )
        }
    }
}

// ── Platform video background ─────────────────────────────────────────────────
@Composable
expect fun AuthVideoBackground()

// ── BoxWithConstraints shim (Compose MP has it under foundation) ─────────────
@Composable
private fun BoxWithConstraints(
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) = androidx.compose.foundation.layout.BoxWithConstraints(content = content)

private typealias BoxWithConstraintsScope =
    androidx.compose.foundation.layout.BoxWithConstraintsScope
