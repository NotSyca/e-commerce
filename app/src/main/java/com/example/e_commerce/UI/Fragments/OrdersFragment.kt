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
import com.example.e_commerce.UI.Adapter.OrdersAdapter
import com.example.e_commerce.UI.HomeActivity
import com.example.e_commerce.databinding.FragmentOrdersBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val TAG = "OrdersFragment"

    private lateinit var ordersAdapter: OrdersAdapter
    private var ordersList: List<OrderHeader> = emptyList()
    private val supabase: SupabaseClient
        get() = (requireActivity() as HomeActivity).supabase
    private val tokenManager
        get() = (requireActivity() as HomeActivity).tokenManager


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadOrders()
    }

    private fun setupRecyclerView() {
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        // Inicializar el adapter con la lista vacía o actual
        ordersAdapter = OrdersAdapter(ordersList) // Asume que OrdersAdapter está definido
        binding.rvOrders.adapter = ordersAdapter
    }

    /**
     * Carga las órdenes del usuario actual desde Supabase.
     */
    private fun loadOrders() {
        val userId = tokenManager.getUserId() ?: supabase.auth.currentUserOrNull()?.id

        if (userId.isNullOrBlank()) {
            Log.e(TAG, "LOAD_FAIL: No se encontró ID de usuario para cargar órdenes.")
            showEmptyState()
            Toast.makeText(requireContext(), "Inicia sesión para ver tu historial.", Toast.LENGTH_LONG).show()
            return
        }

        showLoading()
        Log.d(TAG, "LOAD_DB: Consultando órdenes para User ID: $userId")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Consulta la tabla 'orders' filtrando por el user_id
                val response = supabase.from("orders")
                    .select(

                        columns = io.github.jan.supabase.postgrest.query.Columns.raw(
                            "*, order_items(*, product:Product(*))")){
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<OrderHeader>()

                withContext(Dispatchers.Main) {
                    ordersList = response
                    ordersAdapter.updateOrders(ordersList) // Asume este método en tu adapter
                    Log.i(TAG, "LOAD_SUCCESS: Órdenes cargadas. Total: ${ordersList.size}")

                    if (ordersList.isNotEmpty()) {
                        showOrders()
                    } else {
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "LOAD_ERROR: Fallo al cargar órdenes: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error de red/DB al cargar historial.", Toast.LENGTH_LONG).show()
                    showEmptyState()
                }
            }
        }
    }


    private fun showEmptyState() {
        binding.emptyStateCard.visibility = View.VISIBLE
        binding.rvOrders.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun showOrders() {
        binding.emptyStateCard.visibility = View.GONE
        binding.rvOrders.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateCard.visibility = View.GONE
        binding.rvOrders.visibility = View.GONE
    }

    // NOTA: Si el fragmento se recarga (onResume), debes volver a llamar a loadOrders().
    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}