package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.Model.BrandModel
import com.example.e_commerce.Model.MainViewModel
import com.example.e_commerce.R
import com.example.e_commerce.UI.Adapter.CategoryGridAdapter
import com.example.e_commerce.databinding.FragmentCategoriesBinding

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

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        setupRecyclerView()
        loadCategories()
    }

    private fun setupRecyclerView() {
        // Cambiado a una cuadrícula de 2 columnas
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun loadCategories() {
        showLoading()

        viewModel.brands.observe(viewLifecycleOwner) { brands ->
            if (brands.isNotEmpty()) {
                binding.rvCategories.adapter = CategoryGridAdapter(
                    brands as MutableList<BrandModel>
                ) { brandId ->
                    onCategoryClick(brandId)
                }
                showCategories()
            } else {
                showLoading()
            }
        }
        viewModel.loadBrands()
    }

    private fun onCategoryClick(brandId: Int) {
        if (brandId == -1) {
            // El usuario deseleccionó la categoría
            return
        }

        // Obtener el nombre de la categoría del viewModel
        val categoryName = viewModel.brands.value?.find { it.id == brandId }?.name ?: "Categoría"

        // Navegar al fragment de productos de la categoría
        val categoryProductsFragment = CategoryProductsFragment.newInstance(brandId, categoryName)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, categoryProductsFragment)
            .addToBackStack(null)
            .commit()
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