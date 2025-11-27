package com.example.e_commerce.Api // Paquete donde vive el cliente Retrofit

import android.content.Context // Import para usar Context al construir interceptores dependientes de token
import com.example.e_commerce.Api.ApiConfig.BaseUrl
import okhttp3.OkHttpClient // Cliente HTTP subyacente usado por Retrofit
import okhttp3.logging.HttpLoggingInterceptor // Interceptor de logging para depuración
import retrofit2.Retrofit // Clase principal para construir el cliente Retrofit
import retrofit2.converter.gson.GsonConverterFactory // Convertidor JSON (Gson) para serialización/deserialización
import java.util.concurrent.TimeUnit // Utilidad para definir timeouts

/**
 * RetrofitClient
 * Centraliza la creación de instancias de Retrofit y OkHttp.
 *
 * Se ha modificado para soportar el flujo de autenticación en dos pasos:
 * 1. Un servicio de autenticación PÚBLICO (para el login).
 * 2. Un servicio de autenticación PRIVADO (para /auth/me y otras llamadas que requieran token).
 */
object RetrofitClient { // Objeto singleton que expone métodos de fábrica

    // Builder base de OkHttp configurado con logging y timeouts. No necesita cambios.
    private fun baseOkHttpBuilder(): OkHttpClient.Builder {
        val logging = HttpLoggingInterceptor().apply { // Creamos el interceptor de logging
            // Nivel BODY útil en desarrollo para ver requests y responses completas.
            level = HttpLoggingInterceptor.Level.BODY // Establecemos el nivel de detalle
        }
        return OkHttpClient.Builder() // Iniciamos el builder de OkHttp
            .addInterceptor(logging) // Añadimos el interceptor de logging
            .connectTimeout(30, TimeUnit.SECONDS) // Timeout de conexión
            .readTimeout(30, TimeUnit.SECONDS) // Timeout de lectura
            .writeTimeout(30, TimeUnit.SECONDS) // Timeout de escritura
    }

    // Cliente OkHttp PRIVADO que incluye el interceptor de autenticación.
    // Se centraliza su creación para reutilizarlo en todos los servicios que requieran token.
    private fun createAuthenticatedClient(context: Context): OkHttpClient {
        val tokenManager = TokenManager(context)
        return baseOkHttpBuilder()
            .addInterceptor(AuthInterceptor { tokenManager.getToken() })
            .build()
    }


    // Función que construye Retrofit con baseUrl y cliente. No necesita cambios.
    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder() // Iniciamos builder de Retrofit
            .baseUrl(baseUrl) // Establecemos base URL
            .client(client) // Asociamos cliente OkHttp
            .addConverterFactory(GsonConverterFactory.create()) // Añadimos convertidor Gson
            .build() // Construimos instancia Retrofit

    /**
     * ¡FUNCIÓN MODIFICADA Y UNIFICADA!
     * Fábrica para AuthService. Ahora acepta un parámetro 'requiresAuth'.
     *
     * @param context El contexto de la aplicación.
     * @param requiresAuth Si es 'true', se creará un cliente con el interceptor de token.
     *                     Si es 'false' (por defecto), se creará un cliente público sin token.
     * @return Una instancia de AuthService.
     */
    fun createAuthService(context: Context, requiresAuth: Boolean = false): AuthService {
        // Determina qué cliente OkHttp usar.
        // Si se requiere autenticación, usa el cliente autenticado.
        // De lo contrario, usa un cliente público simple construido desde el builder base.
        val client = if (requiresAuth) {
            createAuthenticatedClient(context)
        } else {
            baseOkHttpBuilder().build()
        }
        return retrofit(BaseUrl, client).create(AuthService::class.java)
    }

    // Fábrica para ProductService (con Authorization). No necesita cambios.
    fun createProductService(context: Context): ProductService {
        val tokenManager = TokenManager(context) // Acceso al TokenManager para obtener el token
        val client = baseOkHttpBuilder() // Partimos del builder base
            .addInterceptor(AuthInterceptor { tokenManager.getToken() }) // Añadimos nuestro interceptor que inserta Bearer token
            .build() // Construimos cliente OkHttp
        return retrofit(BaseUrl, client).create(ProductService::class.java) // Construimos Retrofit con base de tienda y generamos servicio
    }

    // Fábrica para UploadService (usa Authorization). No necesita cambios.
    fun createUploadService(context: Context): UploadService {
        val tokenManager = TokenManager(context) // Obtenemos el token desde TokenManager
        val client = baseOkHttpBuilder() // Builder base
            .addInterceptor(AuthInterceptor { tokenManager.getToken() }) // Interceptor de Authorization
            .build() // Construimos cliente
        return retrofit(BaseUrl, client).create(UploadService::class.java) // Reutilizamos storeBaseUrl para subida de archivos
    }
}
