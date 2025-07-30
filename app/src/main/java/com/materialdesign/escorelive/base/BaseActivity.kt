package com.materialdesign.escorelive.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.materialdesign.escorelive.utils.LocaleManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var localeManager: LocaleManager

    override fun attachBaseContext(newBase: Context?) {

        val context = newBase?.let {
            LocaleManager().onAttachBaseContext(it)
        } ?: newBase
        super.attachBaseContext(context)
    }

    protected fun changeLanguage(languageCode: String) {
        localeManager.setLocale(this, languageCode)
        localeManager.restartActivity(this)
    }

    protected fun getCurrentLanguage(): String {
        return localeManager.getLanguage(this)
    }
}