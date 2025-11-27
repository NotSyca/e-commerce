package com.example.e_commerce.Api

import android.content.Context
import android.content.SharedPreferences

/**
 * TokenManager
 * Maneja la sesión del usuario guardando el userId y el token de Supabase Auth
 */
class TokenManager(context: Context) {

    private val PREFS_NAME = "user_session"
    private val KEY_USER_ID = "user_id"
    private val KEY_ACCESS_TOKEN = "access_token"
    private val KEY_FIRST_NAME = "first_name"
    private val KEY_LAST_NAME = "last_name"

    private val KEY_PHONE = "phone"

    private val KEY_ADDRESS = "address"

    private val KEY_IS_ADMIN = "is_admin"

    private val KEY_CART_ID = "cart_id"

    private val KEY_EMAIL = "email" // La clave para guardar el email

    // Esta es la instancia correcta de SharedPreferences
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Guarda la sesión del usuario con su información básica
     */
    fun saveSession(
        userId: String,
        accessToken: String,
        firstName: String,
        lastName: String,
        phone: String? = null,
        address: String? = null,
        isAdmin: Boolean = false,
        cartId: Long,
        email: String? = null
    ) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_PHONE, phone)
            putString(KEY_ADDRESS, address)
            putBoolean(KEY_IS_ADMIN, isAdmin)
            putBoolean(KEY_IS_ADMIN, isAdmin)
            putLong(KEY_CART_ID, cartId)
            putString(KEY_EMAIL, email) // Guarda el email
            apply()
        }
    }

    fun getCartId(): Long? {
        val cartId = sharedPreferences.getLong(KEY_CART_ID, -1L)
        return if (cartId == -1L) null else cartId
    }

    /**
     * Obtiene el ID del usuario actual
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * Obtiene el token de acceso de Supabase
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Obtiene el nombre completo del usuario
     */
    fun getUserName(): String? {
        val firstName = sharedPreferences.getString(KEY_FIRST_NAME, null)
        val lastName = sharedPreferences.getString(KEY_LAST_NAME, null)
        return if (firstName != null && lastName != null) {
            "$firstName $lastName"
        } else {
            null
        }
    }

    /**
     * Obtiene el nombre (first name) del usuario
     */
    fun getFirstName(): String? {
        return sharedPreferences.getString(KEY_FIRST_NAME, null)
    }

    /**
     * Obtiene el apellido (last name) del usuario
     */
    fun getLastName(): String? {
        return sharedPreferences.getString(KEY_LAST_NAME, null)
    }

    fun getPhone(): String? {
        return sharedPreferences.getString(KEY_PHONE, null)
    }

    fun getAddress(): String? {
        return sharedPreferences.getString(KEY_ADDRESS, null)
    }


    /**
     * Verifica si el usuario es administrador
     */
    fun isAdmin(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_ADMIN, false)
    }

    // Este es el único y correcto método para obtener el email
    fun getEmail(): String? {
        return sharedPreferences.getString(KEY_EMAIL, null)
    }

    /**
     * Verifica si hay una sesión activa
     */
    fun isLoggedIn(): Boolean {
        return getToken() != null && getUserId() != null
    }

    /**
     * Limpia toda la sesión del usuario
     */
    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    fun saveFirstName(firstName: String) {
        sharedPreferences.edit().putString(KEY_FIRST_NAME, firstName).apply()
    }

    fun saveLastName(lastName: String) {
        sharedPreferences.edit().putString(KEY_LAST_NAME, lastName).apply()
    }

    fun savePhone(phone: String) {
        sharedPreferences.edit().putString(KEY_PHONE, phone).apply()
    }

    fun saveAddress(address: String) {
        sharedPreferences.edit().putString(KEY_ADDRESS, address).apply()
    }


    fun saveEmail(email: String) {
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply()
    }
}