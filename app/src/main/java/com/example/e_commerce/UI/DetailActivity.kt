// Archivo: UI/DetailActivity.kt

package com.example.e_commerce.UI

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.e_commerce.R // Asegúrate de que este es tu paquete
import com.example.e_commerce.UI.Fragments.DetailProductFragment
import com.example.e_commerce.databinding.ActivityDetailBinding

// Asume que BaseActivity hereda AppCompatActivity, inicializa 'supabase' y 'tokenManager',
// y expone 'val supabase: SupabaseClient' y 'val tokenManager: TokenManager'
class DetailActivity : BaseActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var currentProductId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentProductId = intent.getIntExtra("EXTRA_PRODUCT_ID", 0)

        // 2. Cargar el fragmento de detalles solo si el ID es válido (ID > 0)
        if (savedInstanceState == null && currentProductId > 0) { // Usamos > 0 como verificación
            showDetailFragment(currentProductId)
        } else {
            // Manejar el error si el ID del Intent no era válido o estaba ausente
            Log.e("DetailActivity", "Error: ID de producto inválido o no recibido. ID: $currentProductId")
            Toast.makeText(this, "Error al cargar el producto. ID no encontrado.", Toast.LENGTH_LONG).show()
            finish() // Cerrar la Activity si el recurso clave no está disponible
        }
    }

    /**
     * Muestra el fragmento de detalles del producto (DetailProductFragment).
     */
    private fun showDetailFragment(productId: Int) {
        val detailFragment = DetailProductFragment.newInstance(productId)
        replaceFragment(detailFragment, false)
    }

    /**
     * Muestra el fragmento de edición del producto (EditProductFragment).
     * Llamada desde el DetailProductFragment cuando se presiona 'Edit'.
     */
    fun showEditFragment(productId: Int) {
        val editFragment = EditProductFragment.newInstance(productId)
        replaceFragment(editFragment, true)
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()

        transaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        // R.id.fragment_container debe ser el ID del FrameLayout en activity_detail.xml
        transaction.replace(com.example.e_commerce.R.id.fragment_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }
}