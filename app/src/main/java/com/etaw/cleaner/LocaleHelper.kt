package com.etaw.cleaner

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun currentLocale(lang: String): Locale = when (lang) {
        "en" -> Locale.ENGLISH
        "zh-rTW" -> Locale.TAIWAN
        else -> Locale.SIMPLIFIED_CHINESE
    }

    /**
     * 返回应用了目标语言的 Context（createConfigurationContext 方案，
     * 兼容 API 23~34，含 Android 13+ 的系统级语言覆盖场景）。
     */
    fun apply(context: Context): Context {
        val locale = currentLocale(Prefs.lang(context))
        Locale.setDefault(locale)

        // 方案一：为当前 Context 创建带目标语言配置的派生 Context（各版本均生效）
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localized = context.createConfigurationContext(config)

        // 方案二：同步更新原 resources（旧系统即时生效，Android 13+ 由方案一兜底）
        try {
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {
            // ignore
        }
        return localized
    }
}
