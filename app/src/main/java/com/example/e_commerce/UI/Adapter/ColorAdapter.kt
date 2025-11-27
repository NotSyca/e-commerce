package com.example.e_commerce.UI.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.e_commerce.R
import com.example.e_commerce.databinding.ViewholderColorBinding

class ColorAdapter (
    val items: List<String>
): RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
        private var selectedPosition=-1
    private var lastSelectedPosition=-1
    private lateinit var context : Context

    class ViewHolder (val binding: ViewholderColorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorAdapter.ViewHolder {
        context= parent.context
        val binding=ViewholderColorBinding.inflate(LayoutInflater.from(context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item=items[position]

        Glide.with(holder.itemView.context)
            .load(items[position])
            .into(holder.binding.pic)

        holder.binding.root.setOnClickListener {
            lastSelectedPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(lastSelectedPosition)
            notifyItemChanged(selectedPosition)

        if (selectedPosition==position){
            holder.binding.colorLayout.setBackgroundResource(R.drawable.grey_bg_selected)
        } else {
            holder.binding.colorLayout.setBackgroundResource(R.drawable.grey_bg)
        }

    }
    }

    override fun getItemCount(): Int = items.size

}