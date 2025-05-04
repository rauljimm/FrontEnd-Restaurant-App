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
                
                // Formatear fecha
                val fechaStr = dateFormat.format(fecha)
                
                // Crear JSON para enviar al API
                val reservaJson = JSONObject().apply {
                    put("mesa_id", mesaId)
                    put("cliente_nombre", clienteNombre)
                    put("cliente_telefono", clienteTelefono)
                    put("fecha", fechaStr)
                    put("hora", hora)
                    put("num_personas", numPersonas)
                    if (observaciones.isNotEmpty()) {
                        put("observaciones", observaciones)
                    }
                    put("estado", EstadoReserva.PENDIENTE.name.lowercase())
                }
                
                Log.d(TAG, "Enviando reserva: ${reservaJson.toString()}")
                
                val jsonString = reservaJson.toString()
                val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
                
                // Llamar al API
                val response = api.createReserva("Bearer $token", requestBody)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Reserva creada correctamente")
                    _operationSuccess.value = true
                    // Recargar la lista de reservas
                    cargarReservas(fecha)
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
                    // Recargar la lista de reservas
                    cargarReservas()
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
} 