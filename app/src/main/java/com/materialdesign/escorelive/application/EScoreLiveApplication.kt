package com.materialdesign.escorelive.application

import android.app.Application
import android.content.Context
import com.materialdesign.escorelive.utils.LocaleManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EScoreLiveApplication : Application() {

    @Inject
    lateinit var localeManager: LocaleManager

    override fun onCreate() {
        super.onCreate()
        try {
            val language = LocaleManager().getLanguage(this)
            LocaleManager().setLocale(this, language)
        } catch (e: Exception) {
            LocaleManager().setLocale(this, LocaleManager.LANGUAGE_ENGLISH)
        }
    }

    override fun attachBaseContext(base: Context?) {
        val context = base?.let {
            LocaleManager().onAttachBaseContext(it)
        } ?: base
        super.attachBaseContext(context)
    }
}