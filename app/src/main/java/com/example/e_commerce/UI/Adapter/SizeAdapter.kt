package com.example.e_commerce.UI.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.e_commerce.R
import com.example.e_commerce.databinding.ViewholderSizeBinding

interface SizeSelectedListener {
    fun onSizeSelected(size: String)
}

class SizeAdapter(
    private val items: List<String>,
    private val listener: SizeSelectedListener
) : RecyclerView.Adapter<SizeAdapter.ViewHolder>() {

    private var selectedPosition = -1

    class ViewHolder(val binding: ViewholderSizeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderSizeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val size = items[position]
        val context = holder.itemView.context
        val isSelected = selectedPosition == position
        holder.binding.sizeTxt.text = items[position]

        holder.binding.sizeTxt.text = size

        if (selectedPosition==position){
            holder.binding.sizeLayout.setBackgroundResource(R.drawable.grey_bg_selected)
            holder.binding.sizeTxt.setTextColor(ContextCompat.getColor(context, R.color.background_component_selected))
        } else {
            holder.binding.sizeLayout.setBackgroundResource(R.drawable.grey_bg)
            holder.binding.sizeTxt.setTextColor(ContextCompat.getColor(context,R.color.background_component))
        }

        holder.binding.root.setOnClickListener {
            val prevSelectedPosition = selectedPosition
            selectedPosition = position

            // Notificar el cambio de estado
            notifyItemChanged(prevSelectedPosition)
            notifyItemChanged(selectedPosition)

            // CRÍTICO: Notificar la talla seleccionada al Activity
            listener.onSizeSelected(size)
        }
    }



    override fun getItemCount(): Int = items.size

}