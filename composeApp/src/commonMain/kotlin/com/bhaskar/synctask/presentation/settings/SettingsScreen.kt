package com.bhaskar.synctask.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bhaskar.synctask.domain.model.ActiveSubscriptionInfo
import kotlinx.coroutines.launch
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import com.bhaskar.synctask.platform.openUrl
import com.bhaskar.synctask.presentation.settings.components.SettingsEvent
import com.bhaskar.synctask.presentation.settings.components.SettingsState
import com.bhaskar.synctask.domain.model.ThemeMode
import com.bhaskar.synctask.presentation.create.ui_components.SingleSelectionDialog
import com.bhaskar.synctask.presentation.create.ui_components.CompactSelectableRow
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.mutableStateOf
import com.bhaskar.synctask.presentation.settings.components.ManageTagsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // CREATE permission controller locally
    val factory = rememberPermissionsControllerFactory()
    val permissionsController = remember(factory) { factory.createPermissionsController() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Bind permission controller to lifecycle
    BindEffect(permissionsController)

    // Re-check permission when screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Check permissions status locally
                scope.launch {
                    try {
                        val isGranted = permissionsController.isPermissionGranted(dev.icerock.moko.permissions.Permission.REMOTE_NOTIFICATION)
                        onEvent(SettingsEvent.OnPushToggled(isGranted))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = state.themeMode,
            onModeSelected = { onEvent(SettingsEvent.OnThemeChanged(it)) },
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (state.showTagsDialog) {
        ManageTagsDialog(
            tags = state.tags,
            newTagName = state.newTagName,
            newTagColor = state.newTagColor,
            error = state.tagsDialogError,
            onNameChange = { onEvent(SettingsEvent.OnNewTagNameChanged(it)) },
            onColorChange = { onEvent(SettingsEvent.OnNewTagColorChanged(it)) },
            onCreateTag = { onEvent(SettingsEvent.OnCreateTag) },
            onDeleteTag = { onEvent(SettingsEvent.OnDeleteTag(it)) },
            onDismissRequest = { onEvent(SettingsEvent.OnHideTagsDialog) }
        )
    }

    if (state.showPremiumDialog) {
        if (state.isMaxLimitReached) {
            com.bhaskar.synctask.presentation.components.MaxLimitReachedDialog(
                message = state.premiumDialogMessage,
                onDismiss = { onEvent(SettingsEvent.OnDismissPremiumDialog) }
            )
        } else {
            com.bhaskar.synctask.presentation.components.PremiumLimitDialog(
                message = state.premiumDialogMessage,
                onDismiss = { onEvent(SettingsEvent.OnDismissPremiumDialog) },
                onUpgrade = {
                    onEvent(SettingsEvent.OnDismissPremiumDialog)
                    onNavigateToPaywall()
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile
            ProfileSection(
                name = state.userName, 
                email = state.userEmail, 
                imageBitmap = state.userProfileImage,
                isPremium = state.isPremium,
                onLogoutClick = { onEvent(SettingsEvent.OnLogout) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Subscription Section
            SubscriptionSection(
                isPremium = state.isPremium,
                activeSubscriptionInfo = state.activeSubscriptionInfo,
                managementUrl = state.managementUrl,
                isRestoring = state.isRestoring,
                onUpgradeClick = onNavigateToPaywall,
                onManageClick = { url ->
                    openUrl(url)
                },
                onRestoreClick = {
                    onEvent(SettingsEvent.OnRestorePurchases)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier
                    .padding(
                        start = 8.dp,
                        bottom = 8.dp
                    )
            )
            CompactSelectableRow(
                title = "Theme",
                selectedOption = when(state.themeMode) {
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.SYSTEM -> "System Default"
                },
                onClick = { showThemeDialog = true },
                icon = Icons.Default.Palette,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            Text(
                text = "CONTENT",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier
                    .padding(
                        start = 8.dp,
                        bottom = 8.dp
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onEvent(SettingsEvent.OnShowTagsDialog) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Manage Tags",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Manage Tags",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications
            Text("NOTIFICATIONS", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            
            // Simplified Notification Item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFCC80).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Push Notifications", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Switch(
                    checked = state.isPushEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // Request permission
                            scope.launch {
                                try {
                                    permissionsController.providePermission(dev.icerock.moko.permissions.Permission.REMOTE_NOTIFICATION)
                                    onEvent(SettingsEvent.OnPushToggled(true))
                                } catch (e: Exception) {
                                    // Denied or error
                                    onEvent(SettingsEvent.OnPushToggled(false))
                                    if (e is dev.icerock.moko.permissions.DeniedAlwaysException) {
                                        permissionsController.openAppSettings()
                                    }
                                }
                            }
                        } else {
                            // Just disable in state
                            onEvent(SettingsEvent.OnPushToggled(false))
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Log Out (Redundant now, but acceptable to leave or remove. The user request "replace the edit button with logout" is satisfied by ProfileSection update. I'll remove the big bottom button to clean up)
            /*
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .clickable { viewModel.onEvent(SettingsEvent.OnLogout) }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Log Out", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
            */
            Spacer(modifier = Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Version 1.0.2", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}


@Composable
fun ProfileSection(
    name: String, 
    email: String, 
    imageBitmap: ImageBitmap?, 
    isPremium: Boolean,
    onLogoutClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = imageBitmap,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(name.ifEmpty { "User" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(email.ifEmpty { "No email" }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (isPremium) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (isPremium) "Premium" else "Free Account",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPremium) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Logout Button
        Box(
            modifier = Modifier
                .clickable(onClick = onLogoutClick)
                .padding(8.dp)
        ) {
            Text(
                "Log Out", 
                color = Color(0xFFEF4444), 
                fontWeight = FontWeight.SemiBold, 
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun SubscriptionSection(
    isPremium: Boolean,
    activeSubscriptionInfo: ActiveSubscriptionInfo?,
    managementUrl: String?,
    isRestoring: Boolean,
    onUpgradeClick: () -> Unit,
    onManageClick: (String) -> Unit,
    onRestoreClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPremium) MaterialTheme.colorScheme.tertiary else Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Star, 
                        contentDescription = null, 
                        tint = if (isPremium) Color.White else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isPremium) "Premium Active" else "Free Plan",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isPremium && activeSubscriptionInfo != null) {
                        val renewText = if (activeSubscriptionInfo.willRenew) "Renews" else "Expires"
                        Text(
                            "$renewText ${activeSubscriptionInfo.expirationDateFormatted ?: "soon"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activeSubscriptionInfo.willRenew) Color.Gray else Color(0xFFEF4444)
                        )
                    } else {
                        Text(
                            "Upgrade for unlimited features",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isPremium && managementUrl != null) {
                // Premium user - show manage subscription button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onManageClick(managementUrl) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Manage Subscription",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Free user - show upgrade and restore buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Upgrade button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiary)
                            .clickable { onUpgradeClick() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Upgrade to Premium",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Restore purchases button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !isRestoring) { onRestoreClick() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isRestoring) "Restoring..." else "Restore Purchases",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    SingleSelectionDialog(
        title = "App Theme",
        options = ThemeMode.entries,
        selectedOption = currentMode,
        onOptionSelected = {
            onModeSelected(it)
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
        labelProvider = {
            when(it) {
                ThemeMode.LIGHT -> "Light"
                ThemeMode.DARK -> "Dark"
                ThemeMode.SYSTEM -> "System Default"
            }
        }
    )
}


