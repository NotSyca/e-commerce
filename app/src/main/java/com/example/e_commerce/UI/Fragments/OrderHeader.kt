package com.example.e_commerce.UI.Fragments

import com.example.e_commerce.Model.Product
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val first_name: String? = null, // Haz que sean anulables para mayor robustez
    val last_name: String? = null,
    val email: String? = null // Incluye otros campos que uses en la consulta JOIN
)
@Serializable
data class OrderHeader(
    val id: Int,
    val user_id: String,
    val total_amount: Double?,
    val status: String,
    val created_at: String,
    val order_items: List<OrderItemDetail>? = null,
    val client: UserProfile? = null
)

@Serializable
data class OrderItemDetail(
    val product_id: Int,
    val quantity: Int,
    val product: Product?,
    val selected_size: String?
)


