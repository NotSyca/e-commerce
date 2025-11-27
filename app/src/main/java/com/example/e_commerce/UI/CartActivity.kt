package com.example.e_commerce.UI

import OrderIdResponse
import OrderItem
import OrderRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope // Importar para la coroutine
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.UI.Adapter.CartAdapter
import com.example.e_commerce.databinding.ActivityCartBinding
import com.example.e_commerce.Helper.ChangeNumberItemsListener
import com.example.e_commerce.Helper.ManagmentCart
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch // Importar launch
import java.util.Locale
import java.util.UUID
import kotlin.collections.map
import kotlinx.serialization.Serializable // Importar Serializable

@Serializable
data class OrderItemInsert(
    val order_id: String,
    val product_id: String,
    val quantity: Int,
    val unit_price_cents: Double,
    val selected_size: String? = null
)

class CartActivity : BaseActivity() {
    private lateinit var binding: ActivityCartBinding
    // NOTA: managmentCart ahora se inicializa con supabase y tokenManager en onCreate
    private lateinit var managmentCart: ManagmentCart
    private var tax: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar ManagmentCart con las dependencias heredadas
        managmentCart = ManagmentCart(this, supabase, tokenManager)

        setVariable()
        // La inicialización y el cálculo DEBEN ir dentro de una coroutine
        loadAndSetupCart()
    }

    public override fun onResume() {
        super.onResume()
        refreshCartData()
    }
    private fun loadAndSetupCart() {
        // Mostrar un ProgressBar de carga inicial si es necesario
        // binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {

            // 1. Obtener los ítems del carrito de forma asíncrona
            val cartItems = managmentCart.getListCart()

            // 2. Configurar la lista del carrito
            binding.viewCart.layoutManager =
                LinearLayoutManager(this@CartActivity, LinearLayoutManager.VERTICAL, false)

            binding.viewCart.adapter =
                CartAdapter(
                    ArrayList(cartItems), this@CartActivity, managmentCart, this@CartActivity, object : ChangeNumberItemsListener {
                        override fun onChanged() {
                            calculatedCart()
                        }
                    }
                )

            // 3. Establecer visibilidad
            val isEmpty = cartItems.isEmpty()
            binding.EmptyTxt.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.scrolView.visibility = if (isEmpty) View.GONE else View.VISIBLE

            // 4. Calcular los totales de forma inmediata
            calculatedCart()

            // binding.progressBar.visibility = View.GONE
        }
    }

    private fun calculatedCart() {
        // La función de cálculo debe ser ASÍNCRONA ya que getTotalFee() es suspend
        lifecycleScope.launch {

            val percentTax = 0.02
            val deliveryFee = 10.0

            // 1. Calcular el subtotal de forma asíncrona
            val subtotal = managmentCart.getTotalFee()

            // 2. Cálculos y formateo
            val subtotalSafe = subtotal // subtotal ya es seguro Double o 0.0
            tax = String.format(Locale.US, "%.2f", subtotalSafe * percentTax).toDouble()
            val total = String.format(Locale.US, "%.2f", subtotalSafe + tax + deliveryFee).toDouble()

            with(binding) {
                // Actualizar los TextViews en el Main Thread
                totalfeeTxt.text = String.format(Locale.US, "$%.2f", subtotalSafe)
                taxTxt.text = String.format(Locale.US, "$%.2f", tax)
                DeliveryTxt.text = String.format(Locale.US, "$%.2f", deliveryFee)
                totalTxt.text = String.format(Locale.US, "$%.2f", total)
            }
        }
    }


    private fun startCheckout() {
        // 1. Recoger datos necesarios (ajusta los IDs del binding según tu layout)
        val shippingAddress = binding.etShippingAddress.text?.toString()?.trim()
        // Asegúrate de que el total se ha calculado y se muestra en la UI
        val totalAmount = binding.totalTxt.text?.toString()?.removePrefix("$")?.toDoubleOrNull()

        val currentUserId = supabase.auth.currentUserOrNull()?.id

        if (shippingAddress.isNullOrBlank() || totalAmount == null || totalAmount <= 0 || currentUserId.isNullOrBlank()) {
            Toast.makeText(this, "Verifica la dirección, el total del pedido y la sesión.", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Obtener ítems del carrito (de ManagmentCart)
        lifecycleScope.launch {
            val orderItemsToSave = managmentCart.getCartItemsForCheckout()

            if (orderItemsToSave.isEmpty()) {
                Toast.makeText(this@CartActivity, "El carrito está vacío.", Toast.LENGTH_LONG).show()
                return@launch
            }

            // 3. SIMULAR EL PAGO
            // En una app real, aquí se llamaría al SDK de pagos (Stripe, PayPal)
            val simulatedPaymentIntent = "SIMULATED-${UUID.randomUUID()}"
            Log.d(TAG, "CHECKOUT: Pago simulado exitoso. ID: $simulatedPaymentIntent")

            // 4. Proceder a crear la orden en la base de datos
            createOrder(currentUserId, totalAmount, shippingAddress, orderItemsToSave)
        }
    }


    private fun createOrder(
        userId: String,
        totalAmount: Double,
        address: String,
        items: List<OrderItem>
    ) {
       lifecycleScope.launch {
            try {
                // A. Insertar Cabecera de la Orden (orders)
                val orderRequest = OrderRequest(
                    user_id = userId,
                    total_amount = totalAmount,
                    status = "Pendiente", // Estado inicial
                    address_shipping = address
                )

                // Insertar y obtener el ID de la orden creada
                val orderResult = supabase.from("orders")
                    .insert(orderRequest) {
                        select(columns = Columns.list("id"))
                    }
                    .decodeSingleOrNull<OrderIdResponse>() // Decodificamos el resultado para obtener el ID

                val orderId = orderResult?.id.toString()

                if (orderId == null) {
                    throw Exception("Fallo al obtener el ID de la orden después de la inserción.")
                }

                Log.i(TAG, "ORDER_CREATE: Orden principal creada con ID: $orderId")

                // B. Preparar e Insertar Detalles de la Orden (order_items)
                val orderItemsWithId = items.map { item ->
                    OrderItemInsert(
                        order_id = orderId,
                        product_id = item.product_id.toString(),
                        quantity = item.quantity,
                        unit_price_cents = item.unit_price_cents,
                        selected_size = item.selected_size
                    )
                }

                supabase.from("order_items")
                    .insert(orderItemsWithId) // Ejecutar la inserción de la lista de ítems

                Log.i(TAG, "ORDER_CREATE: Detalles de orden insertados correctamente.")

                // C. Limpieza Final y Notificación
                // Asegúrate de que orderId es un Int para clearCartItems

                Toast.makeText(this@CartActivity, "¡Pedido realizado con éxito! Tu orden ha sido registrada. 🛍️", Toast.LENGTH_LONG).show()
                (this@CartActivity as CartActivity).managmentCart.clearCartItems(orderId.toInt())
                this@CartActivity.finish()

            } catch (e: Exception) {
                Log.e(TAG, "ERROR_ORDER: Fallo al crear la orden: ${e.message}", e)
                Toast.makeText(this@CartActivity, "Fallo al procesar el pedido. Intenta de nuevo.", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun refreshCartData() {
        lifecycleScope.launch {
            delay(400)
            // 1. Obtener la nueva lista de ítems de Supabase
            val updatedCartItems = managmentCart.getListCart()

            // 2. Obtener el adaptador actual
            val adapter = binding.viewCart.adapter as? CartAdapter

            // 3. Notificar al adaptador si existe
            if (adapter != null) {

                adapter.updateData(updatedCartItems)

                val isEmpty = updatedCartItems.isEmpty()
                binding.EmptyTxt.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.scrolView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        }
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener {
            finish()
        }
        binding.pagarBtn.setOnClickListener {
            startCheckout()
        }
    }

    val TAG = "CartActivity"

}