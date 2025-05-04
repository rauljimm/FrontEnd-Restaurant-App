package rjm.frontrestaurante.ui.pedidos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.api.PedidoUpdateRequest
import rjm.frontrestaurante.api.RestauranteApi
import rjm.frontrestaurante.api.RetrofitClient
import rjm.frontrestaurante.model.EstadoPedido
import rjm.frontrestaurante.model.Pedido
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException

class DetallePedidoViewModel : ViewModel() {

    private val _pedido = MutableLiveData<Pedido>()
    val pedido: LiveData<Pedido> = _pedido

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var pedidoId: Int = -1
    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)

    /**
     * Establece el ID del pedido a cargar
     */
    fun setPedidoId(id: Int) {
        pedidoId = id
    }

    /**
     * Carga los detalles del pedido desde la API
     */
    fun cargarPedido() {
        if (pedidoId <= 0) {
            _error.value = "ID de pedido inválido"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }

                // Agregar log para depuración
                android.util.Log.d("DetallePedidoViewModel", "Cargando pedido con ID: $pedidoId")
                
                val response = api.getPedidoById("Bearer $token", pedidoId)
                if (response.isSuccessful && response.body() != null) {
                    val pedido = response.body()!!
                    android.util.Log.d("DetallePedidoViewModel", "Pedido cargado: $pedido con ${pedido.detalles.size} detalles")
                    
                    // Verificar si hay detalles
                    if (pedido.detalles.isEmpty()) {
                        android.util.Log.d("DetallePedidoViewModel", "No hay detalles en el pedido")
                    } else {
                        for (detalle in pedido.detalles) {
                            android.util.Log.d("DetallePedidoViewModel", "Detalle: ID=${detalle.id}, ProductoID=${detalle.productoId}, Producto=${detalle.producto?.nombre ?: "null"}")
                        }
                    }
                    
                    _pedido.value = pedido
                } else {
                    _error.value = "Error: ${response.code()} - ${response.message()}"
                    android.util.Log.e("DetallePedidoViewModel", "Error al cargar pedido: ${response.code()} - ${response.message()}")
                    if (response.errorBody() != null) {
                        android.util.Log.e("DetallePedidoViewModel", "Error body: ${response.errorBody()?.string() ?: "vacío"}")
                    }
                }
            } catch (e: IOException) {
                _error.value = "Error de conexión: ${e.message}"
                android.util.Log.e("DetallePedidoViewModel", "Error de conexión", e)
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                android.util.Log.e("DetallePedidoViewModel", "Error general", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza el estado del pedido
     */
    fun actualizarEstadoPedido(nuevoEstado: EstadoPedido) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }

                val pedidoUpdateRequest = PedidoUpdateRequest(
                    estado = nuevoEstado.name.lowercase()
                )

                val response = api.updatePedido(
                    "Bearer $token",
                    pedidoId,
                    pedidoUpdateRequest
                )

                if (response.isSuccessful) {
                    // Recargar el pedido para ver los cambios
                    cargarPedido()
                } else {
                    _error.value = "Error al actualizar el pedido: ${response.code()} - ${response.message()}"
                    _isLoading.value = false
                }
            } catch (e: IOException) {
                _error.value = "Error de conexión: ${e.message}"
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
} 