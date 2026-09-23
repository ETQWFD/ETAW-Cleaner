package com.etaw.cleaner

import android.graphics.drawable.Drawable

data class AppInfo(
    val pkg: String,
    val name: String,
    val icon: Drawable?,
    val installTime: Long,
    val website: String?
)

data class RecordItem(
    val id: Long,
    val pkg: String,
    val name: String,
    val website: String,
    val installTime: Long,
    val uninstallTime: Long,
    val sha256: String,
    val residueCount: Int,
    val note: String
)
