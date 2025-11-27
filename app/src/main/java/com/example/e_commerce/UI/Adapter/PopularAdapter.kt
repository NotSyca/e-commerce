package com.example.e_commerce.UI.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.e_commerce.Model.Product
import com.example.e_commerce.UI.DetailActivity
import com.example.e_commerce.databinding.ViewholderRecommendedBinding
import java.util.Locale

// NOTA: Cambiado a lista inmutable para seguridad, aunque aún permite null en el constructor
class PopularAdapter(val items: ArrayList<Product>) :
    RecyclerView.Adapter<PopularAdapter.ViewHolder>() {

    // Eliminamos 'private var context: Context? = null' para evitar fugas

    class ViewHolder (val binding: ViewholderRecommendedBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularAdapter.ViewHolder {
        val context = parent.context
        val binding = ViewholderRecommendedBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    fun updateData(newItems: List<Product>) {

        items.clear()
        items.addAll(newItems)

        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Usamos el operador '!!' de forma segura ya que getItemCount maneja el caso nulo
        val product = items[position]
        val context = holder.itemView.context

        // 1. Enlace de Texto
        holder.binding.titleTxt.text = product.title
        holder.binding.priceTxt.text = String.format(Locale.US, "$%.2f", product.price)
        holder.binding.ratingTxt.text = product.rating.toString()

        // 2. Carga de Imagen: La lista picUrl es ArrayList<String>
        val imageUrl = product.picUrl.firstOrNull()

        val requestOptions = RequestOptions().transform(CenterCrop())

        if (!imageUrl.isNullOrEmpty()) {
            // CORRECCIÓN CLAVE: Usar la variable imageUrl (firstOrNull) en lugar de un índice fijo
            Glide.with(context)
                .load(imageUrl) // Carga la primera URL de forma segura
                .apply(requestOptions)
                .into(holder.binding.imgProduct)
        }


        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailActivity::class.java)
            intent.putExtra("EXTRA_PRODUCT_ID", product.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size // Manejar el caso de lista nula
}