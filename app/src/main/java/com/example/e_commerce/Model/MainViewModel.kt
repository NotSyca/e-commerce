package com.example.e_commerce.Model

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.e_commerce.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage // Importación necesaria para instalar el módulo
import io.github.jan.supabase.storage.storage // Extensión del cliente (NO USAMOS ESTE NOMBRE DE CLASE)
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(): ViewModel() {

    // 1. Inicializa el cliente Supabase con Postgrest y Storage
    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Storage)
    }
    private val _banner = MutableLiveData<List<SliderModel>>()
    val _brand = MutableLiveData<MutableList<BrandModel>>()
    private val _popular = MutableLiveData<MutableList<Product>>()

    val brands: LiveData<MutableList<BrandModel>> = _brand
    val banners: LiveData<List<SliderModel>> = _banner
    val populars: LiveData<MutableList<Product>> = _popular

    // Nombre del bucket de Storage de Supabase
    private val STORAGE_BUCKET_NAME = "banners"
    private val PRODUCTS_STORAGE_BUCKET = "Products"
    private val CATEGORIES_TABLE_NAME = "categories"
    private val PRODUCTS_TABLE_NAME = "Product"

    suspend fun addProduct(
        context: Context, // Contexto necesario para leer la Uri
        title: String,
        description: String?,
        brand: Int,
        price: Double,
        rating: Double,
        size: List<String>,
        imageUris: List<Uri>
    ): Boolean {

        // 1. Subida de Archivos y Generación de URLs (Tu código)
        val uploadJobs = imageUris.map { uri ->
            // Ejecutar la subida en paralelo
            viewModelScope.async {
                try {
                    // Generar un nombre único para el archivo
                    val fileName = "prod_${System.currentTimeMillis()}_${uri.lastPathSegment ?: UUID.randomUUID()}.png"
                    // Leer los bytes del archivo local usando el ContentResolver del contexto
                    val fileByteArray = context.contentResolver.openInputStream(uri)?.readBytes()

                    if (fileByteArray != null) {
                        // Subir el archivo al Storage
                        supabase.storage.from(PRODUCTS_STORAGE_BUCKET).upload(fileName, fileByteArray)

                        // Obtener la URL pública del archivo subido
                        return@async supabase.storage.from(PRODUCTS_STORAGE_BUCKET).publicUrl(fileName)
                    }
                    return@async null
                } catch (e: Exception) {
                    Log.e("addProduct", "Fallo al subir imagen para URI $uri: ${e.message}", e)
                    return@async null
                }
            }
        }

        // 2. Esperar Resultados y Filtrar (CRÍTICO)
        // Usamos awaitAll() para esperar que todas las corrutinas de subida terminen.
        val picUrls = uploadJobs.awaitAll().filterNotNull()

        if (picUrls.isEmpty()) {
            Log.e("addProduct", "No se pudo subir ninguna imagen. Cancelando inserción en DB.")
            return false
        }
        val request = description?.let {
            CreateProductRequest(
                title = title,
                description = it,
                picUrl = picUrls,
                brand = brand,
                price = price,
                rating = rating,
                size = size
            )
        }

        return try {
            supabase.from(PRODUCTS_TABLE_NAME)
                .insert(listOf(request))

            Log.i("addProduct", "Producto '$title' insertado con éxito.")
            true
        } catch (e: Exception) {
            Log.e("addProduct", "Fallo al insertar producto en DB: ${e.message}", e)
            // Opcional: Implementar lógica para eliminar los archivos subidos del Storage si la inserción en DB falla
            false
        }
    }

    fun loadProductsByBrandId(brandId: Int) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Iniciando filtro por Brand ID: $brandId")

                // Usamos la tabla 'Product' y filtramos por la columna 'brand'
                val filteredList = supabase.from(PRODUCTS_TABLE_NAME)
                    .select() {
                        // CRÍTICO: Filtrar la columna 'brand' por el ID recibido
                        filter {
                            eq("brand", brandId)
                        }
                        order("rating", Order.DESCENDING)
                    }
                    .decodeList<Product>()

                // Actualizamos el LiveData de productos populares con los resultados filtrados
                _popular.postValue(filteredList.toMutableList())
                Log.d("MainViewModel", "Productos filtrados cargados: ${filteredList.size}")

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error al filtrar productos: ${e.message}", e)
                _popular.postValue(mutableListOf())
            }
        }
    }
    fun loadBanners() {
        viewModelScope.launch {
            try {
                val bucket = supabase.storage.from(STORAGE_BUCKET_NAME)
                val fileObjects = bucket.list()
                val sliderItems = fileObjects.map { file ->
                    val publicUrl = bucket.publicUrl(file.name)
                    SliderModel(url = publicUrl)
                }
                _banner.postValue(sliderItems)
                Log.d("EditProductFragment", "Banners cargados: ${sliderItems.size}")

            } catch (e: Exception) {
                Log.e("EditProductFragment", "Error al cargar banners desde Storage: ${e.message}", e)
                _banner.postValue(emptyList())
            }
        }
    }

    fun loadBrands() {
        viewModelScope.launch {
            try {
                val brandsList = supabase.from(CATEGORIES_TABLE_NAME)
                    .select()
                    .decodeList<BrandModel>()
                _brand.postValue(brandsList.toMutableList())
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error al cargar marcas desde PostgREST: ${e.message}", e)
                _brand.postValue(mutableListOf())
            }
        }
    }

    fun loadPopular() {
        // Usa viewModelScope para iniciar la operación de red de forma asíncrona
        viewModelScope.launch {
            try {
                val popularProducts = supabase.from(PRODUCTS_TABLE_NAME)
                    .select() {
                        order("rating", Order.DESCENDING)
                        limit(10) // Limitar a los más populares
                    }
                    .decodeList<Product>()

                _popular.postValue(popularProducts.toMutableList())
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error al cargar productos populares: ${e.message}", e)
                _popular.postValue(mutableListOf())
            }
        }
    }
}

