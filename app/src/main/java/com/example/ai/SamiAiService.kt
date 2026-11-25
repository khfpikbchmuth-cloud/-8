package com.example.ai

import android.util.Log
import com.example.reels.ReelColorGrade
import com.example.reels.ReelMotionStyle
import com.example.reels.ReelsDirectorPlan
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

private const val TAG = "SamiAiService"
private const val INFERENCE_TIMEOUT_MS = 45_000L
private const val TEXT_FIRST_TOKEN_TIMEOUT_SEC = 18
private const val MEDIA_FIRST_TOKEN_TIMEOUT_SEC = 38
private const val FALLBACK_TIMEOUT_MS = 30_000L

data class StructuredMediaReport(
    val title: String,
    val summary: String,
    val keyHighlights: List<String>,
    val extractedTextOrAudio: String,
    val suggestedQuestions: List<String>
)

data class ChatHistoryTurn(
    val role: String,
    val text: String
)

class SamiAiService {

    private val systemPrompt = content {
        text(
            """
            You are Sami King (سامی کینگ), an advanced, friendly, and highly capable multimodal AI assistant built for Android.
            Core principles:
            - Respond in the same language the user writes in (primarily Persian/Farsi when asked in Persian, or English/other languages when asked in them).
            - Give clear, accurate, complete answers to every question—whether about general knowledge, coding, science, writing, translation, math, images, or videos.
            - Use clean, well-structured formatting (short headings, bullet points, numbered steps, or code blocks where helpful).
            - Never invent facts about an image or video if something is unclear; describe honestly what is visible or audible.
            """.trimIndent()
        )
    }

    private val chatModels = mutableMapOf<SamiModelOption, GenerativeModel>()

    private val structuredAnalyzerModel: GenerativeModel by lazy {
        val reportSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Short descriptive title in the user's language"),
                "summary" to Schema.string(description = "Comprehensive 2-4 sentence overview of the image or video"),
                "keyHighlights" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 6 concrete observations, objects, scenes, or moments"
                ),
                "extractedTextOrAudio" to Schema.string(
                    description = "Any visible text (OCR), signs, code, or spoken/caption summary; empty if none"
                ),
                "suggestedQuestions" to Schema.array(
                    items = Schema.string(),
                    description = "3 helpful follow-up questions the user might want to ask about this media"
                )
            ),
            optionalProperties = listOf("extractedTextOrAudio")
        )

        Firebase.ai.generativeModel(
            modelName = SamiModelOption.PRO_BALANCED.modelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reportSchema
            }
        )
    }

    private val reelsDirectorModel: GenerativeModel by lazy {
        val reelsSchema = Schema.obj(
            properties = mapOf(
                "hookTitle" to Schema.string(
                    description = "Short punchy cinematic hook title (2 to 5 words) for the Reel opening overlay"
                ),
                "captions" to Schema.array(
                    items = Schema.string(),
                    description = "Exactly 3 short cinematic subtitle lines (4 to 8 words each) for beginning, middle, and ending of the Reel"
                ),
                "recommendedMotion" to Schema.enumeration(
                    values = listOf("ZOOM_IN", "PAN_DRIFT", "DRAMATIC_PULL", "ORBIT_PULSE"),
                    description = "Best camera motion matching the mood of the image(s)"
                ),
                "recommendedGrade" to Schema.enumeration(
                    values = listOf("GOLDEN_CINEMA", "CYBER_NEON", "NOIR_LUXE", "WARM_SUNSET", "CRISP_STUDIO"),
                    description = "Best cinematic color grade matching the image(s)"
                ),
                "socialCaption" to Schema.string(
                    description = "Engaging Instagram Reels caption with 4 to 6 relevant hashtags"
                )
            )
        )

        Firebase.ai.generativeModel(
            modelName = SamiModelOption.PRO_BALANCED.modelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reelsSchema
            }
        )
    }

    private fun buildSystemPrompt(
        responseStyle: com.example.config.ResponseStyle,
        customInstruction: String
    ): Content = content {
        text(
            buildString {
                appendLine(
                    """
                    You are Sami King (سامی کینگ), an advanced, friendly, and highly capable multimodal AI assistant built for Android.
                    Core principles:
                    - Respond in the same language the user writes in (primarily Persian/Farsi when asked in Persian, or English/other languages when asked in them).
                    - Give clear, accurate, complete answers to every question—whether about general knowledge, coding, science, writing, translation, math, images, or videos.
                    - Use clean, well-structured formatting (short headings, bullet points, numbered steps, or code blocks where helpful).
                    - Never invent facts about an image or video if something is unclear; describe honestly what is visible or audible.
                    """.trimIndent()
                )
                appendLine()
                append("Response style preference: ")
                appendLine(responseStyle.promptHint)
                if (customInstruction.isNotBlank()) {
                    appendLine()
                    append("User custom instructions: ")
                    append(customInstruction.trim())
                }
            }
        )
    }

    private fun createChatModel(
        option: SamiModelOption,
        responseStyle: com.example.config.ResponseStyle,
        customInstruction: String,
        temperature: Float
    ): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = option.modelId,
            generationConfig = generationConfig {
                this.temperature = temperature.coerceIn(0.1f, 1.3f)
            },
            systemInstruction = buildSystemPrompt(responseStyle, customInstruction)
        )
    }

    private fun buildValidHistory(history: List<ChatHistoryTurn>): List<Content> {
        val sanitized = mutableListOf<ChatHistoryTurn>()
        for (turn in history) {
            if (turn.text.isBlank()) continue
            val normalizedRole = if (turn.role == "model") "model" else "user"
            if (sanitized.isEmpty()) {
                if (normalizedRole == "user") {
                    sanitized.add(ChatHistoryTurn("user", turn.text))
                }
            } else if (sanitized.last().role != normalizedRole) {
                sanitized.add(ChatHistoryTurn(normalizedRole, turn.text))
            }
        }
        if (sanitized.isNotEmpty() && sanitized.last().role == "user") {
            sanitized.removeAt(sanitized.lastIndex)
        }
        return sanitized.takeLast(10).map { turn ->
            content(role = turn.role) {
                text(turn.text)
            }
        }
    }

    private fun buildUserContent(
        prompt: String,
        mediaPayload: InferenceMediaPayload?
    ): Content = content(role = "user") {
        when (mediaPayload) {
            is InferenceMediaPayload.ImagePayload -> {
                image(mediaPayload.bitmap)
            }
            is InferenceMediaPayload.InlineVideoPayload -> {
                inlineData(mediaPayload.bytes, mediaPayload.mimeType)
            }
            is InferenceMediaPayload.VideoFramesPayload -> {
                text(
                    "Note: Key frames from a ${mediaPayload.durationMs / 1000}s video are provided in chronological order."
                )
                mediaPayload.frames.forEach { frame ->
                    image(frame)
                }
            }
            null -> Unit
        }
        val effectivePrompt = prompt.ifBlank {
            when (mediaPayload) {
                is InferenceMediaPayload.ImagePayload -> "لطفاً این تصویر را به طور کامل تحلیل و توضیح بده."
                is InferenceMediaPayload.InlineVideoPayload,
                is InferenceMediaPayload.VideoFramesPayload -> "لطفاً این ویدیو را به طور کامل تحلیل و خلاصه کن."
                null -> ""
            }
        }
        if (effectivePrompt.isNotBlank()) {
            text(effectivePrompt)
        }
    }

    @OptIn(FlowPreview::class)
    fun streamChatReply(
        modelOption: SamiModelOption,
        history: List<ChatHistoryTurn>,
        prompt: String,
        mediaPayload: InferenceMediaPayload?,
        responseStyle: com.example.config.ResponseStyle = com.example.config.ResponseStyle.BALANCED,
        customInstruction: String = "",
        temperature: Float = 0.7f
    ): Flow<String> = flow {
        val sdkHistory = buildValidHistory(history)
        val userMessage = buildUserContent(prompt, mediaPayload)
        val chunkTimeoutSec = if (mediaPayload != null) {
            MEDIA_FIRST_TOKEN_TIMEOUT_SEC
        } else {
            TEXT_FIRST_TOKEN_TIMEOUT_SEC
        }

        Log.d(
            TAG,
            "streamChatReply start model=${modelOption.modelId} history=${sdkHistory.size} hasMedia=${mediaPayload != null}"
        )

        var emittedAnyChunk = false
        try {
            val model = createChatModel(modelOption, responseStyle, customInstruction, temperature)
            val streamFlow = if (sdkHistory.isEmpty()) {
                model.generateContentStream(userMessage)
            } else {
                model.startChat(history = sdkHistory).sendMessageStream(userMessage)
            }

            streamFlow
                .timeout(chunkTimeoutSec.seconds)
                .collect { chunk ->
                    val text = chunk.text
                    if (!text.isNullOrEmpty()) {
                        emittedAnyChunk = true
                        emit(text)
                    }
                }
        } catch (e: TimeoutCancellationException) {
            if (!emittedAnyChunk) {
                Log.w(TAG, "Stream timed out before first chunk; falling back to fast unary call", e)
                val fallbackModel = createChatModel(
                    SamiModelOption.TURBO_FAST,
                    responseStyle,
                    customInstruction,
                    temperature
                )
                val fallbackResponse = withTimeout(FALLBACK_TIMEOUT_MS) {
                    if (sdkHistory.isEmpty()) {
                        fallbackModel.generateContent(userMessage)
                    } else {
                        fallbackModel.startChat(history = sdkHistory).sendMessage(userMessage)
                    }
                }
                val fallbackText = fallbackResponse.text
                if (!fallbackText.isNullOrBlank()) {
                    emit(fallbackText)
                    return@flow
                }
            }
            throw e
        } catch (e: CancellationException) {
            throw e
        }
    }

    suspend fun analyzeMediaStructured(
        mediaPayload: InferenceMediaPayload,
        customInstruction: String,
        preferPersian: Boolean
    ): StructuredMediaReport {
        val languageInstruction = if (preferPersian) {
            "پاسخ را کاملاً به زبان فارسی روان و دقیق بنویس."
        } else {
            "Write the entire response in clear, natural English."
        }

        val requestContent = content {
            when (mediaPayload) {
                is InferenceMediaPayload.ImagePayload -> image(mediaPayload.bitmap)
                is InferenceMediaPayload.InlineVideoPayload ->
                    inlineData(mediaPayload.bytes, mediaPayload.mimeType)
                is InferenceMediaPayload.VideoFramesPayload -> {
                    text("Chronological key frames from a ${mediaPayload.durationMs / 1000}s video:")
                    mediaPayload.frames.forEach { image(it) }
                }
            }
            val instruction = buildString {
                append(languageInstruction)
                append(" Analyze this media thoroughly.")
                if (customInstruction.isNotBlank()) {
                    append(" User focus: ")
                    append(customInstruction)
                }
            }
            text(instruction)
        }

        val response = withTimeout(INFERENCE_TIMEOUT_MS) {
            structuredAnalyzerModel.generateContent(requestContent)
        }

        val rawJson = response.text
        if (rawJson.isNullOrBlank()) {
            Log.e(TAG, "Structured media analysis returned empty text")
            throw IllegalStateException("مدل پاسخی برنگرداند.")
        }

        val json = JSONObject(rawJson)
        val highlightsArray = json.optJSONArray("keyHighlights")
        val highlights = buildList {
            if (highlightsArray != null) {
                for (i in 0 until highlightsArray.length()) {
                    val item = highlightsArray.optString(i)
                    if (item.isNotBlank()) add(item)
                }
            }
        }
        val questionsArray = json.optJSONArray("suggestedQuestions")
        val questions = buildList {
            if (questionsArray != null) {
                for (i in 0 until questionsArray.length()) {
                    val item = questionsArray.optString(i)
                    if (item.isNotBlank()) add(item)
                }
            }
        }

        return StructuredMediaReport(
            title = json.optString("title", "تحلیل رسانه"),
            summary = json.optString("summary", ""),
            keyHighlights = highlights,
            extractedTextOrAudio = json.optString("extractedTextOrAudio", ""),
            suggestedQuestions = questions
        )
    }

    suspend fun directReelsPlan(
        imagePayloads: List<InferenceMediaPayload.ImagePayload>,
        userConcept: String,
        preferPersian: Boolean
    ): ReelsDirectorPlan {
        val langPrompt = if (preferPersian) {
            "Write hookTitle, all 3 captions, and socialCaption in stylish, engaging modern Persian (Farsi) suitable for viral Instagram Reels."
        } else {
            "Write hookTitle, all 3 captions, and socialCaption in punchy, engaging cinematic English for viral Reels."
        }

        val requestContent = content {
            imagePayloads.take(5).forEach { img ->
                image(img.bitmap)
            }
            text(
                buildString {
                    append("You are an elite cinematic Reels director. ")
                    append(langPrompt)
                    append(" Based on the attached photo(s)")
                    if (userConcept.isNotBlank()) {
                        append(" and the user's concept: \"$userConcept\"")
                    }
                    append(", create a cinematic Reels plan with a punchy opening hook, 3 timed subtitle lines, best camera motion, color grade, and Instagram caption.")
                }
            )
        }

        val response = withTimeout(INFERENCE_TIMEOUT_MS) {
            reelsDirectorModel.generateContent(requestContent)
        }

        val rawJson = response.text ?: throw IllegalStateException("Empty Reels director output")
        val json = JSONObject(rawJson)
        val capsArray = json.optJSONArray("captions")
        val captions = buildList {
            if (capsArray != null) {
                for (i in 0 until capsArray.length()) {
                    val item = capsArray.optString(i).trim()
                    if (item.isNotBlank()) add(item)
                }
            }
        }

        return ReelsDirectorPlan(
            hookTitle = json.optString("hookTitle", if (preferPersian) "لحظه‌ای سینمایی" else "Cinematic Moment"),
            captions = captions.ifEmpty {
                if (preferPersian) listOf("جزئیاتی فراتر از تصویر", "ساخته شده با هوش مصنوعی سامی کینگ", "کیفیت سینمایی ریلز")
                else listOf("Every detail tells a story", "Directed by Sami King AI", "Pure cinematic Reels")
            },
            recommendedMotion = ReelMotionStyle.fromId(json.optString("recommendedMotion")),
            recommendedGrade = ReelColorGrade.fromId(json.optString("recommendedGrade")),
            socialCaption = json.optString(
                "socialCaption",
                if (preferPersian) "ساخته شده با هوش مصنوعی سامی کینگ ✨🎬 #ریلز #سینمایی #هوش_مصنوعی"
                else "Created with Sami King AI ✨🎬 #Reels #Cinematic"
            )
        )
    }
}
