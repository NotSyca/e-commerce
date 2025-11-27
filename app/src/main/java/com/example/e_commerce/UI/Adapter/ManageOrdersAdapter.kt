// Archivo: UI/Adapter/ManageOrdersAdapter.kt

package com.example.e_commerce.UI.Adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.e_commerce.R // Asumimos que tienes un menú XML aquí
import com.example.e_commerce.databinding.ViewholderManageOrderBinding
import com.example.e_commerce.UI.Fragments.OrderHeader
import io.github.jan.supabase.postgrest.query.Columns

import java.util.Locale
import java.time.Instant
import java.text.SimpleDateFormat

class ManageOrdersAdapter(
    private val items: MutableList<OrderHeader>,
    private val onActionSelected: (orderId: String, action: String) -> Unit
) : RecyclerView.Adapter<ManageOrdersAdapter.ViewHolder>() {

    class ViewHolder(val binding: ViewholderManageOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ViewholderManageOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = items[position]
        val context = holder.itemView.context

        // NOTA: Asumimos que OrderHeader tiene un campo 'user: UserProfile'
        // Y que UserProfile tiene first_name y last_name.
        val clientName =
            order.client?.let { "${it.first_name} ${it.last_name}" } ?: "Usuario Desconocido"

        val totalItems = order.order_items?.size ?: 0

        // 1. Corrección de la Conversión de ID: usar .id.toString().take(4) para el UUID
        val shortId = order.id.toString().take(4).uppercase(Locale.getDefault())

        // Formateo de Fecha (Asumiendo que has importado java.time.Instant correctamente)
        val dateString = try {
            val date = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant.parse(order.created_at).toEpochMilli()
            } else {
                0L
            }
            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "Fecha N/A"
        }


        // 1. Enlace de datos de la tarjeta
        holder.binding.tvOrderHeader.text = "Orden #$shortId - $clientName" // Usamos el short ID
        holder.binding.tvOrderStatus.text = order.status.uppercase(Locale.getDefault())
        holder.binding.tvOrderSummary.text = String.format(
            Locale.US, "$%.2f | %d ítems | %s",
            order.total_amount,
            totalItems,
            dateString
        )

        // 2. Listener del Menú de Acciones
        holder.binding.btnActions.setOnClickListener { view ->
            // Convertimos el ID de la orden (UUID) a String de forma segura
            showPopupMenu(view, order.id.toString())
        }
    }

    override fun getItemCount(): Int = items.size

    // Método para actualizar la lista (llamado desde loadAllOrders)
    fun notifyItemUpdated(orderId: String) {
        val index = items.indexOfFirst { it.id.toString() == orderId }
        if (index != RecyclerView.NO_POSITION) {
            notifyItemChanged(index)
        }
    }

    fun updateOrders(newItems: List<OrderHeader>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Función para mostrar el menú de acciones de la orden
    private fun showPopupMenu(view: View, orderId: String) {
        val popup = PopupMenu(view.context, view)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // GRAVITY_END (similar a derecha) puede funcionar mejor en tu caso que el valor por defecto
            popup.setGravity(android.view.Gravity.END)
        }

        popup.menu.add(0, 1, 0, "Marcar EN CAMINO")
        popup.menu.add(0, 2, 1, "Marcar ENTREGADO")
        popup.menu.add(0, 3, 2, "Ver Detalles")
        popup.menu.add(0, 4, 3, "Eliminar Orden")


        popup.setOnMenuItemClickListener { menuItem ->
            val action = when (menuItem.itemId) {
                1 -> "EN CAMINO"
                2 -> "ENTREGADO"
                3 -> "VER DETALLES" // <-- Usar el string exacto que se espera en el Fragmento
                4 -> "ELIMINAR"
                else -> return@setOnMenuItemClickListener true
            }

            // Llamamos a la acción solo si se seleccionó una de las acciones de gestión
            onActionSelected(orderId, action)

            // Devolvemos true para indicar que el clic fue manejado
            true
        }

        // Añadir listener para cuando el menú se cierra sin selección
        popup.setOnDismissListener {
            // Log.d(TAG, "Menú de acciones cerrado.")
        }

        // ✅ CRÍTICO: Asegurarse de que el Contexto de la vista no es nulo
        if (view.context != null) {
            popup.show()
        }
    }
}