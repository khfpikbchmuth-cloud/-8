package com.example.reels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class CinematicReelsRenderer(private val context: Context) {

    companion object {
        private const val VIDEO_MIME = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val WIDTH = 720
        private const val HEIGHT = 1280
        private const val FPS = 24
        private const val BIT_RATE = 4_200_000
        private const val I_FRAME_INTERVAL = 1
        private const val DEQUEUE_TIMEOUT_US = 10_000L
    }

    suspend fun renderReelMp4(
        config: ReelsRenderConfig,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.Default) {
        require(config.imagePaths.isNotEmpty()) { "At least one image is required." }

        val sourceBitmaps = config.imagePaths.mapNotNull { path ->
            BitmapFactory.decodeFile(path)?.let { bmp -> prepareOverscanBitmap(bmp) }
        }
        require(sourceBitmaps.isNotEmpty()) { "Could not decode selected images." }

        val outDir = File(context.filesDir, "media").apply { mkdirs() }
        val outputFile = File(outDir, "sami_reel_${UUID.randomUUID()}.mp4")

        val typeface = ResourcesCompat.getFont(context, R.font.vazirmatn) ?: Typeface.DEFAULT_BOLD
        val totalFrames = (config.durationSeconds.coerceIn(4, 15) * FPS)

        val frameBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)
        val argbPixels = IntArray(WIDTH * HEIGHT)

        val colorFilter = buildColorGradeFilter(config.colorGrade)
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.colorFilter = colorFilter
        }
        val transitionPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.colorFilter = colorFilter
        }

        val format = MediaFormat.createVideoFormat(VIDEO_MIME, WIDTH, HEIGHT).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        }

        val encoder = MediaCodec.createEncoderByType(VIDEO_MIME)
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var frameIndex = 0

            while (!outputDone) {
                coroutineContext.ensureActive()

                if (!inputDone) {
                    val inputIndex = encoder.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        if (frameIndex >= totalFrames) {
                            encoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                frameIndex * 1_000_000L / FPS,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            drawCinematicFrame(
                                canvas = frameCanvas,
                                frameIndex = frameIndex,
                                totalFrames = totalFrames,
                                bitmaps = sourceBitmaps,
                                config = config,
                                typeface = typeface,
                                imagePaint = imagePaint,
                                transitionPaint = transitionPaint
                            )
                            frameBitmap.getPixels(argbPixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)

                            val presentationTimeUs = frameIndex * 1_000_000L / FPS
                            val inputImage = encoder.getInputImage(inputIndex)
                            if (inputImage != null) {
                                writeArgbToYuv420Image(argbPixels, inputImage)
                                inputImage.close()
                                // Standard full-frame YUV420 size hint for ByteBuffer accounting
                                val frameByteSize = WIDTH * HEIGHT * 3 / 2
                                encoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    frameByteSize,
                                    presentationTimeUs,
                                    0
                                )
                            } else {
                                val buffer = encoder.getInputBuffer(inputIndex)!!
                                val written = writeArgbToI420Buffer(argbPixels, buffer)
                                encoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    written,
                                    presentationTimeUs,
                                    0
                                )
                            }

                            frameIndex++
                            onProgress((frameIndex.toFloat() / totalFrames.toFloat()).coerceIn(0f, 0.98f))
                        }
                    }
                }

                // Drain available encoded output frames
                var draining = true
                while (draining) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            draining = false
                        }
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) {
                                error("Encoder output format changed twice")
                            }
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outIndex)
                                ?: error("Encoder output buffer $outIndex was null")

                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }

                            if (bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }

                            encoder.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true
                                draining = false
                            }
                        }
                    }
                }
            }

            onProgress(1f)
            outputFile
        } catch (t: Throwable) {
            outputFile.delete()
            throw t
        } finally {
            frameBitmap.recycle()
            sourceBitmaps.forEach { it.recycle() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
        }
    }

    private fun prepareOverscanBitmap(raw: Bitmap): Bitmap {
        // Fit/cover 720x1280 with 24% overscan so pan/zoom never shows gaps
        val targetW = (WIDTH * 1.28f).toInt()
        val targetH = (HEIGHT * 1.28f).toInt()
        val scale = maxOf(targetW.toFloat() / raw.width, targetH.toFloat() / raw.height)
        val scaledW = (raw.width * scale).toInt().coerceAtLeast(targetW)
        val scaledH = (raw.height * scale).toInt().coerceAtLeast(targetH)
        return Bitmap.createScaledBitmap(raw, scaledW, scaledH, true)
    }

    private fun drawCinematicFrame(
        canvas: Canvas,
        frameIndex: Int,
        totalFrames: Int,
        bitmaps: List<Bitmap>,
        config: ReelsRenderConfig,
        typeface: Typeface,
        imagePaint: Paint,
        transitionPaint: Paint
    ) {
        canvas.drawColor(Color.BLACK)

        val globalProgress = frameIndex.toFloat() / (totalFrames - 1).coerceAtLeast(1).toFloat()
        val count = bitmaps.size
        val segmentFloat = globalProgress * count
        val currentIdx = segmentFloat.toInt().coerceIn(0, count - 1)
        val segmentProgress = (segmentFloat - currentIdx).coerceIn(0f, 1f)

        val currentBmp = bitmaps[currentIdx]
        val matrix = computeCameraMatrix(
            bitmap = currentBmp,
            progress = if (count == 1) globalProgress else segmentProgress,
            motion = config.motionStyle,
            sceneIndex = currentIdx
        )
        imagePaint.alpha = 255
        canvas.drawBitmap(currentBmp, matrix, imagePaint)

        // Smooth crossfade into next scene if multiple photos
        if (count > 1 && currentIdx < count - 1 && segmentProgress > 0.78f) {
            val blend = ((segmentProgress - 0.78f) / 0.22f).coerceIn(0f, 1f)
            val nextBmp = bitmaps[currentIdx + 1]
            val nextMatrix = computeCameraMatrix(
                bitmap = nextBmp,
                progress = 0f,
                motion = config.motionStyle,
                sceneIndex = currentIdx + 1
            )
            transitionPaint.alpha = (blend * 255).toInt()
            canvas.drawBitmap(nextBmp, nextMatrix, transitionPaint)
        }

        // Cinematic Radial Vignette
        drawVignette(canvas)

        // Animated Warm/Cyan Light Leak Glow
        if (config.includeLightLeak) {
            drawLightLeak(canvas, globalProgress, config.colorGrade)
        }

        // Subtle Bottom Readability Gradient Scrim
        val bottomScrimPaint = Paint().apply {
            shader = LinearGradient(
                0f,
                HEIGHT * 0.56f,
                0f,
                HEIGHT.toFloat(),
                intArrayOf(Color.TRANSPARENT, Color.argb(195, 4, 8, 18)),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, HEIGHT * 0.56f, WIDTH.toFloat(), HEIGHT.toFloat(), bottomScrimPaint)

        // Anamorphic Letterbox Bars
        if (config.includeCinemaBars) {
            val barH = 74f
            val barPaint = Paint().apply { color = Color.BLACK }
            canvas.drawRect(0f, 0f, WIDTH.toFloat(), barH, barPaint)
            canvas.drawRect(0f, HEIGHT - barH, WIDTH.toFloat(), HEIGHT.toFloat(), barPaint)

            val accentLine = Paint().apply {
                color = Color.argb(110, 245, 196, 81)
                strokeWidth = 2f
            }
            canvas.drawLine(0f, barH, WIDTH.toFloat(), barH, accentLine)
            canvas.drawLine(0f, HEIGHT - barH, WIDTH.toFloat(), HEIGHT - barH, accentLine)
        }

        // Opening Hook Pill Overlay (first ~38% of Reel)
        if (config.hookTitle.isNotBlank() && globalProgress < 0.42f) {
            val hookAlpha = when {
                globalProgress < 0.08f -> globalProgress / 0.08f
                globalProgress > 0.34f -> (0.42f - globalProgress) / 0.08f
                else -> 1f
            }.coerceIn(0f, 1f)
            val slideY = (1f - easeOutCubic(hookAlpha)) * 28f
            drawHookBadge(canvas, config.hookTitle, typeface, hookAlpha, slideY)
        }

        // Timed Cinematic Subtitles across the Reel
        val cleanCaptions = config.captions.filter { it.isNotBlank() }
        if (cleanCaptions.isNotEmpty()) {
            val capIdx = (globalProgress * cleanCaptions.size).toInt()
                .coerceIn(0, cleanCaptions.lastIndex)
            val capSegSize = 1f / cleanCaptions.size.toFloat()
            val localP = ((globalProgress - capIdx * capSegSize) / capSegSize).coerceIn(0f, 1f)
            val capAlpha = when {
                localP < 0.14f -> localP / 0.14f
                localP > 0.86f -> (1f - localP) / 0.14f
                else -> 1f
            }.coerceIn(0f, 1f)
            drawSubtitleText(canvas, cleanCaptions[capIdx], typeface, capAlpha)
        }

        // Cinema Fade-In and Fade-Out
        val fadeAlpha = when {
            globalProgress < 0.06f -> 1f - (globalProgress / 0.06f)
            globalProgress > 0.93f -> (globalProgress - 0.93f) / 0.07f
            else -> 0f
        }.coerceIn(0f, 1f)
        if (fadeAlpha > 0f) {
            canvas.drawColor(Color.argb((fadeAlpha * 255).toInt(), 0, 0, 0))
        }
    }

    private fun computeCameraMatrix(
        bitmap: Bitmap,
        progress: Float,
        motion: ReelMotionStyle,
        sceneIndex: Int
    ): Matrix {
        val eased = easeInOutCubic(progress)
        val baseScale = maxOf(WIDTH.toFloat() / bitmap.width, HEIGHT.toFloat() / bitmap.height)
        val cx = WIDTH / 2f
        val cy = HEIGHT / 2f

        var scaleFactor = 1.0f
        var dx = 0f
        var dy = 0f
        var rotation = 0f

        when (motion) {
            ReelMotionStyle.ZOOM_IN -> {
                scaleFactor = 1.02f + eased * 0.18f
                dx = sin(eased * PI).toFloat() * 14f * if (sceneIndex % 2 == 0) 1f else -1f
                dy = -eased * 18f
                rotation = (eased - 0.5f) * 0.8f
            }
            ReelMotionStyle.PAN_DRIFT -> {
                scaleFactor = 1.14f
                val dir = if (sceneIndex % 2 == 0) 1f else -1f
                dx = (eased - 0.5f) * 90f * dir
                dy = sin(eased * PI).toFloat() * 16f
            }
            ReelMotionStyle.DRAMATIC_PULL -> {
                scaleFactor = 1.24f - eased * 0.21f
                dy = (1f - eased) * 24f
                rotation = (0.5f - eased) * 1.1f
            }
            ReelMotionStyle.ORBIT_PULSE -> {
                scaleFactor = 1.10f + sin(eased * PI * 2f).toFloat() * 0.06f
                dx = cos(eased * PI * 2f).toFloat() * 24f
                dy = sin(eased * PI * 2f).toFloat() * 24f
                rotation = sin(eased * PI * 2f).toFloat() * 1.2f
            }
        }

        val finalScale = baseScale * scaleFactor
        val bmpCenterX = bitmap.width / 2f
        val bmpCenterY = bitmap.height / 2f

        return Matrix().apply {
            postTranslate(-bmpCenterX, -bmpCenterY)
            postScale(finalScale, finalScale)
            postRotate(rotation)
            postTranslate(cx + dx, cy + dy)
        }
    }

    private fun drawVignette(canvas: Canvas) {
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                WIDTH / 2f,
                HEIGHT / 2f,
                HEIGHT * 0.68f,
                intArrayOf(Color.TRANSPARENT, Color.argb(145, 0, 0, 0)),
                floatArrayOf(0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), vignettePaint)
    }

    private fun drawLightLeak(canvas: Canvas, progress: Float, grade: ReelColorGrade) {
        val glowColor = when (grade) {
            ReelColorGrade.CYBER_NEON -> Color.argb(90, 56, 189, 248)
            ReelColorGrade.NOIR_LUXE -> Color.argb(55, 226, 232, 240)
            else -> Color.argb(95, 245, 196, 81)
        }
        val x = WIDTH * (0.15f + 0.7f * sin(progress * PI).toFloat())
        val y = HEIGHT * (0.18f + 0.12f * cos(progress * PI * 2f).toFloat())
        val leakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                x,
                y,
                WIDTH * 0.65f,
                intArrayOf(glowColor, Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), leakPaint)
    }

    private fun drawHookBadge(
        canvas: Canvas,
        title: String,
        typeface: Typeface,
        alpha: Float,
        offsetY: Float
    ) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((alpha * 255).toInt(), 255, 255, 255)
            textSize = 42f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            setShadowLayer(12f, 0f, 4f, Color.argb((alpha * 180).toInt(), 0, 0, 0))
        }
        val maxWidth = WIDTH - 120
        val layout = StaticLayout.Builder
            .obtain(title, 0, title.length, textPaint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f, 1.1f)
            .build()

        val pillPadH = 28f
        val pillPadV = 18f
        val boxTop = 135f + offsetY
        val boxBottom = boxTop + layout.height + pillPadV * 2
        val rect = RectF(46f, boxTop, WIDTH - 46f, boxBottom)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((alpha * 165).toInt(), 11, 16, 33)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.argb((alpha * 210).toInt(), 245, 196, 81)
        }
        canvas.drawRoundRect(rect, 26f, 26f, bgPaint)
        canvas.drawRoundRect(rect, 26f, 26f, borderPaint)

        canvas.save()
        canvas.translate(60f, boxTop + pillPadV)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawSubtitleText(
        canvas: Canvas,
        caption: String,
        typeface: Typeface,
        alpha: Float
    ) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((alpha * 255).toInt(), 248, 250, 252)
            textSize = 36f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            setShadowLayer(14f, 0f, 3f, Color.argb((alpha * 230).toInt(), 0, 0, 0))
        }
        val maxWidth = WIDTH - 100
        val layout = StaticLayout.Builder
            .obtain(caption, 0, caption.length, textPaint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f, 1.15f)
            .build()

        val topY = HEIGHT - 150f - layout.height
        canvas.save()
        canvas.translate(50f, topY)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun buildColorGradeFilter(grade: ReelColorGrade): ColorMatrixColorFilter? {
        val cm = ColorMatrix()
        when (grade) {
            ReelColorGrade.GOLDEN_CINEMA -> {
                val contrast = 1.12f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                cm.set(
                    floatArrayOf(
                        contrast * 1.07f, 0f, 0f, 0f, translate + 6f,
                        0f, contrast * 1.02f, 0f, 0f, translate + 2f,
                        0f, 0f, contrast * 0.90f, 0f, translate - 4f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ReelColorGrade.CYBER_NEON -> {
                val sat = ColorMatrix().apply { setSaturation(1.28f) }
                val tint = ColorMatrix(
                    floatArrayOf(
                        0.95f, 0f, 0f, 0f, -6f,
                        0f, 1.05f, 0f, 0f, 4f,
                        0f, 0f, 1.18f, 0f, 12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(sat)
                cm.postConcat(tint)
            }
            ReelColorGrade.NOIR_LUXE -> {
                val bw = ColorMatrix().apply { setSaturation(0f) }
                val contrast = 1.24f
                val t = (-0.5f * contrast + 0.5f) * 255f
                val c = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, t,
                        0f, contrast, 0f, 0f, t,
                        0f, 0f, contrast, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(bw)
                cm.postConcat(c)
            }
            ReelColorGrade.WARM_SUNSET -> {
                cm.set(
                    floatArrayOf(
                        1.14f, 0f, 0f, 0f, 10f,
                        0f, 1.02f, 0f, 0f, 2f,
                        0f, 0f, 0.86f, 0f, -8f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ReelColorGrade.CRISP_STUDIO -> {
                val sat = ColorMatrix().apply { setSaturation(1.12f) }
                cm.postConcat(sat)
            }
        }
        return ColorMatrixColorFilter(cm)
    }

    private fun writeArgbToYuv420Image(argb: IntArray, image: android.media.Image) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        var idx = 0
        for (j in 0 until HEIGHT) {
            val yRowOffset = j * yRowStride
            val uvRowOffsetU = (j shr 1) * uRowStride
            val uvRowOffsetV = (j shr 1) * vRowStride

            for (i in 0 until WIDTH) {
                val px = argb[idx++]
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF

                val yVal = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
                yBuf.put(yRowOffset + i * yPixelStride, yVal.toByte())

                if ((j and 1) == 0 && (i and 1) == 0) {
                    val uVal = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                    val vVal = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                    val uvCol = i shr 1
                    uBuf.put(uvRowOffsetU + uvCol * uPixelStride, uVal.toByte())
                    vBuf.put(uvRowOffsetV + uvCol * vPixelStride, vVal.toByte())
                }
            }
        }
    }

    private fun writeArgbToI420Buffer(argb: IntArray, buffer: ByteBuffer): Int {
        buffer.clear()
        val ySize = WIDTH * HEIGHT
        val uOffset = ySize
        val vOffset = ySize + ySize / 4

        var idx = 0
        var uvIdx = 0
        for (j in 0 until HEIGHT) {
            for (i in 0 until WIDTH) {
                val px = argb[idx++]
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                val yVal = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
                buffer.put(j * WIDTH + i, yVal.toByte())

                if ((j and 1) == 0 && (i and 1) == 0) {
                    val uVal = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                    val vVal = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                    buffer.put(uOffset + uvIdx, uVal.toByte())
                    buffer.put(vOffset + uvIdx, vVal.toByte())
                    uvIdx++
                }
            }
        }
        return ySize * 3 / 2
    }

    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) {
            4f * t * t * t
        } else {
            1f - (-2f * t + 2f).pow(3) / 2f
        }
    }

    private fun easeOutCubic(t: Float): Float = 1f - (1f - t).pow(3)
}
