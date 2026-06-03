package com.porarrirr.sumahohikakuku

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object AppLanguageController {
    private const val PREFS_NAME = "app_language_settings"
    private const val LANGUAGE_OVERRIDE_KEY = "app_language_override"

    fun getLanguageTag(context: Context): String {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_OVERRIDE_KEY, "")
            .orEmpty()
    }

    fun setLanguageTag(activity: Activity, languageTag: String) {
        val normalized = normalizeLanguageTag(languageTag)
        val current = getLanguageTag(activity)
        if (current == normalized) return

        activity
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_OVERRIDE_KEY, normalized)
            .apply()
        activity.recreate()
    }

    fun wrap(base: Context): Context {
        val languageTag = getLanguageTag(base)
        if (languageTag.isBlank()) {
            Locale.setDefault(base.resources.configuration.locales.get(0))
            return base
        }

        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)

        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(LocaleList(locale))
        return base.createConfigurationContext(configuration)
    }

    private fun normalizeLanguageTag(languageTag: String): String {
        return when (languageTag) {
            "", "ja", "en", "zh-Hans", "zh-Hant" -> languageTag
            else -> error("Unsupported language tag: $languageTag")
        }
    }
}
