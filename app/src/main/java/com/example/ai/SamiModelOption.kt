package com.example.ai

enum class SamiModelOption(
    val modelId: String,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val badgeFa: String
) {
    PRO_BALANCED(
        modelId = "gemini-3.8-flash",
        titleFa = "سامی کینگ پرو (۳.۸ فلش)",
        titleEn = "Sami King Pro (3.8 Flash)",
        descriptionFa = "هوش بالا، پاسخ به همه سوالات، تحلیل تصویر، ویدیو و کدنویسی",
        descriptionEn = "High intelligence for Q&A, vision, video & coding",
        badgeFa = "پیشنهادی • رایگان"
    ),
    TURBO_FAST(
        modelId = "gemini-3.5-flash-lite",
        titleFa = "سامی کینگ توربو (۳.۵ لایت)",
        titleEn = "Sami King Turbo (3.5 Lite)",
        descriptionFa = "پاسخ‌گویی فوق‌سریع برای سوالات روزمره، خلاصه و ترجمه",
        descriptionEn = "Ultra-fast answers for everyday questions & summaries",
        badgeFa = "سریع‌ترین • رایگان"
    );

    companion object {
        fun fromId(id: String?): SamiModelOption =
            entries.firstOrNull { it.modelId == id } ?: PRO_BALANCED
    }
}
