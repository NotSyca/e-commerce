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
import com.example.e_commerce.R
import com.example.e_commerce.UI.Adapter.PopularAdapter
import com.example.e_commerce.databinding.FragmentCategoryProductsBinding

class CategoryProductsFragment : Fragment() {

    private var _binding: FragmentCategoryProductsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private var brandId: Int = -1
    private var categoryName: String = ""

    private val TAG = "CategoryProductsFragment"

    companion object {
        private const val ARG_BRAND_ID = "brand_id"
        private const val ARG_CATEGORY_NAME = "category_name"

        fun newInstance(brandId: Int, categoryName: String): CategoryProductsFragment {
            val fragment = CategoryProductsFragment()
            val args = Bundle()
            args.putInt(ARG_BRAND_ID, brandId)
            args.putString(ARG_CATEGORY_NAME, categoryName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            brandId = it.getInt(ARG_BRAND_ID, -1)
            categoryName = it.getString(ARG_CATEGORY_NAME, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryProductsBinding.inflate(inflater, container, false)
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
        binding.tvCategoryName.text = categoryName

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
        if (brandId == -1) {
            Log.e(TAG, "Invalid brand ID")
            showEmptyState()
            return
        }

        showLoading()

        Log.d(TAG, "Loading products for brand ID: $brandId")

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

        viewModel.loadProductsByBrandId(brandId)
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
