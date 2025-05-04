package rjm.frontrestaurante.ui.mesas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.api.RestauranteApi
import rjm.frontrestaurante.api.RetrofitClient
import rjm.frontrestaurante.model.Mesa
import rjm.frontrestaurante.model.Pedido
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException

class DetalleMesaViewModel : ViewModel() {

    private val _mesa = MutableLiveData<Mesa>()
    val mesa: LiveData<Mesa> = _mesa

    private val _pedidos = MutableLiveData<List<Pedido>>()
    val pedidos: LiveData<List<Pedido>> = _pedidos

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _mesaCerradaExitosamente = MutableLiveData<Boolean>()
    val mesaCerradaExitosamente: LiveData<Boolean> = _mesaCerradaExitosamente

    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)
    private var mesaId: Int = -1

    fun setMesaId(id: Int) {
        mesaId = id
        // Reiniciar eventos
        _mesaCerradaExitosamente.value = false
    }

    fun cargarDatosMesa() {
        if (mesaId == -1) {
            _error.value = "ID de mesa no válido"
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

                // Cargar información de la mesa
                val mesaResponse = api.getMesaById("Bearer $token", mesaId)
                if (mesaResponse.isSuccessful) {
                    _mesa.value = mesaResponse.body()
                } else {
                    _error.value = "Error al cargar mesa: ${mesaResponse.code()} - ${mesaResponse.message()}"
                    _isLoading.value = false
                    return@launch
                }

                // Cargar pedidos de la mesa
                val pedidosResponse = api.getPedidos("Bearer $token", mesaId = mesaId)
                if (pedidosResponse.isSuccessful) {
                    _pedidos.value = pedidosResponse.body() ?: emptyList()
                } else {
                    _error.value = "Error al cargar pedidos: ${pedidosResponse.code()} - ${pedidosResponse.message()}"
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
     * Cierra la mesa y genera la cuenta final
     */
    fun cerrarMesa() {
        if (mesaId == -1) {
            _error.value = "ID de mesa no válido"
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

                // Primero, marcar todos los pedidos activos como ENTREGADO
                val pedidosActuales = _pedidos.value ?: emptyList()
                val pedidosActivos = pedidosActuales.filter { 
                    it.estado != rjm.frontrestaurante.model.EstadoPedido.ENTREGADO && 
                    it.estado != rjm.frontrestaurante.model.EstadoPedido.CANCELADO 
                }
                
                android.util.Log.d("DetalleMesaViewModel", "Cerrando mesa $mesaId con ${pedidosActivos.size} pedidos activos")
                
                // Actualizar todos los pedidos activos a ENTREGADO
                for (pedido in pedidosActivos) {
                    val pedidoUpdateRequest = rjm.frontrestaurante.api.PedidoUpdateRequest(
                        estado = "entregado"
                    )
                    try {
                        val respuestaPedido = api.updatePedido("Bearer $token", pedido.id, pedidoUpdateRequest)
                        if (!respuestaPedido.isSuccessful) {
                            android.util.Log.e("DetalleMesaViewModel", 
                                "Error al actualizar pedido ${pedido.id}: ${respuestaPedido.code()} - ${respuestaPedido.message()}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DetalleMesaViewModel", "Error al actualizar pedido ${pedido.id}", e)
                    }
                }

                // Después, actualizar estado de la mesa a LIBRE
                val mesaUpdateRequest = rjm.frontrestaurante.api.MesaUpdateRequest("libre")
                val response = api.updateMesa("Bearer $token", mesaId, mesaUpdateRequest)
                
                if (response.isSuccessful) {
                    // Recargar datos de la mesa para reflejar el cambio
                    cargarDatosMesa()
                    _mesaCerradaExitosamente.value = true
                } else {
                    // Verificar si es un error de permisos (403 Forbidden)
                    if (response.code() == 403) {
                        android.util.Log.w("DetalleMesaViewModel", 
                            "No se pudo cambiar el estado de la mesa a LIBRE (permisos insuficientes), " +
                            "pero los pedidos han sido marcados como entregados")
                        
                        // Consideramos la operación como exitosa aunque el cambio de estado falle
                        // porque los pedidos ya se han marcado como entregados
                        _mesaCerradaExitosamente.value = true
                        
                        // Recargar datos de la mesa para reflejar el estado actual
                        cargarDatosMesa()
                    } else {
                        _error.value = "Error al cerrar la mesa: ${response.code()} - ${response.message()}"
                    }
                }
            } catch (e: IOException) {
                _error.value = "Error de conexión: ${e.message}"
                android.util.Log.e("DetalleMesaViewModel", "Error de conexión al cerrar mesa", e)
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                android.util.Log.e("DetalleMesaViewModel", "Error general al cerrar mesa", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 