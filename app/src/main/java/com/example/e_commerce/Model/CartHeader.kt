package com.example.e_commerce.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CartHeader(
    val id: Long,
    @SerialName("user_id")
    val userId: String,
    val status: String,

)
@Serializable
data class CartItem(
    val id: Long? = null,

    @SerialName("cart_id")
    val cartId: Long,

    @SerialName("product_id")
    val productId: Long,

    val quantity: Int,

    @SerialName("unit_price_cents")
    val unitPriceCents: Int,

    @SerialName("created_at")
    val createdAt: String? = null
)

// Para crear un carrito nuevo
@Serializable
data class CreateCartRequest(
    @SerialName("user_id")
    val userId: String,

    val status: String = "active"
)

@Serializable
data class CartItemUpdate(
    val quantity: Int,
    val unit_price_cents: Int)

@Serializable
data class CartItemRequest(
    val id: Long?,
    val cart_id: Int,
    val product_id: Int,
    val quantity: Int,
    val unit_price_cents: Int,
    val size: String,

)

@Serializable
data class CartItemInsertRequest(
    val cart_id: Int,
    val product_id: Int,
    val quantity: Int,
    val unit_price_cents: Int,
    val size: String,
)

@Serializable
data class CartProductDetail(
    val product_id: Int,
    val quantity: Int,

    val product: Product
)