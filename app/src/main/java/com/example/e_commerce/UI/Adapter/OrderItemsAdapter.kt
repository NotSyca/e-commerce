// Archivo: UI/Adapter/OrderItemsAdapter.kt

package com.example.e_commerce.UI.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.e_commerce.UI.Fragments.OrderItemDetail
import com.example.e_commerce.databinding.ViewholderOrderitemBinding // Necesitas crear este binding
import java.util.Locale

class OrderItemsAdapter(
    private var items: List<OrderItemDetail>
) : RecyclerView.Adapter<OrderItemsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ViewholderOrderitemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Asegúrate de que ViewholderOrderItemBinding exista y use viewholder_order_item.xml
        val binding = ViewholderOrderitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val product = item.product // El objeto Producto completo

        holder.binding.tvItemName.text = product?.title // Asumo un TextView 'tvItemName'
        holder.binding.tvItemQuantity.text = "Cant: ${item.quantity}" // Asumo 'tvItemQuantity'
        holder.binding.tvItemPrice.text = String.format(Locale.US, "$%.2f", item.product?.price)
        holder.binding.tvItemSize.text = "Talla: ${item.selected_size}" // Asumo 'tvItemSize'

        Glide.with(holder.itemView.context).load(product?.picUrl?.firstOrNull()).into(holder.binding.imgItem)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<OrderItemDetail>?) {
        items = newItems ?: emptyList()
        notifyDataSetChanged()
    }
}