package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.config.SamiOwnerBuildConfig
import com.example.ui.SamiViewModel
import com.example.ui.theme.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SamiViewModel,
    onBack: () -> Unit,
    onOpenOwnerPanel: () -> Unit
) {
    BackHandler(onBack = onBack)

    val config by viewModel.configState.collectAsStateWithLifecycle()
    val themePreference = config.themePreference
    val isPersian = config.isPersianUi

    var showClearDialog by remember { mutableStateOf(false) }
    var footerTapCount by remember { mutableIntStateOf(0) }
    var showOwnerPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (isPersian) "بازگشت" else "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = if (isPersian) "تنظیمات سامی کینگ" else "Sami King Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Free tier info
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isPersian) "سرویس کاملاً رایگان" else "100% Free Access",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (isPersian) {
                                    "تمام قابلیت‌های سامی کینگ بدون نیاز به خرید اشتراک، پرداخت هزینه یا وارد کردن کلید API کار می‌کنند."
                                } else {
                                    "All Sami King AI capabilities work with zero subscription fees and no API key setup required."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Visible ONLY if Owner mode is unlocked on this device
                if (SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT && config.isOwnerUnlocked) {
                    item {
                        Button(
                            onClick = onOpenOwnerPanel,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_owner_panel_button")
                        ) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isPersian) "ورود به پنل مدیریت مالک" else "Open Owner Management Console"
                            )
                        }
                    }
                }

                // Appearance & Language (Standard user preferences)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (isPersian) "ظاهر و زبان" else "Appearance & Language",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeOptionChip(
                                    label = if (isPersian) "تیره" else "Dark",
                                    selected = themePreference == ThemePreference.DARK,
                                    onClick = { viewModel.setThemePreference(ThemePreference.DARK) },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    label = if (isPersian) "روشن" else "Light",
                                    selected = themePreference == ThemePreference.LIGHT,
                                    onClick = { viewModel.setThemePreference(ThemePreference.LIGHT) },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    label = if (isPersian) "سیستم" else "System",
                                    selected = themePreference == ThemePreference.SYSTEM,
                                    onClick = { viewModel.setThemePreference(ThemePreference.SYSTEM) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (isPersian) "رابط کاربری فارسی (راست‌به‌چپ)" else "Persian RTL Interface",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = if (isPersian) "برای انگلیسی خاموش کنید" else "Turn on for Persian RTL",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isPersian,
                                    onCheckedChange = { viewModel.setPersianUi(it) },
                                    modifier = Modifier.testTag("language_switch")
                                )
                            }
                        }
                    }
                }

                // Clear history
                item {
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_history_button")
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPersian) "پاک کردن تاریخچه گفتگوهای من" else "Clear My Chat History")
                    }
                }

                // Discreet brand footer (5 taps triggers Owner PIN dialog only if ENABLE_OWNER_MANAGEMENT == true)
                item {
                    Text(
                        text = if (isPersian) "سامی کینگ • نسخه ۱.۰" else "Sami King • Version 1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT) {
                                    footerTapCount++
                                    if (footerTapCount >= 5) {
                                        footerTapCount = 0
                                        if (config.isOwnerUnlocked) {
                                            onOpenOwnerPanel()
                                        } else {
                                            pinInput = ""
                                            pinError = false
                                            showOwnerPinDialog = true
                                        }
                                    }
                                }
                            }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(if (isPersian) "حذف تمام گفتگوها؟" else "Clear all chats?") },
            text = {
                Text(
                    if (isPersian) "تمام پیام‌ها و گفتگوهای ذخیره‌شده روی گوشی شما حذف خواهند شد."
                    else "All locally saved conversations and messages will be removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = if (isPersian) "حذف کن" else "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        )
    }

    if (showOwnerPinDialog && SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT) {
        AlertDialog(
            onDismissRequest = { showOwnerPinDialog = false },
            title = { Text(if (isPersian) "تأیید هویت مالک" else "Owner Verification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text(if (isPersian) "رمز اختصاصی مالک" else "Owner PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_pin_input")
                    )
                    if (pinError) {
                        Text(
                            text = if (isPersian) "رمز اشتباه است." else "Incorrect PIN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (viewModel.unlockOwnerMode(pinInput)) {
                            showOwnerPinDialog = false
                            onOpenOwnerPanel()
                        } else {
                            pinError = true
                        }
                    },
                    modifier = Modifier.testTag("owner_pin_confirm")
                ) {
                    Text(if (isPersian) "ورود" else "Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOwnerPinDialog = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified
            )
        }
    }
}
