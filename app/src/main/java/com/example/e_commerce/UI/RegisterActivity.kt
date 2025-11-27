package com.example.e_commerce.UI

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.e_commerce.BuildConfig
import com.example.e_commerce.databinding.ActivityRegisterBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.exception.AuthRestException
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


/**
 * Data class para insertar en la tabla profiles
 */
@Serializable
data class ProfileInsert(
    val user_id: String,
    val first_name: String,
    val last_name: String,
    val phone: String?,
    val address: String?,
    val is_admin: Boolean = false,
    val email: String
)
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var supabase: SupabaseClient

    companion object {
        private const val TAG = "RegisterActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeSupabase()

        binding.btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun initializeSupabase() {
        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
    }



    /**
     * Función principal para registrar un usuario.
     * 1. Crea el usuario en Supabase Auth (email + password)
     * 2. Inserta el perfil en la tabla public.profiles con el user_id
     */
    private fun registerUser() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val isAdmin = binding.isAdmin.isChecked

        // Validaciones
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
            password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Los campos obligatorios son: nombre, apellido, email y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progress.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Iniciando registro de usuario: $email")

                // PASO 1: Crear usuario en Supabase Auth
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                Log.d(TAG, "Usuario creado en Auth, obteniendo sesión...")

                // Ahora que desactivaste la confirmación de email, la sesión se crea automáticamente
                val currentUser = supabase.auth.currentUserOrNull()

                if (currentUser == null) {
                    Log.e(TAG, "Error: No hay sesión después del signup")
                    throw Exception("No se pudo crear la sesión del usuario")
                }

                val userId = currentUser.id
                Log.d(TAG, "Usuario ID obtenido: $userId")

                // PASO 2: Insertar perfil en la tabla public.profiles
                val profileData = ProfileInsert(
                    user_id = userId,
                    first_name = firstName,
                    last_name = lastName,
                    phone = phone,
                    address = address,
                    is_admin = isAdmin,
                    email = email
                )

                Log.d(TAG, "Insertando perfil en profiles...")

                supabase.from("profiles")
                    .insert(profileData)

                Log.d(TAG, "Perfil creado exitosamente para user_id: $userId")

                Toast.makeText(
                    this@RegisterActivity,
                    "¡Registro exitoso!",
                    Toast.LENGTH_LONG
                ).show()

                clearForm()

                // Navegar de regreso al login
                navigateToLogin()

            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar usuario", e)
                Toast.makeText(
                    this@RegisterActivity,
                    "Error al registrar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progress.visibility = View.GONE
                binding.btnRegister.isEnabled = true
            }
        }
    }

    private fun clearForm() {
        binding.etFirstName.text?.clear()
        binding.etLastName.text?.clear()
        binding.etPhone.text?.clear()
        binding.etAddress.text?.clear()
        binding.etEmail.text?.clear()
        binding.etPassword.text?.clear()
        binding.etConfirmPassword.text?.clear()
        binding.isAdmin.isChecked = false
    }

    /**
     * Navega de regreso a la pantalla de login
     */
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}