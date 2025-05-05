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
import rjm.frontrestaurante.model.Reserva
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class DetalleMesaViewModel : ViewModel() {

    private val _mesa = MutableLiveData<Mesa>()
    val mesa: LiveData<Mesa> = _mesa

    private val _pedidos = MutableLiveData<List<Pedido>>()
    val pedidos: LiveData<List<Pedido>> = _pedidos

    private val _reservaActiva = MutableLiveData<Reserva?>()
    val reservaActiva: LiveData<Reserva?> = _reservaActiva

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _mesaCerradaExitosamente = MutableLiveData<Boolean>()
    val mesaCerradaExitosamente: LiveData<Boolean> = _mesaCerradaExitosamente

    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)
    private var mesaId: Int = -1
    private var fechaInicioSesion: String? = null

    /**
     * Inicializa el ViewModel con el ID de la mesa
     */
    fun setMesaId(id: Int) {
        mesaId = id
        loadData()
        // Reiniciar eventos
        _mesaCerradaExitosamente.value = false
        
        // Guardar la fecha actual como inicio de sesión si es la primera vez que se carga
        if (fechaInicioSesion == null) {
            val formato = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            formato.timeZone = java.util.TimeZone.getTimeZone("UTC")
            fechaInicioSesion = formato.format(java.util.Date())
            android.util.Log.d("DetalleMesaViewModel", "Fecha inicio sesión: $fechaInicioSesion")
        }
    }

    /**
     * Actualiza los datos cuando el usuario hace pull-to-refresh
     */
    fun refreshData() {
        loadData()
    }

    /**
     * Carga los detalles de la mesa y sus pedidos
     */
    fun loadData() {
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

                // Cargar detalles de la mesa
                val mesaResponse = api.getMesaById("Bearer $token", mesaId)
                if (mesaResponse.isSuccessful) {
                    _mesa.value = mesaResponse.body()
                    
                    // Si la mesa está reservada, cargar la reserva activa
                    if (_mesa.value?.estado == rjm.frontrestaurante.model.EstadoMesa.RESERVADA) {
                        cargarReservaActiva()
                    } else {
                        _reservaActiva.value = null
                    }
                } else {
                    _error.value = "Error al cargar detalles de la mesa: ${mesaResponse.code()} - ${mesaResponse.message()}"
                }

                // Cargar pedidos de la mesa
                val pedidosResponse = api.getPedidos(
                    "Bearer $token", 
                    mesaId = mesaId,
                    activos = false, // Cargar todos los pedidos de la mesa, incluyendo los entregados
                    fechaInicio = fechaInicioSesion // Solo los pedidos desde el inicio de la sesión
                )
                if (pedidosResponse.isSuccessful) {
                    val pedidos = pedidosResponse.body() ?: emptyList()
                    
                    // Si no se encontraron pedidos con el filtro de fecha, intentar cargar todos
                    if (pedidos.isEmpty() && fechaInicioSesion != null) {
                        android.util.Log.d("DetalleMesaViewModel", "No se encontraron pedidos con filtro de fecha. Intentando sin filtro...")
                        val allPedidosResponse = api.getPedidos(
                            "Bearer $token", 
                            mesaId = mesaId,
                            activos = false,
                            fechaInicio = null
                        )
                        if (allPedidosResponse.isSuccessful) {
                            _pedidos.value = allPedidosResponse.body() ?: emptyList()
                        } else {
                            _pedidos.value = emptyList()
                        }
                    } else {
                        _pedidos.value = pedidos
                    }
                    android.util.Log.d("DetalleMesaViewModel", "Pedidos cargados: ${_pedidos.value?.size ?: 0}")
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
    fun cerrarMesa(metodoPago: String = "efectivo") {
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
                val mesaUpdateRequest = rjm.frontrestaurante.api.MesaUpdateRequest(
                    estado = "libre",
                    metodoPago = metodoPago
                )
                val response = api.updateMesa("Bearer $token", mesaId, mesaUpdateRequest)
                
                if (response.isSuccessful) {
                    // Recargar datos de la mesa para reflejar el cambio
                    loadData()
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
                        loadData()
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

    private fun cargarReservaActiva() {
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

                // Cargar la reserva activa de la mesa
                val reservaResponse = api.getReservaActiva("Bearer $token", mesaId)
                if (reservaResponse.isSuccessful) {
                    _reservaActiva.value = reservaResponse.body()
                } else if (reservaResponse.code() == 404) {
                    // Este es un caso normal cuando no hay reservas activas
                    _reservaActiva.value = null
                    android.util.Log.d("DetalleMesaViewModel", "No hay reservas activas para esta mesa")
                } else {
                    _error.value = "Error al cargar la reserva activa: ${reservaResponse.code()} - ${reservaResponse.message()}"
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
     * Actualiza el estado de la reserva cuando el cliente llega
     */
    fun confirmarLlegadaCliente() {
        val reserva = _reservaActiva.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }
                
                // Crear JSON para la actualización
                val requestBody = RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    """{"estado": "cliente_llego"}"""
                )
                
                // Actualizar estado de la reserva
                val response = api.updateReserva("Bearer $token", reserva.id, requestBody)
                
                if (response.isSuccessful) {
                    android.util.Log.d("DetalleMesaViewModel", "Llegada de cliente confirmada correctamente")
                    // Recargar datos de la mesa
                    loadData()
                } else {
                    android.util.Log.e("DetalleMesaViewModel", "Error al confirmar llegada: ${response.code()} - ${response.message()}")
                    _error.value = "Error al confirmar llegada: ${response.code()} - ${response.message()}"
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
     * Actualiza el estado de la reserva cuando el cliente no se presenta
     */
    fun confirmarNoLlegadaCliente() {
        val reserva = _reservaActiva.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }
                
                // Crear JSON para la actualización
                val requestBody = RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    """{"estado": "cliente_no_llego"}"""
                )
                
                // Actualizar estado de la reserva
                val response = api.updateReserva("Bearer $token", reserva.id, requestBody)
                
                if (response.isSuccessful) {
                    android.util.Log.d("DetalleMesaViewModel", "No llegada de cliente registrada correctamente")
                    // Recargar datos de la mesa
                    loadData()
                } else {
                    android.util.Log.e("DetalleMesaViewModel", "Error al registrar no llegada: ${response.code()} - ${response.message()}")
                    _error.value = "Error al registrar no llegada: ${response.code()} - ${response.message()}"
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
} 