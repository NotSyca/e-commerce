package com.example.e_commerce.UI.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.e_commerce.UI.BaseActivity // Usado para acceder a servicios de la Activity
import com.example.e_commerce.databinding.FragmentProfileBinding
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.Model.User
import com.example.e_commerce.UI.MainActivity
import com.example.e_commerce.UI.EditProfileActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

/**
 * ProfileFragment
 * Optimizado para carga rápida y acceso seguro a servicios heredados.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // CRÍTICO: Acceder a los servicios de la Activity contenedora (HomeActivity/BaseActivity)
    // Esto es más rápido y previene la inicialización tardía en el Fragmento.
    private val tokenManager: TokenManager by lazy { (requireActivity() as BaseActivity).tokenManager }
    private val supabase: SupabaseClient by lazy { (requireActivity() as BaseActivity).supabase }

    private val TAG = "ProfileFragment"
    private val PROFILES_TABLE_NAME = "profiles" // Nombre de la tabla de perfiles

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cargar información del usuario al crear la vista
        loadUserProfile()

        // Configurar listeners
        setupListeners()
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

                Log.d(TAG, "Cargando perfil optimizado para ID: $userId")
                val userProfile = supabase.from(PROFILES_TABLE_NAME)
                    .select() {
                        // Filtro que coincide con el ID de la sesión actual
                        filter { eq("user_id", userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<User>() // Devuelve null si no se encuentra

                // Obtener el email de Auth (ya que la tabla 'profiles' podría no tenerlo)
                val email = supabase.auth.currentUserOrNull()?.email ?: "No disponible"

                if (userProfile != null) {
                    displayUserProfile(userProfile, email)
                } else {
                    Log.w(TAG, "Perfil no encontrado en DB, mostrando datos de sesión.")
                    showDefaultProfile(email = email)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar/decodificar el perfil", e)
                showDefaultProfile(email = tokenManager.getEmail() ?: "Error de conexión")
            }
        }
    }

    private fun displayUserProfile(user: User, email: String) {
        if (_binding == null) return

        // Nombre completo (se asume que firstName y lastName no son nulos)
        val fullName = "${user.firstName} ${user.lastName}"
        binding.tvName.text = fullName.trim()

        // Avatar con inicial
        val initial = user.firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        binding.tvAvatarInitial.text = initial

        // Email y Contacto (IDs sincronizados con el XML)
        binding.tvEmail.text = email
        binding.tvPhone.text = user.phone ?: "Teléfono: No registrado"
        binding.tvAddress.text = user.address ?: "Dirección: No registrada"

        // Rol
        val roleText = if (user.isAdmin == true) "ADMINISTRADOR" else "USUARIO ESTÁNDAR"
        binding.tvRole.text = "Rol: $roleText"

        Log.d(TAG, "Perfil mostrado: $fullName")
    }

    private fun showDefaultProfile(email: String) {
        if (_binding == null) return
        // Muestra datos genéricos usando tokenManager y marcadores
        binding.tvName.text = tokenManager.getUserName() ?: "USUARIO INVITADO"
        binding.tvAvatarInitial.text = tokenManager.getUserName()?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        binding.tvEmail.text = email
        binding.tvPhone.text = "Teléfono: N/D"
        binding.tvAddress.text = "Dirección: N/D"
        binding.tvRole.text = "ROL: Invitado"
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    // -------------------------------------------------------------------
    // LÓGICA DE CIERRE DE SESIÓN Y CICLO DE VIDA
    // -------------------------------------------------------------------

    private fun performLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                supabase.auth.signOut()
                tokenManager.clearSession()

                // Navegar al login y limpiar la pila
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()

            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar sesión", e)
                tokenManager.clearSession()
                // Forzar la navegación incluso si la API falla
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarga cuando el usuario regresa de EditProfileActivity
        refreshData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshData()
        }
    }

    private fun refreshData() {
        if (_binding != null) {
            loadUserProfile()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}