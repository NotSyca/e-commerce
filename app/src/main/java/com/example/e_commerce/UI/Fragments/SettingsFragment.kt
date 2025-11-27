package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.Model.SettingsViewModel
import com.example.e_commerce.R
import com.example.e_commerce.UI.Adapter.SearchHistoryAdapter
import com.example.e_commerce.UI.BaseActivity
import com.example.e_commerce.UI.EditCategoryFragment
import com.example.e_commerce.databinding.FragmentSettingsBinding
import io.github.jan.supabase.gotrue.auth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        setupAdminOptions()
        setupClickListeners()
        setupSearchHistoryDropdown()
    }

    private fun setupAdminOptions() {
        val isAdmin = tokenManager.isAdmin()
        binding.ajustesAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.tvAdminSectionTitle.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun setupSearchHistoryDropdown() {
        searchHistoryAdapter = SearchHistoryAdapter(emptyList())
        binding.rvSearchHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchHistoryAdapter
        }

        settingsViewModel.searchHistory.observe(viewLifecycleOwner) {
            searchHistoryAdapter.updateData(it)
        }

        settingsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarHistory.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        binding.historyHeader.setOnClickListener {
            val content = binding.historyContent
            val arrow = binding.arrowIcon

            if (content.visibility == View.GONE) {
                content.visibility = View.VISIBLE
                arrow.rotation = 180f
                loadSearchHistory()
            } else {
                content.visibility = View.GONE
                arrow.rotation = 0f
            }
        }
    }

    private fun loadSearchHistory() {
        val userId = (requireActivity() as BaseActivity).supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            settingsViewModel.fetchSearchHistory(userId)
        } else {
            binding.historyContent.visibility = View.GONE 
        }
    }

    private fun setupClickListeners() {
        binding.optionAddProduct.setOnClickListener {
            navigateToFragment(AddProductFragment())
        }

        binding.optionAddCategoria.setOnClickListener {
            navigateToFragment(AddCategoryFragment())
        }

        binding.optionEditCategoria.setOnClickListener {
            navigateToFragment(EditCategoryFragment())
        }

        binding.optionNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Abrir Notificaciones", Toast.LENGTH_SHORT).show()
        }

        binding.optionPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Abrir Privacidad", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}