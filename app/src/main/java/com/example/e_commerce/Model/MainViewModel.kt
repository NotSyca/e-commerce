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
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel : ViewModel() {

    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Storage)
    }

    private val _banner = MutableLiveData<List<SliderModel>>()
    val banners: LiveData<List<SliderModel>> = _banner

    private val _brand = MutableLiveData<MutableList<BrandModel>>()
    val brands: LiveData<MutableList<BrandModel>> = _brand

    private val _popular = MutableLiveData<MutableList<Product>>()
    val populars: LiveData<MutableList<Product>> = _popular

    private val _searchResults = MutableLiveData<List<Product>>()
    val searchResults: LiveData<List<Product>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val STORAGE_BUCKET_NAME = "banners"
    private val PRODUCTS_STORAGE_BUCKET = "Products"
    private val CATEGORIES_TABLE_NAME = "categories"
    private val PRODUCTS_TABLE_NAME = "Product"
    private val SEARCH_HISTORY_TABLE_NAME = "search_history"

    fun addSearchToHistory(userId: String, query: String) {
        viewModelScope.launch {
            try {
                val request = SearchHistoryRequest(user_id = userId, query = query)
                supabase.from(SEARCH_HISTORY_TABLE_NAME).insert(request)
                Log.i("MainViewModel", "Search history added for user $userId with query '$query'")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to add search history: ${e.message}", e)
            }
        }
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val results = supabase.from(PRODUCTS_TABLE_NAME)
                    .select() {
                        filter {
                            ilike("title", "%${query}%")
                        }
                    }
                    .decodeList<Product>()
                _searchResults.postValue(results)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error searching products: ${e.message}", e)
                _searchResults.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    suspend fun addProduct(
        context: Context,
        title: String,
        description: String?,
        brand: Int,
        price: Double,
        rating: Double,
        size: List<String>,
        imageUris: List<Uri>
    ): Boolean {
        val uploadJobs = imageUris.map {
            viewModelScope.async {
                try {
                    val fileName = "prod_${System.currentTimeMillis()}_${it.lastPathSegment ?: UUID.randomUUID()}.png"
                    val fileByteArray = context.contentResolver.openInputStream(it)?.readBytes()
                    if (fileByteArray != null) {
                        supabase.storage.from(PRODUCTS_STORAGE_BUCKET).upload(fileName, fileByteArray)
                        return@async supabase.storage.from(PRODUCTS_STORAGE_BUCKET).publicUrl(fileName)
                    }
                    return@async null
                } catch (e: Exception) {
                    Log.e("addProduct", "Fallo al subir imagen para URI $it: ${e.message}", e)
                    return@async null
                }
            }
        }

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
            false
        }
    }

    fun loadProductsByBrandId(brandId: Int) {
        viewModelScope.launch {
            try {
                val filteredList = supabase.from(PRODUCTS_TABLE_NAME)
                    .select() {
                        filter {
                            eq("brand", brandId)
                        }
                        order("rating", Order.DESCENDING)
                    }
                    .decodeList<Product>()
                _popular.postValue(filteredList.toMutableList())
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
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error al cargar banners desde Storage: ${e.message}", e)
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
        viewModelScope.launch {
            try {
                val popularProducts = supabase.from(PRODUCTS_TABLE_NAME)
                    .select() {
                        order("rating", Order.DESCENDING)
                    }
                    .decodeList<Product>()
                
                // Línea de diagnóstico
                Log.d("MainViewModel", "Productos populares cargados desde Supabase: ${popularProducts.size}")

                _popular.postValue(popularProducts.toMutableList())
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error al cargar productos populares: ${e.message}", e)
                _popular.postValue(mutableListOf())
            }
        }
    }
}