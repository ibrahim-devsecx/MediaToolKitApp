package com.example.mediatoolkitapp.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mediatoolkitapp.data.local.MediaHistoryEntity
import com.example.mediatoolkitapp.databinding.ItemMediaHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaHistoryAdapter(
    private val onOpenClick: (MediaHistoryEntity) -> Unit,
    private val onShareClick: (MediaHistoryEntity) -> Unit,
    private val onDeleteClick: (MediaHistoryEntity) -> Unit
) : ListAdapter<MediaHistoryEntity, MediaHistoryAdapter.MediaHistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MediaHistoryViewHolder {
        val binding = ItemMediaHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MediaHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MediaHistoryViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class MediaHistoryViewHolder(
        private val binding: ItemMediaHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaHistoryEntity) {
            val date = SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).format(Date(item.createdAt))

            binding.tvFileName.text = item.inputName
            binding.tvOperation.text = "Operation: ${item.operationType}"
            binding.tvStatus.text = "Status: ${item.status}"
            binding.tvDate.text = "Date: $date"

            val rangeText = if (item.startSeconds != null && item.endSeconds != null) {
                "Range: ${item.startSeconds}s - ${item.endSeconds}s"
            } else {
                "Range: Not required"
            }

            binding.tvRange.text = rangeText
            binding.tvMessage.text = item.message ?: ""

            val hasOutput = item.status == "SUCCESS" && !item.outputPath.isNullOrBlank()

            binding.btnOpen.isEnabled = hasOutput
            binding.btnShare.isEnabled = hasOutput
            binding.btnDelete.isEnabled = hasOutput

            binding.btnOpen.setOnClickListener {
                onOpenClick(item)
            }

            binding.btnShare.setOnClickListener {
                onShareClick(item)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<MediaHistoryEntity>() {
            override fun areItemsTheSame(
                oldItem: MediaHistoryEntity,
                newItem: MediaHistoryEntity
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: MediaHistoryEntity,
                newItem: MediaHistoryEntity
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}