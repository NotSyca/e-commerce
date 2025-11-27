package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.e_commerce.Model.MainViewModel
import com.example.e_commerce.UI.Adapter.PopularAdapter
import com.example.e_commerce.databinding.FragmentAllProductsBinding

class AllProductsFragment : Fragment() {

    private var _binding: FragmentAllProductsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel

    private val TAG = "AllProductsFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        setupUI()
        setupRecyclerView()
        loadProducts()
    }

    private fun setupUI() {
        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.rvProducts.isNestedScrollingEnabled = false
        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = PopularAdapter(arrayListOf())
    }

    private fun loadProducts() {
        showLoading()

        Log.d(TAG, "Loading all products")

        viewModel.populars.observe(viewLifecycleOwner) { productsList ->
            hideLoading()

            if (productsList.isEmpty()) {
                showEmptyState()
            } else {
                showProducts()
                (binding.rvProducts.adapter as? PopularAdapter)?.updateData(productsList)
                Log.d(TAG, "Products loaded: ${productsList.size}")
            }
        }

        viewModel.loadPopular()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvProducts.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showProducts() {
        binding.rvProducts.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.rvProducts.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
