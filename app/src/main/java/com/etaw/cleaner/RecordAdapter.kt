package com.etaw.cleaner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etaw.cleaner.databinding.ItemRecordBinding

class RecordAdapter(
    private var items: List<RecordItem>,
    private val onClick: (RecordItem) -> Unit,
    private val onLongClick: (RecordItem) -> Unit
) : RecyclerView.Adapter<RecordAdapter.VH>() {

    class VH(val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<RecordItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        val ctx = holder.itemView.context
        holder.binding.recordName.text = r.name
        holder.binding.recordMeta.text =
            "${r.pkg}  ·  卸载于 ${AppRepository.formatTime(r.uninstallTime)}"
        holder.binding.recordHash.text = r.sha256.take(16)
        holder.binding.recordSite.text =
            if (r.website.isNotBlank()) r.website else ctx.getString(R.string.site_unknown)
        holder.itemView.setOnClickListener { onClick(r) }
        holder.itemView.setOnLongClickListener {
            onLongClick(r)
            true
        }
    }
}
