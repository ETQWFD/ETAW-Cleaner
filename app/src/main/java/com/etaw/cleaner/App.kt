package com.etaw.cleaner

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 进程最早期初始化日志：任何阶段崩溃（含资源/attachBaseContext 前）都会落盘，
        // 用户可从日志页复制堆栈，实现"第二次闪退"也能定位。
        AppLog.init(this)
        AppLog.i("App", "process started, v${BuildConfig.VERSION_NAME}")
    }
}
