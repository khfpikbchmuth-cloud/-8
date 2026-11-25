package com.example.config

import com.example.ai.SamiModelOption
import com.example.ui.theme.ThemePreference

/**
 * Owner build configuration:
 * - Set [ENABLE_OWNER_MANAGEMENT] to `true` for your personal Owner APK.
 * - Set [ENABLE_OWNER_MANAGEMENT] to `false` before exporting/sharing a public APK
 *   if you want zero owner/management UI or unlock triggers compiled into the shared app.
 */
object SamiOwnerBuildConfig {
    const val ENABLE_OWNER_MANAGEMENT: Boolean = true
    const val DEFAULT_OWNER_PIN: String = "7799"
}

enum class ResponseStyle(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val promptHint: String
) {
    CONCISE(
        id = "CONCISE",
        titleFa = "کوتاه و سریع",
        titleEn = "Concise",
        promptHint = "Keep your answers concise, direct, and to the point unless details are explicitly requested."
    ),
    BALANCED(
        id = "BALANCED",
        titleFa = "متعادل",
        titleEn = "Balanced",
        promptHint = "Provide clear, well-structured, balanced answers with helpful examples."
    ),
    DETAILED(
        id = "DETAILED",
        titleFa = "کامل و تخصصی",
        titleEn = "Detailed",
        promptHint = "Provide thorough, in-depth, step-by-step explanations covering edge cases and practical examples."
    );

    companion object {
        fun fromId(id: String?): ResponseStyle =
            entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

/**
 * Central registry of modular capabilities in Sami King.
 * Add new feature modules here to expose them in Owner Management & Home Workspace.
 */
enum class SamiFeatureModule(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val defaultEnabled: Boolean = true
) {
    REELS_STUDIO(
        id = "reels_studio",
        titleFa = "ویدیوساز سینمایی ریلز",
        titleEn = "Cinematic Reels Maker",
        descriptionFa = "تبدیل تصویر به ویدیوی ۹:۱۶ با حرکت دوربین و اصلاح رنگ",
        descriptionEn = "Turn photos into 9:16 MP4 Reels with motion and color grading"
    ),
    MEDIA_LAB(
        id = "media_lab",
        titleFa = "استودیو تحلیل تصویر و ویدیو",
        titleEn = "Media Analyzer Studio",
        descriptionFa = "استخراج متن، خلاصه و نکات کلیدی از عکس و ویدیو",
        descriptionEn = "Structured OCR, highlights, and summaries from photos & videos"
    ),
    VOICE_TOOLS(
        id = "voice_tools",
        titleFa = "ابزارهای صوتی (ورودی و خواندن متن)",
        titleEn = "Voice Input & Read Aloud",
        descriptionFa = "امکان تایپ صوتی و خواندن خودکار یا دستی پاسخ‌ها",
        descriptionEn = "Voice dictation and Text-to-Speech playback for answers"
    )
}

data class SamiConfigState(
    val selectedModel: SamiModelOption = SamiModelOption.PRO_BALANCED,
    val themePreference: ThemePreference = ThemePreference.DARK,
    val isPersianUi: Boolean = true,
    val customInstruction: String = "",
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val creativityTemperature: Float = 0.7f,
    val autoSpeakReplies: Boolean = false,
    val enabledModules: Set<SamiFeatureModule> = SamiFeatureModule.entries.toSet(),
    val isOwnerUnlocked: Boolean = false
)
