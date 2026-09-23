package com.etaw.cleaner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etaw.cleaner.databinding.ItemAppBinding

class AppAdapter(
    private var items: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    class VH(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<AppInfo>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        val ctx = holder.itemView.context
        holder.binding.appIcon.setImageDrawable(app.icon)
        holder.binding.appName.text = app.name
        holder.binding.appMeta.text =
            "${app.pkg}  ·  安装于 ${AppRepository.formatTime(app.installTime)}"
        val site = app.website
        if (site != null) {
            holder.binding.appSite.text = site
            holder.binding.appSite.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.appSite.text = ctx.getString(R.string.site_unknown)
            holder.binding.appSite.visibility = android.view.View.VISIBLE
        }
        holder.itemView.setOnClickListener { onClick(app) }
        holder.itemView.setOnLongClickListener {
            onLongClick(app)
            true
        }
    }
}
