package com.example.e_commerce.UI.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.e_commerce.Model.SearchHistory
import com.example.e_commerce.databinding.ItemSearchHistoryBinding

class SearchHistoryAdapter(private var items: List<SearchHistory>) : 
    RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSearchHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.queryTxt.text = item.query
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<SearchHistory>) {
        items = newItems
        notifyDataSetChanged()
    }
}
