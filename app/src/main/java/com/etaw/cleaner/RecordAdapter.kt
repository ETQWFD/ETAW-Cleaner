package com.etaw.cleaner

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
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
        if (r.website.isNotBlank()) {
            holder.binding.recordSite.text = ctx.getString(R.string.visit_site_link, r.website)
            holder.binding.recordSite.paint?.isUnderlineText = true
            holder.binding.recordSite.setOnClickListener {
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.website)))
                } catch (e: Exception) {
                    Toast.makeText(ctx, R.string.no_browser, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            holder.binding.recordSite.text = ctx.getString(R.string.site_unknown)
            holder.binding.recordSite.paint?.isUnderlineText = false
            holder.binding.recordSite.setOnClickListener(null)
        }
        holder.itemView.setOnClickListener { onClick(r) }
        holder.itemView.setOnLongClickListener {
            onLongClick(r)
            true
        }
    }
}
