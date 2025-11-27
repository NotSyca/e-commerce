package com.example.e_commerce.UI

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.e_commerce.Model.MainViewModel
import com.example.e_commerce.databinding.ActivityHomeBinding
import com.example.e_commerce.UI.Fragments.ExplorerFragment // Nuevo fragmento
import com.example.e_commerce.UI.Fragments.ProfileFragment // Fragmento del perfil
import com.example.e_commerce.UI.Fragments.SettingsFragment

/**
 * HomeActivity - Contenedor principal de la aplicación.
 * Maneja la navegación inferior y aloja los Fragments de contenido.
 */
class HomeActivity : BaseActivity(){

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: MainViewModel by viewModels() // ViewModel compartido

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asumo que el fragmento 'Explorer' debe ser la pantalla inicial
        if (savedInstanceState == null) {
            replaceFragment(ExplorerFragment())
            setActiveNavItem(0) // Explorer es el item activo inicial
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.navBtnExplore.setOnClickListener {
            replaceFragment(ExplorerFragment())
            setActiveNavItem(0)
        }

        binding.navBtnProfile.setOnClickListener {
            replaceFragment(ProfileFragment())
            setActiveNavItem(1)
        }

        binding.navBtnSettings.setOnClickListener {
            replaceFragment(SettingsFragment())
            setActiveNavItem(2)
        }
    }

    private fun setActiveNavItem(position: Int) {
        // Resetear todos los items a estado inactivo
        binding.navBtnExplore.setBackgroundResource(com.example.e_commerce.R.drawable.bg_nav_item_inactive)
        binding.navBtnProfile.setBackgroundResource(com.example.e_commerce.R.drawable.bg_nav_item_inactive)
        binding.navBtnSettings.setBackgroundResource(com.example.e_commerce.R.drawable.bg_nav_item_inactive)

        // Activar el item seleccionado
        when (position) {
            0 -> binding.navBtnExplore.setBackgroundResource(com.example.e_commerce.R.drawable.bg_nav_item_active)
            1 -> binding.navBtnProfile.setBackgroundResource(com.example.e_commerce.R.drawable.bg_nav_item_active)
            2 -> binding.navBtnSettings.setBackgroundResource(com.example.e_commerce.R.drawable.bg_nav_item_active)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment) // Asumo ID fragmentContainer en XML
            .commit()
    }
}