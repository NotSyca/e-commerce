package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.e_commerce.Model.MainViewModel
import com.example.e_commerce.Model.Product
import com.example.e_commerce.Model.ProfileViewModel
import com.example.e_commerce.Model.ProfileViewModelFactory
import com.example.e_commerce.UI.Adapter.PopularAdapter
import com.example.e_commerce.UI.BaseActivity
import com.example.e_commerce.databinding.FragmentSearchBinding
import io.github.jan.supabase.gotrue.auth

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var searchAdapter: PopularAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        // Inicializar ProfileViewModel para obtener el ID del usuario
        val baseActivity = requireActivity() as BaseActivity
        val factory = ProfileViewModelFactory(baseActivity.supabase, baseActivity.tokenManager)
        profileViewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)
        profileViewModel.loadUserProfile() // Cargar datos del usuario

        setupRecyclerView()
        setupSearch()
        observeViewModel()

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = PopularAdapter(arrayListOf()) // Inicializar con una lista vacía
        binding.searchResultsList.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = searchAdapter
        }
    }

    private fun setupSearch() {
        binding.searchEdt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 2) { // Empezar a buscar con al menos 2 caracteres
                    // 1. Ejecutar la búsqueda de productos
                    viewModel.searchProducts(query)

                    // 2. Guardar la búsqueda en el historial
                    val userId = (requireActivity() as BaseActivity).supabase.auth.currentUserOrNull()?.id
                    if (userId != null) {
                        viewModel.addSearchToHistory(userId, query)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            updateSearchResults(results)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarSearch.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun updateSearchResults(results: List<Product>) {
        binding.noResultsTxt.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        searchAdapter.updateData(results)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
