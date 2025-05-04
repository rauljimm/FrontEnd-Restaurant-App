package rjm.frontrestaurante

import android.app.Application
import androidx.room.Room
import rjm.frontrestaurante.api.ApiClient
import rjm.frontrestaurante.api.ApiService
import rjm.frontrestaurante.util.AppPreferences
import rjm.frontrestaurante.util.db.AppDatabase

/**
 * Clase principal de la aplicación para inicializar componentes globales
 */
class RestauranteApp : Application() {

    // Instancias de servicios globales
    lateinit var apiService: ApiService
    lateinit var database: AppDatabase
    lateinit var preferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        // Inicializar instancia
        instance = this
        
        // Inicializar API
        apiService = ApiClient.crearApiService()
        
        // Inicializar base de datos local (solo para usuarios)
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "restaurante_db"
        ).fallbackToDestructiveMigration().build()
        
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