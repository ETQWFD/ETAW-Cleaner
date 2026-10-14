package com.etaw.cleaner

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.etaw.cleaner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.init(this)
        val restoring = savedInstanceState != null
        AppLog.i("Main", "onCreate v=${packageManager.getPackageInfo(packageName,0).versionName} restore=$restoring")
        applySavedTheme()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 冷启动恢复防御：进程被杀后重启时，系统会按上次状态恢复 Fragment。
        // 恢复路径在不同设备/模拟器上行为差异大，任何隐藏问题都会在页面显示前崩溃（表现为"第二次使用闪退"）。
        // 这里统一丢弃旧恢复状态、总是重建干净界面，从根上杜绝恢复期崩溃。
        if (restoring) {
            try {
                val fm = supportFragmentManager
                fm.fragments.forEach { f ->
                    if (f.isAdded) fm.beginTransaction().remove(f).commitNowAllowingStateLoss()
                }
            } catch (e: Exception) {
                AppLog.e("Main", "restore cleanup failed: ${e.message}")
            }
        }
        showFragment(AppsFragment())
        binding.bottomNav.selectedItemId = R.id.nav_apps

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_apps -> { showFragment(AppsFragment()); true }
                R.id.nav_junk -> { showFragment(JunkFragment()); true }
                R.id.nav_recycle -> { showFragment(RecycleFragment()); true }
                R.id.nav_logs -> { showFragment(LogsFragment()); true }
                R.id.nav_settings -> { showFragment(SettingsFragment()); true }
                else -> false
            }
        }
    }

    private fun applySavedTheme() {
        when (Prefs.theme(this)) {
            Prefs.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Prefs.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commitAllowingStateLoss()
    }
}
