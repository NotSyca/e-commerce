package com.example.e_commerce.UI

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope // Importar para la coroutine
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.UI.Adapter.CartAdapter
import com.example.e_commerce.databinding.ActivityCartBinding
import com.example.e_commerce.Helper.ChangeNumberItemsListener
import com.example.e_commerce.Helper.ManagmentCart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch // Importar launch
import java.util.Locale

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
    }
}