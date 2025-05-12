package rjm.frontrestaurante.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.RestauranteApp
import rjm.frontrestaurante.model.Producto

class DetailViewModel(private val productoId: Int) : ViewModel() {

    private val _producto = MutableLiveData<Producto>()
    val producto: LiveData<Producto> = _producto

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _productoEliminado = MutableLiveData<Boolean>()
    val productoEliminado: LiveData<Boolean> = _productoEliminado

    private val apiService = RestauranteApp.getInstance().apiService
    private val database = RestauranteApp.getInstance().database
    private val preferences = RestauranteApp.getInstance().preferences

    /**
     * Carga los detalles del producto
     */
    fun cargarProducto() {
        viewModelScope.launch {
            try {
                // Obtener token desde SessionManager
                val token = rjm.frontrestaurante.util.SessionManager.getToken()
                
                if (token.isNullOrEmpty()) {
                    _error.value = "Error de autenticación: Token no disponible"
                    return@launch
                }
                
                val authToken = "Bearer $token"
                val response = apiService.getProductoById(authToken, productoId)
                
                if (response.isSuccessful && response.body() != null) {
                    val productoApi = response.body()!!
                    _producto.value = productoApi
                } else {
                    _error.value = "Error al cargar el producto: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }
    
    /**
     * Elimina el producto actual
     */
    fun eliminarProducto() {
        viewModelScope.launch {
            try {
                // Obtener token desde SessionManager en lugar de preferences
                val token = rjm.frontrestaurante.util.SessionManager.getToken()
                
                if (token.isNullOrEmpty()) {
                    _error.value = "Error de autenticación: Token no disponible"
                    _productoEliminado.value = false
                    return@launch
                }
                
                val authToken = "Bearer $token"
                val response = apiService.deleteProducto(authToken, productoId)
                
                if (response.isSuccessful) {
                    _productoEliminado.value = true
                } else {
                    _error.value = "Error al eliminar el producto: ${response.message()}"
                    _productoEliminado.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión al eliminar: ${e.message}"
                _productoEliminado.value = false
            }
        }
    }
}

/**
 * Factory para crear el ViewModel con el ID del producto
 */
class DetailViewModelFactory(private val productoId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(productoId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 