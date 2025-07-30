package com.materialdesign.escorelive.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor() {

    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_AZERBAIJANI = "az"
        const val PREF_SELECTED_LANGUAGE = "selected_language"
    }

    fun setLocale(context: Context, language: String): Context {
        persistLanguage(context, language)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, language)
        } else {
            updateResourcesLegacy(context, language)
        }
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return prefs.getString(PREF_SELECTED_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    private fun persistLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_SELECTED_LANGUAGE, language).apply()
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }

    private fun updateResourcesLegacy(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }

    fun onAttachBaseContext(context: Context): Context {
        val language = getLanguage(context)
        return setLocale(context, language)
    }

    fun restartActivity(activity: Activity) {
        activity.recreate()
    }

    fun getAvailableLanguages(): List<LanguageItem> {
        return listOf(
            LanguageItem(LANGUAGE_ENGLISH, "English", "🇬🇧"),
            LanguageItem(LANGUAGE_AZERBAIJANI, "Azərbaycan", "🇦🇿")
        )
    }

    fun getLanguageDisplayName(context: Context, languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_ENGLISH -> "English"
            LANGUAGE_AZERBAIJANI -> "Azərbaycan"
            else -> "English"
        }
    }

    fun isRTL(language: String): Boolean {
        return false
    }
}

data class LanguageItem(
    val code: String,
    val name: String,
    val flag: String
)

object LocaleHelper {
    fun setLocale(context: Context, language: String): Context {
        return LocaleManager().setLocale(context, language)
    }

    fun getLanguage(context: Context): String {
        return LocaleManager().getLanguage(context)
    }

    fun onAttachBaseContext(context: Context): Context {
        return LocaleManager().onAttachBaseContext(context)
    }
}