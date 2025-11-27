package com.example.e_commerce.Model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProductRequest(
    val title: String,
    val description: String? = null,
    val price: Float? = null, // Usar Float? si el precio puede ser nulo, aunque aquí es requerido
    val brand: Int? = null,
    val rating: Float? = null,
    // List<String> es el tipo de Kotlin que mejor mapea a array de texto (_text) en la DB
    val size: List<String>? = null,
    val picUrl: List<String>? = null
    // Agrega aquí cualquier otro campo que puedas actualizar
)