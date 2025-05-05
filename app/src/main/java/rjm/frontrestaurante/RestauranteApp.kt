package rjm.frontrestaurante

import android.app.Application
import androidx.room.Room
import rjm.frontrestaurante.api.RestauranteApi
import rjm.frontrestaurante.api.RetrofitClient
import rjm.frontrestaurante.util.AppPreferences
import rjm.frontrestaurante.util.db.AppDatabase

/**
 * Clase principal de la aplicación para inicializar componentes globales
 */
class RestauranteApp : Application() {

    // Instancias de servicios globales
    lateinit var apiService: RestauranteApi
    lateinit var database: AppDatabase
    lateinit var preferences: AppPreferences
    
    /**
     * Método explícito para obtener el servicio API
     * Este método es usado por todos los componentes que necesitan acceder a la API
     */
    fun obtenerServicioAPI(): RestauranteApi {
        // Asegurar que la instancia existe antes de devolverla
        if (!::apiService.isInitialized) {
            apiService = RetrofitClient.getClient().create(RestauranteApi::class.java)
        }
        return apiService
    }

    override fun onCreate() {
        super.onCreate()
        // Inicializar instancia
        instance = this
        
        // Inicializar API
        apiService = RetrofitClient.getClient().create(RestauranteApi::class.java)
        
        // Inicializar base de datos local (solo para usuarios)
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "restaurante_db"
        )
        .fallbackToDestructiveMigration() // Destruir y recrear la base de datos si hay cambios de esquema
        .build()
        
        // Inicializar preferencias
        preferences = AppPreferences(applicationContext)
    }

    companion object {
        private lateinit var instance: RestauranteApp
        
        fun getInstance(): RestauranteApp {
            return instance
        }
    }
} 