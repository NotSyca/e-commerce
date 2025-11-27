package com.example.e_commerce.UI.Fragments

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.Model.BrandModel
import com.example.e_commerce.Model.MainViewModel
import com.example.e_commerce.Model.Product
import com.example.e_commerce.Model.ProfileViewModel
import com.example.e_commerce.Model.ProfileViewModelFactory
import com.example.e_commerce.Model.SliderModel
import com.example.e_commerce.UI.Adapter.BrandAdapter
import com.example.e_commerce.UI.Adapter.PopularAdapter
import com.example.e_commerce.UI.Adapter.SliderAdapter
import com.example.e_commerce.UI.BaseActivity
import com.example.e_commerce.UI.CartActivity
import com.example.e_commerce.databinding.FragmentExplorerBinding

/**
 * ExplorerFragment: Contiene los Banners, Marcas y Productos Populares.
 * Migrado de HomeActivity.
 */
class ExplorerFragment : Fragment() {

    // Cambiamos a _binding de Fragment
    private var _binding: FragmentExplorerBinding? = null
    private val binding get() = _binding!!

    // El ViewModel se comparte con la Activity contenedora o se inicializa con la Factory
    private lateinit var viewModel: MainViewModel // Será inicializado en onViewCreated
    private lateinit var profileViewModel: ProfileViewModel

    private lateinit var handler: Handler
    private lateinit var autoScrollRunnable: Runnable
    private val SCROLL_DELAY: Long = 5000 // 5 segundos de retardo

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExplorerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // El ViewModel se obtiene de la Activity (recomendado para compartir datos)
        // O lo puedes inicializar aquí si quieres que el Fragment lo posea:
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        // Inicializar ProfileViewModel para obtener datos del usuario
        val baseActivity = requireActivity() as BaseActivity
        val factory = ProfileViewModelFactory(baseActivity.supabase, baseActivity.tokenManager)
        profileViewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)

        handler = Handler(Looper.getMainLooper())

        // Cargar nombre del usuario
        loadUserName()

        // Inicializar la carga de datos
        initBanner()
        initBrand()
        initPopular()

        binding.NavBtnCart.setOnClickListener {
            // CORRECCIÓN CLAVE: Crear el Intent con la clase de destino y usar startActivity(Intent)
            val intent = Intent(this@ExplorerFragment.requireContext(), CartActivity::class.java).apply {

                // Iniciar la actividad
                startActivity(this)
            }
        }
        // Nota: Los listeners de la barra inferior (navBtnCart, navBtnProfile)
        // deben estar en la HomeActivity contenedora.
    }

    private fun loadUserName() {
        try {
            profileViewModel.userProfile.observe(viewLifecycleOwner) { user ->
                user?.let {
                    val firstName = it.firstName ?: "Usuario"
                    binding.tvUserName.text = firstName
                }
            }
            profileViewModel.loadUserProfile()
        } catch (e: Exception) {
            Log.e("ExplorerFragment", "Error loading user name: ${e.message}", e)
            binding.tvUserName.text = "Usuario"
        }
    }

    // -------------------------------------------------------------------
    // LÓGICA DE CARGA Y SCROLL (Migrada del Activity)
    // -------------------------------------------------------------------

    private fun initBanner() {
        binding.progressBar.visibility = View.VISIBLE
        // Usar viewLifecycleOwner para observar
        viewModel.banners.observe(viewLifecycleOwner) { items ->
            banners(items)
            binding.progressBar.visibility = View.GONE
        }
        viewModel.loadBanners()
    }

    private fun initBrand() {
        binding.progressBarBrand.visibility = View.VISIBLE
        viewModel.brands.observe(viewLifecycleOwner) { items ->
            if (items.isEmpty()) {
                binding.progressBarBrand.visibility = View.GONE
                return@observe
            }

            binding.viewBrand.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.viewBrand.adapter = BrandAdapter(items, { brandId ->
                manageProductFilter(brandId)
            }) { brandId ->
                manageProductFilter(brandId)
            }
            binding.progressBarBrand.visibility = View.GONE
        }

        viewModel.brands.observe(viewLifecycleOwner) { items ->
            binding.viewBrand.layoutManager = LinearLayoutManager(
                this@ExplorerFragment.requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false)

            // CONEXIÓN CLAVE: El callback recibe el ID de la marca (brandId)
            binding.viewBrand.adapter = BrandAdapter(items as MutableList<BrandModel>, { brandId ->
                manageProductFilter(brandId)
            }) { brandId ->
                manageProductFilter(brandId)
            }
            binding.progressBarBrand.visibility = View.GONE
        }
        viewModel.loadBrands()
    }

    private fun manageProductFilter(brandId: Int) {

        // Mostramos el indicador de carga
        binding.progressBarPopular.visibility = View.VISIBLE

        if (brandId != -1) {
            // CASO 1: FILTRAR POR MARCA (El usuario seleccionó una marca)
            Log.d(TAG, "Aplicando filtro por Brand ID: $brandId")
            viewModel.loadProductsByBrandId(brandId) // Carga productos filtrados
        } else {
            // CASO 2: RESTABLECER LA LISTA (El usuario deseleccionó la marca)
            Log.d("ExplorerFragment", "Restableciendo la lista de productos completa.")
            viewModel.loadPopular() // Llama a la función que trae todos los productos populares
        }

        // Nota: El progressBar se ocultará automáticamente en el observador
        // de viewModel.populars cuando los nuevos datos (filtrados o completos) lleguen.
    }

    // Función de centrado de marca (necesita el contexto del Fragment)
    fun centerBrandItem(position: Int) {
        val recyclerView = binding.viewBrand
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager

        if (layoutManager != null) {
            val itemView = layoutManager.findViewByPosition(position)
            if (itemView != null) {
                val center = recyclerView.width / 2
                val itemCenter = itemView.width / 2
                val scrollDistance = itemView.left - center + itemCenter
                recyclerView.smoothScrollBy(scrollDistance, 0)
            }
        }
    }

    private fun initPopular() {
        binding.progressBarPopular.visibility = View.VISIBLE
        viewModel.populars.observe(viewLifecycleOwner) { popularsList ->
            val data = popularsList.toMutableList()
            binding.progressBarPopular.visibility = View.GONE

            val adapter = binding.viewPopular.adapter

            if (adapter == null) {
                binding.viewPopular.layoutManager = GridLayoutManager(requireContext(), 2)
                binding.viewPopular.adapter = PopularAdapter(data as ArrayList<Product>)

            } else {
                (adapter as? PopularAdapter)?.updateData(data)
            }
            binding.viewPopular.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
        }

        // 5. Iniciar la carga de datos al final de la configuración
        viewModel.loadPopular()
    }

    private fun banners(images: List<SliderModel>){
        if (images.isEmpty()) { return }
        binding.viewPagerSlider.adapter = SliderAdapter(images, binding.viewPagerSlider)
        binding.viewPagerSlider.clipToPadding = false
        binding.viewPagerSlider.clipChildren = false
        binding.viewPagerSlider.offscreenPageLimit = 3

        // Uso de requireView() para acceder al ViewPager2 si getChildAt(0) falla
        val recyclerView = binding.viewPagerSlider.getChildAt(0)
        if (recyclerView is RecyclerView) {
            recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }

        val compositePageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(40))
        }
        binding.viewPagerSlider.setPageTransformer(compositePageTransformer)

        if (images.size > 1){
            setupAutoScroll(images.size)
            binding.dotIndicator.visibility = View.VISIBLE
            binding.dotIndicator.attachTo(binding.viewPagerSlider)
        } else {
            binding.dotIndicator.visibility = View.GONE
        }
    }

    // ... (setupAutoScroll se mantiene, usando binding y handler) ...
    private fun setupAutoScroll(listSize: Int) {
        autoScrollRunnable = object : Runnable {
            override fun run() {
                val currentItem = binding.viewPagerSlider.currentItem

                if (currentItem == listSize - 1) {
                    binding.viewPagerSlider.setCurrentItem(0, true)
                } else {
                    // Avanza normal
                    binding.viewPagerSlider.setCurrentItem(currentItem + 1, true)
                }

                handler.postDelayed(this, SCROLL_DELAY)
            }
        }

        handler.postDelayed(autoScrollRunnable, SCROLL_DELAY)
    }

    // -------------------------------------------------------------------
    // CICLO DE VIDA DEL FRAGMENT (Manejo de Fugas de Memoria del Handler)
    // -------------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        // Reiniciar el auto-scroll
        if ((binding.viewPagerSlider.adapter?.itemCount ?: 0) > 0) {
            handler.postDelayed(autoScrollRunnable, SCROLL_DELAY)
        }
    }

    override fun onPause() {
        super.onPause()
        // Detener el auto-scroll
        handler.removeCallbacks(autoScrollRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar el Handler y el Binding
        handler.removeCallbacks(autoScrollRunnable)
        _binding = null
    }
}