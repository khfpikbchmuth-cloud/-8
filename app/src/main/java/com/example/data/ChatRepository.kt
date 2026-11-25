package com.example.data

import com.example.ai.ChatHistoryTurn
import com.example.ai.InferenceMediaPayload
import com.example.ai.MediaKind
import com.example.ai.SamiAiService
import com.example.ai.SamiModelOption
import com.example.ai.StructuredMediaReport
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatDao: ChatDao,
    private val aiService: SamiAiService
) {
    val conversations: Flow<List<ConversationEntity>> = chatDao.observeConversations()

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        chatDao.observeMessages(conversationId)

    suspend fun createConversation(title: String, modelOption: SamiModelOption): Long {
        val now = System.currentTimeMillis()
        return chatDao.insertConversation(
            ConversationEntity(
                title = title,
                modelId = modelOption.modelId,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateConversationTitle(conversationId: Long, title: String) {
        chatDao.updateConversationHeader(
            id = conversationId,
            title = title,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteConversation(conversationId: Long) {
        chatDao.deleteConversation(conversationId)
    }

    suspend fun clearAllConversations() {
        chatDao.clearAllConversations()
    }

    suspend fun addMessage(
        conversationId: Long,
        role: String,
        text: String,
        mediaKind: MediaKind = MediaKind.NONE,
        mediaLocalPath: String? = null,
        mediaMimeType: String? = null,
        isError: Boolean = false,
        modelOption: SamiModelOption = SamiModelOption.PRO_BALANCED
    ): Long {
        val now = System.currentTimeMillis()
        val id = chatDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                text = text,
                mediaKind = mediaKind.name,
                mediaLocalPath = mediaLocalPath,
                mediaMimeType = mediaMimeType,
                timestamp = now,
                isError = isError
            )
        )
        chatDao.touchConversation(conversationId, now, modelOption.modelId)
        return id
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteMessageById(messageId)
    }

    suspend fun getHistoryTurns(conversationId: Long, excludeLastCount: Int = 0): List<ChatHistoryTurn> {
        val messages = chatDao.getMessagesSnapshot(conversationId)
            .filter { !it.isError }
        val trimmed = if (excludeLastCount > 0 && messages.size > excludeLastCount) {
            messages.dropLast(excludeLastCount)
        } else if (excludeLastCount > 0) {
            emptyList()
        } else {
            messages
        }
        return trimmed.map { ChatHistoryTurn(role = it.role, text = it.text) }
    }

    fun streamReply(
        modelOption: SamiModelOption,
        history: List<ChatHistoryTurn>,
        prompt: String,
        mediaPayload: InferenceMediaPayload?,
        responseStyle: com.example.config.ResponseStyle = com.example.config.ResponseStyle.BALANCED,
        customInstruction: String = "",
        temperature: Float = 0.7f
    ): Flow<String> = aiService.streamChatReply(
        modelOption = modelOption,
        history = history,
        prompt = prompt,
        mediaPayload = mediaPayload,
        responseStyle = responseStyle,
        customInstruction = customInstruction,
        temperature = temperature
    )

    suspend fun analyzeMediaStructured(
        mediaPayload: InferenceMediaPayload,
        customInstruction: String,
        preferPersian: Boolean
    ): StructuredMediaReport = aiService.analyzeMediaStructured(
        mediaPayload = mediaPayload,
        customInstruction = customInstruction,
        preferPersian = preferPersian
    )
}
