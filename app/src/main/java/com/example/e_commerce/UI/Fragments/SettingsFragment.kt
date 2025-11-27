package com.example.e_commerce.UI.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.R
import com.example.e_commerce.databinding.FragmentSettingsBinding // Asume el binding correcto

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager


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

        binding.ajustesAdmin.visibility =
            if (tokenManager.isAdmin()) View.VISIBLE else View.GONE

        // Configurar Listeners para la navegación
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Opción: Agregar productos (Abre el Fragmento de adición)
        binding.optionAddProduct.setOnClickListener {
            navigateToFragment(AddProductFragment())
        }

        binding.optionAddCategoria.setOnClickListener {
            navigateToFragment(AddCategoryFragment())
        }


        // Ejemplo: Notificaciones
        binding.optionNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Abrir Notificaciones", Toast.LENGTH_SHORT).show()
        }

        // Ejemplo: Historial de búsqueda
        binding.optionHistory.setOnClickListener {
            Toast.makeText(requireContext(), "Abrir Historial", Toast.LENGTH_SHORT).show()
        }

        // ... (Configurar listeners para optionPrivacy, etc.)
    }

    private fun navigateToFragment(fragment: Fragment) {
        // Asumo que el contenedor de fragmentos está en la Activity principal (R.id.fragment_container)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // Reemplaza R.id.fragment_container con el ID real de tu contenedor
            .addToBackStack(null) // Permite al usuario volver a Ajustes
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}