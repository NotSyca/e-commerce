package com.example.e_commerce.Api // Paquete del servicio de subida de imágenes

import okhttp3.MultipartBody // Import para construir partes multipart (archivo)
import retrofit2.http.Multipart // Import para indicar que el endpoint usa multipart/form-data
import retrofit2.http.POST // Import de anotación para métodoo HTTP POST
import retrofit2.http.Part // Import para anotación de parámetro de parte

/**
 * UploadService
 * Servicio para subir imágenes a Xano usando multipart/form-data.
 */
interface UploadService { // Declaramos una interfaz de Retrofit

    @Multipart // Indicamos que la solicitud será multipart/form-data
    @POST("upload/image") // Ruta del endpoint de subida (POST /upload/image)
    suspend fun uploadImage( // Función suspend (corrutina) para subir la imagen
        // El nombre del campo "content" en createFormData es el que Xano espera.
        // Aquí el nombre del parámetro 'image' no importa.
        @Part image: MultipartBody.Part
        // ¡¡¡CAMBIO CLAVE!!!
        // La API devuelve un Array (una lista) de objetos de imagen, no un solo objeto.
        // Cambiamos el tipo de retorno de 'UploadResponse' a 'List<ProductImage>'.
    )
}
