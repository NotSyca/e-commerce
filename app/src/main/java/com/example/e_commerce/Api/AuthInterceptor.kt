package com.example.e_commerce.Api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor { // Clase que implementa Interceptor; recibe un proveedor de token
    override fun intercept(chain: Interceptor.Chain): Response { // Métodoo obligatorio que intercepta cada request
        val original = chain.request() // Obtenemos la solicitud original
        val token = tokenProvider() // Obtenemos el token actual (puede ser null o vacío)
        val request = if (!token.isNullOrBlank()) { // Si el token no es nulo ni vacío
            original.newBuilder() // Creamos un builder basado en la solicitud original
                .addHeader("Authorization", "Bearer $token") // Añadimos el header Authorization con el esquema Bearer
                .build() // Construimos la nueva solicitud con el header
        } else { // Si no hay token
            original // Usamos la solicitud original tal cual
        }
        return chain.proceed(request) // Continuamos la cadena con la solicitud (modificada o original)
    }
}