package com.example.e_commerce.UI

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.e_commerce.Model.User
import com.example.e_commerce.databinding.ActivityEditProfileBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.exception.AuthRestException // Importa esta excepción
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlin.collections.mapOf

// Asegúrate de que BaseActivity extienda AppCompatActivity y tenga la instancia de supabase y tokenManager
class EditProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityEditProfileBinding

    companion object {
        private const val TAG = "EditProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asocia los listeners de los botones
        setupListeners()

        // Carga los datos actuales del perfil (si los tienes)
        loadProfileData()
    }

    private fun setupListeners() {
        // Botón de retroceso
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Botón GUARDAR CAMBIOS (Información Personal)
        binding.btnSavePersonald.setOnClickListener {
            // Aquí iría tu lógica para guardar nombre, apellido, teléfono, dirección
            Toast.makeText(this, "Guardando información personal...", Toast.LENGTH_SHORT).show()
            savePersonalData()
        }

        // Botón GUARDAR CAMBIOS (Seguridad de Cuenta - Contraseña)
        binding.btnSaveSecurity.setOnClickListener {
            performPasswordChange()
        }

        // Botón CAMBIAR CORREO (Sección de Cambio de Correo)
        binding.btnChangeEmail.setOnClickListener {
            performEmailChangeWithSecurity()
        }
    }

    private fun savePersonalData() {
        val userId = tokenManager.getUserId() ?: return
        val first = binding.etFirstName.text.toString().trim()
        val last = binding.etLastName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        lifecycleScope.launch {
            try {
                supabase.from("profiles").update(
                    mapOf(
                        "first_name" to first,
                        "last_name" to last,
                        "phone" to phone,
                        "address" to address
                    )
                ) {
                    filter{eq("user_id", userId)}
                }

                Toast.makeText(this@EditProfileActivity, "Perfil actualizado.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveEmail(){
        val userId = tokenManager.getUserId() ?: return
        val email = binding.etNewEmail.text.toString().trim()

        lifecycleScope.launch {
            try {
                supabase.from("profiles").update(
                    mapOf(
                        "email" to email
                    )
                ) {
                    filter{eq("user_id", userId)}
                }

                Toast.makeText(this@EditProfileActivity, "Perfil actualizado.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            try {
                val userId = tokenManager.getUserId()

                if (userId.isNullOrBlank()) {
                    Log.w("PROFILE", "Usuario sin ID. Cargando desde token.")
                    loadLocalFallback()
                    return@launch
                }

                Log.d("PROFILE", "Cargando perfil desde Supabase para ID: $userId")

                val userProfile = supabase.from("profiles")
                    .select() {
                        filter { eq("user_id", userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<User>()

                val email = supabase.auth.currentUserOrNull()?.email ?: tokenManager.getEmail() ?: "No disponible"

                if (userProfile != null) {
                    Log.d("PROFILE", "Perfil cargado correctamente.")
                    showProfile(userProfile, email)
                } else {
                    Log.w("PROFILE", "Perfil no encontrado en DB. Usando datos locales.")
                    showFallback(email)
                }

            } catch (e: Exception) {
                Log.e("PROFILE", "Error cargando perfil", e)
                loadLocalFallback()
            }
        }
    }

    private fun showProfile(user: User, email: String) {
        binding.etFirstName.setText(user.firstName)
        binding.etLastName.setText(user.lastName)
        binding.etPhone.setText(user.phone)
        binding.etAddress.setText(user.address)

        // Guarda en cache por si acaso
        tokenManager.saveFirstName(user.firstName)
        tokenManager.saveLastName(user.lastName)
        tokenManager.savePhone(user.phone.toString())
        tokenManager.saveAddress(user.address.toString())
        tokenManager.saveEmail(user.email.toString())
    }





    private fun showFallback(email: String) {
        binding.etEmail.setText(email)
        binding.etCurrentEmail.setText(email)

        binding.etFirstName.setText(tokenManager.getFirstName())
        binding.etLastName.setText(tokenManager.getLastName())
        binding.etPhone.setText(tokenManager.getPhone())
        binding.etAddress.setText(tokenManager.getAddress())
    }

    private fun loadLocalFallback() = showFallback(tokenManager.getEmail() ?: "N/D")

    // Lógica para cambiar la contraseña
    private fun performPasswordChange() {
        val oldPassword = binding.etOldPassword.text?.toString()?.trim().orEmpty()
        val newPassword = binding.etNewPassword.text?.toString()?.trim().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString()?.trim().orEmpty()

        if (oldPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Completa todos los campos de contraseña", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword.length < 6) { // Requisito mínimo de Supabase por defecto
            Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword == oldPassword) {
            Toast.makeText(this, "La nueva contraseña no puede ser igual a la antigua.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingStateForPassword(true)

        lifecycleScope.launch {
            try {
                supabase.auth.updateUser {
                    this.password = newPassword
                }
                Toast.makeText(this@EditProfileActivity, "Contraseña actualizada con éxito.", Toast.LENGTH_LONG).show()
                binding.etOldPassword.text?.clear()
                binding.etNewPassword.text?.clear()
                binding.etConfirmPassword.text?.clear()
            } catch (e: AuthRestException) {
                Log.e(TAG, "Error al cambiar contraseña: ${e.message}", e)
                Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al cambiar contraseña: ${e.message}", e)
                Toast.makeText(this@EditProfileActivity, "Error inesperado al cambiar la contraseña.", Toast.LENGTH_LONG).show()
            } finally {
                setLoadingStateForPassword(false)
            }
        }
    }

    private fun performEmailChangeWithSecurity() {
        val currentEmail = binding.etCurrentEmail.text?.toString()?.trim().orEmpty()
        val currentPassword = binding.etCurrentPasswordForEmail.text?.toString()?.trim().orEmpty()
        val newEmail = binding.etNewEmail.text?.toString()?.trim().orEmpty()

        // Validaciones de campos
        if (currentEmail.isBlank() || currentPassword.isBlank() || newEmail.isBlank()) {
            Toast.makeText(this, "Completa todos los campos para cambiar el correo.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches() ||
            !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Introduce un formato de email válido.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newEmail == currentEmail) {
            Toast.makeText(this, "El nuevo email no puede ser igual al actual.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentPassword.length < 6) { // Requisito mínimo de Supabase por defecto
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres para verificar.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Intentando cambiar el email a: $newEmail, desde: $currentEmail, con contraseña: $currentPassword")
        setLoadingStateForEmailChange(true)
        lifecycleScope.launch {
            try {
                // Re-autenticación obligatoria antes del cambio de correo
                supabase.auth.signInWith(Email) {
                    email = currentEmail
                    password = currentPassword
                }

                // Ahora sí puedes cambiar el correo
                supabase.auth.updateUser {
                    this.email = newEmail
                }

                Toast.makeText(this@EditProfileActivity, "Email actualizado con éxito.", Toast.LENGTH_LONG).show()
                saveEmail()
            } catch (e: AuthRestException) {
                Toast.makeText(this@EditProfileActivity, "Error: ${e}", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Error al cambiar el email: ${e.message}")
            }
        }


    }

    // Funciones para manejar el estado de carga de la UI
    private fun setLoadingStateForPassword(isLoading: Boolean) {
        binding.btnSaveSecurity.isEnabled = !isLoading
        binding.etOldPassword.isEnabled = !isLoading
        binding.etNewPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
    }

    private fun setLoadingStateForEmailChange(isLoading: Boolean) {
        binding.btnChangeEmail.isEnabled = !isLoading
        binding.etCurrentEmail.isEnabled = !isLoading
        binding.etCurrentPasswordForEmail.isEnabled = !isLoading
        binding.etNewEmail.isEnabled = !isLoading
    }
}