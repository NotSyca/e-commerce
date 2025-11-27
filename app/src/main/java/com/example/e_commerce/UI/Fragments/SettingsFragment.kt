package com.example.e_commerce.UI.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.Model.ProfileViewModel
import com.example.e_commerce.Model.ProfileViewModelFactory
import com.example.e_commerce.Model.SettingsViewModel
import com.example.e_commerce.Model.User
import com.example.e_commerce.R
import com.example.e_commerce.UI.Adapter.SearchHistoryAdapter
import com.example.e_commerce.UI.BaseActivity
import com.example.e_commerce.UI.EditCategoryFragment
import com.example.e_commerce.UI.EditProfileActivity
import com.example.e_commerce.UI.MainActivity
import com.example.e_commerce.databinding.FragmentSettingsBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private lateinit var supabase: SupabaseClient
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter

    private val TAG = "SettingsFragment"
    private val PROFILES_TABLE_NAME = "profiles"

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

        val baseActivity = requireActivity() as BaseActivity
        tokenManager = baseActivity.tokenManager
        supabase = baseActivity.supabase
        settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        loadUserProfile()
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

        binding.optionManageUsers.setOnClickListener {
            navigateToFragment(UsersListFragment())
        }

        binding.optionNotifications.setOnClickListener {
            // TODO: Implementar notificaciones
        }

        binding.optionPrivacy.setOnClickListener {
            // TODO: Implementar privacidad
        }

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = tokenManager.getUserId()
                if (userId.isNullOrBlank()) {
                    Log.w(TAG, "Usuario no autenticado o ID nulo.")
                    showDefaultProfile(email = tokenManager.getEmail() ?: "N/D")
                    return@launch
                }

                Log.d(TAG, "Cargando perfil para ID: $userId")
                val userProfile = supabase.from(PROFILES_TABLE_NAME)
                    .select() {
                        filter { eq("user_id", userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<User>()

                val email = supabase.auth.currentUserOrNull()?.email ?: "No disponible"

                if (userProfile != null) {
                    displayUserProfile(userProfile, email)
                } else {
                    Log.w(TAG, "Perfil no encontrado en DB, mostrando datos de sesión.")
                    showDefaultProfile(email = email)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar perfil", e)
                showDefaultProfile(email = tokenManager.getEmail() ?: "Error de conexión")
            }
        }
    }

    private fun displayUserProfile(user: User, email: String) {
        if (_binding == null) return

        val fullName = "${user.firstName} ${user.lastName}"
        binding.tvName.text = fullName.trim()

        val initial = user.firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        binding.tvAvatarInitial.text = initial

        binding.tvEmail.text = email
        binding.tvPhone.text = user.phone ?: "Teléfono: No registrado"
        binding.tvAddress.text = user.address ?: "Dirección: No registrada"

        val roleText = if (user.isAdmin == true) "ADMINISTRADOR" else "USUARIO ESTÁNDAR"
        binding.tvRole.text = "Rol: $roleText"

        Log.d(TAG, "Perfil mostrado: $fullName")
    }

    private fun showDefaultProfile(email: String) {
        if (_binding == null) return
        binding.tvName.text = tokenManager.getUserName() ?: "USUARIO INVITADO"
        binding.tvAvatarInitial.text = tokenManager.getUserName()?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        binding.tvEmail.text = email
        binding.tvPhone.text = "Teléfono: N/D"
        binding.tvAddress.text = "Dirección: N/D"
        binding.tvRole.text = "ROL: Invitado"
    }

    private fun performLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                supabase.auth.signOut()
                tokenManager.clearSession()

                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()

            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar sesión", e)
                tokenManager.clearSession()
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Recargar perfil cuando se regresa de EditProfileActivity
        if (_binding != null) {
            loadUserProfile()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}