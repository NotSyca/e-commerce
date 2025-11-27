package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.R
import com.example.e_commerce.UI.BaseActivity
import com.example.e_commerce.databinding.FragmentAddUserBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ProfileInsert(
    val user_id: String,
    val first_name: String,
    val last_name: String,
    val phone: String,
    val address: String,
    val is_admin: Boolean = false,
    val email: String
)

class AddUserFragment : Fragment() {

    private var _binding: FragmentAddUserBinding? = null
    private val binding get() = _binding!!

    private val tokenManager: TokenManager by lazy { (requireActivity() as BaseActivity).tokenManager }
    private val supabase: SupabaseClient by lazy { (requireActivity() as BaseActivity).supabase }

    private val TAG = "AddUserFragment"
    private val PROFILES_TABLE_NAME = "profiles"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCreate.setOnClickListener {
            createUser()
        }
    }

    private fun createUser() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val isAdmin = binding.cbIsAdmin.isChecked

        // Validaciones
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
            password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Los campos obligatorios son: nombre, apellido, email y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Introduce un formato de email válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreate.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Iniciando creación de usuario: $email")

                // PASO 1: Crear usuario en Supabase Auth
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                Log.d(TAG, "Usuario creado en Auth, obteniendo sesión...")

                // Obtener el usuario creado
                val currentUser = supabase.auth.currentUserOrNull()

                if (currentUser == null) {
                    Log.e(TAG, "Error: No hay sesión después del signup")
                    throw Exception("No se pudo crear la sesión del usuario")
                }

                val userId = currentUser.id
                Log.d(TAG, "Usuario ID obtenido: $userId")

                // PASO 2: Insertar perfil en la tabla public.profiles
                val profileData = ProfileInsert(
                    user_id = userId,
                    first_name = firstName,
                    last_name = lastName,
                    phone = phone,
                    address = address,
                    is_admin = isAdmin,
                    email = email
                )

                Log.d(TAG, "Insertando perfil en profiles...")

                supabase.from(PROFILES_TABLE_NAME)
                    .insert(profileData)

                Log.d(TAG, "Perfil creado exitosamente para user_id: $userId")

                // PASO 3: Cerrar la sesión del usuario recién creado para no afectar la sesión del admin
                supabase.auth.signOut()
                Log.d(TAG, "Sesión del nuevo usuario cerrada")

                Toast.makeText(requireContext(), "Usuario creado exitosamente", Toast.LENGTH_SHORT).show()

                binding.progressBar.visibility = View.GONE
                binding.btnCreate.isEnabled = true

                // Volver a la lista de usuarios
                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                Log.e(TAG, "Error al crear usuario", e)
                Toast.makeText(requireContext(), "Error al crear usuario: ${e.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnCreate.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
