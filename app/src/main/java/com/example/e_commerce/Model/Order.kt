// Archivo: Model/Order.kt (O donde almacenas tus modelos)

import com.example.e_commerce.Model.Product
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OrderIdResponse(
    val id: Long // Asumimos que el ID es un Int simple. Si es un UUID/String, ver Opción 2.
)
// Estructura para la cabecera del pedido (Tabla orders)
@Serializable
data class OrderRequest(
    // El ID de usuario se toma del auth.uid() en la Activity/Fragment
    val user_id: String,
    val total_amount: Double,
    val status: String = "pendiente", // Estado por defecto
    val address_shipping: String // Dirección de envío
)

// Estructura para los ítems del pedido (Tabla order_items)
@Serializable
data class OrderItem(
    // order_id se obtendrá al insertar la cabecera del pedido
    val product_id: Int,
    val quantity: Int,
    val unit_price_cents: Double,
    val selected_size: String? = null
)

