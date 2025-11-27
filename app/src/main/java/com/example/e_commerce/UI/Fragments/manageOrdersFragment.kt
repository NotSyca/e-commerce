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
import com.example.e_commerce.UI.Adapter.ManageOrdersAdapter
import com.example.e_commerce.UI.HomeActivity
import com.example.e_commerce.databinding.FragmentManageOrdersBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class ManageOrdersFragment : Fragment() {

    private var _binding: FragmentManageOrdersBinding? = null
    private val binding get() = _binding!!
    private val TAG = "ManageOrdersFragment"

    private lateinit var ordersAdapter: ManageOrdersAdapter
    private var ordersList: MutableList<OrderHeader> = mutableListOf()

    private val supabase: SupabaseClient
        get() = (requireActivity() as HomeActivity).supabase

    // NOTA: Se asume que OrderHeader y OrderItemDetail están definidos y son @Serializable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadAllOrders()
        setupSearch()
    }

    private fun setupRecyclerView() {
        ordersAdapter = ManageOrdersAdapter(ordersList, ::onOrderActionSelected)
        binding.rvOrdersList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdersList.adapter = ordersAdapter
    }

    /**
     * Carga TODAS las órdenes de TODOS los usuarios (Función de Admin).
     */
    private fun loadAllOrders() {
        binding.progressBar.visibility = View.VISIBLE
        Log.d(TAG, "LOAD_DB: Consultando TODAS las órdenes...")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Consulta para obtener la orden, el detalle y la información del usuario propietario (profiles)
                val response = supabase.from("orders")
                    .select(
                        columns = io.github.jan.supabase.postgrest.query.Columns.raw(
                            "*, order_items(*, product:Product(*)), client:profiles(*)"// <-- USAR product_id COMO NOMBRE DE LA RELACIÓN
                        )
                    ) {
                    }
                    .decodeList<OrderHeader>()

                withContext(Dispatchers.Main) {
                    ordersList.clear()
                    ordersList.addAll(response)
                    ordersAdapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                    Log.i(TAG, "LOAD_SUCCESS: ${ordersList.size} órdenes cargadas.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Log.e(TAG, "LOAD_ERROR: Fallo al cargar órdenes: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error al cargar órdenes de admin.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupSearch() {
        // Implementación de la búsqueda aquí
    }

    // -------------------------------------------------------------------
    // MANEJO DE ACCIONES (Llamado por el Adaptador)
    // -------------------------------------------------------------------

    /**
     * Maneja las acciones seleccionadas en el menú de cada orden (Eliminar, En Camino, Entregado).
     */
    private fun onOrderActionSelected(orderId: String, action: String) {
        Log.d(TAG, "ACTION: Order ID $orderId, Acción: $action")

        when (action) {
            "ENTREGADO" -> updateOrderStatus(orderId, "entregado")
            "EN CAMINO" -> updateOrderStatus(orderId, "en_camino")
            "ELIMINAR" -> confirmDeleteOrder(orderId)

            // ✅ NUEVA LÓGICA: VER DETALLES
            "VER DETALLES" -> showOrderDetail(orderId)
        }
    }

    private fun showOrderDetail(orderId: String) {
        // Asumimos que OrderDetailFragment.newInstance(orderId) existe y toma el UUID
        val detailFragment = OrderDetailFragment.newInstance(orderId.toInt())

        // Usamos el gestor de fragmentos de la actividad para la transición.
        requireActivity().supportFragmentManager.beginTransaction()
            // Reemplaza R.id.fragment_container con el ID de tu FrameLayout principal si es diferente.
            .replace(com.example.e_commerce.R.id.fragment_container, detailFragment)
            .addToBackStack(null) // Permite volver
            .commit()

        Log.d(TAG, "NAVIGATION: Lanzando OrderDetailFragment para ID: $orderId")
    }

    private fun updateOrderStatus(orderId: String, newStatus: String) {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updateData = mapOf("status" to newStatus)

                supabase.from("orders")
                    .update(updateData) { filter { eq("id", orderId) } }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Orden $orderId marcada como $newStatus.", Toast.LENGTH_SHORT).show()
                    loadAllOrders() // Recargar la lista para mostrar el nuevo estado
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "UPDATE_ERROR: Fallo al actualizar estado: ${e.message}", e)
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun deleteOrder(orderId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "DELETE_ORDER: Iniciando eliminación en cascada para Orden ID: $orderId")

                Log.d(TAG, "DELETE_DB: Eliminando ítems del carrito (order_items) asociados a la orden.")
                supabase.from("order_items").delete {
                    filter { eq(column = "order_id", value = orderId) }
                }
                Log.d(TAG, "DELETE_DB: Eliminando la cabecera de la orden.")

                supabase.from("orders").delete {
                    filter { eq(column = "id", value = orderId) }
                }
                Log.i(TAG, "DELETE_SUCCESS: Orden ID $orderId eliminada con éxito.")
                Toast.makeText(requireContext(), "Orden eliminada con éxito. 🗑️", Toast.LENGTH_LONG).show()

                loadAllOrders()
            } catch (e: Exception) {
                Log.e(TAG, "ERROR_DB: Fallo al eliminar la orden: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al eliminar la orden: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmDeleteOrder(orderId: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación de Orden")
            .setMessage("¿Estás seguro de eliminar la Orden # ${orderId.take(4).uppercase()}? Esto eliminará todos los ítems asociados.")
            .setPositiveButton("Eliminar") { dialog, which ->
                deleteOrder(orderId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}