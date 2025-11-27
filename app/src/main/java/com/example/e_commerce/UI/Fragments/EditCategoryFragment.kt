package com.example.e_commerce.UI

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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.e_commerce.Api.TokenManager
import com.example.e_commerce.Model.BrandModel
import com.example.e_commerce.Model.MainViewModel
import com.example.e_commerce.databinding.FragmentEditCategoryBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.util.UUID

// Data Class para la categoría (refleja la tabla categories)
@Serializable
data class Category(
    val id: Int,
    val name: String,
    val picUrl: String? = null
)

class EditCategoryFragment : Fragment() {

    private var _binding: FragmentEditCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel

    private var availableBrands: List<BrandModel> = emptyList()
    private var selectedCategoryId: Int? = null
    private var currentpicUrl: String? = null // URL o URI local temporal

    private val CATEGORY_STORAGE_BUCKET = "brand_logos"

    companion object {
        private const val TAG = "EditCategoryFragment"
    }

    // ------------------- LÓGICA DE IMAGEN -------------------

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Previsualización y almacenamiento de la URI local
            Glide.with(this).load(uri).into(binding.imgCategoryPreview)
            currentpicUrl = uri.toString()
            Log.d(TAG, "IMAGEN: Nueva URI local seleccionada para previsualización.")
        }
    }

    private val supabase: SupabaseClient
        get() = (requireActivity() as HomeActivity).supabase

    // ------------------- CICLO DE VIDA -------------------

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        setupListeners()
        loadInitialData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ------------------- CARGA Y SINCRONIZACIÓN -------------------

    private fun setupListeners() {
        binding.btnSave.setOnClickListener { saveCategoryChanges() }
        binding.btnDelete.setOnClickListener { confirmAndDeleteCategory() }
        binding.btnSelectNewImage.setOnClickListener { pickImage.launch("image/*") }
    }

    /**
     * ✅ ÚNICO PUNTO DE ENTRADA PARA LA CARGA DE DATOS.
     * Inicia la carga del ViewModel y configura el observador.
     */
    private fun loadInitialData() {
        Log.d(TAG, "LOAD: Iniciando carga de LiveData de marcas.")

        // 1. Configura el observador (punto de sincronización)
        setupBrandSelector()

        // 2. Inicia la consulta del ViewModel. Esto garantiza que el LiveData se active.
        viewModel.loadBrands()
    }

    /**
     * Configura el selector de marcas usando el LiveData del ViewModel.
     */
    private fun setupBrandSelector() {
        Log.d(TAG, "SYNC: Configurando observador LiveData para marcas.")

        viewModel.brands.observe(viewLifecycleOwner) { brands ->
            availableBrands = brands
            Log.d(TAG, "DATA: LiveData Marcas recibida. Total: ${brands.size} ítems.")

            val brandNames = brands.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brandNames)
            binding.etCategorySelector.setAdapter(adapter)

            binding.etCategorySelector.onItemClickListener =
                AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val selectedName = parent.getItemAtPosition(position).toString()
                    val brand = availableBrands.find { it.name == selectedName }

                    val selectedId = brand?.id

                    // Asignamos el ID y cargamos los detalles de la categoría seleccionada
                    selectedCategoryId = selectedId
                    Log.i(TAG, "USER_ACTION: Categoría seleccionada: ID $selectedCategoryId")

                    selectedCategoryId?.let { id ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            loadCategoryDetails(id) // Carga los detalles de la categoría
                        }
                    }
                }
        }
    }

    /**
     * Carga los detalles (nombre y URL de imagen) de la categoría seleccionada desde la DB.
     */
    private fun loadCategoryDetails(id: Int) {
        selectedCategoryId = id
        Log.d(TAG, "LOAD_DB: Consultando detalles de Categoría ID: $id")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val categoryObject = supabase.from(table = "categories")
                    .select(){ filter { eq("id", id) } }
                    .decodeSingleOrNull<Category>()

                if (categoryObject != null) {
                    Log.d(TAG, "CARGA: Categoría ID $id cargada. Imagen URL: ${categoryObject.picUrl}")

                    binding.etCategoryName.setText(categoryObject.name)
                    currentpicUrl = categoryObject.picUrl // Guardamos la URL de la DB

                    Glide.with(this@EditCategoryFragment)
                        .load(currentpicUrl)
                        .placeholder(com.example.e_commerce.R.drawable.grey_bg)
                        .into(binding.imgCategoryPreview)

                } else {
                    Log.w(TAG, "ERROR_DB: Categoría ID $id no encontrada.")
                    Toast.makeText(requireContext(), "Error: Categoría no encontrada.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "ERROR: Fallo al cargar detalles de categoría: ${e.message}", e)
                Toast.makeText(requireContext(), "Error de red al cargar categoría.", Toast.LENGTH_LONG).show()
            }
        }
    }


    // ------------------- GUARDAR Y ELIMINAR -------------------

    private fun saveCategoryChanges() {
        if (selectedCategoryId == null) {
            Toast.makeText(requireContext(), "Selecciona una categoría primero.", Toast.LENGTH_SHORT).show()
            return
        }
        val newName = binding.etCategoryName.text?.toString()?.trim()

        if (newName.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "El nombre no puede estar vacío.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            var finalpicUrl = currentpicUrl

            try {
                // 1. Si currentpicUrl contiene una URI local, subirla.
                if (currentpicUrl?.startsWith("content://") == true) {
                    val uri = Uri.parse(currentpicUrl)
                    Log.d(TAG, "STORAGE: Subiendo nueva imagen desde URI.")

                    finalpicUrl = uploadImageAndGetUrl(uri) // Subida asíncrona

                    if (finalpicUrl.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "Fallo crítico al subir imagen. No se pudo guardar.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }

                // 2. Ejecutar el UPDATE en la tabla 'categories'
                val updateData = mapOf(
                    "name" to newName,
                    "picUrl" to finalpicUrl
                )

                val result: PostgrestResult = supabase.from("categories")
                    .update(updateData) {
                        filter { eq(column = "id", value = selectedCategoryId!!) }
                    }

                // ... (manejo de éxito) ...
                Log.i(TAG, "SAVE_DB: Categoría actualizada con éxito.")
                Toast.makeText(requireContext(), "Categoría actualizada con éxito. ✅", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                Log.e(TAG, "ERROR: Fallo al guardar categoría: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmAndDeleteCategory() {
        if (selectedCategoryId == null) {
            Toast.makeText(requireContext(), "Selecciona una categoría para eliminar.", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación de Categoría")
            .setMessage("ADVERTENCIA: Eliminar esta categoría (ID: $selectedCategoryId) dejará a todos los productos asociados sin una categoría válida. ¿Deseas continuar?")
            .setPositiveButton("Eliminar") { dialog, which ->
                deleteCategory(selectedCategoryId!!)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCategory(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "DELETE_DB: Ejecutando DELETE en tabla 'categories' para ID $id.")

                // La DB debe estar configurada con ON DELETE SET NULL en la FK de la tabla Product.
                supabase.from("categories").delete {
                    filter { eq(column = "id", value = id) }
                }

                Log.i(TAG, "DELETE_DB: Categoría eliminada con éxito.")
                Toast.makeText(requireContext(), "Categoría eliminada con éxito. 🗑️", Toast.LENGTH_LONG).show()

                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                Log.e(TAG, "ERROR_DB: Fallo en DELETE de categoría: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()

                if (e.message?.contains("violates foreign key constraint") == true) {
                    Toast.makeText(requireContext(), "Error: Hay productos que aún usan esta categoría. Actualízalos primero.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Helper para compresión/redimensionamiento (copiado de EditProductFragment) */
    private fun compressAndResizeImage(uri: Uri, maxWidth: Int, maxHeight: Int): ByteArray? {
        // ... (Tu implementación de compresión) ...
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            // ... (resto de la lógica de redimensionamiento y compresión) ...
            if (originalBitmap == null) return null
            val scale = kotlin.math.min(maxWidth.toFloat() / originalBitmap.width, maxHeight.toFloat() / originalBitmap.height)
            val newWidth = (originalBitmap.width * scale).toInt()
            val newHeight = (originalBitmap.height * scale).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "ERROR en compresión/redimensionamiento para URI $uri", e)
            null
        }
    }

    private suspend fun uploadImageAndGetUrl(uri: Uri): String? = withContext(Dispatchers.IO) {
        // ... (Tu implementación de subida a Storage) ...
        try {
            val fileName = "category_${UUID.randomUUID()}.jpg"
            val fileByteArray: ByteArray? = compressAndResizeImage(uri, 800, 800)

            if (fileByteArray == null) return@withContext null

            // Subir a Storage
            (requireActivity() as HomeActivity).supabase.storage.from(CATEGORY_STORAGE_BUCKET)
                .upload(fileName, fileByteArray, upsert = true)

            // Obtener URL pública
            val publicUrl = (requireActivity() as HomeActivity).supabase.storage
                .from(CATEGORY_STORAGE_BUCKET)
                .publicUrl(fileName)

            return@withContext publicUrl

        } catch (e: Exception) {
            Log.e(TAG, "ERROR_STORAGE: FALLO CRÍTICO en la subida de categoría: ${e.message}", e)
            return@withContext null
        }
    }
}