package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.reels.ReelColorGrade
import com.example.reels.ReelMotionStyle
import com.example.ui.SamiViewModel
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsStudioScreen(
    viewModel: SamiViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val state by viewModel.reelsState.collectAsStateWithLifecycle()
    val isPreparing by viewModel.isPreparingMedia.collectAsStateWithLifecycle()
    val isPersian by viewModel.isPersianUi.collectAsStateWithLifecycle()

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addReelsPhotos(uris)
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
                            .testTag("reels_back_button")
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
                            imageVector = Icons.Rounded.MovieCreation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isPersian) "ویدیوساز سینمایی ریلز" else "Cinematic Reels Maker",
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
                // Step 1: Pick Photos
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                                )
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isPersian) "۱. تصویر خود را بدهید" else "1. Select Photo(s)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isPersian) {
                                    "یک تا پنج تصویر انتخاب کنید تا سامی کینگ با حرکت دوربین سینمایی، اصلاح رنگ و زیرنویس ریلز یک ویدیوی ۹:۱۶ واقعی (MP4) برایتان بسازد."
                                } else {
                                    "Pick 1 to 5 photos. Sami King AI directs camera motion, color grading, and kinetic captions into a real 9:16 MP4 Reel."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (state.selectedPhotos.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    itemsIndexed(state.selectedPhotos) { index, photo ->
                                        Box(
                                            modifier = Modifier
                                                .size(width = 86.dp, height = 124.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                    RoundedCornerShape(14.dp)
                                                )
                                        ) {
                                            AsyncImage(
                                                model = File(photo.localPreviewPath),
                                                contentDescription = "Selected Reel photo ${index + 1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            IconButton(
                                                onClick = { viewModel.removeReelsPhoto(index) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.68f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Remove photo",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    multiPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isPreparing && !state.isRendering && !state.isDirectingWithAi,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reels_pick_photos_button")
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.selectedPhotos.isEmpty()) {
                                        if (isPersian) "انتخاب تصویر برای ریلز" else "Choose Photo(s)"
                                    } else {
                                        if (isPersian) "افزودن تصویر بیشتر (${state.selectedPhotos.size}/۵)" else "Add More Photos (${state.selectedPhotos.size}/5)"
                                    }
                                )
                            }

                            OutlinedTextField(
                                value = state.conceptPrompt,
                                onValueChange = { viewModel.updateReelsConcept(it) },
                                label = {
                                    Text(
                                        if (isPersian) "موضوع یا حس‌وحال ریلز (اختیاری)" else "Reel topic / vibe (optional)"
                                    )
                                },
                                placeholder = {
                                    Text(
                                        if (isPersian) "مثلاً: لوکس، انگیزشی، معرفی محصول یا سفر" else "e.g., Luxury, travel story, product showcase"
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reels_concept_input")
                            )
                        }
                    }
                }

                // Step 2: Cinematic Controls
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = if (isPersian) "۲. تنظیمات سینمایی" else "2. Cinematic Direction",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Text(
                                text = if (isPersian) "حرکت دوربین" else "Camera Motion",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReelMotionStyle.entries.forEach { motion ->
                                    FilterChip(
                                        selected = state.motionStyle == motion,
                                        onClick = { viewModel.selectReelsMotion(motion) },
                                        label = { Text(if (isPersian) motion.titleFa else motion.titleEn) }
                                    )
                                }
                            }

                            Text(
                                text = if (isPersian) "اصلاح رنگ سینمایی (LUT)" else "Color Grade",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReelColorGrade.entries.forEach { grade ->
                                    FilterChip(
                                        selected = state.colorGrade == grade,
                                        onClick = { viewModel.selectReelsColorGrade(grade) },
                                        label = { Text(if (isPersian) grade.titleFa else grade.titleEn) }
                                    )
                                }
                            }

                            Text(
                                text = if (isPersian) "مدت زمان ویدیو" else "Duration",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(6, 9, 12).forEach { sec ->
                                    FilterChip(
                                        selected = state.durationSeconds == sec,
                                        onClick = { viewModel.selectReelsDuration(sec) },
                                        label = { Text(if (isPersian) "${sec} ثانیه" else "${sec}s") }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isPersian) "نوارهای سینمایی (Letterbox)" else "Cinema Letterbox Bars",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = state.includeCinemaBars,
                                    onCheckedChange = { viewModel.toggleReelsCinemaBars(it) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isPersian) "نورپردازی و درخشش لنز" else "Cinematic Light Leak",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = state.includeLightLeak,
                                    onCheckedChange = { viewModel.toggleReelsLightLeak(it) }
                                )
                            }

                            OutlinedTextField(
                                value = state.hookTitle,
                                onValueChange = {
                                    viewModel.updateReelsText(
                                        hook = it,
                                        line1 = state.captionLine1,
                                        line2 = state.captionLine2,
                                        line3 = state.captionLine3
                                    )
                                },
                                label = { Text(if (isPersian) "تیتر آغازین (Hook)" else "Opening Hook Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = state.captionLine1,
                                onValueChange = {
                                    viewModel.updateReelsText(
                                        hook = state.hookTitle,
                                        line1 = it,
                                        line2 = state.captionLine2,
                                        line3 = state.captionLine3
                                    )
                                },
                                label = { Text(if (isPersian) "زیرنویس اول" else "Subtitle 1") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = state.captionLine2,
                                onValueChange = {
                                    viewModel.updateReelsText(
                                        hook = state.hookTitle,
                                        line1 = state.captionLine1,
                                        line2 = it,
                                        line3 = state.captionLine3
                                    )
                                },
                                label = { Text(if (isPersian) "زیرنویس دوم" else "Subtitle 2") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Step 3: Action Buttons + Render Progress
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.directAndRenderReel() },
                            enabled = state.selectedPhotos.isNotEmpty() && !state.isRendering && !state.isDirectingWithAi,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("reels_ai_create_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPersian) "کارگردانی هوشمند و ساخت ریلز" else "AI Direct & Render Reel",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.renderReelWithCurrentSettings() },
                            enabled = state.selectedPhotos.isNotEmpty() && !state.isRendering && !state.isDirectingWithAi,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reels_quick_render_button")
                        ) {
                            Icon(Icons.Rounded.MovieCreation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPersian) "ساخت سریع با متن و تنظیمات فعلی" else "Render With Current Settings"
                            )
                        }

                        if (state.isDirectingWithAi || state.isRendering) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = state.statusMessage ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (state.isRendering) {
                                        LinearProgressIndicator(
                                            progress = { state.renderProgress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "${(state.renderProgress * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    } else {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }

                        if (!state.errorMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.errorMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                // Step 4: Live 9:16 Video Player Preview & Export
                val videoPath = state.renderedVideoPath
                if (!videoPath.isNullOrBlank() && File(videoPath).exists()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isPersian) "پیش‌نمایش ریلز سینمایی آماده شد" else "Cinematic Reel Ready",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .fillMaxWidth()
                                        .aspectRatio(9f / 16f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            VideoView(ctx).apply {
                                                val controller = MediaController(ctx)
                                                controller.setAnchorView(this)
                                                setMediaController(controller)
                                                tag = videoPath
                                                setVideoPath(videoPath)
                                                setOnPreparedListener { mp ->
                                                    mp.isLooping = true
                                                    start()
                                                }
                                            }
                                        },
                                        update = { view ->
                                            if (view.tag != videoPath) {
                                                view.tag = videoPath
                                                view.setVideoPath(videoPath)
                                                view.start()
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.saveRenderedReelToGallery() },
                                        enabled = !state.savedToGallery,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("reels_save_gallery_button")
                                    ) {
                                        Icon(
                                            imageVector = if (state.savedToGallery) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (state.savedToGallery) {
                                                if (isPersian) "ذخیره شد" else "Saved"
                                            } else {
                                                if (isPersian) "ذخیره در گالری" else "Save MP4"
                                            }
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val videoFile = File(videoPath)
                                            val shareUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                videoFile
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "video/mp4"
                                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                                putExtra(Intent.EXTRA_TEXT, state.socialCaption)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(
                                                Intent.createChooser(
                                                    shareIntent,
                                                    if (isPersian) "اشتراک‌گذاری ریلز" else "Share Reel"
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("reels_share_button")
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isPersian) "اشتراک‌گذاری" else "Share")
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            clipboard.setText(AnnotatedString(state.socialCaption))
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = state.socialCaption,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = if (isPersian) "کپی کپشن" else "Copy caption",
                                            tint = MaterialTheme.colorScheme.primary,
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
}
