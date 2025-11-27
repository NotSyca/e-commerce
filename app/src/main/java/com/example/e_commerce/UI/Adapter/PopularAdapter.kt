package com.example.e_commerce.UI.Adapter

import android.content.Intent
import android.util.Log
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

class PopularAdapter(val items: ArrayList<Product>) :
    RecyclerView.Adapter<PopularAdapter.ViewHolder>() {

    private val TAG = "PopularAdapter"

    class ViewHolder (val binding: ViewholderRecommendedBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val binding = ViewholderRecommendedBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    fun updateData(newItems: List<Product>) {
        // Línea de diagnóstico 1
        Log.d(TAG, "Adapter recibió ${newItems.size} productos para actualizar.")
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]
        val context = holder.itemView.context

        holder.binding.titleTxt.text = product.title
        holder.binding.priceTxt.text = String.format(Locale.US, "$%.2f", product.price)
        holder.binding.ratingTxt.text = product.rating.toString()

        val imageUrl = product.picUrl.firstOrNull()
        val requestOptions = RequestOptions().transform(CenterCrop())

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(holder.binding.imgProduct)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailActivity::class.java)
            intent.putExtra("EXTRA_PRODUCT_ID", product.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        // Línea de diagnóstico 2
        Log.d(TAG, "getItemCount() reportando ${items.size} productos.")
        return items.size
    }
}