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
import com.example.e_commerce.R
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

class ExplorerFragment : Fragment() {

    private var _binding: FragmentExplorerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private lateinit var profileViewModel: ProfileViewModel

    private lateinit var handler: Handler
    private lateinit var autoScrollRunnable: Runnable
    private val SCROLL_DELAY: Long = 5000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExplorerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        val baseActivity = requireActivity() as BaseActivity
        val factory = ProfileViewModelFactory(baseActivity.supabase, baseActivity.tokenManager)
        profileViewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)

        handler = Handler(Looper.getMainLooper())

        loadUserName()
        initBanner()
        initBrand()
        initPopular()

        binding.imageView2.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SearchFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.NavBtnCart.setOnClickListener {
            val intent = Intent(requireContext(), CartActivity::class.java)
            startActivity(intent)
        }
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

    private fun initBanner() {
        binding.progressBar.visibility = View.VISIBLE
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
            binding.viewBrand.adapter = BrandAdapter(items as MutableList<BrandModel>, { brandId ->
                manageProductFilter(brandId)
            }) { position ->
                centerBrandItem(position)
            }
            binding.progressBarBrand.visibility = View.GONE
        }
        viewModel.loadBrands()
    }

    private fun manageProductFilter(brandId: Int) {
        binding.progressBarPopular.visibility = View.VISIBLE

        if (brandId != -1) {
            Log.d(TAG, "Aplicando filtro por Brand ID: $brandId")
            viewModel.loadProductsByBrandId(brandId)
        } else {
            Log.d("ExplorerFragment", "Restableciendo la lista de productos completa.")
            viewModel.loadPopular()
        }
    }

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
        // 1. Configurar la estructura del RecyclerView una sola vez.
        binding.viewPopular.isNestedScrollingEnabled = false
        binding.viewPopular.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.viewPopular.adapter = PopularAdapter(arrayListOf())

        binding.progressBarPopular.visibility = View.VISIBLE

        // 2. Observar los cambios en los datos.
        viewModel.populars.observe(viewLifecycleOwner) { popularsList ->
            binding.progressBarPopular.visibility = View.GONE

            // 3. Actualizar los datos del adaptador.
            (binding.viewPopular.adapter as? PopularAdapter)?.updateData(popularsList)
            
            binding.viewPopular.visibility = if (popularsList.isEmpty()) View.GONE else View.VISIBLE
        }

        // 4. Cargar los datos iniciales.
        viewModel.loadPopular()
    }

    private fun banners(images: List<SliderModel>) {
        if (images.isEmpty()) { return }
        binding.viewPagerSlider.adapter = SliderAdapter(images, binding.viewPagerSlider)
        binding.viewPagerSlider.clipToPadding = false
        binding.viewPagerSlider.clipChildren = false
        binding.viewPagerSlider.offscreenPageLimit = 3

        (binding.viewPagerSlider.getChildAt(0) as? RecyclerView)?.overScrollMode =
            RecyclerView.OVER_SCROLL_NEVER

        val compositePageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(40))
        }
        binding.viewPagerSlider.setPageTransformer(compositePageTransformer)

        if (images.size > 1) {
            setupAutoScroll(images.size)
            binding.dotIndicator.visibility = View.VISIBLE
            binding.dotIndicator.attachTo(binding.viewPagerSlider)
        } else {
            binding.dotIndicator.visibility = View.GONE
        }
    }

    private fun setupAutoScroll(listSize: Int) {
        autoScrollRunnable = object : Runnable {
            override fun run() {
                _binding?.let {
                    val currentItem = it.viewPagerSlider.currentItem
                    val nextItem = if (currentItem == listSize - 1) 0 else currentItem + 1
                    it.viewPagerSlider.setCurrentItem(nextItem, true)
                    handler.postDelayed(this, SCROLL_DELAY)
                }
            }
        }
        handler.postDelayed(autoScrollRunnable, SCROLL_DELAY)
    }

    override fun onResume() {
        super.onResume()
        if (::autoScrollRunnable.isInitialized) {
            handler.postDelayed(autoScrollRunnable, SCROLL_DELAY)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::autoScrollRunnable.isInitialized) {
            handler.removeCallbacks(autoScrollRunnable)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::autoScrollRunnable.isInitialized) {
            handler.removeCallbacks(autoScrollRunnable)
        }
        _binding = null
    }
}
