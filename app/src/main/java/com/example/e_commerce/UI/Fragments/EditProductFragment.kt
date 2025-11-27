package com.example.e_commerce.UI

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.e_commerce.Model.BrandModel
import com.example.e_commerce.Model.MainViewModel
import com.example.e_commerce.Model.Product
import com.example.e_commerce.Model.UpdateProductRequest
import com.example.e_commerce.UI.Adapter.EditableImageAdapter
import com.example.e_commerce.databinding.FragmentEditProductBinding
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.collections.toMap

class EditProductFragment : Fragment() {

    private var _binding: FragmentEditProductBinding? = null
    private val binding get() = _binding!!

    private var productId: Int = -1

    private lateinit var viewModel: MainViewModel

    private val currentImageUrls = mutableListOf<String>()
    private lateinit var imageAdapter: EditableImageAdapter

    private var availableBrands: List<BrandModel> = emptyList()
    private var selectedBrandId: Int? = null

    companion object {
        private const val TAG = "EditProductFragment"
        private const val ARG_PRODUCT_ID = "product_id"

        fun newInstance(productId: Int) = EditProductFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_PRODUCT_ID, productId)
            }
        }
    }

    private val supabase: SupabaseClient
        get() = (requireActivity() as DetailActivity).supabase

    // ---------------------------------------------------------------------------------
    //  CICLO DE VIDA Y SETUP
    // ---------------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProductBinding.inflate(inflater, container, false)
        Log.d(TAG, "LIFECYCLE: onCreateView ejecutado.")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        Log.d(TAG, "LIFECYCLE: ViewModel inicializado.")

        productId = arguments?.getInt(ARG_PRODUCT_ID, -1) ?: -1
        Log.d(TAG, "LIFECYCLE: Producto ID recibido: $productId")

        if (productId != -1) {
            setupImageRecyclerView()
            setupListeners()
            loadInitialData(productId)
        } else {
            Log.e(TAG, "ERROR: Producto ID es inválido, saliendo.")
            Toast.makeText(requireContext(), "Error: ID de producto no válido", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "LIFECYCLE: onDestroyView ejecutado (Binding limpiado).")
    }

    private fun setupImageRecyclerView() {
        Log.d(TAG, "SETUP: Configurando RecyclerView de imágenes.")
        imageAdapter = EditableImageAdapter(requireContext(), currentImageUrls) { position ->
            imageAdapter.removeImageAt(position)
            Log.d(TAG, "USER_ACTION: Imagen eliminada localmente en posición: $position")
            Toast.makeText(requireContext(), "Imagen eliminada.", Toast.LENGTH_SHORT).show()
        }
        binding.imageRecyclerView.adapter = imageAdapter
        binding.imageRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupListeners() {
        Log.d(TAG, "SETUP: Configurando listeners de UI.")
        binding.backBtn.setOnClickListener {
            Log.d(TAG, "USER_ACTION: Botón 'Atrás' presionado. Pop back stack.")
            parentFragmentManager.popBackStack()
        }
        binding.saveBtn.setOnClickListener { saveProductChanges() }
        binding.deleteProductBtn.setOnClickListener { confirmAndDeleteProduct(productId) }
        binding.btnSelectImage.setOnClickListener { simulateImageSelection() }
    }

    private fun simulateImageSelection() {
        val newDummyUrl = "https://picsum.photos/id/${(100..150).random()}/200/200"
        currentImageUrls.add(newDummyUrl)
        imageAdapter.notifyItemInserted(currentImageUrls.size - 1)
        Log.d(TAG, "DATA_CHANGE: URL de simulación añadida. Total URLs: ${currentImageUrls.size}")
        Toast.makeText(requireContext(), "Imagen añadida (Simulación).", Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------------------------------
    //  CARGA DE DATOS Y SINCRONIZACIÓN
    // ---------------------------------------------------------------------------------

    private fun loadInitialData(id: Int) {
        Log.d(TAG, "LOAD: Iniciando carga de datos iniciales.")

        setupBrandSelector() // Inicia la observación de marcas (LiveData)

        viewLifecycleOwner.lifecycleScope.launch {
            loadProductDetails(id) // Carga los detalles del producto
        }
    }

    private suspend fun loadProductDetails(id: Int) {
        Log.d(TAG, "LOAD_DB: Producto - Consultando ID: $id")
        try {
            // Decodificación directa a la clase Product (@Serializable)
            val productObject = supabase.from(table = "Product")
                .select(){ filter { eq("id", id) } }
                .decodeSingleOrNull<Product>()

            if (productObject == null) {
                Log.e(TAG, "ERROR_DB: Producto ID $id no encontrado.")
                Toast.makeText(requireContext(), "Producto no encontrado.", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
                return
            }

            Log.d(TAG, "DATA: Producto DB cargado. Título: ${productObject.title}, Brand ID crudo: ${productObject.brand}")

            // PASO 1: Guardar el ID de la marca del producto cargado.
            selectedBrandId = productObject.brand

            // Rellenar campos de texto
            binding.etTitle.setText(productObject.title)
            binding.etDescription.setText(productObject.description)
            binding.etPrice.setText(productObject.price.toString())
            binding.etRating.setText(productObject.rating?.toString())
            binding.etSizes.setText(productObject.size?.joinToString(", "))

            // Cargar URLs de imágenes
            val urlsArray = productObject.picUrl ?: emptyList()
            currentImageUrls.clear()
            currentImageUrls.addAll(urlsArray)
            imageAdapter.notifyDataSetChanged()

            // PASO 2: Intentar sincronizar el dropdown si las marcas ya llegaron
            if (availableBrands.isNotEmpty()) {
                val currentBrandName = availableBrands.find { it.id == selectedBrandId }?.name
                binding.etBrand.setText(currentBrandName, false)
                Log.i(TAG, "SYNC_CHECK: Sincronización inmediata. Marca inicial establecida: $currentBrandName (ID: $selectedBrandId)")
            } else {
                Log.w(TAG, "SYNC_CHECK: Marcas aún no disponibles. Esperando LiveData.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "ERROR_DB: Fallo al cargar datos del producto: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al cargar datos.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBrandSelector() {
        Log.d(TAG, "LOAD_VM: Marcas - Configurando observador LiveData.")
        viewModel.brands.observe(viewLifecycleOwner) { brands ->
            availableBrands = brands

            Log.d(TAG, "DATA: LiveData Marcas recibida. Total: ${brands.size} ítems.")

            val brandNames = brands.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brandNames)
            binding.etBrand.setAdapter(adapter)

            binding.etBrand.onItemClickListener =
                AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val selectedName = parent.getItemAtPosition(position).toString()
                    val brand = availableBrands.find { it.name == selectedName }
                    selectedBrandId = brand?.id
                    Log.i(TAG, "USER_ACTION: Marca seleccionada: ID $selectedBrandId")
                }

            // Sincronización Inversa (Se activa cuando LiveData entrega datos):
            if (selectedBrandId != null) {
                val currentBrandName = availableBrands.find { it.id == selectedBrandId }?.name
                binding.etBrand.setText(currentBrandName, false)
                Log.i(TAG, "SYNC_FINAL: LiveData cargó y estableció la marca inicial: $currentBrandName (ID: $selectedBrandId)")
            }
        }
    }

    // ---------------------------------------------------------------------------------
    //  GUARDAR Y ELIMINAR
    // ---------------------------------------------------------------------------------

    private fun saveProductChanges() {
        val updatedTitle = binding.etTitle.text.toString().trim()
        val updatedDescription = binding.etDescription.text.toString().trim().ifEmpty { null }
        val updatedPrice = binding.etPrice.text.toString().toFloatOrNull()
        val updatedRating = binding.etRating.text.toString().toFloatOrNull()

        val updatedSizes = binding.etSizes.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val finalImageUrls = currentImageUrls.toList()

        // LOG: Verificación de datos finales antes del UPDATE
        Log.d(TAG, "SAVE: Iniciando validación de datos.")
        Log.d(TAG, "DATA_OUT: Title='$updatedTitle', Price=$updatedPrice, Brand ID=$selectedBrandId, Sizes: ${updatedSizes.size}, URLs: ${finalImageUrls.size}")

        if (updatedTitle.isEmpty() || updatedPrice == null) {
            Log.w(TAG, "SAVE_FAIL: Validación fallida (Título/Precio).")
            Toast.makeText(requireContext(), "Título y Precio son campos obligatorios.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedBrandId == null) {
            Log.w(TAG, "SAVE_FAIL: selectedBrandId es NULL.")
            Toast.makeText(requireContext(), "Debes seleccionar una Marca.", Toast.LENGTH_SHORT).show()
            return
        }

        val updateRequest = UpdateProductRequest(
            title = updatedTitle,
            description = updatedDescription,
            price = updatedPrice,
            brand = selectedBrandId,
            rating = updatedRating,
            size = updatedSizes,
            picUrl = finalImageUrls
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "SAVE_DB: Ejecutando UPDATE en Supabase para ID $productId. Datos: $updateRequest")

                // ✅ CORRECCIÓN: Solicitamos la representación de la fila actualizada usando returning()
                val result: PostgrestResult = supabase.from("Product")
                    .update(updateRequest) {
                        filter {
                            eq(column = "id", value = productId)
                        }
                        select()
                    }

                // Verificamos las filas afectadas decodificando la respuesta JSON
                val rowsAffected = result.data?.let {
                    kotlinx.serialization.json.Json.parseToJsonElement(it).jsonArray
                }?.size ?: 0

                if (rowsAffected > 0) {
                    Log.i(TAG, "SAVE_DB: UPDATE exitoso. Filas afectadas: $rowsAffected. Datos devueltos: ${result.data}")
                    Toast.makeText(requireContext(), "Producto actualizado con éxito. ✅", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Log.w(TAG, "SAVE_DB: 0 filas afectadas. Posiblemente los datos son idénticos o RLS impide la modificación.")
                    Toast.makeText(requireContext(), "Advertencia: No se detectaron cambios.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR_DB: Fallo en UPDATE: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmAndDeleteProduct(id: Int) {
        Log.d(TAG, "DELETE: Solicitud de eliminación para ID $id.")
        deleteProduct(id)
    }

    private fun deleteProduct(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "DELETE_DB: Ejecutando DELETE en Supabase para ID $id.")
                supabase.from("Product").delete {
                    filter { eq(column = "id", value = id) }
                }

                Log.i(TAG, "DELETE_DB: Producto eliminado con éxito.")
                Toast.makeText(requireContext(), "Producto eliminado con éxito. 🗑️", Toast.LENGTH_LONG).show()
                requireActivity().finish()
            } catch (e: Exception) {
                Log.e(TAG, "ERROR_DB: Fallo en DELETE: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}