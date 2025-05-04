package rjm.frontrestaurante.ui.categorias

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import rjm.frontrestaurante.api.RestauranteApi
import rjm.frontrestaurante.api.RetrofitClient
import rjm.frontrestaurante.model.Categoria
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException

class CategoriasViewModel : ViewModel() {

    private val _categorias = MutableLiveData<List<Categoria>>()
    val categorias: LiveData<List<Categoria>> = _categorias

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)
    private val TAG = "CategoriasViewModel"
    
    /**
     * Carga todas las categorías
     */
    fun cargarCategorias() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }
                
                val response = api.getCategorias("Bearer $token")
                if (response.isSuccessful) {
                    _categorias.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: IOException) {
                _error.value = "Error de conexión: ${e.message}"
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Crea una nueva categoría
     */
    fun crearCategoria(nombre: String, descripcion: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                // Crear JSON para enviar al API
                val categoriaJson = JSONObject().apply {
                    put("nombre", nombre)
                    put("descripcion", descripcion)
                }
                
                Log.d(TAG, "Enviando categoría: ${categoriaJson.toString()}")
                
                val jsonString = categoriaJson.toString()
                val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
                
                // Llamar al API
                val response = api.createCategoria("Bearer $token", requestBody)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Categoría creada correctamente")
                    _operationSuccess.value = true
                    // Recargar la lista de categorías
                    cargarCategorias()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al crear categoría: $errorBody")
                    _error.value = "Error al crear categoría: $errorBody"
                    _operationSuccess.value = false
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error de conexión: ${e.message}")
                _error.value = "Error de conexión. Compruebe su conexión a internet."
                _operationSuccess.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado: ${e.message}")
                _error.value = "Error inesperado: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }
    
    /**
     * Actualiza una categoría existente
     */
    fun actualizarCategoria(id: Int, nombre: String, descripcion: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                // Crear JSON para enviar al API
                val categoriaJson = JSONObject().apply {
                    put("nombre", nombre)
                    put("descripcion", descripcion)
                }
                
                val jsonString = categoriaJson.toString()
                val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
                
                // Llamar al API
                val response = api.updateCategoria("Bearer $token", id, requestBody)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Categoría actualizada correctamente")
                    _operationSuccess.value = true
                    // Recargar la lista de categorías
                    cargarCategorias()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al actualizar categoría: $errorBody")
                    _error.value = "Error al actualizar categoría: $errorBody"
                    _operationSuccess.value = false
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error de conexión: ${e.message}")
                _error.value = "Error de conexión. Compruebe su conexión a internet."
                _operationSuccess.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado: ${e.message}")
                _error.value = "Error inesperado: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }
    
    /**
     * Elimina una categoría
     */
    fun eliminarCategoria(id: Int) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                // Llamar al API
                val response = api.deleteCategoria("Bearer $token", id)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Categoría eliminada correctamente")
                    _operationSuccess.value = true
                    // Recargar la lista de categorías
                    cargarCategorias()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al eliminar categoría: $errorBody")
                    _error.value = "Error al eliminar categoría: $errorBody"
                    _operationSuccess.value = false
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error de conexión: ${e.message}")
                _error.value = "Error de conexión. Compruebe su conexión a internet."
                _operationSuccess.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado: ${e.message}")
                _error.value = "Error inesperado: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }
} 