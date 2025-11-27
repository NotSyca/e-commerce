package com.example.e_commerce.UI.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.e_commerce.Helper.CartProductDetail
import com.example.e_commerce.Helper.ChangeNumberItemsListener
import com.example.e_commerce.Helper.ManagmentCart // Importar ManagmentCart
import com.example.e_commerce.UI.CartActivity
import com.example.e_commerce.UI.DetailActivity // Asumido
import com.example.e_commerce.databinding.ViewholderCartBinding // Asumo el binding
import java.util.Locale

class CartAdapter(
    // CRÍTICO 1: Recibe la lista con los detalles de la cantidad y producto
    val items: List<com.example.e_commerce.Helper.CartProductDetail>,
    private val context: Context,
    // CRÍTICO 2: Recibe el ManagmentCart y el Listener en el constructor
    private val managmentCart: ManagmentCart,
    private val cartActivity: CartActivity,
    private val changeNumberItemsListener: ChangeNumberItemsListener
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    class ViewHolder(val binding: ViewholderCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // La inicialización de contexto ya no está aquí; se recibe en el constructor
        val binding = ViewholderCartBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    fun updateData(newItems: List<CartProductDetail>) {
        // Tu adaptador usa 'val items: List<CartProductDetail>' que es inmutable.
        (items as MutableList).clear()
        (items as MutableList).addAll(newItems)
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemDetail = items[position]
        val product = itemDetail.product

        // CORRECCIÓN 1: Enlace de Texto (usar product.title y itemDetail.quantity)
        holder.binding.titleTxt.text = product.title
        holder.binding.feeEachitem.text = String.format(Locale.US, "$%.2f", product.price)
        holder.binding.totalEachItem.text = String.format(Locale.US, "$%.2f", product.price * itemDetail.quantity)
        holder.binding.sizeTxt.text = String.format("Tamaño: %s", itemDetail.size)
        holder.binding.numberitemTxt.text = itemDetail.quantity.toString() // Muestra la cantidad del carrito (Error 7, 13)
        val imageUrl = product.picUrl.firstOrNull() // picUrl es ArrayList<String>
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(context).load(imageUrl).into(holder.binding.pic) // Asumo imgProduct
        }

        // 3. Listener PLUS (Añadir ítem)
        holder.binding.addBtn.setOnClickListener {
            cartActivity.onResume()
            managmentCart.plusItem(product, changeNumberItemsListener, itemDetail.size)
        }

        // 4. Listener MINUS (Quitar ítem)
        holder.binding.minusBtn.setOnClickListener {
            cartActivity.onResume()
            managmentCart.minusItem(product, changeNumberItemsListener, itemDetail.size)

        }

        // 5. Click en el ítem (Opcional)
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", product) // Pasar el objeto Product
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}