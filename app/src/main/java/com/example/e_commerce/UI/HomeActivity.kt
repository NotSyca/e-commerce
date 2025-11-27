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
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Asumo que tu XML tiene un BottomNavigationView con ID 'bottom_nav_view'
        // o que tu diseño personalizado ya tiene listeners en los Linear Layouts.


        binding.navBtnExplore.setOnClickListener {
            replaceFragment(ExplorerFragment())
        }

        binding.navBtnProfile.setOnClickListener {
            replaceFragment(ProfileFragment())
        }

        binding.navBtnSettings.setOnClickListener {
            replaceFragment(SettingsFragment())

        }

    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment) // Asumo ID fragmentContainer en XML
            .commit()
    }
}