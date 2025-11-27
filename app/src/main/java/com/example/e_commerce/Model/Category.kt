package com.example.e_commerce.Model

import kotlinx.serialization.Serializable

/**
 * Category
 * Modelo de categoría según la tabla public.categories en Supabase
 */
@Serializable
data class Category(
    val id: Int,
    val name: String,
)
