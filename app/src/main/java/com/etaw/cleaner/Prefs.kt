package com.etaw.cleaner

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "etaw_prefs"
    const val KEY_THEME = "theme"            // system / light / dark
    const val KEY_LANG = "lang"              // zh / en / zh-rTW
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun theme(context: Context): String = get(context).getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setTheme(context: Context, value: String) {
        get(context).edit().putString(KEY_THEME, value).apply()
    }

    fun lang(context: Context): String = get(context).getString(KEY_LANG, "zh") ?: "zh"

    fun setLang(context: Context, value: String) {
        get(context).edit().putString(KEY_LANG, value).apply()
    }
}
