package rjm.frontrestaurante.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rjm.frontrestaurante.model.Mesa
import java.util.concurrent.TimeUnit

/**
 * Cliente API para gestionar la conexión con el backend
 */
object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/" // URL para acceder al servidor local desde el emulador
    
    /**
     * Crea e inicializa el servicio API configurado con Retrofit
     */
    fun crearApiService(): ApiService {
        // Configurar interceptor para logging
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Configurar cliente HTTP
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        
        // Configurar GSON con el deserializador personalizado para Mesa
        val gson = GsonBuilder()
            .registerTypeAdapter(Mesa::class.java, MesaDeserializer())
            .create()
        
        // Configurar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        // Crear e inicializar el servicio API
        return retrofit.create(ApiService::class.java)
    }
} 