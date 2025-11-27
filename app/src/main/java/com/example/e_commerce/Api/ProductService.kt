package com.example.e_commerce.Api // Paquete del servicio de productos

import com.example.e_commerce.Model.CreateProductRequest // Import del modelo de request para crear producto
import com.example.e_commerce.Model.CreateProductResponse // Import del modelo de respuesta de creación
import com.example.e_commerce.Model.Product // Import del modelo de producto
import retrofit2.http.Body // Import de anotación para el cuerpo de la solicitud
import retrofit2.http.GET // Import de anotación para métodoo HTTP GET
import retrofit2.http.POST // Import de anotación para métodoo HTTP POST

/**
 * ProductService
 * Endpoints de productos: listar y crear.
 * Base URL usada: ApiConfig.storeBaseUrl
 * Todas las líneas comentadas para explicar cada elemento.
 */
interface ProductService { // Declaramos interfaz de Retrofit para productos
    @GET("product") // Definimos endpoint GET /products
    suspend fun getProducts(): List<Product> // Métodoo suspend que devuelve lista de productos

    @POST("product") // Definimos endpoint POST /products
    suspend fun createProduct(@Body request: CreateProductRequest): CreateProductResponse // Métodoo suspend para crear producto, con cuerpo JSON
}