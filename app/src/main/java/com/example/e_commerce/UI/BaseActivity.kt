package com.example.e_commerce.UI

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.e_commerce.Api.TokenManager // Asume la ubicación de TokenManager
import com.example.e_commerce.BuildConfig // Necesario para las claves de Supabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth // Para el módulo de autenticación
import io.github.jan.supabase.postgrest.Postgrest // Para el módulo de base de datos
import io.github.jan.supabase.storage.Storage // Para el módulo de almacenamiento

/**
 * Clase base para todas las Activities en la aplicación.
 * Centraliza la configuración de UI, TokenManager y la inicialización del cliente Supabase.
 */
open class BaseActivity : AppCompatActivity() {

    // Propiedades lateinit que serán inicializadas en onCreate()
    lateinit var tokenManager: TokenManager
    lateinit var supabase: SupabaseClient

    companion object {
        private const val TAG = "BaseActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configuración de la Ventana (Extender contenido más allá de las barras)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // 2. Inicialización de Servicios CLAVE (Antes de que la Activity use ViewModels)
        tokenManager = TokenManager(this)
        initializeSupabaseClient()
    }

    /**
     * Inicializa el cliente Supabase con todos los módulos que necesita la aplicación.
     * Asigna el resultado a la propiedad lateinit 'supabase'.
     */
    private fun initializeSupabaseClient() {
        try {
            // CRÍTICO: La inicialización DEBE asignar el resultado a la propiedad 'supabase'
            supabase = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_KEY
            ) {
                // Instalar todos los módulos usados en HomeActivity, ProfileActivity, y ViewModels
                install(Postgrest) // Para la carga de productos, marcas, y perfiles
                install(Auth)     // Para el manejo de sesiones (login, logout, obtener email)
                install(Storage)  // Para la carga de banners y logos de marcas
            }
            Log.d(TAG, "Cliente Supabase inicializado correctamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error CRÍTICO al inicializar Supabase: ${e.message}", e)
            // En un entorno de producción, podrías mostrar un diálogo de error y cerrar la aplicación aquí.
        }
    }
}