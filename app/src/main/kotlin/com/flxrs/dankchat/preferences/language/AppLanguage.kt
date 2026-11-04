package com.flxrs.dankchat.preferences.language

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/**
 * The interface language of the app. Android 13 and newer keep the choice per app themselves,
 * older versions get it from here by wrapping the context of each activity.
 */
object AppLanguage {
    const val SYSTEM = ""

    private const val PREFS_NAME = "app_language"
    private const val KEY_TAG = "tag"

    // One per translation in res, the two languages the fork is written in come first
    val AVAILABLE: List<String> =
        listOf("en", "ru-RU") +
            listOf(
                "ar", "be-BY", "bg", "ca", "cs", "da", "de-DE", "el", "es-ES", "fa-IR", "fi-FI", "fr-FR", "hu-HU", "id-ID", "it", "ja-JP", "kk-KZ", "ko",
                "nl", "no", "or-IN", "pl-PL", "pt-BR", "pt-PT", "ro", "sr", "sv", "tr-TR", "uk-UA", "vi", "zh-CN", "zh-Hant-TW",
            )

    fun current(context: Context): String {
        val tag =
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
                else -> context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TAG, SYSTEM).orEmpty()
            }
        return matchAvailable(tag)
    }

    fun set(
        context: Context,
        tag: String,
    ) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
            }

            else -> {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_TAG, tag) }
                (context as? Activity)?.recreate()
            }
        }
    }

    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return base
        }
        val tag = base.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TAG, SYSTEM).orEmpty()
        if (tag == SYSTEM) {
            return base
        }
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(configuration)
    }

    /** The language in its own words, since someone looking for it may not read the current one. */
    fun displayName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        val name =
            when (locale.language) {
                // These come in two variants that would read the same
                "pt", "zh" -> locale.getDisplayName(locale)
                else -> locale.getDisplayLanguage(locale)
            }
        return name.replaceFirstChar { it.titlecase(locale) }
    }

    // The system settings can hand back a plain language where the list has a regional one
    private fun matchAvailable(tag: String): String {
        val first = tag.substringBefore(',')
        if (first in AVAILABLE) {
            return first
        }
        val language = Locale.forLanguageTag(first).language
        return AVAILABLE.firstOrNull { Locale.forLanguageTag(it).language == language } ?: SYSTEM
    }
}
