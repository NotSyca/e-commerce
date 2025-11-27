package com.example.e_commerce.UI.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.request.RequestOptions
import com.example.e_commerce.Model.SliderModel
import com.example.e_commerce.R

class SliderAdapter(
    // Si planeas hacer un bucle infinito, considera que esta lista puede ser muy grande.
    private var sliderItems: List<SliderModel>,
    private val viewPager2: ViewPager2
) : RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

    // El Runnable para notificar el cambio (usado para auto-scroll o loop)
    private val runnable = Runnable {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SliderAdapter.SliderViewHolder {
        // Usa LayoutInflater correctamente.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.slider_item_container, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        // Pasar el Context al ViewHolder para que Glide pueda funcionar
        holder.setImage(sliderItems[position], holder.itemView.context)

    }
    override fun getItemCount(): Int {
        return sliderItems.size
    }

    // Función pública para actualizar los datos si cambian (ej. al cargar de Supabase)
    @SuppressLint("NotifyDataSetChanged")    fun updateData(newItems: List<SliderModel>) {
        sliderItems = newItems
        notifyDataSetChanged()
    }

    // El ViewHolder es la clase interna
    class SliderViewHolder(itemView:View):RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.imageSlider)

        // La función setImage recibe el Context
        fun setImage(sliderItem: SliderModel, context: Context) {
            val requestOptions = RequestOptions().transform(CenterInside())

            // Usamos Glide para cargar la imagen desde la URL del SliderModel
            Glide.with(context)
                .load(sliderItem.url)
                .apply(requestOptions)
                .into(imageView)
        }
    }
}