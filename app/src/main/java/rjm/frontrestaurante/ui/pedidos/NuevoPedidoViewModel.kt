package rjm.frontrestaurante.ui.pedidos

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import rjm.frontrestaurante.api.RestauranteApi
import rjm.frontrestaurante.api.RetrofitClient
import rjm.frontrestaurante.model.Producto
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * ViewModel para el fragmento de nuevo pedido
 */
class NuevoPedidoViewModel : ViewModel() {
    
    private val _mesaId = MutableLiveData<Int>()
    
    private val _productos = MutableLiveData<List<Producto>>()
    val productos: LiveData<List<Producto>> = _productos
    
    private val _productosSeleccionados = MutableLiveData<Map<Producto, Int>>(emptyMap())
    val productosSeleccionados: LiveData<Map<Producto, Int>> = _productosSeleccionados
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _pedidoCreado = MutableLiveData<Boolean>()
    val pedidoCreado: LiveData<Boolean> = _pedidoCreado
    
    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)
    private val TAG = "NuevoPedidoViewModel"
    
    /**
     * Establece el ID de la mesa para el nuevo pedido
     */
    fun setMesaId(mesaId: Int) {
        _mesaId.value = mesaId
    }
    
    /**
     * Carga la lista de productos disponibles
     */
    fun cargarProductos() {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                // Solo obtener productos disponibles
                val response = api.getProductos("Bearer $token", disponible = true)
                
                if (response.isSuccessful) {
                    _productos.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Error al cargar productos: ${response.code()} - ${response.message()}"
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error de conexión: ${e.message}")
                _error.value = "Error de conexión. Compruebe su conexión a internet."
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado: ${e.message}")
                _error.value = "Error inesperado: ${e.message}"
            }
        }
    }
    
    /**
     * Agrega un producto al pedido con la cantidad especificada
     */
    fun agregarProductoAlPedido(producto: Producto, cantidad: Int) {
        val currentMap = _productosSeleccionados.value?.toMutableMap() ?: mutableMapOf()
        
        // Si el producto ya está en el pedido, actualizar cantidad
        val cantidadActual = currentMap[producto] ?: 0
        currentMap[producto] = cantidadActual + cantidad
        
        _productosSeleccionados.value = currentMap
    }
    
    /**
     * Crea un nuevo pedido con los productos seleccionados
     */
    fun crearPedido(observaciones: String) {
        val mesaId = _mesaId.value
        if (mesaId == null) {
            _error.value = "No se ha especificado la mesa"
            return
        }
        
        val productosSeleccionados = _productosSeleccionados.value
        if (productosSeleccionados.isNullOrEmpty()) {
            _error.value = "No hay productos seleccionados"
            return
        }
        
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                // Crear JSON para el pedido
                val jsonObject = JSONObject()
                jsonObject.put("mesa_id", mesaId)
                
                if (observaciones.isNotEmpty()) {
                    jsonObject.put("observaciones", observaciones)
                }
                
                // Crear array de detalles
                val detallesArray = JSONArray()
                
                productosSeleccionados.forEach { (producto, cantidad) ->
                    val detalleJson = JSONObject()
                    detalleJson.put("producto_id", producto.id)
                    detalleJson.put("cantidad", cantidad)
                    detallesArray.put(detalleJson)
                }
                
                jsonObject.put("detalles", detallesArray)
                
                Log.d(TAG, "Enviando pedido: $jsonObject")
                
                // Crear request body
                val requestBody = jsonObject.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())
                
                // Enviar pedido
                val response = api.createPedido("Bearer $token", requestBody)
                
                if (response.isSuccessful) {
                    _pedidoCreado.value = true
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al crear pedido: $errorBody")
                    _error.value = "Error al crear pedido: $errorBody"
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error de conexión: ${e.message}")
                _error.value = "Error de conexión. Compruebe su conexión a internet."
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado: ${e.message}")
                _error.value = "Error inesperado: ${e.message}"
            }
        }
    }
} 