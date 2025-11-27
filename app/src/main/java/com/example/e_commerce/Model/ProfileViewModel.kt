package com.example.e_commerce.Model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import com.example.e_commerce.Api.TokenManager // Necesitas esto para obtener el ID de usuario
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileViewModel(
    private val supabase: SupabaseClient,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val USERS_TABLE_NAME = "profiles" // Asegúrate que este nombre coincida con tu tabla real

    /**
     * Carga los datos del perfil del usuario autenticado usando su ID.
     */
    fun loadUserProfile() {
        val userId = tokenManager.getUserId()

        if (userId.isNullOrBlank()) {
            Log.w("ProfileVM", "No hay ID de usuario disponible.")
            _userProfile.value = null
            return
        }

        viewModelScope.launch {
            try {
                Log.d("ProfileVM", "Cargando perfil para ID: $userId")

                // Consulta la tabla 'User' filtrando por user_id (que es la clave primaria)
                val user = supabase.from(USERS_TABLE_NAME)
                    .select() {
                        filter {
                            // Asumo que la columna de la clave primaria en la tabla es 'user_id'
                            eq("user_id", userId)
                        }
                        limit(1)
                    }
                    // Intentamos decodificar como un solo objeto, ya que filtramos por ID
                    .decodeSingleOrNull<User>()

                _userProfile.postValue(user)

                if (user == null) {
                    Log.w("ProfileVM", "Perfil no encontrado para ID: $userId")
                }

            } catch (e: Exception) {
                Log.e("ProfileVM", "Error al cargar perfil: ${e.message}", e)
                _userProfile.postValue(null)
            }
        }
    }

    suspend fun updateProfile(updatedUser: User): Boolean {
        // Es crucial que esta operación se ejecute en el hilo de IO
        return withContext(Dispatchers.IO) {
            try {
                val userId = updatedUser.userId
                if (userId.isBlank()) {
                    Log.e("ProfileVM", "Error: No se puede actualizar el perfil sin un user ID válido.")
                    return@withContext false
                }

                Log.d("ProfileVM", "Iniciando actualización de perfil para ID: $userId")

                // Usamos la función update() para enviar solo los campos modificables.
                // La actualización se realiza filtrando por la clave única 'user_id'.
                supabase.from(USERS_TABLE_NAME)
                    .update(updatedUser) {
                        filter {
                            // Filtramos para asegurar que solo se actualice la fila del usuario actual
                            eq("user_id", userId)
                        }
                    } // Ejecuta la operación de actualización (PATCH)

                Log.i("ProfileVM", "Perfil actualizado exitosamente para ID: $userId")
                return@withContext true

            } catch (e: Exception) {
                Log.e("ProfileVM", "Fallo al actualizar perfil en DB: ${e.message}", e)
                return@withContext false
            }
        }
    }

    // NOTA: Para que este ViewModel funcione en ProfileActivity, necesitas un ViewModelFactory
}