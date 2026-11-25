package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ai.MediaKind
import com.example.ai.SamiModelOption
import com.example.config.SamiFeatureModule
import com.example.config.SamiOwnerBuildConfig
import com.example.ui.InferenceUiState
import com.example.ui.SamiViewModel
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ComposerDock
import com.example.ui.components.StreamingAiBubble
import com.example.ui.components.WelcomeHeroSection
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: SamiViewModel,
    onOpenReelsStudio: () -> Unit,
    onOpenMediaLab: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOwnerPanel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val config by viewModel.configState.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val inferenceState by viewModel.inferenceState.collectAsStateWithLifecycle()
    val pendingAttachment by viewModel.pendingAttachment.collectAsStateWithLifecycle()
    val isPreparingMedia by viewModel.isPreparingMedia.collectAsStateWithLifecycle()
    val speakingMessageId by viewModel.speakingMessageId.collectAsStateWithLifecycle()

    val selectedModel = config.selectedModel
    val isPersian = config.isPersianUi
    val showReels = SamiFeatureModule.REELS_STUDIO in config.enabledModules
    val showMediaLab = SamiFeatureModule.MEDIA_LAB in config.enabledModules
    val showVoice = SamiFeatureModule.VOICE_TOOLS in config.enabledModules
    val ownerActive = SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT && config.isOwnerUnlocked

    var promptText by rememberSaveable { mutableStateOf("") }
    var showModelPickerSheet by remember { mutableStateOf(false) }
    var showOwnerPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachMedia(uri, MediaKind.IMAGE)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachMedia(uri, MediaKind.VIDEO)
        }
    }

    val cameraPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.attachMedia(uri, MediaKind.IMAGE)
        }
    }

    val launchCameraCapture = {
        val uri = createTempCameraUri(context)
        pendingCameraUri = uri
        cameraPictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (isPersian) "دسترسی دوربین رد شد. می‌توانید از گالری عکس انتخاب کنید."
                    else "Camera permission denied. You can still pick photos from gallery."
                )
            }
        }
    }

    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                promptText = if (promptText.isBlank()) spoken else "$promptText $spoken"
            }
        }
    }

    LaunchedEffect(messages.size, inferenceState) {
        if (messages.isNotEmpty()) {
            val targetIndex = messages.lastIndex + if (inferenceState is InferenceUiState.Loading || inferenceState is InferenceUiState.Streaming) 1 else 0
            listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 310.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_sami_icon_fg),
                            contentDescription = "Sami King Emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(
                                text = if (isPersian) "سامی کینگ" else "Sami King",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isPersian) "رایگان • بدون اشتراک" else "100% Free AI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.startNewChat()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_new_chat_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isPersian) "گفتگوی جدید" else "New Chat")
                    }

                    if (showReels) {
                        NavigationDrawerItem(
                            label = { Text(if (isPersian) "ویدیوساز سینمایی ریلز" else "Cinematic Reels Maker") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.MovieCreation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenReelsStudio()
                            },
                            modifier = Modifier.testTag("drawer_reels_item")
                        )
                    }

                    if (showMediaLab) {
                        NavigationDrawerItem(
                            label = { Text(if (isPersian) "استودیو تصویر و ویدیو" else "Media Studio") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenMediaLab()
                            },
                            modifier = Modifier.testTag("drawer_media_lab_item")
                        )
                    }

                    // Owner panel item ONLY appears when Owner Mode is explicitly unlocked on this device
                    if (ownerActive) {
                        NavigationDrawerItem(
                            label = { Text(if (isPersian) "پنل مدیریت مالک" else "Owner Console") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenOwnerPanel()
                            },
                            modifier = Modifier.testTag("drawer_owner_panel_item")
                        )
                    }

                    NavigationDrawerItem(
                        label = { Text(if (isPersian) "تنظیمات" else "Settings") },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenSettings()
                        },
                        modifier = Modifier.testTag("drawer_settings_item")
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Text(
                        text = if (isPersian) "تاریخچه گفتگوها" else "Recent Conversations",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (conversations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPersian) "هنوز گفتگویی ذخیره نشده است" else "No saved conversations yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(conversations, key = { _, item -> item.id }) { _, conv ->
                                val isSelected = conv.id == activeConversationId
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.selectConversation(conv)
                                            scope.launch { drawerState.close() }
                                        }
                                        .testTag("conversation_item_${conv.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = conv.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteConversation(conv.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = if (isPersian) "حذف گفتگو" else "Delete chat",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = if (isPersian) "منو و تاریخچه" else "Open drawer"
                            )
                        }
                    },
                    title = {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .combinedClickable(
                                    onClick = {
                                        if (ownerActive) {
                                            showModelPickerSheet = true
                                        }
                                    },
                                    onLongClick = {
                                        if (SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT) {
                                            if (config.isOwnerUnlocked) {
                                                onOpenOwnerPanel()
                                            } else {
                                                pinInput = ""
                                                pinError = false
                                                showOwnerPinDialog = true
                                            }
                                        }
                                    }
                                )
                                .testTag("model_selector_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isPersian) "سامی کینگ" else "Sami King",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (ownerActive) {
                                    Text(
                                        text = if (selectedModel == SamiModelOption.PRO_BALANCED) "3.8" else "3.5",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ExpandMore,
                                        contentDescription = if (isPersian) "تغییر مدل" else "Switch model",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (showReels) {
                            IconButton(
                                onClick = onOpenReelsStudio,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("top_reels_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MovieCreation,
                                    contentDescription = if (isPersian) "ساخت ویدیو ریلز" else "Create Reel",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.startNewChat() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("top_new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = if (isPersian) "گفتگوی جدید" else "New chat"
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = if (isPersian) "تنظیمات" else "Settings"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ComposerDock(
                        promptText = promptText,
                        onPromptChange = { promptText = it },
                        pendingAttachment = pendingAttachment,
                        isPreparingMedia = isPreparingMedia,
                        isStreaming = inferenceState is InferenceUiState.Loading || inferenceState is InferenceUiState.Streaming,
                        isPersian = isPersian,
                        showVoiceTools = showVoice,
                        mediaProcessor = viewModel.mediaProcessor,
                        onPickImage = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onPickVideo = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onCaptureCamera = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                launchCameraCapture()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onVoiceInput = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE,
                                    if (isPersian) "fa-IR" else "en-US"
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    if (isPersian) "صحبت کنید..." else "Speak now..."
                                )
                            }
                            try {
                                voiceInputLauncher.launch(intent)
                            } catch (_: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (isPersian) "تشخیص گفتار روی این دستگاه در دسترس نیست."
                                        else "Speech recognition is not available on this device."
                                    )
                                }
                            }
                        },
                        onRemoveAttachment = { viewModel.clearPendingAttachment() },
                        onSend = {
                            val textToSend = promptText
                            promptText = ""
                            viewModel.sendMessage(textToSend)
                        },
                        onStop = { viewModel.stopGeneration() },
                        modifier = Modifier.widthIn(max = 780.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 780.dp)
                ) {
                    val errorState = inferenceState as? InferenceUiState.Error
                    if (errorState != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissError() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (messages.isEmpty() && inferenceState !is InferenceUiState.Loading && inferenceState !is InferenceUiState.Streaming) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            item {
                                WelcomeHeroSection(
                                    isPersian = isPersian,
                                    showReelsModule = showReels,
                                    showMediaLabModule = showMediaLab,
                                    onPickImage = {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onPickVideo = {
                                        videoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    onOpenReelsStudio = onOpenReelsStudio,
                                    onQuickPrompt = { starter ->
                                        viewModel.sendMessage(starter)
                                    },
                                    onOpenMediaLab = onOpenMediaLab
                                )
                            }
                        }
                    } else {
                        val lastModelIndex = messages.indexOfLast { it.role == "model" }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
                        ) {
                            itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                                ChatMessageBubble(
                                    message = msg,
                                    isLastModelMessage = index == lastModelIndex &&
                                        inferenceState !is InferenceUiState.Loading &&
                                        inferenceState !is InferenceUiState.Streaming,
                                    isSpeaking = speakingMessageId == msg.id,
                                    isPersian = isPersian,
                                    showVoiceTools = showVoice,
                                    mediaProcessor = viewModel.mediaProcessor,
                                    onToggleSpeak = { viewModel.toggleSpeakMessage(msg.id, msg.text) },
                                    onRegenerate = { viewModel.regenerateLastResponse() }
                                )
                            }

                            when (val state = inferenceState) {
                                is InferenceUiState.Loading -> {
                                    item(key = "streaming_loading") {
                                        StreamingAiBubble(
                                            streamingText = "",
                                            isLoadingOnly = true,
                                            isPersian = isPersian
                                        )
                                    }
                                }
                                is InferenceUiState.Streaming -> {
                                    item(key = "streaming_active") {
                                        StreamingAiBubble(
                                            streamingText = state.text,
                                            isLoadingOnly = false,
                                            isPersian = isPersian
                                        )
                                    }
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    if (showModelPickerSheet && ownerActive) {
        ModalBottomSheet(
            onDismissRequest = { showModelPickerSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isPersian) "انتخاب موتور هوش مصنوعی (مالک)" else "Choose AI Engine (Owner)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SamiModelOption.entries.forEach { option ->
                    val active = option == selectedModel
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                viewModel.selectModel(option)
                                showModelPickerSheet = false
                            }
                            .testTag("sheet_model_${option.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPersian) option.titleFa else option.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
                        modifier = Modifier.fillMaxWidth()
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
                    }
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

private fun createTempCameraUri(context: Context): Uri {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val photoFile = File(cameraDir, "cam_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}
