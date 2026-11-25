package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ai.MediaKind
import com.example.ai.SamiModelOption

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val modelId: String = SamiModelOption.PRO_BALANCED.modelId,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val conversationId: Long,
    val role: String, // "user" or "model"
    val text: String,
    val mediaKind: String = MediaKind.NONE.name,
    val mediaLocalPath: String? = null,
    val mediaMimeType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)
