package com.example.reels

enum class ReelMotionStyle(
    val id: String,
    val titleFa: String,
    val titleEn: String
) {
    ZOOM_IN("ZOOM_IN", "زوم سینمایی", "Cinematic Zoom In"),
    PAN_DRIFT("PAN_DRIFT", "حرکت پارالاکس", "Parallax Pan"),
    DRAMATIC_PULL("DRAMATIC_PULL", "عقب‌کشی دراماتیک", "Dramatic Pull Out"),
    ORBIT_PULSE("ORBIT_PULSE", "نبض سینمایی", "Orbit Pulse");

    companion object {
        fun fromId(raw: String?): ReelMotionStyle =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) } ?: ZOOM_IN
    }
}

enum class ReelColorGrade(
    val id: String,
    val titleFa: String,
    val titleEn: String
) {
    GOLDEN_CINEMA("GOLDEN_CINEMA", "طلایی هالیوودی", "Golden Cinema"),
    CYBER_NEON("CYBER_NEON", "سایبر نئون", "Cyber Neon"),
    NOIR_LUXE("NOIR_LUXE", "نوار کلاسیک", "Noir Luxe"),
    WARM_SUNSET("WARM_SUNSET", "غروب گرم", "Warm Sunset"),
    CRISP_STUDIO("CRISP_STUDIO", "استودیویی شفاف", "Crisp Studio");

    companion object {
        fun fromId(raw: String?): ReelColorGrade =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) } ?: GOLDEN_CINEMA
    }
}

data class ReelsDirectorPlan(
    val hookTitle: String,
    val captions: List<String>,
    val recommendedMotion: ReelMotionStyle,
    val recommendedGrade: ReelColorGrade,
    val socialCaption: String
)

data class ReelsRenderConfig(
    val imagePaths: List<String>,
    val hookTitle: String,
    val captions: List<String>,
    val motionStyle: ReelMotionStyle,
    val colorGrade: ReelColorGrade,
    val durationSeconds: Int = 9,
    val includeCinemaBars: Boolean = true,
    val includeLightLeak: Boolean = true
)
