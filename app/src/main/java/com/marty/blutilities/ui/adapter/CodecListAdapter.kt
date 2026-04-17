package com.marty.blutilities.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.marty.blutilities.R
import com.marty.blutilities.model.BluetoothCodecInfo

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_ITEM   = 1

class CodecListAdapter(
    private val onItemClick: (BluetoothCodecInfo) -> Unit
) : ListAdapter<BluetoothCodecInfo, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isHeader) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_codec_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_codec, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> holder.bind(item)
            is ItemViewHolder   -> holder.bind(item, onItemClick)
        }
    }

    // ViewHolders

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvHeader)
        fun bind(item: BluetoothCodecInfo) {
            tvHeader.text = item.codecName
        }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvCodecName:  TextView  = view.findViewById(R.id.tvCodecName)
        private val tvSubLabel:   TextView  = view.findViewById(R.id.tvSubLabel)
        private val ivSelected:   ImageView = view.findViewById(R.id.ivSelected)

        fun bind(item: BluetoothCodecInfo, onClick: (BluetoothCodecInfo) -> Unit) {
            tvCodecName.text = item.codecName
            if (item.qualityLabel != null) {
                tvSubLabel.visibility = View.VISIBLE
                tvSubLabel.text = item.qualityLabel
            } else {
                tvSubLabel.visibility = View.GONE
            }
            ivSelected.visibility = if (item.isSelected) View.VISIBLE else View.INVISIBLE
            itemView.setOnClickListener { onClick(item) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<BluetoothCodecInfo>() {
        override fun areItemsTheSame(a: BluetoothCodecInfo, b: BluetoothCodecInfo) =
            a.codecType == b.codecType && a.codecPriority == b.codecPriority && a.isHeader == b.isHeader

        override fun areContentsTheSame(a: BluetoothCodecInfo, b: BluetoothCodecInfo) = a == b
    }
}