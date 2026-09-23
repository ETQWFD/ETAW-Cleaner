package com.etaw.cleaner

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun apply(context: Context): Context {
        val lang = Prefs.lang(context)
        val locale = when (lang) {
            "en" -> Locale.ENGLISH
            "zh-rTW" -> Locale.TAIWAN
            else -> Locale.SIMPLIFIED_CHINESE
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        return context
    }
}
