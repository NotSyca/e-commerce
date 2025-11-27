package com.example.e_commerce.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User
 * Modelo básico de usuario segun lo definido en la db postgres en Supabase
 * Se usara Supabase.Auth para la autenticación del usuario por ende no son necesario los campos de email y password.
 */

// table profiles
@Serializable
data class User(
    @SerialName("user_id")
    val userId: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("is_admin")
    val isAdmin: Boolean? = false,
    @SerialName("email")
    val email: String? = null,
)

@Serializable
data class UserUpdateAuth(
    val email: String? = null,
    val password: String? = null
)

