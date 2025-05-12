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

class PedidosActivosViewModel : ViewModel() {

    private val _pedidos = MutableLiveData<List<Pedido>>()
    val pedidos: LiveData<List<Pedido>> = _pedidos

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)

    /**
     * Carga los pedidos con estado RECIBIDO o EN_PREPARACION
     */
    fun cargarPedidosActivos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }

                // Obtener todos los pedidos
                android.util.Log.d("PedidosActivosViewModel", "Cargando pedidos activos con token: ${token.take(10)}...")
                val response = api.getPedidos("Bearer $token")
                
                if (response.isSuccessful) {
                    // Log para depuración
                    android.util.Log.d("PedidosActivosViewModel", "Respuesta exitosa con ${response.body()?.size ?: 0} pedidos")
                    
                    // Obtener el rol del usuario para aplicar filtros según corresponda
                    val userRole = SessionManager.getUserRole()
                    android.util.Log.d("PedidosActivosViewModel", "Rol de usuario: $userRole")
                    
                    // Si no hay pedidos, mostrar log
                    if (response.body() == null || response.body()?.isEmpty() == true) {
                        android.util.Log.d("PedidosActivosViewModel", "No se encontraron pedidos")
                        _pedidos.value = emptyList()
                        _isLoading.value = false
                        return@launch
                    }
                    
                    val pedidosActivos = when (userRole) {
                        "cocinero" -> {
                            // Cocineros solo ven pedidos recibidos o en preparación
                            response.body()?.filter { 
                                it.estado == EstadoPedido.RECIBIDO || it.estado == EstadoPedido.EN_PREPARACION 
                            } ?: emptyList()
                        }
                        else -> {
                            // Camareros y administradores ven todos los pedidos activos
                            response.body()?.filter { 
                                it.estado == EstadoPedido.RECIBIDO || 
                                it.estado == EstadoPedido.EN_PREPARACION || 
                                it.estado == EstadoPedido.LISTO 
                            } ?: emptyList()
                        }
                    }
                    
                    android.util.Log.d("PedidosActivosViewModel", "Filtrando para rol $userRole: ${pedidosActivos.size} pedidos activos")
                    _pedidos.value = pedidosActivos
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    android.util.Log.e("PedidosActivosViewModel", "Error ${response.code()}: $errorBody")
                    _error.value = "Error: ${response.code()} - ${response.message()}\n$errorBody"
                }
            } catch (e: IOException) {
                android.util.Log.e("PedidosActivosViewModel", "Error de conexión: ${e.message}", e)
                _error.value = "Error de conexión: ${e.message}"
            } catch (e: Exception) {
                android.util.Log.e("PedidosActivosViewModel", "Error general: ${e.message}", e)
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza el estado de un pedido
     */
    fun actualizarEstadoPedido(pedidoId: Int, nuevoEstado: EstadoPedido) {
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
                    // Recargar la lista de pedidos
                    cargarPedidosActivos()
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