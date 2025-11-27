package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.Model.User
import com.example.e_commerce.R
import com.example.e_commerce.UI.Adapter.UserAdapter
import com.example.e_commerce.UI.BaseActivity
import com.example.e_commerce.databinding.FragmentUsersListBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class UsersListFragment : Fragment() {

    private var _binding: FragmentUsersListBinding? = null
    private val binding get() = _binding!!

    private val tokenManager: TokenManager by lazy { (requireActivity() as BaseActivity).tokenManager }
    private val supabase: SupabaseClient by lazy { (requireActivity() as BaseActivity).supabase }

    private lateinit var userAdapter: UserAdapter
    private var allUsers: List<User> = emptyList()

    private val TAG = "UsersListFragment"
    private val PROFILES_TABLE_NAME = "profiles"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupFab()
        loadUsers()
    }

    private fun setupFab() {
        binding.fabAddUser.setOnClickListener {
            navigateToAddUser()
        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(emptyList()) { user ->
            navigateToEditUser(user)
        }

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }
    }

    private fun setupSearchBar() {
        binding.etSearchUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Cargando lista de usuarios...")

                val users = supabase.from(PROFILES_TABLE_NAME)
                    .select()
                    .decodeList<User>()

                // Filtrar solo usuarios que NO son administradores
                val nonAdminUsers = users.filter { user ->
                    user.isAdmin != true
                }

                allUsers = nonAdminUsers
                userAdapter.updateData(nonAdminUsers)

                binding.progressBar.visibility = View.GONE

                if (nonAdminUsers.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "No hay usuarios disponibles"
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                }

                Log.d(TAG, "Usuarios no administradores cargados: ${nonAdminUsers.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar usuarios", e)
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Error al cargar usuarios"
            }
        }
    }

    private fun filterUsers(query: String) {
        if (query.isEmpty()) {
            userAdapter.updateData(allUsers)
            binding.tvEmptyState.visibility = if (allUsers.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        val filteredUsers = allUsers.filter { user ->
            val fullName = "${user.firstName} ${user.lastName}".lowercase()
            val email = user.email?.lowercase() ?: ""
            val searchQuery = query.lowercase()

            fullName.contains(searchQuery) || email.contains(searchQuery)
        }

        userAdapter.updateData(filteredUsers)
        binding.tvEmptyState.visibility = if (filteredUsers.isEmpty()) View.VISIBLE else View.GONE

        if (filteredUsers.isEmpty() && query.isNotEmpty()) {
            binding.tvEmptyState.text = "No se encontraron usuarios"
        } else {
            binding.tvEmptyState.text = "No hay usuarios disponibles"
        }
    }

    private fun navigateToEditUser(user: User) {
        val editUserFragment = EditUserFragment.newInstance(user.userId)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, editUserFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToAddUser() {
        val addUserFragment = AddUserFragment()

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, addUserFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
