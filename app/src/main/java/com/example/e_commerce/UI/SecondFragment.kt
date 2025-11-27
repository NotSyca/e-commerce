package com.example.e_commerce.UI

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.e_commerce.databinding.FragmentSecondBinding
// Importa las clases Directions generadas por Safe Args (esto asume que ya lo configuraste)
// import com.example.e_commerce.UI.SecondFragmentDirections


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 * NOTA: Esta clase ha sido renombrada de 'FirstFragment' a 'SecondFragment'
 * para que coincida con el layout (FragmentSecondBinding) y la lógica de navegación.
 */
class SecondFragment : Fragment() { // CLASE RENOMBRADA

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla el layout del segundo fragmento
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {

            try {
                val actionId = resources.getIdentifier("action_SecondFragment_to_FirstFragment", "id", requireContext().packageName)
                if (actionId != 0) {
                    findNavController().navigate(actionId)
                } else {
                    Toast.makeText(requireContext(), "Error: ID de acción no encontrado.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                findNavController().navigate(com.example.e_commerce.R.id.FirstFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}