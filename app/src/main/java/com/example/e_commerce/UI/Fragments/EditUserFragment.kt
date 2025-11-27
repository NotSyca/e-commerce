package com.example.e_commerce.UI.Fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.Model.User
import com.example.e_commerce.Model.UserProfileUpdate
import com.example.e_commerce.R
import com.example.e_commerce.UI.BaseActivity
import com.example.e_commerce.databinding.FragmentEditUserBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class EditUserFragment : Fragment() {

    private var _binding: FragmentEditUserBinding? = null
    private val binding get() = _binding!!

    private val tokenManager: TokenManager by lazy { (requireActivity() as BaseActivity).tokenManager }
    private val supabase: SupabaseClient by lazy { (requireActivity() as BaseActivity).supabase }

    private var userId: String? = null
    private var currentUser: User? = null

    private val TAG = "EditUserFragment"
    private val PROFILES_TABLE_NAME = "profiles"

    companion object {
        private const val ARG_USER_ID = "user_id"

        fun newInstance(userId: String): EditUserFragment {
            val fragment = EditUserFragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        loadUserData()
    }

    private fun setupListeners() {
        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSave.setOnClickListener {
            saveUserChanges()
        }

        binding.btnDelete.setOnClickListener {
            confirmDeleteUser()
        }
    }

    private fun loadUserData() {
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Cargando datos del usuario: $userId")

                val user = supabase.from(PROFILES_TABLE_NAME)
                    .select() {
                        filter { eq("user_id", userId!!) }
                        limit(1)
                    }
                    .decodeSingleOrNull<User>()

                if (user != null) {
                    currentUser = user
                    displayUserData(user)
                } else {
                    Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }

                binding.progressBar.visibility = View.GONE

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar usuario", e)
                Toast.makeText(requireContext(), "Error al cargar usuario: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayUserData(user: User) {
        binding.etFirstName.setText(user.firstName)
        binding.etLastName.setText(user.lastName)
        binding.etEmail.setText(user.email ?: "")
        binding.etPhone.setText(user.phone ?: "")
        binding.etAddress.setText(user.address ?: "")
        binding.cbIsAdmin.isChecked = user.isAdmin ?: false
    }

    private fun saveUserChanges() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val isAdmin = binding.cbIsAdmin.isChecked

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre y apellido son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "El email es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Actualizando usuario: $userId")

                val updateData = UserProfileUpdate(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    address = address,
                    isAdmin = isAdmin
                )

                supabase.from(PROFILES_TABLE_NAME).update(updateData) {
                    filter { eq("user_id", userId!!) }
                }

                Toast.makeText(requireContext(), "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show()

                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true

                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar usuario", e)
                Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun confirmDeleteUser() {
        val currentUserId = tokenManager.getUserId()

        if (userId == currentUserId) {
            Toast.makeText(requireContext(), "No puedes eliminar tu propia cuenta", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de que deseas eliminar este usuario? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteUser()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteUser() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnDelete.isEnabled = false
        binding.btnSave.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Eliminando usuario: $userId")

                val result = supabase.from(PROFILES_TABLE_NAME).delete {
                    filter { eq("user_id", userId!!) }
                }

                Log.d(TAG, "Usuario eliminado de profiles exitosamente")

                Toast.makeText(requireContext(), "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show()

                binding.progressBar.visibility = View.GONE

                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar usuario: ${e.javaClass.simpleName}", e)
                Log.e(TAG, "Mensaje de error: ${e.message}", e)
                Log.e(TAG, "Stack trace completo:", e)
                Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnDelete.isEnabled = true
                binding.btnSave.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
