package rjm.frontrestaurante.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.RestauranteApp
import rjm.frontrestaurante.model.Categoria
import rjm.frontrestaurante.model.Producto
import rjm.frontrestaurante.util.SessionManager

/**
 * ViewModel para la pantalla principal
 */
class MainViewModel : ViewModel() {

    private val _productos = MutableLiveData<List<Producto>>()
    val productos: LiveData<List<Producto>> = _productos

    private val _categorias = MutableLiveData<List<Categoria>>()
    val categorias: LiveData<List<Categoria>> = _categorias

    private val _productosPorCategoria = MutableLiveData<Map<Categoria, List<Producto>>>()
    val productosPorCategoria: LiveData<Map<Categoria, List<Producto>>> = _productosPorCategoria

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
                // Obtener token de autenticación
                val token = "Bearer ${preferences.getAuthToken()}"
                
                // Cargar todos los productos
                val productosResponse = apiService.getProductos(token)
                if (productosResponse.isSuccessful && productosResponse.body() != null) {
                    val productosList = productosResponse.body()!!
                    // Filtrar solo productos disponibles
                    val productosDisponibles = productosList.filter { it.disponible }
                    _productos.value = productosDisponibles
                    Log.d("MainViewModel", "Productos cargados: ${productosDisponibles.size}")
                } else {
                    _error.value = "Error al cargar productos: ${productosResponse.message()}"
                    Log.e("MainViewModel", "Error al cargar productos: ${productosResponse.code()} - ${productosResponse.message()}")
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
                Log.e("MainViewModel", "Error de conexión", e)
            }
        }
    }

    /**
     * Carga productos agrupados por categoría (método original)
     */
    fun cargarProductosPorCategoria() {
        viewModelScope.launch {
            try {
                // Obtener token de autenticación
                val token = "Bearer ${preferences.getAuthToken()}"
                
                // Primero cargar las categorías
                val categoriasResponse = apiService.getCategorias(token)
                if (categoriasResponse.isSuccessful && categoriasResponse.body() != null) {
                    val categoriasList = categoriasResponse.body()!!
                    _categorias.value = categoriasList
                    Log.d("MainViewModel", "Categorías cargadas: ${categoriasList.size}")
                    
                    // Luego cargar todos los productos
                    val productosResponse = apiService.getProductos(token)
                    if (productosResponse.isSuccessful && productosResponse.body() != null) {
                        val productosList = productosResponse.body()!!
                        _productos.value = productosList
                        Log.d("MainViewModel", "Productos cargados: ${productosList.size}")
                        
                        // Agrupar productos por categoría
                        val productosAgrupados = mutableMapOf<Categoria, List<Producto>>()
                        
                        for (categoria in categoriasList) {
                            val productosDeCategoria = productosList.filter { 
                                it.categoriaId == categoria.id && it.disponible
                            }
                            Log.d("MainViewModel", "Categoría ${categoria.nombre} (${categoria.id}): ${productosDeCategoria.size} productos")
                            if (productosDeCategoria.isNotEmpty()) {
                                productosAgrupados[categoria] = productosDeCategoria
                            }
                        }
                        
                        Log.d("MainViewModel", "Total categorías con productos: ${productosAgrupados.size}")
                        _productosPorCategoria.value = productosAgrupados
                    } else {
                        _error.value = "Error al cargar productos: ${productosResponse.message()}"
                        Log.e("MainViewModel", "Error al cargar productos: ${productosResponse.code()} - ${productosResponse.message()}")
                    }
                } else {
                    _error.value = "Error al cargar categorías: ${categoriasResponse.message()}"
                    Log.e("MainViewModel", "Error al cargar categorías: ${categoriasResponse.code()} - ${categoriasResponse.message()}")
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
                Log.e("MainViewModel", "Error de conexión", e)
            }
        }
    }

    /**
     * Cierra la sesión del usuario
     */
    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Iniciando cierre de sesión...")
                
                // 1. Limpiar base de datos local primero
                try {
                    database.usuarioDao().deleteAllUsuarios()
                    Log.d("MainViewModel", "Base de datos local limpiada correctamente")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error al limpiar base de datos local", e)
                    // Continuar con el proceso aunque falle la limpieza de BD
                }
                
                // 2. Limpiar SessionManager y AppPreferences
                SessionManager.logout()
                preferences.clearPreferences()
                
                // 3. Verificar que todo se haya limpiado correctamente
                val isLoggedIn = SessionManager.isLoggedIn()
                val token = SessionManager.getToken()
                val userRole = SessionManager.getUserRole()
                Log.d("MainViewModel", "Estado después de logout: isLoggedIn=$isLoggedIn, token=${token ?: "null"}, userRole=$userRole")
                
                if (isLoggedIn || token != null || userRole.isNotEmpty()) {
                    Log.w("MainViewModel", "Sesión no cerrada completamente, intentando de nuevo")
                    // Intentar una vez más
                    SessionManager.logout()
                    preferences.clearPreferences()
                }
                
                Log.d("MainViewModel", "Cierre de sesión completado")
                
            } catch (e: Exception) {
                _error.value = "Error al cerrar sesión: ${e.message}"
                Log.e("MainViewModel", "Error al cerrar sesión", e)
            }
        }
    }
} 