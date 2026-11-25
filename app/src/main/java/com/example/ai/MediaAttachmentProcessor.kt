package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class MediaKind {
    NONE,
    IMAGE,
    VIDEO
}

data class PreparedAttachment(
    val kind: MediaKind,
    val localPreviewPath: String,
    val mimeType: String,
    val displayName: String,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val usedKeyFrameSampling: Boolean = false
)

sealed interface InferenceMediaPayload {
    data class ImagePayload(val bitmap: Bitmap) : InferenceMediaPayload
    data class InlineVideoPayload(val bytes: ByteArray, val mimeType: String) : InferenceMediaPayload
    data class VideoFramesPayload(val frames: List<Bitmap>, val durationMs: Long) : InferenceMediaPayload
}

class MediaAttachmentProcessor(private val context: Context) {

    companion object {
        private const val MAX_IMAGE_DIM = 1024
        private const val MAX_INLINE_VIDEO_BYTES = 4L * 1024L * 1024L // Fast upload well under 20MB limit
        private const val KEY_FRAME_COUNT = 6
    }

    private val mediaDir: File by lazy {
        File(context.filesDir, "media").apply { mkdirs() }
    }

    suspend fun prepareAttachment(uri: Uri, kind: MediaKind): PreparedAttachment =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val detectedMime = resolver.getType(uri)
                ?: if (kind == MediaKind.VIDEO) "video/mp4" else "image/jpeg"
            val displayName = queryDisplayName(uri)
                ?: if (kind == MediaKind.VIDEO) "video.mp4" else "photo.jpg"

            when (kind) {
                MediaKind.IMAGE -> {
                    val bitmap = decodeScaledBitmap(uri, MAX_IMAGE_DIM)
                    val outFile = File(mediaDir, "img_${UUID.randomUUID()}.jpg")
                    FileOutputStream(outFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                    }
                    PreparedAttachment(
                        kind = MediaKind.IMAGE,
                        localPreviewPath = outFile.absolutePath,
                        mimeType = "image/jpeg",
                        displayName = displayName,
                        sizeBytes = outFile.length()
                    )
                }

                MediaKind.VIDEO -> {
                    val copiedVideo = File(mediaDir, "vid_${UUID.randomUUID()}.mp4")
                    resolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(copiedVideo).use { output ->
                            input.copyTo(output)
                        }
                    } ?: error("Unable to read selected video.")

                    val sizeBytes = copiedVideo.length()
                    val retriever = MediaMetadataRetriever()
                    var durationMs = 0L
                    try {
                        retriever.setDataSource(copiedVideo.absolutePath)
                        durationMs = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull() ?: 0L
                        val thumb = retriever.getFrameAtTime(
                            0L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        val thumbFile = File(mediaDir, "vthumb_${UUID.randomUUID()}.jpg")
                        if (thumb != null) {
                            val scaledThumb = scaleBitmap(thumb, 720)
                            FileOutputStream(thumbFile).use { fos ->
                                scaledThumb.compress(Bitmap.CompressFormat.JPEG, 82, fos)
                            }
                        }
                    } finally {
                        retriever.release()
                    }

                    // Store the copied video path cleanly alongside metadata
                    PreparedAttachment(
                        kind = MediaKind.VIDEO,
                        localPreviewPath = copiedVideo.absolutePath,
                        mimeType = detectedMime,
                        displayName = displayName,
                        durationMs = durationMs,
                        sizeBytes = sizeBytes,
                        usedKeyFrameSampling = sizeBytes > MAX_INLINE_VIDEO_BYTES
                    )
                }

                MediaKind.NONE -> error("Unsupported media kind")
            }
        }

    suspend fun loadInferencePayload(
        localPath: String,
        kind: MediaKind,
        mimeType: String
    ): InferenceMediaPayload? = withContext(Dispatchers.IO) {
        val file = File(localPath)
        if (!file.exists()) return@withContext null

        when (kind) {
            MediaKind.IMAGE -> {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
                InferenceMediaPayload.ImagePayload(scaleBitmap(bitmap, MAX_IMAGE_DIM))
            }

            MediaKind.VIDEO -> {
                if (file.length() <= MAX_INLINE_VIDEO_BYTES) {
                    val bytes = file.readBytes()
                    InferenceMediaPayload.InlineVideoPayload(
                        bytes = bytes,
                        mimeType = mimeType.ifBlank { "video/mp4" }
                    )
                } else {
                    val frames = extractVideoKeyFrames(file.absolutePath, KEY_FRAME_COUNT)
                    val durationMs = extractVideoDuration(file.absolutePath)
                    InferenceMediaPayload.VideoFramesPayload(frames = frames, durationMs = durationMs)
                }
            }

            MediaKind.NONE -> null
        }
    }

    suspend fun extractThumbnailForVideo(videoPath: String): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.let { scaleBitmap(it, 640) }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun extractVideoDuration(videoPath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    private fun extractVideoKeyFrames(videoPath: String, count: Int): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(videoPath)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            val durationUs = (durationMs.coerceAtLeast(1000L)) * 1000L
            val stepUs = durationUs / (count + 1)
            for (i in 1..count) {
                val timeUs = stepUs * i
                val frame = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (frame != null) {
                    frames.add(scaleBitmap(frame, 800))
                }
            }
        } finally {
            retriever.release()
        }
        return frames
    }

    private fun decodeScaledBitmap(uri: Uri, maxDim: Int): Bitmap {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                if (w > maxDim || h > maxDim) {
                    val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
                    decoder.setTargetSize(
                        (w * ratio).toInt().coerceAtLeast(1),
                        (h * ratio).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: error("Failed to decode image")
        }
        return scaleBitmap(raw, maxDim)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxOf(width, height).toFloat()
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun compressBitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }
}
