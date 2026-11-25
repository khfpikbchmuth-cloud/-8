package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.SamiModelOption
import com.example.config.ResponseStyle
import com.example.config.SamiFeatureModule
import com.example.ui.SamiViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerManagementScreen(
    viewModel: SamiViewModel,
    onBack: () -> Unit,
    onLockedExit: () -> Unit
) {
    BackHandler(onBack = onBack)

    val config by viewModel.configState.collectAsStateWithLifecycle()
    val isPersian = config.isPersianUi
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var newPinInput by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                            .testTag("owner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (isPersian) "بازگشت" else "Back"
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isPersian) "پنل مدیریت مالک" else "Owner Management Console",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
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
                // Owner Status & Quick Lock for Sharing
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isPersian) "حالت مالک فعال است" else "Owner Mode Active",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isPersian) {
                                    "مدیریت مدل، دستورالعمل هوش مصنوعی و ماژول‌ها فقط برای شما باز است. اگر گوشی را به کسی می‌دهید یا نسخه عمومی می‌سازید، پنل را قفل کنید تا فقط امکانات عادی دیده شود."
                                } else {
                                    "AI engine, behavior, and module management are unlocked for you. Lock Owner Mode anytime to switch to standard user view."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                            Button(
                                onClick = {
                                    viewModel.lockOwnerMode()
                                    onLockedExit()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("owner_lock_button")
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isPersian) "قفل کردن مدیریت (نمایش حالت کاربر عادی)"
                                    else "Lock Management (Guest View)"
                                )
                            }
                        }
                    }
                }

                // AI Engine Selection
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isPersian) "انتخاب موتور هوش مصنوعی" else "AI Engine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            SamiModelOption.entries.forEach { option ->
                                val active = option == config.selectedModel
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (active) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.selectModel(option) }
                                        .testTag("owner_model_${option.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isPersian) option.titleFa else option.titleEn,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = if (isPersian) option.descriptionFa else option.descriptionEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (active) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Behavior & Custom System Instructions
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isPersian) "شخصی‌سازی و رفتار سامی کینگ" else "AI Behavior & Persona",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            OutlinedTextField(
                                value = config.customInstruction,
                                onValueChange = { viewModel.setCustomInstruction(it) },
                                label = {
                                    Text(
                                        if (isPersian) "دستورالعمل اختصاصی مالک (System Prompt)"
                                        else "Custom Owner System Instruction"
                                    )
                                },
                                placeholder = {
                                    Text(
                                        if (isPersian) "مثلاً: همیشه با لحن حرفه‌ای جواب بده یا همیشه کد کامل کاتلین بنویس"
                                        else "e.g., Always reply concisely with practical examples"
                                    )
                                },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("owner_custom_instruction_input")
                            )

                            Text(
                                text = if (isPersian) "طول و سبک پاسخ" else "Response Style",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ResponseStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = config.responseStyle == style,
                                        onClick = { viewModel.setResponseStyle(style) },
                                        label = { Text(if (isPersian) style.titleFa else style.titleEn) }
                                    )
                                }
                            }

                            Column {
                                val tempPct = (config.creativityTemperature * 100).roundToInt()
                                Text(
                                    text = if (isPersian) {
                                        "میزان خلاقیت پاسخ‌ها: $tempPct%"
                                    } else {
                                        "Creativity (Temperature): ${(config.creativityTemperature * 10).roundToInt() / 10f}"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = config.creativityTemperature,
                                    onValueChange = { viewModel.setCreativityTemperature(it) },
                                    valueRange = 0.1f..1.3f,
                                    modifier = Modifier.testTag("owner_temperature_slider")
                                )
                            }
                        }
                    }
                }

                // Feature Modules Manager
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isPersian) "مدیریت ماژول‌ها و قابلیت‌ها" else "Feature Modules Manager",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            SamiFeatureModule.entries.forEach { module ->
                                val enabled = module in config.enabledModules
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = if (isPersian) module.titleFa else module.titleEn,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = if (isPersian) module.descriptionFa else module.descriptionEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { viewModel.setModuleEnabled(module, it) },
                                        modifier = Modifier.testTag("owner_module_switch_${module.id}")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = if (isPersian) "خواندن خودکار پاسخ‌های هوش مصنوعی" else "Auto Read-Aloud Replies",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = if (isPersian) "پخش صوتی خودکار پس از دریافت پاسخ" else "Speak AI responses automatically",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = config.autoSpeakReplies,
                                    onCheckedChange = { viewModel.setAutoSpeakReplies(it) },
                                    modifier = Modifier.testTag("owner_autospeak_switch")
                                )
                            }
                        }
                    }
                }

                // Change Owner PIN & Distribution Info
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isPersian) "امنیت مالک و ارسال به دیگران" else "Owner Security & Distribution",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { newPinInput = it },
                                label = {
                                    Text(if (isPersian) "تغییر رمز مالک (حداقل ۴ کاراکتر)" else "New Owner PIN (min 4 chars)")
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("owner_new_pin_input")
                            )
                            Button(
                                onClick = {
                                    val saved = viewModel.updateOwnerPin(newPinInput)
                                    scope.launch {
                                        if (saved) {
                                            newPinInput = ""
                                            snackbarHostState.showSnackbar(
                                                if (isPersian) "رمز جدید مالک ذخیره شد." else "Owner PIN updated."
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                if (isPersian) "رمز باید حداقل ۴ کاراکتر باشد." else "PIN must be at least 4 characters."
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isPersian) "ذخیره رمز جدید مالک" else "Save New Owner PIN")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.resetSettingsToDefaults()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isPersian) "تنظیمات به حالت پیش‌فرض بازگشت." else "Settings reset to defaults."
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isPersian) "بازگردانی تنظیمات به پیش‌فرض" else "Reset Settings to Defaults")
                            }
                        }
                    }
                }
            }
        }
    }
}
