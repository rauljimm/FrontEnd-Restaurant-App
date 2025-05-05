package rjm.frontrestaurante.ui.reservas

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
import rjm.frontrestaurante.model.EstadoReserva
import rjm.frontrestaurante.model.Reserva
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReservasViewModel : ViewModel() {

    private val _reservas = MutableLiveData<List<Reserva>>()
    val reservas: LiveData<List<Reserva>> = _reservas

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)
    private val TAG = "ReservasViewModel"
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Carga todas las reservas, opcionalmente filtradas por fecha, mesa o estado
     */
    fun cargarReservas(fecha: Date? = null, mesaId: Int? = null, estado: EstadoReserva? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }
                
                // Formatear fecha si está presente
                val fechaStr = fecha?.let { dateFormat.format(it) }
                // Convertir estado a string si está presente
                val estadoStr = estado?.name?.lowercase()
                
                val response = api.getReservas("Bearer $token", fechaStr, mesaId, estadoStr)
                if (response.isSuccessful) {
                    _reservas.value = response.body() ?: emptyList()
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
     * Crea una nueva reserva
     */
    fun crearReserva(
        mesaId: Int,
        clienteNombre: String,
        clienteApellido: String,
        clienteTelefono: String,
        fecha: Date,
        hora: String,
        numPersonas: Int,
        observaciones: String = ""
    ) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                // Combinar fecha y hora en un formato ISO para enviar al backend
                val calendar = java.util.Calendar.getInstance()
                calendar.time = fecha
                
                // Extraer horas y minutos del string hora (formato: "HH:mm")
                val horaPartes = hora.split(":")
                if (horaPartes.size >= 2) {
                    try {
                        val horas = horaPartes[0].toInt()
                        val minutos = horaPartes[1].toInt()
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, horas)
                        calendar.set(java.util.Calendar.MINUTE, minutos)
                        calendar.set(java.util.Calendar.SECOND, 0)
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Error al parsear la hora: ${e.message}")
                        _error.value = "Formato de hora inválido"
                        return@launch
                    }
                }
                
                // Formatear la fecha combinada en formato ISO 8601
                val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                isoDateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val fechaHoraISO = isoDateFormat.format(calendar.time)
                
                // Crear JSON para enviar al API
                val reservaJson = JSONObject().apply {
                    put("mesa_id", mesaId)
                    put("cliente_nombre", clienteNombre)
                    put("cliente_apellido", clienteApellido)
                    put("cliente_telefono", clienteTelefono)
                    put("fecha", fechaHoraISO)
                    put("num_personas", numPersonas)
                    if (observaciones.isNotEmpty()) {
                        put("observaciones", observaciones)
                    }
                }
                
                Log.d(TAG, "Enviando reserva: ${reservaJson.toString()}")
                
                val jsonString = reservaJson.toString()
                val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
                
                // Llamar al API
                val response = api.createReserva("Bearer $token", requestBody)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Reserva creada correctamente")
                    _operationSuccess.value = true
                    // Recargar la lista de reservas sin pasar la fecha para evitar problemas con el parseo
                    cargarReservas()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al crear reserva: $errorBody")
                    _error.value = "Error al crear reserva: $errorBody"
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
     * Actualiza el estado de una reserva
     */
    fun actualizarEstadoReserva(id: Int, nuevoEstado: EstadoReserva) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                // Crear JSON para enviar al API
                val reservaJson = JSONObject().apply {
                    put("estado", nuevoEstado.name.lowercase())
                }
                
                val jsonString = reservaJson.toString()
                val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
                
                // Llamar al API
                val response = api.updateReserva("Bearer $token", id, requestBody)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Estado de reserva actualizado correctamente")
                    _operationSuccess.value = true
                    
                    // Si el nuevo estado es CLIENTE_LLEGO o CLIENTE_NO_LLEGO, eliminar la reserva automáticamente
                    if (nuevoEstado == EstadoReserva.CLIENTE_LLEGO || nuevoEstado == EstadoReserva.CLIENTE_NO_LLEGO) {
                        eliminarReserva(id)
                    } else {
                        // Recargar la lista de reservas solo si no vamos a eliminar
                        cargarReservas()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al actualizar estado de reserva: $errorBody")
                    _error.value = "Error al actualizar estado de reserva: $errorBody"
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
     * Elimina una reserva
     */
    fun eliminarReserva(id: Int) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                // Llamar al API
                val response = api.deleteReserva("Bearer $token", id)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Reserva eliminada correctamente")
                    _operationSuccess.value = true
                    // Recargar la lista de reservas
                    cargarReservas()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al eliminar reserva: $errorBody")
                    _error.value = "Error al eliminar reserva: $errorBody"
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
     * Elimina automáticamente todas las reservas con estado CLIENTE_LLEGO, CLIENTE_NO_LLEGO o COMPLETADA
     */
    fun eliminarReservasClientes() {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                val reservasActuales = _reservas.value ?: return@launch
                
                // Filtrar reservas con estados a eliminar
                val reservasAEliminar = reservasActuales.filter { 
                    it.estado == EstadoReserva.CLIENTE_LLEGO || 
                    it.estado == EstadoReserva.CLIENTE_NO_LLEGO ||
                    it.estado == EstadoReserva.COMPLETADA
                }
                
                if (reservasAEliminar.isEmpty()) {
                    Log.d(TAG, "No hay reservas para eliminar")
                    _error.value = "No hay reservas completadas para eliminar"
                    return@launch
                }
                
                Log.d(TAG, "Eliminando ${reservasAEliminar.size} reservas")
                
                // Eliminar cada reserva
                var eliminacionesExitosas = 0
                var errores = 0
                for (reserva in reservasAEliminar) {
                    try {
                        val response = api.deleteReserva("Bearer $token", reserva.id)
                        if (response.isSuccessful) {
                            eliminacionesExitosas++
                        } else {
                            errores++
                            Log.e(TAG, "Error al eliminar reserva ${reserva.id}: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        errores++
                        Log.e(TAG, "Excepción al eliminar reserva ${reserva.id}: ${e.message}")
                    }
                }
                
                if (eliminacionesExitosas > 0) {
                    Log.d(TAG, "Se eliminaron $eliminacionesExitosas reservas correctamente")
                    _operationSuccess.value = true
                    
                    // Informar al usuario del resultado
                    if (errores > 0) {
                        _error.value = "Se eliminaron $eliminacionesExitosas reservas, pero hubo $errores errores"
                    } else {
                        _error.value = "Se eliminaron $eliminacionesExitosas reservas correctamente"
                    }
                    
                    // Recargar la lista de reservas
                    cargarReservas()
                } else if (errores > 0) {
                    _error.value = "No se pudo eliminar ninguna reserva. Hubo $errores errores"
                    _operationSuccess.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar reservas automáticamente: ${e.message}")
                _error.value = "Error al eliminar reservas: ${e.message}"
                _operationSuccess.value = false
            }
        }
    }
} 