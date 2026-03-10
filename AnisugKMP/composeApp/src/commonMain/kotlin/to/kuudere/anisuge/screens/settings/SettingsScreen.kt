package to.kuudere.anisuge.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import to.kuudere.anisuge.data.models.SessionInfoResponse

// ── Colours ── Black & white only (matching ScheduleScreen) ─────────────────────
private val BG     = Color(0xFF0B0B0B)
private val BORDER = Color.White.copy(alpha = 0.10f)
private val MUTED  = Color.White.copy(alpha = 0.50f)
private val CARD   = Color.White.copy(alpha = 0.03f)
private val CARD_H = Color.White.copy(alpha = 0.07f)

data class SettingsNavItem(
    val tab: SettingsTab,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf<SettingsTab>(SettingsTab.Preferences) }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            is SettingsTab.Sessions -> viewModel.loadSessions()
            is SettingsTab.Security -> viewModel.loadMfaStatus()
            else -> {}
        }
    }

    val navItems = listOf(
        SettingsNavItem(SettingsTab.Preferences, "Preferences", Icons.Default.Settings),
        SettingsNavItem(SettingsTab.Sessions, "Sessions", Icons.Default.Devices),
        SettingsNavItem(SettingsTab.Security, "Security", Icons.Default.Security)
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (uiState.errorMessage != null) Color(0xFFB71C1C) else Color(0xFF1B5E20),
                    contentColor = Color.White
                )
            }
        },
        containerColor = BG
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isLargeScreen = maxWidth >= 840.dp

            if (isLargeScreen) {
                // Desktop/Tablet: Sidebar layout
                Row(modifier = Modifier.fillMaxSize()) {
                    // Sidebar
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
                    ) {
                        Text(
                            "SETTINGS",
                            color = MUTED,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                        )

                        navItems.forEach { item ->
                            val isSelected = selectedTab == item.tab
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) CARD_H else Color.Transparent,
                                animationSpec = tween(200)
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) Color.White else MUTED,
                                animationSpec = tween(200)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .clickable { selectedTab = item.tab }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    item.label,
                                    color = contentColor,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Separator between sidebar and content
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 1.dp,
                        color = BORDER
                    )

                    // Content area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        SettingsContent(
                            selectedTab = selectedTab,
                            uiState = uiState,
                            navItems = navItems,
                            onLogout = onLogout,
                            viewModel = viewModel
                        )
                    }
                }
            } else {
                // Mobile: Tab layout
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = navItems.indexOfFirst { it.tab == selectedTab },
                        containerColor = BG,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[navItems.indexOfFirst { it.tab == selectedTab }]),
                                color = Color.White
                            )
                        }
                    ) {
                        navItems.forEach { item ->
                            val isSelected = selectedTab == item.tab
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTab = item.tab },
                                text = { Text(item.label) },
                                icon = { Icon(item.icon, contentDescription = null, tint = if (isSelected) Color.White else MUTED) }
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        SettingsContent(
                            selectedTab = selectedTab,
                            uiState = uiState,
                            navItems = navItems,
                            onLogout = onLogout,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    selectedTab: SettingsTab,
    uiState: SettingsUiState,
    navItems: List<SettingsNavItem>,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel,
) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "settings_content"
    ) { tab ->
        when (tab) {
            is SettingsTab.Preferences -> PreferencesTab(
                uiState = uiState,
                onAutoPlayChange = viewModel::setAutoPlay,
                onAutoNextChange = viewModel::setAutoNext,
                onSkipIntroChange = viewModel::setSkipIntro,
                onSkipOutroChange = viewModel::setSkipOutro,
                onDefaultLangChange = viewModel::setDefaultLang,
                onSyncPercentageChange = viewModel::setSyncPercentage,
                onSave = viewModel::savePreferences
            )
            is SettingsTab.Sessions -> SessionsTab(
                uiState = uiState,
                onDeleteSession = viewModel::deleteSession,
                onDeleteAllSessions = viewModel::deleteAllSessions,
                onLogout = onLogout
            )
            is SettingsTab.Security -> SecurityTab(
                uiState = uiState,
                onToggleMfa = viewModel::toggleMfa,
                onSetupTotp = viewModel::setupTotp,
                onVerifyTotp = viewModel::verifyTotp,
                onLoadRecoveryCodes = viewModel::loadRecoveryCodes,
                onDismissRecoveryCodes = viewModel::dismissRecoveryCodes,
                onDismissTotpSetup = viewModel::dismissTotpSetup,
                onPasswordChange = viewModel::changePassword,
                onCurrentPasswordChange = viewModel::setCurrentPassword,
                onNewPasswordChange = viewModel::setNewPassword,
                onConfirmPasswordChange = viewModel::setConfirmPassword
            )
        }
    }
}

@Composable
private fun PreferencesTab(
    uiState: SettingsUiState,
    onAutoPlayChange: (Boolean) -> Unit,
    onAutoNextChange: (Boolean) -> Unit,
    onSkipIntroChange: (Boolean) -> Unit,
    onSkipOutroChange: (Boolean) -> Unit,
    onDefaultLangChange: (Boolean) -> Unit,
    onSyncPercentageChange: (Int) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsSection(title = "Playback") {
            SettingsSwitch(
                title = "Auto Play",
                description = "Automatically start playing videos",
                icon = Icons.Default.PlayArrow,
                checked = uiState.preferences.autoPlay,
                onCheckedChange = onAutoPlayChange
            )
            SettingsDivider()
            SettingsSwitch(
                title = "Auto Next",
                description = "Automatically play next episode",
                icon = Icons.Default.PlayArrow,
                checked = uiState.preferences.autoNext,
                onCheckedChange = onAutoNextChange
            )
            SettingsDivider()
            SettingsSwitch(
                title = "Skip Intro",
                description = "Automatically skip anime intros",
                icon = Icons.Default.Notifications,
                checked = uiState.preferences.skipIntro,
                onCheckedChange = onSkipIntroChange
            )
            SettingsDivider()
            SettingsSwitch(
                title = "Skip Outro",
                description = "Automatically skip anime outros",
                icon = Icons.Default.Notifications,
                checked = uiState.preferences.skipOutro,
                onCheckedChange = onSkipOutroChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Language") {
            SettingsSwitch(
                title = "Default to English Dub",
                description = "Use English dubbed audio when available",
                icon = Icons.Default.Settings,
                checked = uiState.preferences.defaultLang,
                onCheckedChange = onDefaultLangChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Sync") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Watch Progress Sync", color = Color.White, fontWeight = FontWeight.Medium)
                Text(
                    "Progress will sync when ${uiState.preferences.syncPercentage}% watched",
                    color = MUTED,
                    fontSize = 12.sp
                )
                Slider(
                    value = uiState.preferences.syncPercentage.toFloat(),
                    onValueChange = { onSyncPercentageChange(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = BORDER
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSave,
            enabled = uiState.hasPreferencesChanges && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = CARD,
                disabledContentColor = MUTED
            )
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Text("Save Changes")
            }
        }
    }
}

@Composable
private fun SessionsTab(
    uiState: SettingsUiState,
    onDeleteSession: (String, () -> Unit) -> Unit,
    onDeleteAllSessions: (() -> Unit) -> Unit,
    onLogout: () -> Unit,
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.sessions.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CARD),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SessionStat(count = uiState.sessions.size, label = "Active Sessions")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(uiState.sessions) { session ->
                    SessionCard(
                        session = session,
                        isCurrent = session.current,
                        onDelete = {
                            if (session.current) {
                                onDeleteSession("current") { onLogout() }
                            } else {
                                onDeleteSession(session.id) {}
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("End All Sessions")
                    }
                }
            }
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("End All Sessions") },
                text = { Text("This will log you out from all devices. Are you sure?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAllDialog = false
                            onDeleteAllSessions { onLogout() }
                        }
                    ) {
                        Text("Confirm", color = Color(0xFFEF4444))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = BG,
                titleContentColor = Color.White,
                textContentColor = MUTED
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionInfoResponse,
    isCurrent: Boolean,
    onDelete: () -> Unit,
) {
    val deviceDisplay = when {
        !session.deviceName.isNullOrBlank() -> session.deviceName
        !session.osName.isNullOrBlank() && session.osName != "Unknown" -> session.osName
        else -> "Unknown Device"
    }
    val browserDisplay = when {
        !session.clientName.isNullOrBlank() && session.clientName != "Unknown" -> session.clientName
        else -> "Unknown Browser"
    }
    val locationDisplay = session.countryName ?: session.countryCode ?: "Unknown Location"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isCurrent) CARD_H else CARD),
        shape = RoundedCornerShape(12.dp),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, BORDER, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        session.osName?.contains("Android", ignoreCase = true) == true -> Icons.Default.PhoneAndroid
                        session.osName?.contains("iOS", ignoreCase = true) == true -> Icons.Default.PhoneAndroid
                        session.osName?.contains("Tablet", ignoreCase = true) == true -> Icons.Default.TabletAndroid
                        else -> Icons.Default.Computer
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        deviceDisplay,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Current",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(browserDisplay, color = MUTED, fontSize = 12.sp, maxLines = 1)
                    if (locationDisplay != "Unknown Location") {
                        Text(" • ", color = MUTED, fontSize = 12.sp)
                        Text(locationDisplay, color = MUTED, fontSize = 12.sp, maxLines = 1)
                    }
                }

                if (!session.ip.isNullOrBlank()) {
                    Text(
                        "IP: ${session.ip}",
                        color = MUTED.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Text(
                    "Last active: ${formatRelativeTime(session.createdAt)}",
                    color = MUTED.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = if (isCurrent) MUTED else Color(0xFFEF4444))
            ) {
                Text(if (isCurrent) "Sign Out" else "End")
            }
        }
    }
}

@Composable
private fun SessionStat(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MUTED, fontSize = 12.sp)
    }
}

@Composable
private fun SecurityTab(
    uiState: SettingsUiState,
    onToggleMfa: (Boolean) -> Unit,
    onSetupTotp: ((String) -> Unit) -> Unit,
    onVerifyTotp: (String, () -> Unit) -> Unit,
    onLoadRecoveryCodes: () -> Unit,
    onDismissRecoveryCodes: () -> Unit,
    onDismissTotpSetup: () -> Unit,
    onPasswordChange: (() -> Unit) -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
) {
    var showPasswordChange by remember { mutableStateOf(false) }
    var showTotpDialog by remember { mutableStateOf(false) }
    var totpCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsSection(title = "Two-Factor Authentication") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Enable MFA", color = Color.White, fontWeight = FontWeight.Medium)
                    Text(
                        uiState.mfaStatus?.let { if (it.mfaEnabled) "MFA is enabled" else "MFA is disabled" } ?: "Loading...",
                        color = MUTED,
                        fontSize = 12.sp
                    )
                }

                if (uiState.isLoadingMfa) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Switch(
                        checked = uiState.mfaStatus?.mfaEnabled == true,
                        onCheckedChange = { enabled ->
                            if (enabled && uiState.mfaStatus?.totpEnabled != true) {
                                showTotpDialog = true
                                onSetupTotp {}
                            } else {
                                onToggleMfa(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color.White,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = BORDER
                        )
                    )
                }
            }

            if (uiState.mfaStatus?.totpEnabled == true) {
                TextButton(
                    onClick = onLoadRecoveryCodes,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("View Recovery Codes", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Password") {
            if (showPasswordChange) {
                PasswordChangeForm(
                    currentPassword = uiState.currentPassword,
                    newPassword = uiState.newPassword,
                    confirmPassword = uiState.confirmPassword,
                    isChanging = uiState.isChangingPassword,
                    onCurrentPasswordChange = onCurrentPasswordChange,
                    onNewPasswordChange = onNewPasswordChange,
                    onConfirmPasswordChange = onConfirmPasswordChange,
                    onSubmit = { onPasswordChange { showPasswordChange = false } },
                    onCancel = { showPasswordChange = false }
                )
            } else {
                Button(
                    onClick = { showPasswordChange = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Password")
                }
            }
        }
    }

    if (showTotpDialog && uiState.totpSetupData != null) {
        AlertDialog(
            onDismissRequest = {
                showTotpDialog = false
                onDismissTotpSetup()
            },
            title = { Text("Setup Authenticator") },
            text = {
                Column {
                    Text(
                        "Scan the QR code with your authenticator app, then enter the 6-digit code below.",
                        color = MUTED,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val qrCodeBase64 = uiState.totpSetupData.qrCode
                    if (qrCodeBase64.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = qrCodeBase64,
                                    contentDescription = "QR Code",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Text(
                        "Or enter this key manually:",
                        color = MUTED,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CARD),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BORDER)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                uiState.totpSetupData.secret.chunked(4).joinToString(" "),
                                color = Color.White,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = totpCode,
                        onValueChange = { if (it.length <= 6) totpCode = it.filter { c -> c.isDigit() } },
                        label = { Text("6-digit Code") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = BORDER
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onVerifyTotp(totpCode) {
                            showTotpDialog = false
                            totpCode = ""
                        }
                    },
                    enabled = totpCode.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTotpDialog = false
                        onDismissTotpSetup()
                        totpCode = ""
                    }
                ) {
                    Text("Cancel", color = MUTED)
                }
            },
            containerColor = BG,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (uiState.showRecoveryCodes) {
        AlertDialog(
            onDismissRequest = onDismissRecoveryCodes,
            title = { Text("Recovery Codes") },
            text = {
                Column {
                    Text(
                        "Save these codes in a safe place. You can use them to recover your account if you lose access to your authenticator.",
                        color = MUTED,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CARD),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BORDER)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            uiState.recoveryCodes.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    row.forEach { code ->
                                        Text(
                                            code,
                                            color = Color.White,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                if (row.size == 2) Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissRecoveryCodes,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Done")
                }
            },
            containerColor = BG,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
private fun PasswordChangeForm(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    isChanging: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = currentPassword,
            onValueChange = onCurrentPasswordChange,
            label = { Text("Current Password") },
            singleLine = true,
            visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                    Icon(
                        if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MUTED
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = BORDER
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = { Text("New Password") },
            singleLine = true,
            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showNewPassword = !showNewPassword }) {
                    Icon(
                        if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MUTED
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = BORDER
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(
                        if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MUTED
                    )
                }
            },
            isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = BORDER,
                errorBorderColor = Color(0xFFEF4444),
                errorLabelColor = Color(0xFFEF4444)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel", color = MUTED) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSubmit,
                enabled = currentPassword.isNotEmpty() && newPassword.length >= 8 && newPassword == confirmPassword && !isChanging,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                if (isChanging) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Change Password")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CARD),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BORDER)
    ) {
        Column {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider(color = BORDER)
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = BORDER, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium)
                Text(description, color = MUTED, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = BORDER
            )
        )
    }
}

private fun formatRelativeTime(timestamp: String): String {
    return try {
        val instant = kotlinx.datetime.Instant.parse(timestamp)
        val now = kotlinx.datetime.Clock.System.now()
        val diff = now - instant

        when {
            diff.inWholeMinutes < 1 -> "just now"
            diff.inWholeMinutes < 60 -> "${diff.inWholeMinutes}m ago"
            diff.inWholeHours < 24 -> "${diff.inWholeHours}h ago"
            diff.inWholeDays < 30 -> "${diff.inWholeDays}d ago"
            else -> "${diff.inWholeDays / 30}mo ago"
        }
    } catch (e: Exception) {
        timestamp
    }
}
