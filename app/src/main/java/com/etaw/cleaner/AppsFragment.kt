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
import androidx.recyclerview.widget.LinearLayoutManager
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
            onLongClick = { app -> showAppActionsDialog(app) }
        )
        binding.recyclerApps.layoutManager = LinearLayoutManager(requireContext())
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
            try {
                val hasShell = ShizukuHelper.isReady() || RootHelper.isAvailable()
                val disabledSet = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (hasShell) {
                        val exec: (String) -> Pair<Boolean, String> = { cmd ->
                            if (ShizukuHelper.isReady()) ShizukuHelper.exec(cmd) else RootHelper.exec(cmd)
                        }
                        exec("pm list packages -d --user 0").second
                            .lines()
                            .mapNotNull { it.removePrefix("package:") }
                            .filter { it.isNotBlank() }
                            .toSet()
                    } else {
                        emptySet()
                    }
                }
                val apps = AppRepository.loadAppsAsync(requireContext(), disabledSet)
                cachedApps = apps
                adapter.submit(apps)
                binding.tvAppCount.text = getString(R.string.scan_result, apps.size)
                if (apps.isEmpty()) binding.emptyView.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.emptyView.text = getString(R.string.scan_failed, e.message ?: "unknown")
                binding.emptyView.visibility = View.VISIBLE
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnRefresh.isEnabled = true
            }
        }
    }

    /** 长按应用：禁用 / 启用 / 官网搜索 */
    private fun showAppActionsDialog(app: AppInfo) {
        val ctx = requireContext()
        val options = if (app.disabled) {
            arrayOf(getString(R.string.action_enable), getString(R.string.action_site))
        } else {
            arrayOf(getString(R.string.action_disable), getString(R.string.action_site))
        }
        AlertDialog.Builder(ctx)
            .setTitle(getString(R.string.app_action_title, app.name))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> if (app.disabled) enableApp(app) else disableApp(app)
                    1 -> showWebsiteSearch(app)
                }
            }
            .show()
    }

    private fun disableApp(app: AppInfo) {
        val ctx = requireContext()
        if (!ShizukuHelper.isReady() && !RootHelper.isAvailable()) {
            showPermissionGuide(app)
            return
        }
        lifecycleScope.launch {
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val exec: (String) -> Pair<Boolean, String> = { cmd ->
                    if (ShizukuHelper.isReady()) ShizukuHelper.exec(cmd) else RootHelper.exec(cmd)
                }
                val r = exec("pm disable-user --user 0 \"${app.pkg}\"")
                r.first || r.second.contains("already")
            }
            Toast.makeText(ctx, if (ok) getString(R.string.disable_ok, app.name) else getString(R.string.action_failed, app.name), Toast.LENGTH_LONG).show()
            loadApps()
        }
    }

    private fun enableApp(app: AppInfo) {
        val ctx = requireContext()
        if (!ShizukuHelper.isReady() && !RootHelper.isAvailable()) {
            showPermissionGuide(app)
            return
        }
        lifecycleScope.launch {
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val exec: (String) -> Pair<Boolean, String> = { cmd ->
                    if (ShizukuHelper.isReady()) ShizukuHelper.exec(cmd) else RootHelper.exec(cmd)
                }
                val r = exec("pm enable --user 0 \"${app.pkg}\"")
                r.first || r.second.contains("already")
            }
            Toast.makeText(ctx, if (ok) getString(R.string.enable_ok, app.name) else getString(R.string.action_failed, app.name), Toast.LENGTH_LONG).show()
            loadApps()
        }
    }

    /** 无 Shizuku/root 时：引导授权（无需 root）或打开系统设置手动操作 */
    private fun showPermissionGuide(app: AppInfo) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle(R.string.perm_title)
            .setMessage(getString(R.string.perm_msg, app.name))
            .setPositiveButton(R.string.perm_grant) { _, _ ->
                ShizukuHelper.requestPermission()
                Toast.makeText(ctx, R.string.perm_grant_tip, Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(R.string.open_settings) { _, _ ->
                try {
                    startActivity(
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}"))
                    )
                } catch (e: Exception) {
                    Toast.makeText(ctx, R.string.no_browser, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
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
        if (ShizukuHelper.isReady() || RootHelper.isAvailable()) {
            // 高权限通道：Shizuku 静默卸载 / root 卸载（真实卸载）
            val progress = androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle(R.string.uninstalling)
                .setMessage(getString(R.string.uninstalling_detail, app.name))
                .setCancelable(false)
                .show()
            lifecycleScope.launch {
                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (ShizukuHelper.isReady()) ShizukuHelper.uninstall(app.pkg)
                    else RootHelper.uninstall(app.pkg)
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
        startCleanup(app)
    }

    /** 卸载成功后：深度扫描残留 → 用户勾选确认 → 深度删除 → 完整记录 */
    private fun startCleanup(app: AppInfo) {
        val ctx = requireContext()
        val progress = androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(R.string.cleaning)
            .setMessage(getString(R.string.cleaning_detail, app.name))
            .setCancelable(false)
            .show()
        lifecycleScope.launch {
            val hasShell = ShizukuHelper.isReady() || RootHelper.isAvailable()
            val items = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (hasShell) {
                    val exec: (String) -> Pair<Boolean, String> = { cmd ->
                        if (ShizukuHelper.isReady()) ShizukuHelper.exec(cmd) else RootHelper.exec(cmd)
                    }
                    ResidueScanner.deepScan(app.pkg, exec)
                } else {
                    ResidueScanner.externalScan(app.pkg)
                }
            }
            progress.dismiss()
            if (items.isEmpty()) {
                saveRecord(app, items, emptyList(), 0L, note = ctx.getString(R.string.note_no_residue))
                Toast.makeText(ctx, getString(R.string.no_residue, app.name), Toast.LENGTH_LONG).show()
                loadApps()
                return@launch
            }
            showResidueReviewDialog(app, items, hasShell)
        }
    }

    private fun showResidueReviewDialog(app: AppInfo, items: List<ResidueItem>, hasShell: Boolean) {
        val ctx = requireContext()
        val labels = items.map { item ->
            val size = ResidueScanner.formatSize(item.sizeBytes)
            if (item.risky) "${item.path}（$size）${ctx.getString(R.string.risky_note)}"
            else "${item.path}（$size）"
        }.toTypedArray()
        val checked = BooleanArray(items.size) { !items[it].risky }
        AlertDialog.Builder(ctx)
            .setTitle(getString(R.string.residue_title, items.size, ResidueScanner.formatSize(items.sumOf { it.sizeBytes })))
            .setMessage(R.string.residue_msg)
            .setMultiChoiceItems(labels, checked) { _, idx, isChecked -> checked[idx] = isChecked }
            .setPositiveButton(R.string.delete_checked) { _, _ ->
                val selected = items.filterIndexed { i, _ -> checked[i] }
                performDeepDelete(app, items, selected, hasShell)
            }
            .setNegativeButton(R.string.skip_clean) { _, _ ->
                saveRecord(app, items, emptyList(), 0L, note = ctx.getString(R.string.note_skipped))
                Toast.makeText(ctx, getString(R.string.clean_skipped, app.name), Toast.LENGTH_LONG).show()
                loadApps()
            }
            .show()
    }

    private fun performDeepDelete(app: AppInfo, items: List<ResidueItem>, selected: List<ResidueItem>, hasShell: Boolean) {
        val ctx = requireContext()
        if (selected.isEmpty()) {
            saveRecord(app, items, emptyList(), 0L, note = ctx.getString(R.string.note_skipped))
            Toast.makeText(ctx, getString(R.string.clean_skipped, app.name), Toast.LENGTH_LONG).show()
            loadApps()
            return
        }
        val progress = androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(R.string.deleting)
            .setMessage(getString(R.string.deleting_detail, selected.size))
            .setCancelable(false)
            .show()
        lifecycleScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (hasShell) {
                    val exec: (String) -> Pair<Boolean, String> = { cmd ->
                        if (ShizukuHelper.isReady()) ShizukuHelper.exec(cmd) else RootHelper.exec(cmd)
                    }
                    val quoted = selected.joinToString(" ") { "'" + it.path.replace("'", "'\\''") + "'" }
                    exec("rm -rf $quoted")
                    // 删除后复核：确认实际剩余残留，统计真实删除数量
                    val remainingItems = ResidueScanner.deepScan(app.pkg, exec)
                    val remainingPaths = remainingItems.map { it.path }.toSet()
                    val deletedCount = selected.count { it.path !in remainingPaths }
                    Pair(deletedCount, remainingItems)
                } else {
                    val deletedCount = selected.count { item ->
                        try {
                            val f = java.io.File(item.path)
                            f.exists() && f.deleteRecursively()
                        } catch (e: Exception) { false }
                    }
                    Pair(deletedCount, ResidueScanner.externalScan(app.pkg))
                }
            }
            val (deletedCount, remainingItems) = result
            progress.dismiss()
            val freedBytes = selected.take(deletedCount).sumOf { it.sizeBytes }
            val note = buildString {
                append(ctx.getString(R.string.note_deleted_list)).append("\n")
                selected.take(8).forEach {
                    append("· ").append(it.path).append("（").append(ResidueScanner.formatSize(it.sizeBytes)).append("）\n")
                }
                if (selected.size > 8) append(ctx.getString(R.string.note_more, selected.size - 8)).append("\n")
                if (remainingItems.isNotEmpty()) append(ctx.getString(R.string.note_residue_left, remainingItems.size))
            }
            saveRecord(app, items, selected.take(deletedCount), freedBytes, note = note)
            val msg = buildString {
                append(getString(R.string.cleaned_result, deletedCount, ResidueScanner.formatSize(freedBytes)))
                if (remainingItems.isNotEmpty()) append(getString(R.string.residue_left, remainingItems.size))
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
            loadApps()
        }
    }

    private fun saveRecord(
        app: AppInfo,
        scanned: List<ResidueItem>,
        deleted: List<ResidueItem>,
        freedBytes: Long,
        note: String
    ) {
        val ctx = requireContext()
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
                residueCount = (scanned.size - deleted.size).coerceAtLeast(0),
                scannedCount = scanned.size,
                deletedCount = deleted.size,
                freedBytes = freedBytes,
                deletedPaths = deleted.joinToString("\n") { it.path },
                note = note
            )
        )
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
