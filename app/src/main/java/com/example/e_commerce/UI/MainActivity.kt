package com.example.e_commerce.UI

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.e_commerce.Model.CartHeader
import com.example.e_commerce.Model.CreateCartRequest
import com.example.e_commerce.Model.User
import com.example.e_commerce.databinding.ActivityMainBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

/**
 * MainActivity (Login)
 *
 * Autenticación con Supabase Auth y gestión de sesión.
 * Hereda tokenManager y supabase de BaseActivity.
 */
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (tokenManager.isLoggedIn()) {
            Log.d(TAG, "Sesión activa encontrada, navegando a Home")
            goToHome()
            return
        }

        // Botón de Login
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        // Botón de Registro
        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()

        // Validaciones
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Completa email y password", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar progreso
        binding.progress.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Iniciando login para: $email")

                // PASO 1: Autenticar con Supabase Auth
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                // PASO 2: Obtener el usuario autenticado
                val currentUser = supabase.auth.currentUserOrNull()
                if (currentUser == null) {
                    throw Exception("No se pudo obtener la sesión del usuario")
                }

                val userId = currentUser.id
                val accessToken = supabase.auth.currentAccessTokenOrNull()
                    ?: throw Exception("No se pudo obtener el token de acceso")

                Log.d(TAG, "Usuario autenticado con ID: $userId")

                // PASO 3: Obtener el perfil desde la tabla profiles
                Log.d(TAG, "Buscando perfil para user_id: $userId")
                val profiles = try {
                    supabase.from("profiles")
                        .select()
                        .decodeList<User>()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al consultar profiles", e)
                    throw Exception("Error al consultar el perfil: ${e.message}")
                }

                Log.d(TAG, "Total de perfiles encontrados: ${profiles.size}")

                // Buscar el perfil por user_id
                val userProfile = profiles.find { it.userId == userId }

                if (userProfile == null) {
                    Log.e(TAG, "No se encontró perfil para user_id: $userId")
                    Log.e(TAG, "Perfiles disponibles: ${profiles.map { it.userId }}")
                    throw Exception("No se encontró el perfil del usuario. Por favor, completa tu registro.")
                }

                Log.d(TAG, "Perfil obtenido: ${userProfile.firstName} ${userProfile.lastName}")

                val activeCartHeader = supabase.from("carts")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("status", "active") // Usando el estado definido en tu lógica
                        }
                    }
                    .decodeSingleOrNull<CartHeader>()

                val userCartId = if (activeCartHeader != null) {
                    // Si se encontró un carrito activo, usamos su ID
                    Log.d(TAG, "Carrito activo encontrado con ID: ${activeCartHeader.id}")
                    activeCartHeader.id
                } else {
                    // Si no, creamos un nuevo carrito usando CreateCartRequest
                    Log.d(TAG, "No se encontró carrito activo. Creando uno nuevo...")

                    // Usamos tu modelo CreateCartRequest
                    val cartToInsert = CreateCartRequest(userId = userId) // status="active" ya es el valor por defecto

                    val newCartHeader = supabase.from("carts")
                        .insert(cartToInsert) { // Insertamos el objeto específico para la creación
                            select() // Devuelve el objeto Cart completo creado
                        }
                        .decodeSingle<CartHeader>() // Decodificamos la respuesta completa como un objeto Cart

                    Log.d(TAG, "Nuevo carrito creado con ID: ${newCartHeader.id}")
                    newCartHeader.id
                }

                // PASO 4: Guardar la sesión en TokenManager
                tokenManager.saveSession(
                    userId = userId,
                    accessToken = accessToken,
                    firstName = userProfile.firstName,
                    lastName = userProfile.lastName,
                    phone = userProfile.phone,
                    address = userProfile.address,
                    isAdmin = userProfile.isAdmin ?: false,
                    cartId = userCartId
                )

                Log.d(TAG, "Sesión guardada exitosamente")

                // Mostrar mensaje de bienvenida
                Toast.makeText(
                    this@MainActivity,
                    "Bienvenido ${userProfile.firstName} ${userProfile.lastName}",
                    Toast.LENGTH_LONG
                ).show()

                // Navegar a Home
                goToHome()

            } catch (e: Exception) {
                Log.e(TAG, "Error al hacer login", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progress.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}