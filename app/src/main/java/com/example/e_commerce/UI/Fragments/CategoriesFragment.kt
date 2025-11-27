package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.Model.BrandModel
import com.example.e_commerce.Model.MainViewModel
import com.example.e_commerce.UI.Adapter.BrandAdapter
import com.example.e_commerce.databinding.FragmentCategoriesBinding

/**
 * CategoriesFragment - Muestra todas las categorías/marcas disponibles.
 */
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener el ViewModel compartido
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        setupRecyclerView()
        loadCategories()
    }

    private fun setupRecyclerView() {
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadCategories() {
        showLoading()

        viewModel.brands.observe(viewLifecycleOwner) { brands ->
            if (brands.isNotEmpty()) {
                // Usar el BrandAdapter existente en modo vertical
                binding.rvCategories.adapter = BrandAdapter(
                    brands as MutableList<BrandModel>,
                    { brandId -> onCategoryClick(brandId) },
                    { brandId -> onCategoryClick(brandId) }
                )
                showCategories()
            } else {
                showLoading()
            }
        }

        // Cargar las marcas/categorías
        viewModel.loadBrands()
    }

    private fun onCategoryClick(brandId: Int) {
        Toast.makeText(
            requireContext(),
            "Categoría seleccionada: $brandId",
            Toast.LENGTH_SHORT
        ).show()
        // TODO: Navegar a la vista de productos filtrados por esta categoría
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvCategories.visibility = View.GONE
    }

    private fun showCategories() {
        binding.progressBar.visibility = View.GONE
        binding.rvCategories.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
