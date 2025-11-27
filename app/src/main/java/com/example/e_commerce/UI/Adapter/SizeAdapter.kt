package com.example.e_commerce.UI.Adapter

import android.content.Context
import android.util.Log
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
        val context = holder.itemView.context // Contexto del elemento

        // 1. Asignar la talla (solo una vez)
        holder.binding.sizeTxt.text = size

        // 2. Definir si está seleccionado
        val isSelected = selectedPosition == position

        if (isSelected) {
            // Estado SELECCIONADO
            // Asumo que tienes R.drawable.grey_bg_selected y R.color.background_component_selected
            holder.binding.sizeLayout.setBackgroundResource(R.drawable.grey_bg_selected)
            holder.binding.sizeTxt.setTextColor(ContextCompat.getColor(context, R.color.black))
        } else {
            // Estado NORMAL
            // Asumo que tienes R.drawable.grey_bg y R.color.background_component
            holder.binding.sizeLayout.setBackgroundResource(R.drawable.grey_bg)
            holder.binding.sizeTxt.setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        // 3. Listener de Clic
        holder.binding.root.setOnClickListener {
            // Si hace clic en el mismo elemento, deseleccionamos
            if (selectedPosition == position) {
                val prevSelectedPosition = selectedPosition
                selectedPosition = -1 // Deseleccionar
                notifyItemChanged(prevSelectedPosition)
                listener.onSizeSelected("") // Notificar que no hay talla seleccionada
                Log.d(TAG, "Talla deseleccionada.")
                return@setOnClickListener
            }

            // Seleccionar nuevo elemento
            val prevSelectedPosition = selectedPosition
            selectedPosition = position

            // Notificar cambios visuales
            if (prevSelectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(prevSelectedPosition)
            }
            notifyItemChanged(selectedPosition)

            // Notificar la talla seleccionada al Fragmento/Activity
            listener.onSizeSelected(size)
            Log.d(TAG, "Talla seleccionada: $size")
        }
    }

    private val TAG = "SizeAdapter"

    override fun getItemCount(): Int = items.size

}