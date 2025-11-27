// Archivo: UI/Fragments/OrderDetailFragment.kt

package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.databinding.FragmentOrderDetailBinding
import com.example.e_commerce.Model.Product // Asegúrate de que Product esté disponible
import com.example.e_commerce.UI.Adapter.OrderItemsAdapter
import com.example.e_commerce.UI.HomeActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.time.Instant
import java.time.format.DateTimeFormatter

// Estructura que obtendremos de la base de datos (con JOIN anidado)

@Serializable
data class OrderDetailHeader(
    val id: Int,
    val user_id: String,
    val total_amount: Double?,
    val status: String,
    val address_shipping: String?,
    val created_at: String,
    val order_items: List<OrderItemDetail>,
    val payment_intent_id: String? = null
)

class OrderDetailFragment : Fragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private var orderId: Int? = null
    private val TAG = "OrderDetailFragment"

    companion object {
        const val ARG_ORDER_ID = "order_id"

        fun newInstance(orderId: Int) = OrderDetailFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_ORDER_ID, orderId)
            }
        }
    }

    private val supabase: SupabaseClient
        get() = (requireActivity() as HomeActivity).supabase

    private lateinit var orderItemsAdapter: OrderItemsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getInt(ARG_ORDER_ID, 0) // <-- CAMBIO CLAVE AQUÍ: Usar getInt y proporcionar un valor por defecto

        if (orderId != null && orderId != 0) { // Comprobar si orderId no es nulo y es un ID válido
            setupRecyclerView()
            loadOrderDetail(orderId!!)
        } else {
            Toast.makeText(requireContext(), "ID de orden no proporcionado.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        // Inicializar el adaptador con una lista vacía
        orderItemsAdapter = OrderItemsAdapter(emptyList())
        binding.rvOrderItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderItemsAdapter
        }
    }

    /**
     * Carga el detalle completo de la orden incluyendo los ítems anidados.
     */
    private fun loadOrderDetail(id: Int) {
        // Muestra carga (asumimos que tienes un ProgressBar)
        // binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Consulta CRÍTICA: Obtener la cabecera y el detalle anidado (order_items)
                val order = supabase.from("orders")
                    .select(
                        // ✅ CORRECCIÓN: Usamos la sintaxis para forzar el JOIN anidado
                        columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, order_items(*, product:Product(*))")
                    ) {
                        filter { eq("id", id) }
                    }
                    .decodeSingleOrNull<OrderDetailHeader>()// Decodificar a la cabecera con el array de ítems

                withContext(Dispatchers.Main) {
                    if (order != null) {
                        displayOrderDetails(order)
                    } else {
                        Toast.makeText(requireContext(), "Detalles de orden no encontrados.", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error al cargar el detalle de la orden: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error de red al cargar detalles.", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    /**
     * Rellena la UI con los datos de la orden.
     */
    private fun displayOrderDetails(order: OrderDetailHeader) {
        // 1. Cabecera
        val shortId = order.id.toString().take(8).uppercase(Locale.getDefault()) // Convertir a String antes de take(8)
        binding.tvDetailTitle.text = "Detalle de Orden #$shortId"
        binding.tvStatus.text = "Estado: ${order.status.uppercase(Locale.getDefault())}"
        binding.tvShippingAddress.text = "Dirección: ${order.address_shipping ?: "N/A"}"
        binding.tvTotalAmountDetail.text = String.format(Locale.US, "$%.2f", order.total_amount ?: 0.0)

        try {
            val date = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Instant.parse(order.created_at).toEpochMilli()
            } else { 0L }
            val dateFormatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            binding.tvOrderPlaced.text = "Pedido realizado el ${dateFormatter.format(date)}"
        } catch (e: Exception) {
            binding.tvOrderPlaced.text = "Fecha no disponible"
        }

        // 2. Ítems del Pedido
        orderItemsAdapter.updateItems(order.order_items)
        // binding.progressBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}