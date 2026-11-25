package com.example.data

import android.content.Context
import com.example.ai.SamiModelOption
import com.example.config.ResponseStyle
import com.example.config.SamiConfigState
import com.example.config.SamiFeatureModule
import com.example.config.SamiOwnerBuildConfig
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SamiPreferencesRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadInitialState())
    val config: StateFlow<SamiConfigState> = _config.asStateFlow()

    private fun loadInitialState(): SamiConfigState {
        val modelId = prefs.getString(KEY_MODEL_ID, SamiModelOption.PRO_BALANCED.modelId)
        val themeName = prefs.getString(KEY_THEME, ThemePreference.DARK.name)
        val isPersian = prefs.getBoolean(KEY_PERSIAN_UI, true)
        val customInstruction = prefs.getString(KEY_CUSTOM_INSTRUCTION, "") ?: ""
        val responseStyleId = prefs.getString(KEY_RESPONSE_STYLE, ResponseStyle.BALANCED.id)
        val temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f).coerceIn(0.1f, 1.3f)
        val autoSpeak = prefs.getBoolean(KEY_AUTO_SPEAK, false)
        val ownerUnlocked = SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT &&
            prefs.getBoolean(KEY_OWNER_UNLOCKED, false)

        val storedModules = prefs.getStringSet(KEY_ENABLED_MODULES, null)
        val enabledModules = if (storedModules == null) {
            SamiFeatureModule.entries.filter { it.defaultEnabled }.toSet()
        } else {
            SamiFeatureModule.entries.filter { it.id in storedModules }.toSet()
        }

        return SamiConfigState(
            selectedModel = SamiModelOption.fromId(modelId),
            themePreference = runCatching { ThemePreference.valueOf(themeName ?: "") }
                .getOrDefault(ThemePreference.DARK),
            isPersianUi = isPersian,
            customInstruction = customInstruction,
            responseStyle = ResponseStyle.fromId(responseStyleId),
            creativityTemperature = temperature,
            autoSpeakReplies = autoSpeak,
            enabledModules = enabledModules,
            isOwnerUnlocked = ownerUnlocked
        )
    }

    fun verifyAndUnlockOwner(inputPin: String): Boolean {
        if (!SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT) return false
        val expectedPin = prefs.getString(KEY_OWNER_PIN, SamiOwnerBuildConfig.DEFAULT_OWNER_PIN)
            ?: SamiOwnerBuildConfig.DEFAULT_OWNER_PIN
        val matched = inputPin.trim() == expectedPin
        if (matched) {
            prefs.edit().putBoolean(KEY_OWNER_UNLOCKED, true).apply()
            _config.value = _config.value.copy(isOwnerUnlocked = true)
        }
        return matched
    }

    fun lockOwnerMode() {
        prefs.edit().putBoolean(KEY_OWNER_UNLOCKED, false).apply()
        _config.value = _config.value.copy(isOwnerUnlocked = false)
    }

    fun updateOwnerPin(newPin: String): Boolean {
        val clean = newPin.trim()
        if (clean.length < 4) return false
        prefs.edit().putString(KEY_OWNER_PIN, clean).apply()
        return true
    }

    fun setModel(option: SamiModelOption) {
        prefs.edit().putString(KEY_MODEL_ID, option.modelId).apply()
        _config.value = _config.value.copy(selectedModel = option)
    }

    fun setThemePreference(preference: ThemePreference) {
        prefs.edit().putString(KEY_THEME, preference.name).apply()
        _config.value = _config.value.copy(themePreference = preference)
    }

    fun setPersianUi(persian: Boolean) {
        prefs.edit().putBoolean(KEY_PERSIAN_UI, persian).apply()
        _config.value = _config.value.copy(isPersianUi = persian)
    }

    fun setCustomInstruction(instruction: String) {
        prefs.edit().putString(KEY_CUSTOM_INSTRUCTION, instruction).apply()
        _config.value = _config.value.copy(customInstruction = instruction)
    }

    fun setResponseStyle(style: ResponseStyle) {
        prefs.edit().putString(KEY_RESPONSE_STYLE, style.id).apply()
        _config.value = _config.value.copy(responseStyle = style)
    }

    fun setCreativityTemperature(value: Float) {
        val clamped = value.coerceIn(0.1f, 1.3f)
        prefs.edit().putFloat(KEY_TEMPERATURE, clamped).apply()
        _config.value = _config.value.copy(creativityTemperature = clamped)
    }

    fun setAutoSpeakReplies(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply()
        _config.value = _config.value.copy(autoSpeakReplies = enabled)
    }

    fun setModuleEnabled(module: SamiFeatureModule, enabled: Boolean) {
        val next = _config.value.enabledModules.toMutableSet()
        if (enabled) next.add(module) else next.remove(module)
        prefs.edit().putStringSet(KEY_ENABLED_MODULES, next.map { it.id }.toSet()).apply()
        _config.value = _config.value.copy(enabledModules = next)
    }

    fun resetDefaultsKeepOwnerPin() {
        val savedPin = prefs.getString(KEY_OWNER_PIN, SamiOwnerBuildConfig.DEFAULT_OWNER_PIN)
        val unlocked = _config.value.isOwnerUnlocked
        prefs.edit().clear().apply()
        prefs.edit()
            .putString(KEY_OWNER_PIN, savedPin)
            .putBoolean(KEY_OWNER_UNLOCKED, unlocked)
            .apply()
        _config.value = SamiConfigState(isOwnerUnlocked = unlocked)
    }

    companion object {
        private const val PREFS_NAME = "sami_king_prefs"
        private const val KEY_MODEL_ID = "model_id"
        private const val KEY_THEME = "theme_pref"
        private const val KEY_PERSIAN_UI = "persian_ui"
        private const val KEY_CUSTOM_INSTRUCTION = "custom_instruction"
        private const val KEY_RESPONSE_STYLE = "response_style"
        private const val KEY_TEMPERATURE = "creativity_temp"
        private const val KEY_AUTO_SPEAK = "auto_speak"
        private const val KEY_ENABLED_MODULES = "enabled_modules"
        private const val KEY_OWNER_PIN = "owner_pin"
        private const val KEY_OWNER_UNLOCKED = "owner_unlocked"
    }
}
