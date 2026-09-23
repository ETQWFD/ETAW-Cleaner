package com.etaw.cleaner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etaw.cleaner.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        binding.btnTheme.setOnClickListener { showThemeDialog() }
        binding.btnLanguage.setOnClickListener { showLanguageDialog() }
        binding.btnShizuku.setOnClickListener { showShizukuDialog() }
        binding.btnUpdate.setOnClickListener { checkUpdate() }
        binding.btnDeveloper.setOnClickListener { showDeveloperDialog() }
        binding.tvVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)
        refreshShizukuStatus()
    }

    private fun refreshShizukuStatus() {
        binding.tvShizukuStatus.text =
            if (ShizukuHelper.isReady()) getString(R.string.shizuku_ready)
            else getString(R.string.shizuku_not_ready)
    }

    private fun showThemeDialog() {
        val ctx = requireContext()
        val current = Prefs.theme(ctx)
        val options = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        val values = arrayOf(Prefs.THEME_SYSTEM, Prefs.THEME_LIGHT, Prefs.THEME_DARK)
        val checked = values.indexOf(current).coerceAtLeast(0)
        var dialog: AlertDialog? = null
        dialog = AlertDialog.Builder(ctx)
            .setTitle(R.string.theme_title)
            .setSingleChoiceItems(options, checked) { _, which ->
                Prefs.setTheme(ctx, values[which])
                dialog?.dismiss()
                AppCompatDelegate.setDefaultNightMode(
                    when (values[which]) {
                        Prefs.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        Prefs.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
                requireActivity().recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
    }

    private fun showLanguageDialog() {
        val ctx = requireContext()
        val current = Prefs.lang(ctx)
        val options = arrayOf("简体中文", "English", "繁體中文")
        val values = arrayOf("zh", "en", "zh-rTW")
        val checked = values.indexOf(current).coerceAtLeast(0)
        var dialog: AlertDialog? = null
        dialog = AlertDialog.Builder(ctx)
            .setTitle(R.string.language_title)
            .setSingleChoiceItems(options, checked) { _, which ->
                Prefs.setLang(ctx, values[which])
                dialog?.dismiss()
                LocaleHelper.apply(requireActivity().applicationContext)
                requireActivity().recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
    }

    private fun showShizukuDialog() {
        val ctx = requireContext()
        val ready = ShizukuHelper.isReady()
        val builder = AlertDialog.Builder(ctx)
            .setTitle(R.string.shizuku_title)
        val msg = when {
            ready -> getString(R.string.shizuku_ready_detail)
            ShizukuHelper.isAvailable() && !ShizukuHelper.isGranted() ->
                getString(R.string.shizuku_need_grant)
            else -> getString(R.string.shizuku_guide)
        }
        builder.setMessage(msg)
        if (!ready) {
            builder.setPositiveButton(R.string.shizuku_grant) { _, _ ->
                ShizukuHelper.requestPermission()
                refreshShizukuStatus()
            }
        }
        builder.setNegativeButton(R.string.close, null).show()
    }

    private fun checkUpdate() {
        val ctx = requireContext()
        binding.btnUpdate.isEnabled = false
        Toast.makeText(ctx, R.string.checking_update, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val info = UpdateChecker.check()
            binding.btnUpdate.isEnabled = true
            if (info == null) {
                Toast.makeText(ctx, R.string.update_check_failed, Toast.LENGTH_LONG).show()
                return@launch
            }
            if (UpdateChecker.hasNewer(BuildConfig.VERSION_NAME, info.tag)) {
                val builder = AlertDialog.Builder(ctx)
                    .setTitle(getString(R.string.update_found, info.tag))
                    .setMessage(
                        if (info.body.isNotBlank()) info.body
                        else getString(R.string.update_body_default)
                    )
                if (info.apkUrl != null) {
                    builder.setPositiveButton(R.string.update_download) { _, _ ->
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl)))
                        } catch (e: Exception) {
                            Toast.makeText(ctx, R.string.no_browser, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                builder.setNegativeButton(R.string.close, null).show()
            } else {
                Toast.makeText(ctx, R.string.latest_version, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeveloperDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.developer_title)
            .setMessage(getString(R.string.developer_info))
            .setPositiveButton(R.string.contact_email) { _, _ ->
                try {
                    val intent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:2416444244@qq.com")
                    )
                    intent.putExtra(Intent.EXTRA_SUBJECT, "ETAW清理")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), R.string.no_mail, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        refreshShizukuStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
