package rjm.frontrestaurante.ui.pedidos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.api.PedidoUpdateRequest
import rjm.frontrestaurante.api.RestauranteApi
import rjm.frontrestaurante.api.RetrofitClient
import rjm.frontrestaurante.model.DetallePedido
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

    private val _pedidoEliminado = MutableLiveData<Boolean>()
    val pedidoEliminado: LiveData<Boolean> = _pedidoEliminado

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
    
    /**
     * Elimina el pedido actual (solo para admin y camareros, y siempre que no esté entregado)
     */
    fun eliminarPedido() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }
                
                // Verificar si el pedido puede ser eliminado
                val pedidoActual = _pedido.value
                if (pedidoActual != null && pedidoActual.estado == EstadoPedido.ENTREGADO) {
                    _error.value = "No se puede eliminar un pedido ya entregado"
                    _isLoading.value = false
                    return@launch
                }
                
                // Realizar la solicitud de eliminación
                android.util.Log.d("DetallePedidoViewModel", "Eliminando pedido: $pedidoId")
                val response = api.deletePedido("Bearer $token", pedidoId)
                
                if (response.isSuccessful) {
                    android.util.Log.d("DetallePedidoViewModel", "Pedido eliminado correctamente")
                    _pedidoEliminado.value = true
                } else {
                    android.util.Log.e("DetallePedidoViewModel", "Error al eliminar: ${response.code()} - ${response.message()}")
                    _error.value = "Error al eliminar el pedido: ${response.code()} - ${response.message()}"
                    
                    if (response.errorBody() != null) {
                        try {
                            val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                            android.util.Log.e("DetallePedidoViewModel", "Error body: $errorBody")
                            _error.value = errorBody
                        } catch (e: Exception) {
                            // Ignorar error al leer el cuerpo del error
                        }
                    }
                }
            } catch (e: IOException) {
                android.util.Log.e("DetallePedidoViewModel", "Error de conexión", e)
                _error.value = "Error de conexión: ${e.message}"
            } catch (e: Exception) {
                android.util.Log.e("DetallePedidoViewModel", "Error general", e)
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Actualiza la cantidad de un producto en el pedido
     */
    fun actualizarCantidadProducto(detalleId: Int, nuevaCantidad: Int) {
        if (nuevaCantidad <= 0) {
            _error.value = "La cantidad debe ser mayor a 0"
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
                
                // Crear el request para actualizar la cantidad
                val updateRequest = rjm.frontrestaurante.api.DetalleUpdateRequest(
                    cantidad = nuevaCantidad
                )
                
                android.util.Log.d("DetallePedidoViewModel", "Actualizando cantidad de detalle $detalleId a $nuevaCantidad")
                val response = api.updateDetallePedido(
                    "Bearer $token",
                    pedidoId,
                    detalleId,
                    updateRequest
                )
                
                if (response.isSuccessful) {
                    android.util.Log.d("DetallePedidoViewModel", "Cantidad actualizada correctamente")
                    // Recargar el pedido para reflejar los cambios
                    cargarPedido()
                } else {
                    android.util.Log.e("DetallePedidoViewModel", "Error al actualizar: ${response.code()} - ${response.message()}")
                    _error.value = "Error al actualizar la cantidad: ${response.code()} - ${response.message()}"
                    _isLoading.value = false
                }
                
            } catch (e: IOException) {
                android.util.Log.e("DetallePedidoViewModel", "Error de conexión al actualizar cantidad", e)
                _error.value = "Error de conexión: ${e.message}"
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("DetallePedidoViewModel", "Error al actualizar cantidad", e)
                _error.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Elimina un producto del pedido
     */
    fun eliminarProductoDePedido(detalleId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }
                
                android.util.Log.d("DetallePedidoViewModel", "Eliminando producto $detalleId del pedido $pedidoId")
                val response = api.deleteDetallePedido(
                    "Bearer $token",
                    pedidoId,
                    detalleId
                )
                
                if (response.isSuccessful) {
                    android.util.Log.d("DetallePedidoViewModel", "Producto eliminado correctamente")
                    // Recargar el pedido para reflejar los cambios
                    cargarPedido()
                } else {
                    android.util.Log.e("DetallePedidoViewModel", "Error al eliminar producto: ${response.code()} - ${response.message()}")
                    _error.value = "Error al eliminar el producto: ${response.code()} - ${response.message()}"
                    _isLoading.value = false
                }
                
            } catch (e: IOException) {
                android.util.Log.e("DetallePedidoViewModel", "Error de conexión al eliminar producto", e)
                _error.value = "Error de conexión: ${e.message}"
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("DetallePedidoViewModel", "Error al eliminar producto", e)
                _error.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Agrega un producto existente al pedido actual
     */
    fun agregarProductoAlPedido(pedidoId: Int, productoId: Int, cantidad: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }
                
                // Crear el request para agregar el producto
                val detalleRequest = rjm.frontrestaurante.api.DetallePedidoRequest(
                    productoId = productoId,
                    cantidad = cantidad,
                    observaciones = ""
                )
                
                android.util.Log.d("DetallePedidoViewModel", "Agregando producto $productoId al pedido $pedidoId (cantidad: $cantidad)")
                val response = api.addDetallePedido(
                    "Bearer $token",
                    pedidoId,
                    detalleRequest
                )
                
                if (response.isSuccessful) {
                    android.util.Log.d("DetallePedidoViewModel", "Producto agregado correctamente")
                    // Recargar el pedido para reflejar los cambios
                    cargarPedido()
                } else {
                    android.util.Log.e("DetallePedidoViewModel", "Error al agregar producto: ${response.code()} - ${response.message()}")
                    _error.value = "Error al agregar el producto: ${response.code()} - ${response.message()}"
                    _isLoading.value = false
                }
                
            } catch (e: IOException) {
                android.util.Log.e("DetallePedidoViewModel", "Error de conexión al agregar producto", e)
                _error.value = "Error de conexión: ${e.message}"
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("DetallePedidoViewModel", "Error al agregar producto", e)
                _error.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Actualiza un detalle de pedido completo
     */
    fun actualizarDetallePedido(detalle: DetallePedido) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }
                
                // Mostrar diálogo de confirmación
                // Nota: Como no podemos mostrar diálogos directamente desde el ViewModel,
                // esta lógica normalmente se implementaría en el Fragment.
                // Pero para mantener la estructura actual, procesamos directamente.
                
                // Crear el request para actualizar el detalle
                val updateRequest = rjm.frontrestaurante.api.DetalleUpdateRequest(
                    cantidad = detalle.cantidad,
                    estado = detalle.estado.name.lowercase(),
                    observaciones = detalle.observaciones
                )
                
                android.util.Log.d("DetallePedidoViewModel", "Actualizando detalle ${detalle.id} del pedido $pedidoId")
                val response = api.updateDetallePedido(
                    "Bearer $token",
                    pedidoId,
                    detalle.id,
                    updateRequest
                )
                
                if (response.isSuccessful) {
                    android.util.Log.d("DetallePedidoViewModel", "Detalle actualizado correctamente")
                    // Recargar el pedido para reflejar los cambios
                    cargarPedido()
                } else {
                    android.util.Log.e("DetallePedidoViewModel", "Error al actualizar detalle: ${response.code()} - ${response.message()}")
                    _error.value = "Error al actualizar el detalle: ${response.code()} - ${response.message()}"
                    _isLoading.value = false
                }
                
            } catch (e: IOException) {
                android.util.Log.e("DetallePedidoViewModel", "Error de conexión al actualizar detalle", e)
                _error.value = "Error de conexión: ${e.message}"
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("DetallePedidoViewModel", "Error al actualizar detalle", e)
                _error.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
} 