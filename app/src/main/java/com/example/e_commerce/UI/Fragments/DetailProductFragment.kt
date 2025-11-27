// Archivo: UI/Fragments/DetailProductFragment.kt

package com.example.e_commerce.UI.Fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.Model.Product
import com.example.e_commerce.Model.SliderModel
import com.example.e_commerce.UI.Adapter.ColorAdapter
import com.example.e_commerce.UI.Adapter.SizeAdapter
import com.example.e_commerce.UI.Adapter.SliderAdapter
import com.example.e_commerce.Helper.ManagmentCart
import com.example.e_commerce.UI.Adapter.SizeSelectedListener
import com.example.e_commerce.UI.CartActivity
import com.example.e_commerce.UI.DetailActivity
import com.example.e_commerce.databinding.FragmentDetailBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class DetailProductFragment : Fragment(), SizeSelectedListener {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var item: Product // Producto cargado desde Supabase

    private var selectedSize: String? = null
    private var numberOder = 1
    private lateinit var managmentCart: ManagmentCart
    private var productId: Int = -1

    companion object {
        private const val TAG = "DetailProductFragment"
        const val ARG_PRODUCT_ID = "ARG_PRODUCT_ID" // Clave usada por DetailActivity

        fun newInstance(productId: Int) = DetailProductFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_PRODUCT_ID, productId)
            }
        }
    }

    // Accede a las dependencias desde la Activity Contenedora (DetailActivity)
    private val supabase: SupabaseClient
        get() = (requireActivity() as DetailActivity).supabase

    private val tokenManager
        get() = (requireActivity() as DetailActivity).tokenManager


    override fun onAttach(context: Context) {
        super.onAttach(context)
        managmentCart = ManagmentCart(requireActivity(), supabase, tokenManager)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = arguments?.getInt(ARG_PRODUCT_ID, -1) ?: -1

        if (productId != -1) {
            loadProductDetails(productId) // Inicia la carga asíncrona
        } else {
            Log.e(TAG, "No se recibió Product ID.")
            Toast.makeText(requireContext(), "Error al cargar ID del producto.", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadProductDetails(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Decodificar directamente a la clase Product (@Serializable)
                val productObject = supabase.from(table = "Product")
                    .select(){ filter { eq("id", id) } }
                    .decodeSingleOrNull<Product>()

                if (productObject == null) {
                    Log.w(TAG, "Producto no encontrado o respuesta inválida.")
                    Toast.makeText(requireContext(), "Producto no encontrado.", Toast.LENGTH_LONG).show()
                    requireActivity().supportFragmentManager.popBackStack()
                    return@launch
                }

                // Asignación y Configuración de UI
                item = productObject

                setupDetailView()
                banners()
                initList()

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando detalles del producto: ${e.message}", e)
                Toast.makeText(requireContext(), "Error de red al cargar producto.", Toast.LENGTH_LONG).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }

    override fun onSizeSelected(size: String) {
        selectedSize = size
        Log.d(TAG, "Talla seleccionada: $selectedSize")
    }

    private fun initList() {
        // Usa requireContext() para el contexto del fragmento
        val sizeList = item.size ?: emptyList()

        binding.sizeList.apply {
            adapter = SizeAdapter(sizeList, this@DetailProductFragment)
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

    }

    private fun banners() {
        item.picUrl?.let { picUrls ->
            if (picUrls.isEmpty()) return

            val sliderItems = picUrls.map { imageUrl ->
                SliderModel(imageUrl)
            }.toCollection(ArrayList())

            binding.slider.adapter = SliderAdapter(sliderItems, binding.slider)
            // ... (resto de la configuración del ViewPager2) ...
            binding.slider.clipToPadding = false
            binding.slider.clipChildren = false
            binding.slider.offscreenPageLimit = 3
            binding.slider.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            if (sliderItems.size > 1){
                binding.dotIndicator.visibility= View.VISIBLE
                binding.dotIndicator.attachTo(binding.slider)
            }
        }
    }

    private fun setupDetailView() {
        // Mostrar botón de Admin si el usuario es Admin
        binding.ajustesAdmin.visibility =
            if ((requireActivity() as DetailActivity).tokenManager.isAdmin()) View.VISIBLE else View.GONE

        // Enlace de Vistas
        binding.titletext.text = item.title
        binding.descriptionTxt.text = item.description
        binding.priceTxt.text = "$" + item.price.toString()
        binding.ratingTxt.text = item.rating.toString()

        // Botones y Listeners (se mantienen)
        binding.backbtn.setOnClickListener {
            requireActivity().finish()
        }

        binding.cartBtn.setOnClickListener {
            startActivity(Intent(requireContext(), CartActivity::class.java))
        }

        binding.addToCartBtn.setOnClickListener {
            if (selectedSize == null) {
                Toast.makeText(requireContext(), "Selecciona una talla primero.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantityToAdd = numberOder
            viewLifecycleOwner.lifecycleScope.launch {
                managmentCart.insertFood(item, quantityToAdd, selectedSize.toString())
            }
        }

        // Botón EDITAR (Transición al fragmento de edición)
        binding.EditBtn.setOnClickListener {
            val parentActivity = activity as? DetailActivity
            parentActivity?.showEditFragment(item.id)
        }
    }
}