package com.example.e_commerce.UI.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.e_commerce.R

class EditableImageAdapter(
    private val context: Context,
    private val imageList: MutableList<String>, // Lista de URLs
    private val onDeleteClick: (Int) -> Unit // Callback al eliminar
) : RecyclerView.Adapter<EditableImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.productImage)
        val deleteImageBtn: ImageButton = view.findViewById(R.id.deleteImageBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_editable_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageList[position]

        // Cargar imagen con Glide (asumo que usas Glide o Picasso)
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.item_editable_image) // Usa tu placeholder
            .into(holder.productImage)

        // Listener para el botón de eliminación (la 'X')
        holder.deleteImageBtn.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = imageList.size

    // Método para eliminar una imagen de la lista y notificar al RecyclerView
    fun removeImageAt(position: Int) {
        imageList.removeAt(position)
        notifyItemRemoved(position)
    }
}