package com.example.ui

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.InferenceMediaPayload
import com.example.ai.MediaAttachmentProcessor
import com.example.ai.MediaKind
import com.example.ai.PreparedAttachment
import com.example.ai.SamiAiService
import com.example.ai.SamiModelOption
import com.example.ai.StructuredMediaReport
import com.example.config.ResponseStyle
import com.example.config.SamiConfigState
import com.example.config.SamiFeatureModule
import com.example.data.ChatRepository
import com.example.data.ConversationEntity
import com.example.data.MessageEntity
import com.example.data.SamiDatabase
import com.example.data.SamiPreferencesRepository
import com.example.reels.CinematicReelsRenderer
import com.example.reels.ReelColorGrade
import com.example.reels.ReelMotionStyle
import com.example.reels.ReelsRenderConfig
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private const val TAG = "SamiViewModel"

sealed interface InferenceUiState {
    data object Idle : InferenceUiState
    data object Loading : InferenceUiState
    data class Streaming(val text: String) : InferenceUiState
    data class Error(val message: String) : InferenceUiState
}

sealed interface MediaLabUiState {
    data object Idle : MediaLabUiState
    data class Analyzing(val attachment: PreparedAttachment) : MediaLabUiState
    data class Success(
        val attachment: PreparedAttachment,
        val report: StructuredMediaReport
    ) : MediaLabUiState
    data class Error(val message: String, val attachment: PreparedAttachment?) : MediaLabUiState
}

data class ReelsStudioUiState(
    val selectedPhotos: List<PreparedAttachment> = emptyList(),
    val conceptPrompt: String = "",
    val hookTitle: String = "شکوه در هر فریم",
    val captionLine1: String = "جزئیاتی که داستان می‌سازند",
    val captionLine2: String = "نور، حرکت و حس سینمایی",
    val captionLine3: String = "ساخته شده با سامی کینگ",
    val socialCaption: String = "ساخته شده با استودیو ریلز سامی کینگ ✨🎬 #ریلز #سینمایی #سامی_کینگ",
    val motionStyle: ReelMotionStyle = ReelMotionStyle.ZOOM_IN,
    val colorGrade: ReelColorGrade = ReelColorGrade.GOLDEN_CINEMA,
    val durationSeconds: Int = 9,
    val includeCinemaBars: Boolean = true,
    val includeLightLeak: Boolean = true,
    val isDirectingWithAi: Boolean = false,
    val isRendering: Boolean = false,
    val renderProgress: Float = 0f,
    val renderedVideoPath: String? = null,
    val savedToGallery: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class SamiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SamiDatabase.getInstance(application)
    private val aiService = SamiAiService()
    private val repository = ChatRepository(database.chatDao(), aiService)
    private val preferencesRepository = SamiPreferencesRepository(application)
    val mediaProcessor = MediaAttachmentProcessor(application)
    private val reelsRenderer = CinematicReelsRenderer(application)

    val configState: StateFlow<SamiConfigState> = preferencesRepository.config

    val selectedModel: StateFlow<SamiModelOption> = configState
        .map { it.selectedModel }
        .stateIn(viewModelScope, SharingStarted.Eagerly, configState.value.selectedModel)

    val themePreference: StateFlow<ThemePreference> = configState
        .map { it.themePreference }
        .stateIn(viewModelScope, SharingStarted.Eagerly, configState.value.themePreference)

    val isPersianUi: StateFlow<Boolean> = configState
        .map { it.isPersianUi }
        .stateIn(viewModelScope, SharingStarted.Eagerly, configState.value.isPersianUi)

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId == null) flowOf(emptyList()) else repository.observeMessages(convId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    private val _inferenceState = MutableStateFlow<InferenceUiState>(InferenceUiState.Idle)
    val inferenceState: StateFlow<InferenceUiState> = _inferenceState.asStateFlow()

    private val _pendingAttachment = MutableStateFlow<PreparedAttachment?>(null)
    val pendingAttachment: StateFlow<PreparedAttachment?> = _pendingAttachment.asStateFlow()

    private val _isPreparingMedia = MutableStateFlow(false)
    val isPreparingMedia: StateFlow<Boolean> = _isPreparingMedia.asStateFlow()

    private val _mediaLabState = MutableStateFlow<MediaLabUiState>(MediaLabUiState.Idle)
    val mediaLabState: StateFlow<MediaLabUiState> = _mediaLabState.asStateFlow()

    private val _reelsState = MutableStateFlow(ReelsStudioUiState())
    val reelsState: StateFlow<ReelsStudioUiState> = _reelsState.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    private var activeGenerationJob: Job? = null
    private var activeReelRenderJob: Job? = null
    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(getApplication()) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            _speakingMessageId.value = null
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _speakingMessageId.value = null
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TextToSpeech init failed", e)
        }
    }

    fun toggleSpeakMessage(messageId: Long, text: String) {
        val engine = tts ?: return
        if (!ttsReady) return

        if (_speakingMessageId.value == messageId) {
            engine.stop()
            _speakingMessageId.value = null
            return
        }

        val hasPersianChars = text.any { it in '\u0600'..'\u06FF' }
        val targetLocale = if (hasPersianChars) Locale("fa", "IR") else Locale.US
        val langResult = engine.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.US
        }
        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace("**", "")
            .replace("###", "")
            .replace("##", "")
            .trim()
        _speakingMessageId.value = messageId
        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, messageId.toString())
    }

    // --- Settings & Management Actions ---

    fun selectModel(option: SamiModelOption) = preferencesRepository.setModel(option)

    fun setThemePreference(preference: ThemePreference) = preferencesRepository.setThemePreference(preference)

    fun setPersianUi(persian: Boolean) = preferencesRepository.setPersianUi(persian)

    fun setCustomInstruction(instruction: String) = preferencesRepository.setCustomInstruction(instruction)

    fun setResponseStyle(style: ResponseStyle) = preferencesRepository.setResponseStyle(style)

    fun setCreativityTemperature(value: Float) = preferencesRepository.setCreativityTemperature(value)

    fun setAutoSpeakReplies(enabled: Boolean) = preferencesRepository.setAutoSpeakReplies(enabled)

    fun setModuleEnabled(module: SamiFeatureModule, enabled: Boolean) =
        preferencesRepository.setModuleEnabled(module, enabled)

    fun resetSettingsToDefaults() = preferencesRepository.resetDefaultsKeepOwnerPin()

    fun unlockOwnerMode(pin: String): Boolean = preferencesRepository.verifyAndUnlockOwner(pin)

    fun lockOwnerMode() = preferencesRepository.lockOwnerMode()

    fun updateOwnerPin(newPin: String): Boolean = preferencesRepository.updateOwnerPin(newPin)

    // --- Chat Actions ---

    fun startNewChat() {
        activeGenerationJob?.cancel()
        _activeConversationId.value = null
        _inferenceState.value = InferenceUiState.Idle
        _pendingAttachment.value = null
    }

    fun selectConversation(conversation: ConversationEntity) {
        activeGenerationJob?.cancel()
        _activeConversationId.value = conversation.id
        preferencesRepository.setModel(SamiModelOption.fromId(conversation.modelId))
        _inferenceState.value = InferenceUiState.Idle
        _pendingAttachment.value = null
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            if (_activeConversationId.value == conversationId) {
                startNewChat()
            }
            repository.deleteConversation(conversationId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            startNewChat()
            repository.clearAllConversations()
        }
    }

    fun attachMedia(uri: Uri, kind: MediaKind) {
        viewModelScope.launch {
            _isPreparingMedia.value = true
            try {
                val prepared = mediaProcessor.prepareAttachment(uri, kind)
                _pendingAttachment.value = prepared
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prepare attachment", e)
                _inferenceState.value = InferenceUiState.Error(
                    e.localizedMessage ?: "خطا در آماده‌سازی فایل رسانه‌ای."
                )
            } finally {
                _isPreparingMedia.value = false
            }
        }
    }

    fun clearPendingAttachment() {
        _pendingAttachment.value = null
    }

    fun dismissError() {
        if (_inferenceState.value is InferenceUiState.Error) {
            _inferenceState.value = InferenceUiState.Idle
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        val current = _inferenceState.value
        if (current is InferenceUiState.Streaming && current.text.isNotBlank()) {
            val convId = _activeConversationId.value
            val model = configState.value.selectedModel
            if (convId != null) {
                viewModelScope.launch {
                    repository.addMessage(
                        conversationId = convId,
                        role = "model",
                        text = current.text,
                        modelOption = model
                    )
                }
            }
        }
        _inferenceState.value = InferenceUiState.Idle
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        val attachment = _pendingAttachment.value
        if (prompt.isBlank() && attachment == null) return

        activeGenerationJob?.cancel()
        _pendingAttachment.value = null

        activeGenerationJob = viewModelScope.launch {
            _inferenceState.value = InferenceUiState.Loading
            val cfg = configState.value
            val modelOption = cfg.selectedModel
            var targetConvId = _activeConversationId.value

            try {
                if (targetConvId == null) {
                    val title = buildConversationTitle(prompt, attachment)
                    targetConvId = repository.createConversation(title, modelOption)
                    _activeConversationId.value = targetConvId
                }

                val displayPrompt = prompt.ifBlank {
                    when (attachment?.kind) {
                        MediaKind.IMAGE -> "این تصویر را تحلیل کن"
                        MediaKind.VIDEO -> "این ویدیو را تحلیل کن"
                        else -> ""
                    }
                }

                repository.addMessage(
                    conversationId = targetConvId,
                    role = "user",
                    text = displayPrompt,
                    mediaKind = attachment?.kind ?: MediaKind.NONE,
                    mediaLocalPath = attachment?.localPreviewPath,
                    mediaMimeType = attachment?.mimeType,
                    modelOption = modelOption
                )

                val history = repository.getHistoryTurns(targetConvId, excludeLastCount = 1)
                val payload = if (attachment != null) {
                    mediaProcessor.loadInferencePayload(
                        localPath = attachment.localPreviewPath,
                        kind = attachment.kind,
                        mimeType = attachment.mimeType
                    )
                } else {
                    null
                }

                val builder = StringBuilder()
                try {
                    repository.streamReply(
                        modelOption = modelOption,
                        history = history,
                        prompt = displayPrompt,
                        mediaPayload = payload,
                        responseStyle = cfg.responseStyle,
                        customInstruction = cfg.customInstruction,
                        temperature = cfg.creativityTemperature
                    ).collect { chunk ->
                        builder.append(chunk)
                        _inferenceState.value = InferenceUiState.Streaming(builder.toString())
                    }

                    val fullReply = builder.toString().trim()
                    if (fullReply.isNotEmpty()) {
                        val savedId = repository.addMessage(
                            conversationId = targetConvId,
                            role = "model",
                            text = fullReply,
                            modelOption = modelOption
                        )
                        _inferenceState.value = InferenceUiState.Idle
                        if (cfg.autoSpeakReplies && SamiFeatureModule.VOICE_TOOLS in cfg.enabledModules) {
                            toggleSpeakMessage(savedId, fullReply)
                        }
                    } else {
                        _inferenceState.value = InferenceUiState.Error("مدل پاسخی برنگرداند.")
                    }
                } catch (e: TimeoutCancellationException) {
                    val partial = builder.toString().trim()
                    if (partial.isNotEmpty()) {
                        Log.w(TAG, "Stream timed out after receiving partial text", e)
                        repository.addMessage(
                            conversationId = targetConvId,
                            role = "model",
                            text = partial,
                            modelOption = modelOption
                        )
                        _inferenceState.value = InferenceUiState.Idle
                    } else {
                        throw e
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI inference timed out", e)
                val msg = "زمان پاسخ‌گویی به پایان رسید. لطفاً اتصال اینترنت را بررسی کرده و دوباره تلاش کنید."
                targetConvId?.let {
                    repository.addMessage(
                        conversationId = it,
                        role = "model",
                        text = msg,
                        isError = true,
                        modelOption = modelOption
                    )
                }
                _inferenceState.value = InferenceUiState.Error(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI inference failed", e)
                val msg = e.localizedMessage ?: "خطا در برقراری ارتباط با هوش مصنوعی."
                targetConvId?.let {
                    repository.addMessage(
                        conversationId = it,
                        role = "model",
                        text = msg,
                        isError = true,
                        modelOption = modelOption
                    )
                }
                _inferenceState.value = InferenceUiState.Error(msg)
            }
        }
    }

    fun regenerateLastResponse() {
        val convId = _activeConversationId.value ?: return
        val currentMessages = messages.value
        val lastUserMsg = currentMessages.lastOrNull { it.role == "user" } ?: return

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            _inferenceState.value = InferenceUiState.Loading
            val cfg = configState.value
            val modelOption = cfg.selectedModel
            try {
                val indexOfLastUser = currentMessages.indexOfLast { it.id == lastUserMsg.id }
                if (indexOfLastUser >= 0) {
                    currentMessages.drop(indexOfLastUser + 1).forEach { trailing ->
                        repository.deleteMessage(trailing.id)
                    }
                }

                val history = repository.getHistoryTurns(convId, excludeLastCount = 1)
                val kind = runCatching { MediaKind.valueOf(lastUserMsg.mediaKind) }
                    .getOrDefault(MediaKind.NONE)
                val payload = if (kind != MediaKind.NONE && !lastUserMsg.mediaLocalPath.isNullOrBlank()) {
                    mediaProcessor.loadInferencePayload(
                        localPath = lastUserMsg.mediaLocalPath,
                        kind = kind,
                        mimeType = lastUserMsg.mediaMimeType.orEmpty()
                    )
                } else {
                    null
                }

                val builder = StringBuilder()
                try {
                    repository.streamReply(
                        modelOption = modelOption,
                        history = history,
                        prompt = lastUserMsg.text,
                        mediaPayload = payload,
                        responseStyle = cfg.responseStyle,
                        customInstruction = cfg.customInstruction,
                        temperature = cfg.creativityTemperature
                    ).collect { chunk ->
                        builder.append(chunk)
                        _inferenceState.value = InferenceUiState.Streaming(builder.toString())
                    }

                    val fullReply = builder.toString().trim()
                    if (fullReply.isNotEmpty()) {
                        repository.addMessage(
                            conversationId = convId,
                            role = "model",
                            text = fullReply,
                            modelOption = modelOption
                        )
                        _inferenceState.value = InferenceUiState.Idle
                    } else {
                        _inferenceState.value = InferenceUiState.Error("مدل پاسخی برنگرداند.")
                    }
                } catch (e: TimeoutCancellationException) {
                    val partial = builder.toString().trim()
                    if (partial.isNotEmpty()) {
                        Log.w(TAG, "Regenerate stream timed out after partial text", e)
                        repository.addMessage(
                            conversationId = convId,
                            role = "model",
                            text = partial,
                            modelOption = modelOption
                        )
                        _inferenceState.value = InferenceUiState.Idle
                    } else {
                        throw e
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Regenerate timed out", e)
                _inferenceState.value = InferenceUiState.Error("زمان پاسخ‌گویی به پایان رسید.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Regenerate failed", e)
                _inferenceState.value = InferenceUiState.Error(
                    e.localizedMessage ?: "تلاش مجدد ناموفق بود."
                )
            }
        }
    }

    fun analyzeMediaInLab(uri: Uri, kind: MediaKind, focusInstruction: String) {
        viewModelScope.launch {
            var prepared: PreparedAttachment? = null
            try {
                _isPreparingMedia.value = true
                prepared = mediaProcessor.prepareAttachment(uri, kind)
                _isPreparingMedia.value = false
                _mediaLabState.value = MediaLabUiState.Analyzing(prepared)

                val payload = mediaProcessor.loadInferencePayload(
                    localPath = prepared.localPreviewPath,
                    kind = prepared.kind,
                    mimeType = prepared.mimeType
                ) ?: error("خطا در خواندن فایل رسانه")

                val report = repository.analyzeMediaStructured(
                    mediaPayload = payload,
                    customInstruction = focusInstruction,
                    preferPersian = configState.value.isPersianUi
                )
                _mediaLabState.value = MediaLabUiState.Success(
                    attachment = prepared,
                    report = report
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Media lab analysis timed out", e)
                _mediaLabState.value = MediaLabUiState.Error(
                    message = "تحلیل رسانه بیش از حد طول کشید. لطفاً دوباره امتحان کنید.",
                    attachment = prepared
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Media lab analysis failed", e)
                _mediaLabState.value = MediaLabUiState.Error(
                    message = e.localizedMessage ?: "تحلیل رسانه ناموفق بود.",
                    attachment = prepared
                )
            } finally {
                _isPreparingMedia.value = false
            }
        }
    }

    fun resetMediaLab() {
        _mediaLabState.value = MediaLabUiState.Idle
    }

    fun continueLabReportInChat(attachment: PreparedAttachment, followUpQuestion: String) {
        startNewChat()
        _pendingAttachment.value = attachment
        sendMessage(followUpQuestion)
    }

    // --- Cinematic Reels Video Maker ---

    fun addReelsPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isPreparingMedia.value = true
            try {
                val added = uris.take(5).map { uri ->
                    mediaProcessor.prepareAttachment(uri, MediaKind.IMAGE)
                }
                val merged = (_reelsState.value.selectedPhotos + added).take(5)
                _reelsState.value = _reelsState.value.copy(
                    selectedPhotos = merged,
                    renderedVideoPath = null,
                    savedToGallery = false,
                    errorMessage = null,
                    statusMessage = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load Reels photo", e)
                _reelsState.value = _reelsState.value.copy(
                    errorMessage = e.localizedMessage ?: "خطا در افزودن تصویر."
                )
            } finally {
                _isPreparingMedia.value = false
            }
        }
    }

    fun removeReelsPhoto(index: Int) {
        val current = _reelsState.value.selectedPhotos.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _reelsState.value = _reelsState.value.copy(
                selectedPhotos = current,
                renderedVideoPath = null,
                savedToGallery = false
            )
        }
    }

    fun updateReelsConcept(text: String) {
        _reelsState.value = _reelsState.value.copy(conceptPrompt = text)
    }

    fun updateReelsText(hook: String, line1: String, line2: String, line3: String) {
        _reelsState.value = _reelsState.value.copy(
            hookTitle = hook,
            captionLine1 = line1,
            captionLine2 = line2,
            captionLine3 = line3
        )
    }

    fun selectReelsMotion(motion: ReelMotionStyle) {
        _reelsState.value = _reelsState.value.copy(motionStyle = motion)
    }

    fun selectReelsColorGrade(grade: ReelColorGrade) {
        _reelsState.value = _reelsState.value.copy(colorGrade = grade)
    }

    fun selectReelsDuration(seconds: Int) {
        _reelsState.value = _reelsState.value.copy(durationSeconds = seconds)
    }

    fun toggleReelsCinemaBars(enabled: Boolean) {
        _reelsState.value = _reelsState.value.copy(includeCinemaBars = enabled)
    }

    fun toggleReelsLightLeak(enabled: Boolean) {
        _reelsState.value = _reelsState.value.copy(includeLightLeak = enabled)
    }

    fun directAndRenderReel() {
        val state = _reelsState.value
        val persian = configState.value.isPersianUi
        if (state.selectedPhotos.isEmpty()) {
            _reelsState.value = state.copy(
                errorMessage = if (persian) "ابتدا حداقل یک تصویر انتخاب کنید." else "Please select at least one photo."
            )
            return
        }

        activeReelRenderJob?.cancel()
        activeReelRenderJob = viewModelScope.launch {
            _reelsState.value = _reelsState.value.copy(
                isDirectingWithAi = true,
                isRendering = false,
                renderProgress = 0f,
                renderedVideoPath = null,
                savedToGallery = false,
                errorMessage = null,
                statusMessage = if (persian) "سامی کینگ در حال کارگردانی صحنه..." else "Sami King AI is directing your Reel..."
            )

            try {
                val payloads = state.selectedPhotos.mapNotNull { att ->
                    mediaProcessor.loadInferencePayload(
                        att.localPreviewPath,
                        MediaKind.IMAGE,
                        att.mimeType
                    ) as? InferenceMediaPayload.ImagePayload
                }
                if (payloads.isNotEmpty()) {
                    val plan = aiService.directReelsPlan(
                        imagePayloads = payloads,
                        userConcept = state.conceptPrompt,
                        preferPersian = persian
                    )
                    _reelsState.value = _reelsState.value.copy(
                        hookTitle = plan.hookTitle,
                        captionLine1 = plan.captions.getOrElse(0) { _reelsState.value.captionLine1 },
                        captionLine2 = plan.captions.getOrElse(1) { _reelsState.value.captionLine2 },
                        captionLine3 = plan.captions.getOrElse(2) { _reelsState.value.captionLine3 },
                        motionStyle = plan.recommendedMotion,
                        colorGrade = plan.recommendedGrade,
                        socialCaption = plan.socialCaption
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI Reels director unavailable, proceeding with custom settings", e)
            }

            renderCurrentConfigInternal()
        }
    }

    fun renderReelWithCurrentSettings() {
        val state = _reelsState.value
        val persian = configState.value.isPersianUi
        if (state.selectedPhotos.isEmpty()) {
            _reelsState.value = state.copy(
                errorMessage = if (persian) "ابتدا حداقل یک تصویر انتخاب کنید." else "Please select at least one photo."
            )
            return
        }

        activeReelRenderJob?.cancel()
        activeReelRenderJob = viewModelScope.launch {
            renderCurrentConfigInternal()
        }
    }

    private suspend fun renderCurrentConfigInternal() {
        val current = _reelsState.value
        val persian = configState.value.isPersianUi
        _reelsState.value = current.copy(
            isDirectingWithAi = false,
            isRendering = true,
            renderProgress = 0.02f,
            renderedVideoPath = null,
            savedToGallery = false,
            errorMessage = null,
            statusMessage = if (persian) "در حال ساخت ویدیو ریلز سینمایی (MP4)..." else "Rendering 9:16 Cinematic Reels MP4..."
        )

        try {
            val config = ReelsRenderConfig(
                imagePaths = current.selectedPhotos.map { it.localPreviewPath },
                hookTitle = current.hookTitle,
                captions = listOf(current.captionLine1, current.captionLine2, current.captionLine3)
                    .filter { it.isNotBlank() },
                motionStyle = current.motionStyle,
                colorGrade = current.colorGrade,
                durationSeconds = current.durationSeconds,
                includeCinemaBars = current.includeCinemaBars,
                includeLightLeak = current.includeLightLeak
            )
            val mp4File = reelsRenderer.renderReelMp4(config) { progress ->
                _reelsState.value = _reelsState.value.copy(renderProgress = progress)
            }
            _reelsState.value = _reelsState.value.copy(
                isRendering = false,
                renderProgress = 1f,
                renderedVideoPath = mp4File.absolutePath,
                statusMessage = if (persian) "ویدیو ریلز سینمایی آماده است!" else "Your Cinematic Reel is ready!"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Reels MP4 rendering failed", e)
            _reelsState.value = _reelsState.value.copy(
                isRendering = false,
                errorMessage = e.localizedMessage ?: "خطا در ساخت ویدیو."
            )
        }
    }

    fun saveRenderedReelToGallery() {
        val path = _reelsState.value.renderedVideoPath ?: return
        val file = File(path)
        if (!file.exists()) return
        val persian = configState.value.isPersianUi

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, "SamiKing_Reel_${System.currentTimeMillis()}.mp4")
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                MediaStore.Video.Media.RELATIVE_PATH,
                                Environment.DIRECTORY_MOVIES + "/SamiKing"
                            )
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                    }
                    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("Unable to create gallery entry")
                    resolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    } ?: error("Unable to write video stream")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }
                }
                _reelsState.value = _reelsState.value.copy(
                    savedToGallery = true,
                    statusMessage = if (persian) "ویدیو در گالری (Movies/SamiKing) ذخیره شد." else "Saved to Gallery (Movies/SamiKing)."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving reel to gallery", e)
                _reelsState.value = _reelsState.value.copy(
                    errorMessage = e.localizedMessage ?: "خطا در ذخیره در گالری."
                )
            }
        }
    }

    private fun buildConversationTitle(prompt: String, attachment: PreparedAttachment?): String {
        val clean = prompt.replace("\n", " ").trim()
        if (clean.isNotEmpty()) {
            return if (clean.length > 36) clean.take(36) + "…" else clean
        }
        return when (attachment?.kind) {
            MediaKind.IMAGE -> "تحلیل تصویر"
            MediaKind.VIDEO -> "تحلیل ویدیو"
            else -> "گفتگوی جدید"
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
