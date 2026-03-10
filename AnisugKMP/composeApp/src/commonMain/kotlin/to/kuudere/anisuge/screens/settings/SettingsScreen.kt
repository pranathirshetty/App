package to.kuudere.anisuge.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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

// ── Colors ── Black & white theme ────────────────────────────────────────────────
private val BG       = Color(0xFF0B0B0B)
private val BG_CARD  = Color(0xFF141414)
private val BG_HOVER = Color(0xFF1E1E1E)
private val BORDER   = Color.White.copy(alpha = 0.08f)
private val MUTED    = Color.White.copy(alpha = 0.5f)
private val TEXT     = Color.White

// ── Data ────────────────────────────────────────────────────────────────────────
data class SettingsNavItem(
    val tab: SettingsTab,
    val label: String,
    val icon: ImageVector
)

// ── Main Screen ─────────────────────────────────────────────────────────────────
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
        SettingsNavItem(SettingsTab.Security, "Security", Icons.Default.Lock)
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
            val isLargeScreen = maxWidth >= 900.dp

            if (isLargeScreen) {
                // Desktop: Sidebar + Centered Content
                Row(modifier = Modifier.fillMaxSize()) {
                    // Sidebar
                    Sidebar(
                        navItems = navItems,
                        selectedTab = selectedTab,
                        onTabSelect = { selectedTab = it },
                        modifier = Modifier.width(260.dp)
                    )

                    VerticalDivider(thickness = 1.dp, color = BORDER)

                    // Content Area - Centered with max width
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        SettingsContent(
                            selectedTab = selectedTab,
                            uiState = uiState,
                            navItems = navItems,
                            onLogout = onLogout,
                            viewModel = viewModel,
                            modifier = Modifier
                                .widthIn(max = 900.dp)
                                .padding(horizontal = 48.dp, vertical = 40.dp)
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
                            TabRowDefaults.SecondaryIndicator(
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

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        SettingsContent(
                            selectedTab = selectedTab,
                            uiState = uiState,
                            navItems = navItems,
                            onLogout = onLogout,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ── Sidebar ─────────────────────────────────────────────────────────────────────
@Composable
private fun Sidebar(
    navItems: List<SettingsNavItem>,
    selectedTab: SettingsTab,
    onTabSelect: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(BG)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Header
        Text(
            "SETTINGS",
            color = MUTED,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)
        )

        // Nav Items
        navItems.forEach { item ->
            val isSelected = selectedTab == item.tab
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) BG_CARD else Color.Transparent,
                animationSpec = tween(200)
            )
            val textColor = if (isSelected) TEXT else MUTED

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onTabSelect(item.tab) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    item.label,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(thickness = 1.dp, color = BORDER, modifier = Modifier.padding(vertical = 16.dp))

        // App Stats Section
        Text(
            "APP STATS",
            color = MUTED,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
        )

        Column(modifier = Modifier.padding(start = 12.dp)) {
            AppStatItem("Hostname", "kuudere.to")
            AppStatItem("Backend", "Kuudere API")
            AppStatItem("Version", "1.0.0")
        }
    }
}

@Composable
private fun AppStatItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = MUTED, fontSize = 11.sp)
        Text(value, color = TEXT.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Content ─────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsContent(
    selectedTab: SettingsTab,
    uiState: SettingsUiState,
    navItems: List<SettingsNavItem>,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "settings_content",
        modifier = modifier
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

// ── Preferences Tab ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
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
    Column(modifier = Modifier.fillMaxWidth()) {
        // Large Title
        Text(
            "Preferences",
            color = TEXT,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Two Column Layout
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            maxItemsInEachRow = 2,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Auto Play
            SettingCard(
                title = "Auto Play",
                description = "Automatically start playing videos when page loads",
                modifier = Modifier.weight(1f)
            ) {
                SettingToggle(
                    checked = uiState.preferences.autoPlay,
                    onCheckedChange = onAutoPlayChange,
                    label = "Enable Auto Play"
                )
            }

            // Auto Next
            SettingCard(
                title = "Auto Next",
                description = "Automatically play next episode when current ends",
                modifier = Modifier.weight(1f)
            ) {
                SettingToggle(
                    checked = uiState.preferences.autoNext,
                    onCheckedChange = onAutoNextChange,
                    label = "Enable Auto Next"
                )
            }

            // Skip Intro
            SettingCard(
                title = "Skip Intro",
                description = "Automatically skip anime intro sequences",
                modifier = Modifier.weight(1f)
            ) {
                SettingToggle(
                    checked = uiState.preferences.skipIntro,
                    onCheckedChange = onSkipIntroChange,
                    label = "Skip intro automatically"
                )
            }

            // Skip Outro
            SettingCard(
                title = "Skip Outro",
                description = "Automatically skip anime outro/ending sequences",
                modifier = Modifier.weight(1f)
            ) {
                SettingToggle(
                    checked = uiState.preferences.skipOutro,
                    onCheckedChange = onSkipOutroChange,
                    label = "Skip outro automatically"
                )
            }

            // Default Language
            SettingCard(
                title = "Default Language",
                description = "Use English dubbed audio when available",
                modifier = Modifier.weight(1f)
            ) {
                SettingToggle(
                    checked = uiState.preferences.defaultLang,
                    onCheckedChange = onDefaultLangChange,
                    label = "Default to English Dub"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Sync Section - Full Width
        SettingCard(
            title = "Watch Progress Sync",
            description = "The watch percentage required to mark an episode as watched",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${uiState.preferences.syncPercentage}%", color = TEXT, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = uiState.preferences.syncPercentage.toFloat(),
                    onValueChange = { onSyncPercentageChange(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 49,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = BORDER
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Save Button
        if (uiState.hasPreferencesChanges) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Save Changes", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Setting Card Component ──────────────────────────────────────────────────────
@Composable
private fun SettingCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(title, color = TEXT, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(description, color = MUTED, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BG_CARD)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TEXT, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.White.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = BORDER
            )
        )
    }
}

// ── Sessions Tab ─────────────────────────────────────────────────────────────────
@Composable
private fun SessionsTab(
    uiState: SettingsUiState,
    onDeleteSession: (String) -> Unit,
    onDeleteAllSessions: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Sessions",
            color = TEXT,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Manage your active sessions across all devices",
            color = MUTED,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            uiState.currentSession?.let { session ->
                Text("Current Session", color = TEXT, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                SessionCard(session = session, isCurrent = true, onDelete = null)
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (uiState.sessions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Other Sessions", color = TEXT, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(
                        onClick = { onDeleteAllSessions() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                    ) {
                        Text("End All", fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                uiState.sessions.forEach { session ->
                    SessionCard(
                        session = session,
                        isCurrent = false,
                        onDelete = { onDeleteSession(session.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionInfoResponse,
    isCurrent: Boolean,
    onDelete: (() -> Unit)?
) {
    // Better device detection from multiple fields
    val clientName = session.clientName?.takeIf { it.isNotBlank() && it != "---" }
        ?: session.deviceModel?.takeIf { it.isNotBlank() }
        ?: "Unknown Device"

    val deviceIcon = when {
        clientName.contains("Mobile", true) -> Icons.Default.PhoneAndroid
        clientName.contains("Tablet", true) -> Icons.Default.TabletAndroid
        session.deviceName?.contains("Phone", true) == true -> Icons.Default.PhoneAndroid
        session.deviceName?.contains("Tablet", true) == true -> Icons.Default.TabletAndroid
        else -> Icons.Default.Computer
    }

    // Build location info from available fields
    val locationInfo = buildList {
        session.osName?.takeIf { it.isNotBlank() && it != "---" }?.let { add(it) }
        session.countryName?.takeIf { it.isNotBlank() && it != "---" }?.let { add(it) }
        session.ip?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.joinToString(" • ").takeIf { it.isNotEmpty() } ?: "Unknown location"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BG_CARD)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(deviceIcon, contentDescription = null, tint = MUTED, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        clientName,
                        color = TEXT,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        locationInfo,
                        color = MUTED,
                        fontSize = 12.sp
                    )
                }
            }

            if (isCurrent) {
                Text("Current", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            } else {
                onDelete?.let {
                    TextButton(
                        onClick = it,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                    ) {
                        Text("End", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Security Tab ─────────────────────────────────────────────────────────────────
@Composable
private fun SecurityTab(
    uiState: SettingsUiState,
    onToggleMfa: (Boolean) -> Unit,
    onSetupTotp: () -> Unit,
    onVerifyTotp: (String) -> Unit,
    onLoadRecoveryCodes: () -> Unit,
    onDismissRecoveryCodes: () -> Unit,
    onDismissTotpSetup: () -> Unit,
    onPasswordChange: () -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
) {
    var showTotpDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Security",
            color = TEXT,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Manage your account security and authentication",
            color = MUTED,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // MFA Section
        SettingCard(
            title = "Two-Factor Authentication",
            description = "Add an extra layer of security to your account",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (uiState.mfaStatus?.totpEnabled == true) "TOTP is enabled" else "TOTP is disabled",
                        color = TEXT,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = uiState.mfaStatus?.totpEnabled == true,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                onSetupTotp()
                                showTotpDialog = true
                            } else {
                                onToggleMfa(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = BORDER
                        )
                    )
                }
                if (uiState.mfaStatus?.totpEnabled == true) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onLoadRecoveryCodes,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TEXT),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BORDER))
                    ) {
                        Text("View Recovery Codes")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Password Section - Inline Form
        SettingCard(
            title = "Change Password",
            description = "Update your account password. Must be at least 8 characters.",
            modifier = Modifier.fillMaxWidth()
        ) {
            PasswordChangeForm(
                currentPassword = uiState.currentPassword,
                newPassword = uiState.newPassword,
                confirmPassword = uiState.confirmPassword,
                isLoading = uiState.isChangingPassword,
                onCurrentPasswordChange = onCurrentPasswordChange,
                onNewPasswordChange = onNewPasswordChange,
                onConfirmPasswordChange = onConfirmPasswordChange,
                onSubmit = onPasswordChange
            )
        }

        // TOTP Setup Dialog
        if (showTotpDialog && uiState.totpSetupData != null) {
            TotpSetupDialog(
                totpData = uiState.totpSetupData!!,
                onDismiss = {
                    showTotpDialog = false
                    onDismissTotpSetup()
                },
                onVerify = { code ->
                    onVerifyTotp(code)
                    showTotpDialog = false
                }
            )
        }

        // Recovery Codes Dialog
        if (uiState.showRecoveryCodes) {
            RecoveryCodesDialog(
                codes = uiState.recoveryCodes,
                onDismiss = onDismissRecoveryCodes
            )
        }

    }
}

// ── Dialogs ─────────────────────────────────────────────────────────────────────
@Composable
private fun TotpSetupDialog(
    totpData: to.kuudere.anisuge.data.models.TotpSetupData,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BG_CARD,
        title = { Text("Setup Authenticator", color = TEXT) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Scan the QR code with your authenticator app", color = MUTED, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = totpData.qrCode,
                    contentDescription = "TOTP QR Code",
                    modifier = Modifier.size(200.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Or enter the secret manually:", color = MUTED, fontSize = 13.sp)
                Text(
                    totpData.secret,
                    color = TEXT,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Verification Code") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TEXT,
                        unfocusedTextColor = TEXT,
                        focusedLabelColor = MUTED,
                        unfocusedLabelColor = MUTED,
                        focusedBorderColor = TEXT,
                        unfocusedBorderColor = BORDER
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(code) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MUTED)
            }
        }
    )
}

@Composable
private fun RecoveryCodesDialog(
    codes: List<String>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BG_CARD,
        title = { Text("Recovery Codes", color = TEXT) },
        text = {
            Column {
                Text("Save these codes in a secure location. They can be used to recover your account if you lose access to your authenticator.", color = MUTED, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BG)
                        .padding(16.dp)
                ) {
                    Column {
                        codes.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                row.forEach { code ->
                                    Text(
                                        code,
                                        color = TEXT,
                                        fontSize = 14.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun PasswordChangeForm(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    isLoading: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    val isValid = newPassword == confirmPassword && newPassword.length >= 8

    Column {
        OutlinedTextField(
            value = currentPassword,
            onValueChange = onCurrentPasswordChange,
            label = { Text("Current Password") },
            singleLine = true,
            visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showCurrent = !showCurrent }) {
                    Icon(
                        if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MUTED
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TEXT,
                unfocusedTextColor = TEXT,
                focusedLabelColor = MUTED,
                unfocusedLabelColor = MUTED,
                focusedBorderColor = TEXT,
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
            visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showNew = !showNew }) {
                    Icon(
                        if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MUTED
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TEXT,
                unfocusedTextColor = TEXT,
                focusedLabelColor = MUTED,
                unfocusedLabelColor = MUTED,
                focusedBorderColor = TEXT,
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
            isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(
                        if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MUTED
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TEXT,
                unfocusedTextColor = TEXT,
                focusedLabelColor = MUTED,
                unfocusedLabelColor = MUTED,
                focusedBorderColor = TEXT,
                unfocusedBorderColor = BORDER
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = !isLoading && isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Changing...")
            } else {
                Text("Change Password")
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────────
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
