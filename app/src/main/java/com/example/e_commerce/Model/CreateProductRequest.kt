package com.example.e_commerce.Model

import kotlinx.serialization.Serializable
@Serializable
data class CreateProductRequest(
    val title: String,
    val description: String,
    val picUrl: List<String>,
    val brand: Int,
    val price: Double,
    val rating: Double,
    val size: List<String>
)

