package com.example.e_commerce.UI.Adapter

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
import com.example.e_commerce.databinding.ViewholderCategoryGridBinding
import java.util.Locale

class CategoryGridAdapter(
    val items: MutableList<BrandModel>,
    val onCategoryClick: (Int) -> Unit
) : RecyclerView.Adapter<CategoryGridAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(val binding: ViewholderCategoryGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding = ViewholderCategoryGridBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Formatear y asignar el nombre de la categoría
        holder.binding.titlePic.text = item.name.lowercase(Locale.getDefault()).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        Glide.with(holder.itemView.context)
            .load(item.picUrl)
            .into(holder.binding.pic)

        holder.binding.root.setOnClickListener {
            onCategoryClick(item.id)
        }

        // Estilo normal sin selección
        holder.binding.mainLayout.setBackgroundResource(android.R.color.transparent)
        holder.binding.titlePic.setTextColor(context.resources.getColor(R.color.black))
        ImageViewCompat.setImageTintList(
            holder.binding.pic,
            ColorStateList.valueOf(context.getColor(R.color.black))
        )
    }

    override fun getItemCount(): Int = items.size
}
