package com.example.e_commerce.Api

/**
 * AuthResponse
 * Respuesta del endpoint de login.
 * Se asume que devuelve un token y un objeto usuario.
 * Comentado línea por línea para fines educativos.
 */
data class AuthResponse( // data class para la respuesta de login
    val authToken: String, // Token devuelto por el backend (JWT/JWE)
)