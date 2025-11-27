package com.example.e_commerce.UI.Fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.e_commerce.Model.CreateCategoryRequest // Modelo necesario
import com.example.e_commerce.UI.Adapter.ImagePreviewAdapter
import com.example.e_commerce.databinding.FragmentAddCategoryBinding // Asumo el binding correcto
import com.example.e_commerce.BuildConfig // Asumo la ubicación de BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

// -------------------------------------------------------------------
// NOTA: Define este modelo en tu archivo de modelos (ej. Category.kt)
// @Serializable
// data class CreateCategoryRequest(
//     val name: String,
//     val picUrl: String // URL de la imagen del logo
// )
// -------------------------------------------------------------------

class AddCategoryFragment : Fragment() {

    private var _binding: FragmentAddCategoryBinding? = null
    private val binding get() = _binding!!

    private val selectedImageUris = mutableListOf<Uri>() // Usaremos solo la primera URI
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter
    private lateinit var supabase: SupabaseClient
    // Asumo que TokenManager y Supabase se inicializan aquí ya que no hereda de BaseActivity

    private val CATEGORIES_STORAGE_BUCKET = "brand_logos" // Bucket para logos
    private val CATEGORIES_TABLE_NAME = "categories" // Tabla PostgREST

    companion object {
        private const val TAG = "AddCategoryFragment"
    }

    private fun initializeSupabase() {
        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Storage)
        }
    }

    // Selector de imagen (solo una imagen)
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUris.clear()
            selectedImageUris.add(uri) // Solo permitimos una
            imagePreviewAdapter.notifyDataSetChanged()
            binding.rvImagePreview.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inicializar servicios que requieren Contexto/Configuración
        initializeSupabase()
        // tokenManager = TokenManager(requireContext()) // Asumo que no se usa TokenManager aquí

        _binding = FragmentAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.btnSelectImage.setOnClickListener {
            // Solo una imagen para el logo de la categoría
            pickImage.launch("image/*")
        }

        binding.btnSubmit.setOnClickListener {
            submitCategory()
        }
    }

    private fun setupRecyclerView() {
        // Usamos una lista de una sola imagen para la vista previa
        imagePreviewAdapter = ImagePreviewAdapter(selectedImageUris)
        binding.rvImagePreview.adapter = imagePreviewAdapter
    }

    /**
     * Envía la categoría a Supabase: Sube la imagen del logo e inserta el registro.
     */
    private fun submitCategory() {
        // 1. Capturar y validar datos
        val name = binding.etName.text?.toString()?.trim().orEmpty()

        if (name.isBlank()) {
            Toast.makeText(requireContext(), "El nombre de la categoría es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(requireContext(), "Debes seleccionar un logo/imagen", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progress.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 2. Subir imagen y obtener URL (solo la primera URI)
                val picUrls = uploadImagesAndGetUrls(selectedImageUris)
                val picUrl = picUrls.firstOrNull() // Obtener la única URL

                if (picUrl.isNullOrEmpty()) {
                    throw Exception("Fallo al subir o obtener la URL del logo.")
                }

                // 3. Crear el objeto de solicitud
                val categoryToCreate = CreateCategoryRequest(
                    name = name,
                    picUrl = picUrl // URL pública del logo
                )

                // 4. Insertar en PostgREST
                supabase.from(CATEGORIES_TABLE_NAME).insert(categoryToCreate)// Terminar con execute

                Log.d(TAG, "Categoría creada exitosamente: $name")
                Toast.makeText(requireContext(), "Categoría '$name' creada exitosamente", Toast.LENGTH_LONG).show()
                clearForm()

            } catch (e: Exception) {
                Log.e(TAG, "Error al crear categoría: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progress.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    // -------------------------------------------------------------------
    // FUNCIÓN DE SUBIDA A STORAGE (Adaptada para el Fragmento)
    // -------------------------------------------------------------------

    /**
     * Sube las imágenes a Storage y devuelve una lista de URLs públicas.
     * Adaptada de la lógica del Producto, pero aquí solo se usa la primera URI.
     */
    private suspend fun uploadImagesAndGetUrls(uris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext emptyList()

        val uri = uris.first()
        val uploadJob = async {
            try {
                val fileName = "category_logo_${UUID.randomUUID()}.jpg"

                // Convertir URI a ByteArray (usando la lógica de compresión/redimensionamiento)
                val fileByteArray: ByteArray =
                    compressAndResizeImage(uri, 400, 400)!! // Logos más pequeños

                // Subir a Storage
                supabase.storage.from(CATEGORIES_STORAGE_BUCKET)
                    .upload(fileName, fileByteArray, upsert = true)


                // Obtener URL pública
                return@async supabase.storage.from(CATEGORIES_STORAGE_BUCKET).publicUrl(fileName)

            } catch (e: Exception) {
                Log.e(TAG, "Fallo al subir logo: ${e.message}", e)
                return@async null
            }
        }

        // Esperar el resultado y devolver la lista (de 0 o 1 URL)
        uploadJob.await()?.let { listOf(it) } ?: emptyList()
    }

    // ... (La función compressAndResizeImage se mantiene igual, adaptada para este Fragmento) ...
    private fun compressAndResizeImage(uri: Uri, maxWidth: Int, maxHeight: Int): ByteArray? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            val scale = Math.min(maxWidth.toFloat() / originalBitmap.width, maxHeight.toFloat() / originalBitmap.height)
            val newWidth = (originalBitmap.width * scale).toInt()
            val newHeight = (originalBitmap.height * scale).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error en compresión/redimensionamiento", e)
            null
        }
    }


    private fun clearForm() {
        binding.etName.text?.clear()
        selectedImageUris.clear()
        imagePreviewAdapter.notifyDataSetChanged()
        binding.rvImagePreview.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}