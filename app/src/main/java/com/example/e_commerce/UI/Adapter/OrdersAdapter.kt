package com.example.e_commerce.UI.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.e_commerce.databinding.ViewholderOrderBinding
import android.widget.Toast
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_commerce.UI.Fragments.OrderDetailFragment
import com.example.e_commerce.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.time.Instant
import android.util.Log // Importar Log

class OrdersAdapter(private var items: List<com.example.e_commerce.UI.Fragments.OrderHeader>) : RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    class ViewHolder(val binding: ViewholderOrderBinding) : RecyclerView.ViewHolder(binding.root)

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val TAG = "OrdersAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = items[position]

        Log.d(TAG, "Binding order at position $position: ID = ${order.id}, Total = ${order.total_amount}, Status = ${order.status}, CreatedAt = ${order.created_at}")

        // 1. ID de la Orden
        // order.id es ahora Int, no String o nullable. Convertirlo a String para take(8)
        val idText = order.id.toString()
        val shortId = idText.take(8).uppercase(Locale.getDefault())
        holder.binding.tvOrderId.text = "Orden ID: #$shortId"

        // 2. Monto Total
        // Manejar total_amount que ahora es Double?
        val totalAmount = order.total_amount ?: 0.0 // Usar 0.0 si es null
        holder.binding.tvOrderTotal.text = String.format(Locale.US, "$%.2f", totalAmount)

        // 3. Estado y Color
        holder.binding.tvOrderStatus.text = order.status.uppercase(Locale.getDefault())
        // Puedes implementar lógica aquí para cambiar el color del estado
        if (order.status == "enviado") holder.binding.tvOrderStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))

        // 4. Fecha
        // Convertir la cadena de fecha ISO (created_at) a un formato legible
        try {
             val date = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Instant.parse(order.created_at).toEpochMilli()
            } else {
                // Alternativa para APIs antiguas si usas Date:
                // dateFormatter.parse(order.created_at)?.time
                0L // Valor por defecto
            }
            holder.binding.tvOrderDate.text = "Fecha: ${dateFormatter.format(date)}"
        } catch (e: Exception) {
            holder.binding.tvOrderDate.text = "Fecha: N/A"
        }

        holder.itemView.setOnClickListener {
            val fragment = OrderDetailFragment()
            val args = Bundle()
            args.putInt("order_id", order.id) // <-- CAMBIO CLAVE AQUÍ: Usar putInt
            fragment.arguments = args

            val activity = holder.itemView.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment) // Replace 'fragment_container' with your actual container ID
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getItemCount(): Int = items.size
    fun updateOrders(newItems: List<com.example.e_commerce.UI.Fragments.OrderHeader>) {
        items = newItems
        notifyDataSetChanged()
    }
}