package com.example.e_commerce.Helper

import OrderItem
import android.R
import android.content.Context
import android.util.Log
import android.util.Size
import android.widget.Toast
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.Model.CartHeader
import com.example.e_commerce.Model.CartItem
import com.example.e_commerce.Model.CartItemInsertRequest
import com.example.e_commerce.Model.Product
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import com.example.e_commerce.Model.CartItemRequest // Importado de tu lista
import com.example.e_commerce.Model.CartItemUpdate
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap // Importado de tu lista

// ==========================================================
// DEFINICIONES REQUERIDAS (Asumidas para la DB)
// ==========================================================

// Modelo para manejar el JOIN del carrito
@Serializable
data class CartProductDetail(
    val product_id: Int,
    val quantity: Int,
    val size: String,
    val product: Product // Debe coincidir con la relación JOIN en la DB
)

// ==========================================================

class ManagmentCart(
    private val context: Context,
    private val supabase: SupabaseClient,
    private val tokenManager: TokenManager
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val CART_ITEMS_TABLE = "cart_items"
    private val CARTS_TABLE = "carts"

    private val PRODUCTS_TABLE = "Product"

    private val activeCartIdCache = ConcurrentHashMap<String, Int>()

    private suspend fun getOrCreateActiveCartId(userId: String): Int? = withContext(Dispatchers.IO) {
        val cachedId = activeCartIdCache[userId]
        if (cachedId != null) return@withContext cachedId

        try {
            // 1. Intentamos encontrar un carrito activo
            val existingCart = supabase.from(CARTS_TABLE)
                .select() {
                    filter { eq("user_id", userId); eq("status", "active") }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<CartHeader>()

            if (existingCart != null) {
                // Extracción simple: Long a Int
                val id = existingCart.id.toInt()
                if (id != null) activeCartIdCache[userId] = id
                return@withContext id
            }

            // 2. Si no existe, crear uno nuevo
            val newCart = mapOf("user_id" to userId, "status" to "active")
            val insertedCart = supabase.from(CARTS_TABLE)
                .insert(newCart)
                .decodeSingle<CartHeader>()

            val newId = insertedCart.id.toInt()
            if (newId != null) activeCartIdCache[userId] = newId
            return@withContext newId

        } catch (e: Exception) {
            Log.e("ManagmentCart", "Error al obtener/crear carrito: ${e.message}")
            return@withContext null
        }
    }

    private suspend fun getOrderItems(): List<CartProductDetail> = withContext(Dispatchers.IO) {
        return@withContext getListCart()
    }

    suspend fun getCartItemsForCheckout(): List<OrderItem> {
        val cartDetails = getOrderItems()

        return cartDetails.map { detail ->
            OrderItem(
                product_id = detail.product_id,
                quantity = detail.quantity,
                // Precio debe ser unitario, usando el precio actual del producto cargado
                unit_price_cents = detail.product.price?.toDouble() ?: 0.0,
                selected_size = detail.size
            )
        }
    }
    private suspend fun updateCartItem(
        cartId: Int,
        item: Product,
        quantityChange: Int,
        operation: String,
        sizeSelected: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Obtener la fila actual del cart_items
            val currentItem = supabase.from(CART_ITEMS_TABLE)
                .select() {
                    filter { eq("cart_id", cartId); eq("product_id", item.id); eq("size",sizeSelected) }
                    limit(1)
                }
                // Asumo que CartItem es el modelo simple de la fila cart_items (id, product_id, quantity...)
                .decodeSingleOrNull<CartItemRequest>()



            val currentQuantity = currentItem?.quantity ?: 0
            val newQuantity = when (operation) {
                "PLUS" -> currentQuantity + quantityChange
                "MINUS" -> currentQuantity - quantityChange
                else -> currentQuantity + quantityChange
            }

            // --- LÓGICA DE ELIMINACIÓN ---
            if (newQuantity <= 0) {
                // Eliminar el ítem si la cantidad es 0 o menos
                if (currentItem != null && currentItem.id != null) {
                    // CORRECCIÓN 1: Usar el ID del registro para eliminar la fila.
                    // DEBE terminar con .execute()
                    supabase.from(CART_ITEMS_TABLE).delete {
                        filter { eq("id", currentItem.id) }
                    }
                }
                return@withContext true
            }

            // 2. Preparar el Request
            val priceInCents = ((item.price ?: 0.0) * 100).toInt()

            val request = CartItemRequest(
                // El ID de la clave primaria (id de la fila) solo se usa para UPDATE.
                id = currentItem?.id,
                cart_id = cartId,
                product_id = item.id,
                quantity = newQuantity,
                unit_price_cents = priceInCents,
                size = sizeSelected
            )

            // --- LÓGICA DE INSERCIÓN/ACTUALIZACIÓN CONDICIONAL ---
            if (currentItem?.id != null) {
                // CASO DE ACTUALIZACIÓN (UPDATE)

                // 1. Crear un objeto de actualización que solo incluya los campos mutables
                val updateRequest = CartItemUpdate(
                    quantity = newQuantity,
                    unit_price_cents = priceInCents
                )

                // 2. Ejecutar la actualización (PATCH) sin enviar el ID inmutable
                supabase.from(CART_ITEMS_TABLE)
                    .update(updateRequest) { // Usamos el request limpio
                        filter { eq("id", currentItem.id!!) } // Filtramos por el ID del registro
                    }

            } else {
                // CASO DE INSERCIÓN (INSERT)
                val insertRequest = CartItemInsertRequest(
                    cart_id = cartId,
                    product_id = item.id,
                    quantity = newQuantity,
                    unit_price_cents = priceInCents,
                    size = sizeSelected

                )

                // 2. Ejecutar el INSERT
                supabase.from(CART_ITEMS_TABLE)
                    .insert(insertRequest) // Usamos el Request limpio sin 'id'
            }

            return@withContext true
        } catch (e: Exception) {
            Log.e("ManagmentCart", "Error al actualizar ítem: ${e.message}")
            return@withContext false
        }
    }

    fun insertFood(item: Product, quantityChange: Int = 1, selectedSize: String) {
        scope.launch {
            val userId = tokenManager.getUserId() ?: return@launch
            val cartId = getOrCreateActiveCartId(userId) ?: return@launch

            val success = updateCartItem(cartId, item, quantityChange, "PLUS", selectedSize)

            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Producto añadido.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Fallo al añadir producto.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    suspend fun getListCart(): List<CartProductDetail> = withContext(Dispatchers.IO) {
        val userId = tokenManager.getUserId() ?: return@withContext emptyList()
        val cartId = getOrCreateActiveCartId(userId) ?: return@withContext emptyList()

        try {
            val itemsList = supabase.from(CART_ITEMS_TABLE)
                .select(columns = Columns.raw("product_id, quantity, size, product:Product(*)")) {
                    filter {
                        eq("cart_id", cartId)
                    }
                }
                .decodeList<CartProductDetail>()

            return@withContext itemsList
        } catch (e: Exception) {
            Log.e("ManagmentCart", "Error al obtener lista del carrito: ${e.message}")
            return@withContext emptyList()
        }
    }

    fun minusItem(item: Product, listener: ChangeNumberItemsListener,size: String) {
        scope.launch {
            val userId = tokenManager.getUserId() ?: return@launch
            val cartId = getOrCreateActiveCartId(userId) ?: return@launch



            // Restar 1 al quantity
            val success = updateCartItem(cartId, item, 1, "MINUS",size )
            if (success) withContext(Dispatchers.Main) { listener.onChanged() }
        }
    }

    fun plusItem(item: Product, listener: ChangeNumberItemsListener,size: String) {
        scope.launch {
            val userId = tokenManager.getUserId() ?: return@launch
            val cartId = getOrCreateActiveCartId(userId) ?: return@launch

            // Sumar 1 al quantity
            val success = updateCartItem(cartId, item, 1, "PLUS", size)
            if (success) withContext(Dispatchers.Main) { listener.onChanged() }
        }
    }

    suspend fun getTotalFee(): Double = withContext(Dispatchers.IO) {
        val userId = tokenManager.getUserId() ?: return@withContext 0.0
        val cartId = getOrCreateActiveCartId(userId) ?: return@withContext 0.0

        try {
            // Se asume que la lógica de CartActivity utiliza esta tarifa.
            val cartDetails = getListCart()

            // Calculamos el total localmente
            val totalFee = cartDetails.sumOf { detail ->
                // Usamos el precio del producto y la cantidad del carrito
                (detail.product.price ?: 0.0) * detail.quantity
            }

            return@withContext totalFee

        } catch (e: Exception) {
            Log.e("ManagmentCart", "Error al calcular la tarifa total: ${e.message}")
            return@withContext 0.0
        }
    }

    suspend fun clearCartItems(cartId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("ManagmentCart", "Limpiando ítems del carrito ID: $cartId")

            // Eliminar todas las filas de cart_items asociadas a este cart_id
            supabase.from(CART_ITEMS_TABLE).delete {
                filter { eq("cart_id", cartId) }
            }

            return@withContext true
        } catch (e: Exception) {
            Log.e("ManagmentCart", "Error al limpiar ítems del carrito: ${e.message}")
            return@withContext false
        }
    }
}