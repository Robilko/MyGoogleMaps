package com.example.mygooglemaps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mygooglemaps.databinding.MarkersListRecyclerItemBinding
import com.google.android.gms.maps.model.Marker

class MarkersListAdapter(private val listener: RecyclerItemListener) : RecyclerView.Adapter<MarkersListAdapter.MarkersViewHolder>() {

    private val newsListDiffer = AsyncListDiffer(this, DIFF_CALLBACK)

    fun submitList(list: MutableList<Marker>) = newsListDiffer.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkersViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = MarkersListRecyclerItemBinding.inflate(inflater, parent, false)
        return MarkersViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarkersViewHolder, position: Int) {
        val marker = newsListDiffer.currentList[position]
        holder.bind(item = marker)
    }

    override fun getItemCount(): Int = newsListDiffer.currentList.size

    inner class MarkersViewHolder(private val binding: MarkersListRecyclerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Marker) = with(binding) {
            markerTitle.text = item.title
            markerSnippet.text = item.snippet

            itemView.setOnClickListener{ listener.onItemClick(marker = item) }
        }

    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Marker>() {
            override fun areItemsTheSame(oldItem: Marker, newItem: Marker): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Marker, newItem: Marker): Boolean {
                return oldItem == newItem
            }
        }
    }
}