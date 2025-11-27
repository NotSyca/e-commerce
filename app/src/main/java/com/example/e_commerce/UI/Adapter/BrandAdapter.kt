package com.example.e_commerce.UI.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.e_commerce.Model.BrandModel
import com.example.e_commerce.R
import com.example.e_commerce.databinding.ViewholderBrandBinding
import java.util.Locale

@Suppress("DEPRECATION")
class BrandAdapter(
    val items: MutableList<BrandModel>,
    val onBrandSelected: (Int) -> Unit,
    val onCenterRequested: (Int) -> Unit
) : RecyclerView.Adapter<BrandAdapter.ViewHolder>() {
    private var selectedPosition = -1
    private lateinit var context: Context

    class ViewHolder(val binding: ViewholderBrandBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding =
            ViewholderBrandBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 1. Formatear y asignar el nombre de la marca/categoría
        holder.binding.titlePic.text = item.name.lowercase(Locale.getDefault()).replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }

        Glide.with(holder.itemView.context)
            .load(item.picUrl)
            .into(holder.binding.pic)

        holder.binding.root.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            val currentPosition = holder.adapterPosition
            val idToSend: Int

            if (selectedPosition == currentPosition) {
                selectedPosition = -1
                idToSend = -1
            } else {
                selectedPosition = currentPosition
                idToSend = item.id
            }

            if (previousSelectedPosition != -1) {
                notifyItemChanged(previousSelectedPosition)
            }
            notifyItemChanged(selectedPosition)

            onBrandSelected(idToSend)

            if (idToSend != -1) {
                onCenterRequested(currentPosition)
            }
        }

        // 2. Lógica de selección visual actualizada
        // El texto ahora SIEMPRE es visible gracias al nuevo layout
        holder.binding.titlePic.visibility = View.VISIBLE

        if (selectedPosition == position) {
            // Estado seleccionado: fondo de color y texto blanco
            holder.binding.mainLayout.setBackgroundResource(R.drawable.bg_nav_item_active)
            holder.binding.titlePic.setTextColor(context.resources.getColor(R.color.white))
            ImageViewCompat.setImageTintList(
                holder.binding.pic,
                ColorStateList.valueOf(context.getColor(R.color.white))
            )
        } else {
            // Estado no seleccionado: fondo transparente para mostrar el borde y texto negro
            holder.binding.mainLayout.setBackgroundResource(android.R.color.transparent)
            holder.binding.titlePic.setTextColor(context.resources.getColor(R.color.black))
            ImageViewCompat.setImageTintList(
                holder.binding.pic,
                ColorStateList.valueOf(context.getColor(R.color.black))
            )
        }
    }

    override fun getItemCount(): Int = items.size
}