package com.etaw.cleaner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.etaw.cleaner.databinding.FragmentAppsBinding
import kotlinx.coroutines.launch

class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppAdapter
    private var cachedApps: List<AppInfo> = emptyList()
    private var pendingUninstallPkg: String? = null

    private val uninstallLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val pkg = pendingUninstallPkg
            pendingUninstallPkg = null
            if (result.resultCode == android.app.Activity.RESULT_OK && pkg != null) {
                onUninstallConfirmed(pkg)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AppAdapter(
            emptyList(),
            onClick = { app -> showUninstallDialog(app) },
            onLongClick = { app -> showWebsiteSearch(app) }
        )
        binding.recyclerApps.adapter = adapter
        binding.btnRefresh.setOnClickListener { loadApps() }
        binding.btnRefresh.isEnabled = false
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        lifecycleScope.launch {
            val apps = AppRepository.loadAppsAsync(requireContext())
            cachedApps = apps
            adapter.submit(apps)
            binding.progressBar.visibility = View.GONE
            binding.btnRefresh.isEnabled = true
            binding.tvAppCount.text = getString(R.string.scan_result, apps.size)
            if (apps.isEmpty()) binding.emptyView.visibility = View.VISIBLE
        }
    }

    private fun showUninstallDialog(app: AppInfo) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle(getString(R.string.uninstall_title, app.name))
            .setMessage(getString(R.string.uninstall_msg, app.name))
            .setPositiveButton(R.string.yes) { _, _ -> secondVerify(app) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** 双重验证：第二道确认 */
    private fun secondVerify(app: AppInfo) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle(R.string.second_verify_title)
            .setMessage(getString(R.string.second_verify_msg, app.name))
            .setPositiveButton(R.string.confirm_uninstall) { _, _ -> doUninstall(app) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun doUninstall(app: AppInfo) {
        val activity = requireActivity()
        if (ShizukuHelper.isReady()) {
            // Shizuku 授权模式：真实静默卸载 + 残留清理
            val progress = androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle(R.string.uninstalling)
                .setMessage(getString(R.string.uninstalling_detail, app.name))
                .setCancelable(false)
                .show()
            lifecycleScope.launch {
                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ShizukuHelper.uninstall(app.pkg)
                }
                progress.dismiss()
                if (ok) {
                    onUninstallConfirmed(app.pkg)
                } else {
                    Toast.makeText(activity, getString(R.string.uninstall_failed, app.name), Toast.LENGTH_LONG).show()
                    loadApps()
                }
            }
        } else {
            // 系统卸载通道：系统弹窗即第二道验证
            pendingUninstallPkg = app.pkg
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}"))
            uninstallLauncher.launch(intent)
        }
    }

    private fun onUninstallConfirmed(pkg: String) {
        val app = cachedApps.firstOrNull { it.pkg == pkg } ?: return
        val ctx = requireContext()
        val progress = androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(R.string.cleaning)
            .setMessage(getString(R.string.cleaning_detail, app.name))
            .setCancelable(false)
            .show()
        lifecycleScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var residues = ShizukuHelper.findResidues(pkg)
                val externalCleaned = ShizukuHelper.tryExternalClean(pkg)
                if (residues.isNotEmpty()) {
                    ShizukuHelper.deleteResidues(residues)
                }
                residues = ShizukuHelper.findResidues(pkg)
                val note = buildString {
                    if (ShizukuHelper.isReady()) {
                        append(ctx.getString(R.string.note_shizuku))
                        if (residues.isNotEmpty()) append(" ${ctx.getString(R.string.note_residue_left, residues.size)}")
                    } else {
                        append(ctx.getString(R.string.note_system_mode))
                        if (externalCleaned > 0) append(" ${ctx.getString(R.string.note_ext_cleaned, externalCleaned)}")
                    }
                }
                Triple(residues.size, externalCleaned, note)
            }
            val (residueLeft, extCleaned, note) = result
            val db = RecordDb(ctx)
            db.insert(
                RecordItem(
                    id = 0,
                    pkg = app.pkg,
                    name = app.name,
                    website = app.website ?: "",
                    installTime = app.installTime,
                    uninstallTime = System.currentTimeMillis(),
                    sha256 = HashHelper.appFingerprint(app),
                    residueCount = residueLeft,
                    note = note
                )
            )
            progress.dismiss()
            val msg = buildString {
                append(getString(R.string.clean_done, app.name))
                if (ShizukuHelper.isReady() && residueLeft == 0) append(getString(R.string.clean_all))
                if (!ShizukuHelper.isReady()) append(getString(R.string.clean_limited))
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
            loadApps()
        }
    }

    private fun showWebsiteSearch(app: AppInfo) {
        val site = app.website
        if (site != null) {
            openUrl(site)
        } else {
            val ctx = requireContext()
            val q = Uri.encode("${app.name} ${app.pkg} 官网")
            AlertDialog.Builder(ctx)
                .setTitle(R.string.site_search_title)
                .setMessage(getString(R.string.site_search_msg, app.name))
                .setPositiveButton(R.string.site_search_go) { _, _ ->
                    openUrl("https://www.bing.com/search?q=$q")
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
