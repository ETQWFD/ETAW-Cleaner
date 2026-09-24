package com.etaw.cleaner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etaw.cleaner.databinding.FragmentRecycleBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecycleFragment : Fragment() {

    private var _binding: FragmentRecycleBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecycleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = RecordAdapter(
            emptyList(),
            onClick = { r -> showRecordDialog(r) },
            onLongClick = { r ->
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_record_title)
                    .setMessage(getString(R.string.delete_record_msg, r.name))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        RecordDb(requireContext()).delete(r.id)
                        refresh()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
        binding.recyclerRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecords.adapter = adapter
        binding.btnClearRecords.setOnClickListener { clearAll() }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) { RecordDb(requireContext()).all() }
            adapter.submit(list)
            binding.tvRecordCount.text = getString(R.string.record_count, list.size)
            binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showRecordDialog(r: RecordItem) {
        val ctx = requireContext()
        val sb = StringBuilder()
        sb.append(ctx.getString(R.string.record_pkg, r.pkg)).append("\n")
        sb.append(ctx.getString(R.string.record_install, AppRepository.formatTime(r.installTime))).append("\n")
        sb.append(ctx.getString(R.string.record_uninstall, AppRepository.formatTime(r.uninstallTime))).append("\n")
        sb.append(ctx.getString(R.string.record_hash, r.sha256)).append("\n")
        sb.append(ctx.getString(R.string.record_scanned, r.scannedCount)).append("\n")
        sb.append(ctx.getString(R.string.record_deleted, r.deletedCount, ResidueScanner.formatSize(r.freedBytes))).append("\n")
        sb.append(ctx.getString(R.string.record_residue, r.residueCount)).append("\n")
        if (r.note.isNotBlank()) sb.append(ctx.getString(R.string.record_note, r.note)).append("\n")
        val website = r.website
        val builder = AlertDialog.Builder(ctx)
            .setTitle(r.name)
            .setMessage(sb.toString())
        if (website.isNotBlank()) {
            builder.setPositiveButton(R.string.visit_site) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(website)))
                } catch (e: Exception) {
                    Toast.makeText(ctx, R.string.no_browser, Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton(R.string.close, null).show()
    }

    private fun clearAll() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_records_title)
            .setMessage(R.string.clear_records_msg)
            .setPositiveButton(R.string.delete) { _, _ ->
                RecordDb(requireContext()).clear()
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
