package com.example.e_commerce.Model

import kotlinx.serialization.Serializable

/**
 * Modelo utilizado para la inserción de una nueva categoría en la tabla 'categories'.
 */
@Serializable
data class CreateCategoryRequest(
    val name: String,
    val picUrl: String

)