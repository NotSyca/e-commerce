package com.example.e_commerce.UI.Fragments

import android.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.e_commerce.Model.CreateProductRequest
import com.example.e_commerce.Model.BrandModel
import com.example.e_commerce.BuildConfig
import com.example.e_commerce.databinding.FragmentAddProductBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage // Importar Storage
import io.github.jan.supabase.storage.storage // Extensión Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import com.example.e_commerce.Model.MainViewModel
import com.example.e_commerce.UI.Adapter.ImagePreviewAdapter
import io.github.jan.supabase.postgrest.from

class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter
    private lateinit var supabase: SupabaseClient
    private lateinit var viewModel: MainViewModel // ViewModel para cargar marcas

    private var availableBrands: List<BrandModel> = emptyList()
    private var selectedBrandId: Int? = null
    private val PRODUCTS_STORAGE_BUCKET = "Products" // Tu bucket de productos

    companion object {
        const val TAG = "AddProductFragment"
    }

    private fun initializeSupabase() {
        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Storage) // CRÍTICO: Instalar módulo Storage
        }
    }

    // Selector de imagen (Permite múltiples selecciones para el Array de URLs)
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)
            imagePreviewAdapter.notifyDataSetChanged()
            binding.rvImagePreview.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        initializeSupabase()
        // Obtenemos el ViewModel de la Activity contenedora (o del Fragment si está definido con Factory)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBrandSelector() // Configurar el selector de marcas

        binding.btnSelectImage.setOnClickListener {
            pickImages.launch("image/*") // Iniciar selección múltiple
        }

        binding.btnSubmit.setOnClickListener {
            submitProduct()
        }
    }

    private fun setupRecyclerView() {
        imagePreviewAdapter = ImagePreviewAdapter(selectedImageUris)
        binding.rvImagePreview.adapter = imagePreviewAdapter
    }

    // -------------------------------------------------------------------
    // LÓGICA DE SELECCIÓN DE MARCAS
    // -------------------------------------------------------------------

    private fun setupBrandSelector() {
        viewModel.brands.observe(viewLifecycleOwner) { brands ->
            availableBrands = brands
            val brandNames = brands.map { it.name }

            val adapter = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, brandNames)
            val brandTextView = binding.etBrand as? AutoCompleteTextView
            brandTextView?.setAdapter(adapter)

            brandTextView?.onItemClickListener =
                AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val selectedName = parent.getItemAtPosition(position).toString()
                    val brand = availableBrands.find { it.name == selectedName }
                    selectedBrandId = brand?.id
                    Log.d(TAG, "Marca seleccionada: $selectedName (ID: $selectedBrandId)")
                }
        }
        // Nota: Asegúrate que loadBrands() se llama en HomeActivity o ExplorerFragment
    }

    /**
     * Envía el producto a Supabase: Sube la imagen(es) a Storage e inserta la URL en PostgREST.
     */
    private fun submitProduct() {
        // 1. Capturar y validar datos
        val title = binding.etName.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim()
        val priceText = binding.etPrice.text?.toString()?.trim()
        val ratingText = binding.etRating.text?.toString()?.trim()
        val stockText = binding.etStock.text?.toString()?.trim()
        val sizesText = binding.etSizes.text?.toString()?.trim() // Campo para Tallas

        // **Validaciones**
        if (title.isBlank() || priceText.isNullOrBlank() || selectedBrandId == null || selectedImageUris.isEmpty()) {
            Toast.makeText(requireContext(), "Faltan datos obligatorios.", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull() ?: run { Toast.makeText(requireContext(), "Precio inválido.", Toast.LENGTH_SHORT).show(); return }
        val rating = ratingText?.toDoubleOrNull() ?: 0.0
        val stock = stockText?.toIntOrNull() ?: 0
        val sizes = sizesText?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: listOf()

        binding.progress.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 2. Subir imágenes y obtener URLs
                val picUrls = uploadImagesAndGetUrls(selectedImageUris)

                if (picUrls.isEmpty()) {
                    throw Exception("Fallo al subir o obtener las URLs de las imágenes.")
                }

                // 3. Crear el producto para inserción
                val productToCreate = CreateProductRequest(
                        title = title,
                        description = description.toString(),
                        brand = selectedBrandId!!,
                        price = price,
                        rating = rating,
                        picUrl = picUrls,
                        size = sizes
                    )

                // 4. Insertar en PostgREST
                // **NOTA:** La función 'addProduct' debería estar en el ViewModel para encapsulación,
                // pero si la haces aquí, usa from() del cliente Supabase inicializado.
                supabase.from("Product").insert(productToCreate)

                Toast.makeText(requireContext(), "Producto '$title' creado exitosamente", Toast.LENGTH_LONG).show()
                clearForm()

            } catch (e: Exception) {
                Log.e(TAG, "Error al crear producto: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progress.visibility = View.GONE
                binding.btnSubmit.isEnabled = false
            }
        }
    }

    // -------------------------------------------------------------------
    // FUNCIÓN DE SUBIDA A STORAGE (Migrada del ViewModel para acceso a Context)
    // -------------------------------------------------------------------

    private suspend fun uploadImagesAndGetUrls(uris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Inicio de la subida de imágenes. Cantidad de URIs: ${uris.size}")

        val uploadJobs = uris.map { uri ->
            async {
                try {
                    // Generar nombre de archivo único
                    val fileName = "product_${UUID.randomUUID()}.jpg"
                    Log.d(TAG, "Procesando URI: $uri -> Nombre de archivo: $fileName")

                    // 1. Convertir URI a ByteArray (usando la lógica de compresión)
                    // Usamos '?' para manejo seguro de la función compressAndResizeImage
                    val fileByteArray: ByteArray? = compressAndResizeImage(uri, 800, 800)

                    if (fileByteArray == null) {
                        Log.e(TAG, "Error: fileByteArray es NULL para URI $uri (Fallo al leer/comprimir).")
                        return@async null
                    }

                    Log.d(TAG, "Tamaño de bytes leídos: ${fileByteArray.size} bytes.")

                    // 2. Subir a Storage
                    Log.d(TAG, "Subiendo a bucket '$PRODUCTS_STORAGE_BUCKET'...")

                    // NOTA: Asegúrate de que el cliente Supabase (supabase) esté inicializado
                    supabase.storage.from(PRODUCTS_STORAGE_BUCKET)
                        .upload(fileName, fileByteArray, upsert = true)

                    Log.i(TAG, "Éxito en la subida a Storage.")

                    // 3. Obtener URL pública
                    val publicUrl = supabase.storage
                        .from(PRODUCTS_STORAGE_BUCKET)
                        .publicUrl(fileName)

                    Log.i(TAG, "URL pública obtenida: $publicUrl")
                    return@async publicUrl

                } catch (e: Exception) {
                    // Captura fallos de red o de permisos de Storage
                    Log.e(TAG, "FALLO CRÍTICO en la subida para URI $uri: ${e.message}", e)
                    return@async null
                }
            }
        }

        // Esperar a que todas las subidas terminen
        val results = uploadJobs.awaitAll().filterNotNull()
        Log.d(TAG, "Proceso de subida finalizado. URLs válidas generadas: ${results.size}")

        return@withContext results
    }

    /** Helper para compresión/redimensionamiento (lógica similar a la original) */
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
        binding.etDescription.text?.clear()
        binding.etPrice.text?.clear()
        (binding.etBrand as? AutoCompleteTextView)?.setText("", false) // Limpiar el Spinner
        binding.etRating.text?.clear()
        binding.etStock.text?.clear()
        binding.etSizes.text?.clear()
        selectedImageUris.clear()
        imagePreviewAdapter.notifyDataSetChanged()
        binding.rvImagePreview.visibility = View.GONE
        selectedBrandId = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}