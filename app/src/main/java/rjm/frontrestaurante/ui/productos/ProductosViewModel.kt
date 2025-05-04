package rjm.frontrestaurante.ui.productos

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import rjm.frontrestaurante.api.ApiClient
import rjm.frontrestaurante.api.ApiService
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException

/**
 * ViewModel para gestionar operaciones relacionadas con productos
 */
class ProductosViewModel : ViewModel() {

    private val _resultado = MutableLiveData<Boolean>()
    val resultado: LiveData<Boolean> get() = _resultado

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    // Obtener instancia de ApiService
    private val apiService: ApiService = ApiClient.crearApiService()
    private val TAG = "ProductosViewModel"

    /**
     * Crea un nuevo producto
     */
    fun crearProducto(nombre: String, descripcion: String, precio: Double, tipo: String) {
        // Llamamos al método completo con valores por defecto para los parámetros adicionales
        crearProducto(nombre, descripcion, precio, tipo, 1, 15)
    }
    
    /**
     * Crea un nuevo producto con todos los parámetros requeridos por el backend
     */
    fun crearProducto(
        nombre: String, 
        descripcion: String, 
        precio: Double, 
        tipo: String,
        categoriaId: Int,
        tiempoPreparacion: Int
    ) {
        viewModelScope.launch {
            try {
                // Obtener token de sesión
                val token = "Bearer ${SessionManager.getToken()}"
                
                // Convertir el tipo a minúsculas para que coincida con lo esperado por el backend
                val tipoLowercase = tipo.lowercase()
                
                // Crear JSON para enviar al API según el esquema ProductoCreate del backend
                val productoJson = JSONObject().apply {
                    put("nombre", nombre)
                    put("descripcion", descripcion)
                    put("precio", precio)
                    put("tipo", tipoLowercase)
                    put("categoria_id", categoriaId)
                    put("tiempo_preparacion", tiempoPreparacion)
                    put("disponible", true)
                    put("imagen_url", null) // Opcional
                }
                
                Log.d(TAG, "Enviando producto: ${productoJson.toString()}")
                
                val jsonString = productoJson.toString()
                val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
                
                // Llamar al API
                val response = apiService.crearProducto(token, requestBody)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Producto creado correctamente")
                    _resultado.value = true
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al crear producto: $errorBody")
                    _error.value = "Error al crear producto: $errorBody"
                    _resultado.value = false
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error de conexión: ${e.message}")
                _error.value = "Error de conexión. Compruebe su conexión a internet."
                _resultado.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado: ${e.message}")
                _error.value = "Error inesperado: ${e.message}"
                _resultado.value = false
            }
        }
    }
} 