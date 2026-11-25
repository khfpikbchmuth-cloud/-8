package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.MediaAttachmentProcessor
import com.example.ai.MediaKind
import com.example.ai.PreparedAttachment
import com.example.data.MessageEntity
import java.io.File

@Composable
fun WelcomeHeroSection(
    isPersian: Boolean,
    showReelsModule: Boolean = true,
    showMediaLabModule: Boolean = true,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onOpenReelsStudio: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onOpenMediaLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_sami_banner),
                contentDescription = "Sami King AI Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sami_icon_fg),
                        contentDescription = "Sami King Crown Emblem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                RoundedCornerShape(16.dp)
                            )
                    )
                    Column {
                        Text(
                            text = if (isPersian) "سلام! من سامی کینگ هستم" else "Hello! I am Sami King",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = if (isPersian) "رایگان همیشگی • متن، عکس و ویدیو" else "100% Free • Text, Photo & Video",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (isPersian) {
                        "هر سوالی داری بپرس، عکس یا ویدیو ارسال کن تا دقیق تحلیل کنم و سریع جواب بدهم."
                    } else {
                        "Ask any question or attach photos and videos for instant multimodal intelligence."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }

        // Quick Action Grid (Adaptive 2 columns on phones, 4 on wide screens)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 600.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showReelsModule) {
                        CapabilityCard(
                            icon = Icons.Rounded.MovieCreation,
                            title = if (isPersian) "ساخت ویدیو ریلز" else "Reels Maker",
                            subtitle = if (isPersian) "تبدیل عکس به ریلز سینمایی" else "Turn photo into MP4 Reel",
                            accent = MaterialTheme.colorScheme.primary,
                            testTag = "quick_reels_card",
                            onClick = onOpenReelsStudio,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    CapabilityCard(
                        icon = Icons.Rounded.Image,
                        title = if (isPersian) "تحلیل تصویر" else "Analyze Photo",
                        subtitle = if (isPersian) "خواندن متن و توضیح عکس" else "Vision Q&A & OCR",
                        accent = MaterialTheme.colorScheme.secondary,
                        testTag = "quick_photo_card",
                        onClick = onPickImage,
                        modifier = Modifier.weight(1f)
                    )
                    CapabilityCard(
                        icon = Icons.Rounded.VideoLibrary,
                        title = if (isPersian) "تحلیل ویدیو" else "Analyze Video",
                        subtitle = if (isPersian) "خلاصه و بررسی کلیپ" else "Summarize & explain video",
                        accent = MaterialTheme.colorScheme.primary,
                        testTag = "quick_video_card",
                        onClick = onPickVideo,
                        modifier = Modifier.weight(1f)
                    )
                    if (showMediaLabModule) {
                        CapabilityCard(
                            icon = Icons.Rounded.AutoAwesome,
                            title = if (isPersian) "استودیو رسانه" else "Media Studio",
                            subtitle = if (isPersian) "گزارش ساختاریافته پیشرفته" else "Structured deep report",
                            accent = MaterialTheme.colorScheme.tertiary,
                            testTag = "quick_lab_card",
                            onClick = onOpenMediaLab,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showReelsModule) {
                            CapabilityCard(
                                icon = Icons.Rounded.MovieCreation,
                                title = if (isPersian) "ساخت ویدیو ریلز" else "Reels Maker",
                                subtitle = if (isPersian) "تبدیل عکس به ریلز سینمایی" else "Photo to MP4 Reel",
                                accent = MaterialTheme.colorScheme.primary,
                                testTag = "quick_reels_card",
                                onClick = onOpenReelsStudio,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        CapabilityCard(
                            icon = Icons.Rounded.Image,
                            title = if (isPersian) "تحلیل تصویر" else "Analyze Photo",
                            subtitle = if (isPersian) "خواندن متن و توضیح عکس" else "Vision Q&A & OCR",
                            accent = MaterialTheme.colorScheme.secondary,
                            testTag = "quick_photo_card",
                            onClick = onPickImage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CapabilityCard(
                            icon = Icons.Rounded.VideoLibrary,
                            title = if (isPersian) "تحلیل ویدیو" else "Analyze Video",
                            subtitle = if (isPersian) "خلاصه و بررسی کلیپ" else "Summarize video",
                            accent = MaterialTheme.colorScheme.primary,
                            testTag = "quick_video_card",
                            onClick = onPickVideo,
                            modifier = Modifier.weight(1f)
                        )
                        if (showMediaLabModule) {
                            CapabilityCard(
                                icon = Icons.Rounded.AutoAwesome,
                                title = if (isPersian) "استودیو رسانه" else "Media Studio",
                                subtitle = if (isPersian) "تحلیل عمیق عکس و ویدیو" else "Deep media breakdown",
                                accent = MaterialTheme.colorScheme.tertiary,
                                testTag = "quick_lab_card",
                                onClick = onOpenMediaLab,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Suggested Question Chips
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (isPersian) "پیشنهادهای سریع" else "Quick starters",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val prompts = if (isPersian) {
                listOf(
                    "فرق هوش مصنوعی نسل جدید با مدل‌های قبلی چیست؟",
                    "یک متن حرفه‌ای برای معرفی کسب‌وکار بنویس",
                    "بهترین تمرین‌های ورزشی روزانه در خانه را بگو"
                )
            } else {
                listOf(
                    "Explain quantum computing in simple terms",
                    "Write a clean Kotlin coroutine retry helper",
                    "Plan a productive 7-day learning sprint"
                )
            }
            prompts.forEachIndexed { idx, promptText ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onQuickPrompt(promptText) }
                        .testTag("starter_prompt_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = promptText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: MessageEntity,
    isLastModelMessage: Boolean,
    isSpeaking: Boolean,
    isPersian: Boolean,
    showVoiceTools: Boolean = true,
    mediaProcessor: MediaAttachmentProcessor,
    onToggleSpeak: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"
    val clipboard = LocalClipboardManager.current

    if (isUser) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val mediaKind = runCatching { MediaKind.valueOf(message.mediaKind) }
                        .getOrDefault(MediaKind.NONE)
                    if (mediaKind != MediaKind.NONE && !message.mediaLocalPath.isNullOrBlank()) {
                        AttachedMessageMediaPreview(
                            path = message.mediaLocalPath,
                            kind = mediaKind,
                            mediaProcessor = mediaProcessor
                        )
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (isPersian) "سامی کینگ" else "Sami King",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (message.isError) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (message.isError) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (message.isError) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (isLastModelMessage) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onRegenerate)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isPersian) "تلاش مجدد" else "Retry",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    } else {
                        RichMarkdownText(markdown = message.text)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!message.isError) {
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(message.text)) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_msg_${message.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = if (isPersian) "کپی پاسخ" else "Copy",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (showVoiceTools) {
                                IconButton(
                                    onClick = onToggleSpeak,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("speak_msg_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Rounded.StopCircle else Icons.Rounded.VolumeUp,
                                        contentDescription = if (isPersian) "خواندن صوتی" else "Read aloud",
                                        tint = if (isSpeaking) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                        if (isLastModelMessage) {
                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("regenerate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = if (isPersian) "پاسخ مجدد" else "Regenerate",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachedMessageMediaPreview(
    path: String,
    kind: MediaKind,
    mediaProcessor: MediaAttachmentProcessor
) {
    if (kind == MediaKind.IMAGE) {
        AsyncImage(
            model = File(path),
            contentDescription = "Attached image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(14.dp))
        )
    } else if (kind == MediaKind.VIDEO) {
        val thumb by produceState<Bitmap?>(initialValue = null, path) {
            value = mediaProcessor.extractThumbnailForVideo(path)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (thumb != null) {
                Image(
                    bitmap = thumb!!.asImageBitmap(),
                    contentDescription = "Attached video thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
fun StreamingAiBubble(
    streamingText: String,
    isLoadingOnly: Boolean,
    isPersian: Boolean
) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = if (isPersian) "سامی کینگ در حال فکر کردن..." else "Sami King is thinking...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                if (isLoadingOnly) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (isPersian) "در حال پردازش و آماده‌سازی پاسخ..." else "Processing your request...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    RichMarkdownText(markdown = "$streamingText ▍")
                }
            }
        }
    }
}

@Composable
fun ComposerDock(
    promptText: String,
    onPromptChange: (String) -> Unit,
    pendingAttachment: PreparedAttachment?,
    isPreparingMedia: Boolean,
    isStreaming: Boolean,
    isPersian: Boolean,
    showVoiceTools: Boolean = true,
    mediaProcessor: MediaAttachmentProcessor,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onCaptureCamera: () -> Unit,
    onVoiceInput: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(visible = isPreparingMedia || pendingAttachment != null) {
                if (isPreparingMedia) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = if (isPersian) "در حال آماده‌سازی عکس/ویدیو..." else "Preparing media...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (pendingAttachment != null) {
                    PendingAttachmentChip(
                        attachment = pendingAttachment,
                        isPersian = isPersian,
                        mediaProcessor = mediaProcessor,
                        onRemove = onRemoveAttachment
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            if (promptText.isEmpty()) {
                                Text(
                                    text = if (isPersian) {
                                        "هر سوالی داری از سامی کینگ بپرس..."
                                    } else {
                                        "Ask Sami King anything..."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = promptText,
                                onValueChange = onPromptChange,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("prompt_input")
                            )
                        }

                        if (isStreaming) {
                            IconButton(
                                onClick = onStop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
                                    .testTag("stop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = if (isPersian) "توقف" else "Stop",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else if (promptText.isNotBlank() || pendingAttachment != null) {
                            IconButton(
                                onClick = onSend,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = if (isPersian) "ارسال" else "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else if (showVoiceTools) {
                            IconButton(
                                onClick = onVoiceInput,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("voice_input_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = if (isPersian) "ورودی صوتی" else "Voice input",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onSend,
                                enabled = false,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }

                    // Bottom Attachment Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPickImage,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("attach_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = if (isPersian) "انتخاب تصویر" else "Pick photo",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = onPickVideo,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("attach_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideoLibrary,
                                contentDescription = if (isPersian) "انتخاب ویدیو" else "Pick video",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onCaptureCamera,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("capture_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = if (isPersian) "گرفتن عکس با دوربین" else "Take photo",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (isPersian) "عکس و ویدیو پشتیبانی می‌شود" else "Photo & Video ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingAttachmentChip(
    attachment: PreparedAttachment,
    isPersian: Boolean,
    mediaProcessor: MediaAttachmentProcessor,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (attachment.kind == MediaKind.IMAGE) {
                AsyncImage(
                    model = File(attachment.localPreviewPath),
                    contentDescription = "Selected photo preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                val thumb by produceState<Bitmap?>(initialValue = null, attachment.localPreviewPath) {
                    value = mediaProcessor.extractThumbnailForVideo(attachment.localPreviewPath)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumb != null) {
                        Image(
                            bitmap = thumb!!.asImageBitmap(),
                            contentDescription = "Video thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.widthIn(max = 180.dp)) {
                Text(
                    text = attachment.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = if (attachment.kind == MediaKind.VIDEO) {
                    val secs = (attachment.durationMs / 1000).coerceAtLeast(1)
                    if (attachment.usedKeyFrameSampling) {
                        if (isPersian) "ویدیو (${secs}ث) • فریم‌های کلیدی" else "Video (${secs}s) • Key frames"
                    } else {
                        if (isPersian) "ویدیو (${secs}ث) • کامل" else "Video (${secs}s) • Full"
                    }
                } else {
                    if (isPersian) "تصویر آماده تحلیل" else "Photo ready"
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("remove_attachment_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = if (isPersian) "حذف فایل" else "Remove attachment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
