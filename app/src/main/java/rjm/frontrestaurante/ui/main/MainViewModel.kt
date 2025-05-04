package rjm.frontrestaurante.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.RestauranteApp
import rjm.frontrestaurante.model.Producto

/**
 * ViewModel para la pantalla principal
 */
class MainViewModel : ViewModel() {

    private val _productos = MutableLiveData<List<Producto>>()
    val productos: LiveData<List<Producto>> = _productos

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val apiService = RestauranteApp.getInstance().apiService
    private val database = RestauranteApp.getInstance().database
    private val preferences = RestauranteApp.getInstance().preferences

    /**
     * Carga la lista de productos desde el API
     */
    fun cargarProductos() {
        viewModelScope.launch {
            try {
                // Cargar directamente desde la API
                val token = "Bearer ${preferences.getAuthToken()}"
                val response = apiService.getProductos(token)
                
                if (response.isSuccessful && response.body() != null) {
                    val productosApi = response.body()!!
                    _productos.value = productosApi
                } else {
                    _error.value = "Error al cargar productos: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }

    /**
     * Cierra la sesión del usuario
     */
    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                // Solo limpiar datos de usuario y preferencias
                database.usuarioDao().deleteAllUsuarios()
                
                // Limpiar preferencias
                preferences.clearPreferences()
                
                // Limpiar SessionManager
                rjm.frontrestaurante.util.SessionManager.logout()
            } catch (e: Exception) {
                _error.value = "Error al cerrar sesión: ${e.message}"
            }
        }
    }
} 