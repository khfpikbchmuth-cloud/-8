package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ai.MediaKind
import com.example.ai.PreparedAttachment
import com.example.ui.MediaLabUiState
import com.example.ui.SamiViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLabScreen(
    viewModel: SamiViewModel,
    onBack: () -> Unit,
    onContinueInChat: () -> Unit
) {
    BackHandler(onBack = onBack)

    val labState by viewModel.mediaLabState.collectAsStateWithLifecycle()
    val isPreparingMedia by viewModel.isPreparingMedia.collectAsStateWithLifecycle()
    val isPersian by viewModel.isPersianUi.collectAsStateWithLifecycle()
    var focusNote by rememberSaveable { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeMediaInLab(uri, MediaKind.IMAGE, focusNote)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeMediaInLab(uri, MediaKind.VIDEO, focusNote)
        }
    }

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
                            .testTag("media_lab_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (isPersian) "بازگشت" else "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = if (isPersian) "استودیو تصویر و ویدیو" else "Photo & Video Studio",
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
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isPersian) "تحلیل هوشمند ساختاریافته" else "Structured Media Intelligence",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isPersian) {
                                    "یک عکس یا ویدیو انتخاب کنید تا سامی کینگ به صورت دقیق خلاصه، نکات کلیدی، متن‌های داخل تصویر/ویدیو و سوالات پیشنهادی را استخراج کند."
                                } else {
                                    "Select any photo or video for a structured breakdown including summary, key moments, OCR/audio notes, and smart follow-ups."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = focusNote,
                                onValueChange = { focusNote = it },
                                label = {
                                    Text(
                                        if (isPersian) "تمرکز دلخواه (اختیاری)" else "Custom focus (optional)"
                                    )
                                },
                                placeholder = {
                                    Text(
                                        if (isPersian) "مثلاً: متن داخل تصویر را ترجمه کن" else "e.g., Extract and translate visible text"
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("media_lab_focus_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isPreparingMedia && labState !is MediaLabUiState.Analyzing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("media_lab_pick_photo"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isPersian) "انتخاب عکس" else "Pick Photo")
                                }
                                OutlinedButton(
                                    onClick = {
                                        videoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    enabled = !isPreparingMedia && labState !is MediaLabUiState.Analyzing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("media_lab_pick_video"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isPersian) "انتخاب ویدیو" else "Pick Video")
                                }
                            }
                        }
                    }
                }

                when (val state = labState) {
                    is MediaLabUiState.Idle -> {
                        if (isPreparingMedia) {
                            item {
                                LoadingMediaCard(
                                    text = if (isPersian) "در حال آماده‌سازی فایل رسانه..." else "Preparing media..."
                                )
                            }
                        }
                    }

                    is MediaLabUiState.Analyzing -> {
                        item {
                            LabAttachmentPreview(state.attachment, viewModel)
                        }
                        item {
                            LoadingMediaCard(
                                text = if (isPersian) "سامی کینگ در حال تحلیل دقیق رسانه است..." else "Sami King is analyzing media..."
                            )
                        }
                    }

                    is MediaLabUiState.Error -> {
                        if (state.attachment != null) {
                            item { LabAttachmentPreview(state.attachment, viewModel) }
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    OutlinedButton(onClick = { viewModel.resetMediaLab() }) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isPersian) "تلاش مجدد" else "Reset")
                                    }
                                }
                            }
                        }
                    }

                    is MediaLabUiState.Success -> {
                        item { LabAttachmentPreview(state.attachment, viewModel) }
                        item {
                            val report = state.report
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = report.title,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = report.summary,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (report.keyHighlights.isNotEmpty()) {
                                        Text(
                                            text = if (isPersian) "نکات کلیدی" else "Key Highlights",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        report.keyHighlights.forEach { bullet ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("•", color = MaterialTheme.colorScheme.primary)
                                                Text(
                                                    text = bullet,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    if (report.extractedTextOrAudio.isNotBlank()) {
                                        Text(
                                            text = if (isPersian) "متن یا جزئیات گفتاری استخراج‌شده" else "Extracted Text / Audio",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = report.extractedTextOrAudio,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }

                                    if (report.suggestedQuestions.isNotEmpty()) {
                                        Text(
                                            text = if (isPersian) "ادامه گفتگو در چت:" else "Ask a follow-up in chat:",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        report.suggestedQuestions.forEach { q ->
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        viewModel.continueLabReportInChat(state.attachment, q)
                                                        onContinueInChat()
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = q,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
            }
        }
    }
}

@Composable
private fun LoadingMediaCard(text: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LabAttachmentPreview(
    attachment: PreparedAttachment,
    viewModel: SamiViewModel
) {
    if (attachment.kind == MediaKind.IMAGE) {
        AsyncImage(
            model = File(attachment.localPreviewPath),
            contentDescription = "Selected lab photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
        )
    } else if (attachment.kind == MediaKind.VIDEO) {
        val thumb by produceState<Bitmap?>(initialValue = null, attachment.localPreviewPath) {
            value = viewModel.mediaProcessor.extractThumbnailForVideo(attachment.localPreviewPath)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (thumb != null) {
                Image(
                    bitmap = thumb!!.asImageBitmap(),
                    contentDescription = "Selected lab video",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}
