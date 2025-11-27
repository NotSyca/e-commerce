package com.example.e_commerce.Api

import android.content.Context
import android.util.Log
import com.example.e_commerce.Model.User
import com.example.e_commerce.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.runBlocking

/**
 * AuthService
 * Servicio de autenticación que usa Supabase Auth y obtiene el perfil del usuario
 * desde la tabla public.profiles
 */
class AuthService(private val context: Context) {

    private val supabase: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
    }

    companion object {
        private const val TAG = "AuthService"
    }

    /**
     * Proceso completo de login: autentica con Supabase Auth y obtiene el perfil del usuario
     * IMPORTANTE: Esta función usa runBlocking y debe ser llamada desde una corrutina o hilo de fondo
     */
    fun loginAndGetUser(email: String, password: String): User? {
        return try {
            runBlocking {
                // PASO 1: Autenticar con Supabase Auth
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                // PASO 2: Obtener el usuario actual
                val currentUser = supabase.auth.currentUserOrNull()
                if (currentUser == null) {
                    Log.e(TAG, "No se pudo obtener el usuario actual después del login")
                    return@runBlocking null
                }

                val userId = currentUser.id
                Log.d(TAG, "Usuario autenticado con ID: $userId")

                // PASO 3: Obtener el perfil del usuario desde la tabla profiles
                val profiles = supabase.from("profiles")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<User>()

                if (profiles.isEmpty()) {
                    Log.e(TAG, "No se encontró perfil para el usuario: $userId")
                    return@runBlocking null
                }

                val userProfile = profiles.first()
                Log.d(TAG, "Perfil obtenido: ${userProfile.firstName} ${userProfile.lastName}")

                userProfile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en loginAndGetUser", e)
            null
        }
    }

    /**
     * Obtiene el perfil de usuario por su ID desde la tabla profiles
     */
    fun obtenerUsuarioPorId(userId: String): User? {
        return try {
            runBlocking {
                val profiles = supabase.from("profiles")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<User>()

                profiles.firstOrNull()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuario por ID", e)
            null
        }
    }

    /**
     * Cierra la sesión actual
     */
    fun logout() {
        try {
            runBlocking {
                supabase.auth.signOut()
                Log.d(TAG, "Sesión cerrada exitosamente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar sesión", e)
        }
    }
}