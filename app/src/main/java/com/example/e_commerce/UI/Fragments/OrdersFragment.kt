package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.databinding.FragmentOrdersBinding

/**
 * OrdersFragment - Muestra el historial de órdenes del usuario.
 */
class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

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
        // TODO: Configurar el adapter cuando se implemente la funcionalidad de órdenes
        // binding.rvOrders.adapter = OrdersAdapter(ordersList)
    }

    private fun loadOrders() {
        // Mostrar estado vacío por defecto
        // TODO: Implementar carga de órdenes desde la base de datos
        showEmptyState()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
