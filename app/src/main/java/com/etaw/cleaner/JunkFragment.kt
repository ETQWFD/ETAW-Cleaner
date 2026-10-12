package com.etaw.cleaner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etaw.cleaner.databinding.FragmentJunkBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JunkFragment : Fragment() {

    private var _binding: FragmentJunkBinding? = null
    private val binding get() = _binding!!
    private var items: List<JunkItem> = emptyList()
    private val checked = HashSet<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJunkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerJunk.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerJunk.adapter = adapter
        binding.btnScanJunk.setOnClickListener { scan() }
        binding.btnClean.setOnClickListener { confirmClean() }
    }

    private val adapter = object : RecyclerView.Adapter<JunkVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JunkVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_junk, parent, false)
            return JunkVH(v)
        }
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: JunkVH, position: Int) {
            val item = items[position]
            holder.cat.text = item.category
            holder.path.text = item.path
            holder.owner.text = getString(R.string.junk_owner, item.ownerApp)
            holder.size.text = JunkScanner.formatSize(item.sizeBytes)
            holder.chk.setOnCheckedChangeListener(null)
            holder.chk.isChecked = checked.contains(position)
            holder.chk.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) checked += position else checked -= position
                updateTotal()
            }
            holder.itemView.setOnClickListener {
                holder.chk.toggle()
            }
        }
    }

    class JunkVH(v: View) : RecyclerView.ViewHolder(v) {
        val chk: CheckBox = v.findViewById(R.id.chk)
        val cat: TextView = v.findViewById(R.id.tvCat)
        val path: TextView = v.findViewById(R.id.tvPath)
        val owner: TextView = v.findViewById(R.id.tvOwner)
        val size: TextView = v.findViewById(R.id.tvSize)
    }

    private fun scan() {
        AppLog.i("Junk", "junk scan started")
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        binding.btnScanJunk.isEnabled = false
        lifecycleScope.launch {
            items = withContext(Dispatchers.IO) {
                try { JunkScanner.scan() } catch (e: Exception) {
                    AppLog.e("Junk", "scan failed: ${e.message}"); emptyList()
                }
            }
            checked.clear()
            items.indices.forEach { checked += it }
            AppLog.i("Junk", "scan found ${items.size} items, total=${JunkScanner.formatSize(items.sumOf { it.sizeBytes })}")
            binding.progressBar.visibility = View.GONE
            binding.btnScanJunk.isEnabled = true
            adapter.notifyDataSetChanged()
            binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            updateTotal()
        }
    }

    private fun updateTotal() {
        val total = checked.sumOf { items.getOrNull(it)?.sizeBytes ?: 0L }
        val n = checked.size
        binding.tvJunkTotal.text = getString(R.string.junk_selected, n, JunkScanner.formatSize(total))
        binding.btnClean.isEnabled = n > 0
    }

    private fun confirmClean() {
        val selected = checked.mapNotNull { items.getOrNull(it) }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.junk_confirm_title)
            .setMessage(getString(R.string.junk_confirm_msg, selected.size,
                JunkScanner.formatSize(selected.sumOf { it.sizeBytes })))
            .setPositiveButton(R.string.junk_clean) { _, _ -> doClean(selected.map { it.path }) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun doClean(paths: List<String>) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { JunkScanner.delete(paths) }
            Toast.makeText(requireContext(), getString(R.string.junk_done, ok), Toast.LENGTH_LONG).show()
            scan()
        }
    }
}
