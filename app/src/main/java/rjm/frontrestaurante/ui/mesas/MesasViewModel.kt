package rjm.frontrestaurante.ui.mesas

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import rjm.frontrestaurante.R
import rjm.frontrestaurante.api.RestauranteApi
import rjm.frontrestaurante.api.RetrofitClient
import rjm.frontrestaurante.model.Mesa
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException

class MesasViewModel : ViewModel() {

    private val _mesas = MutableLiveData<List<Mesa>>()
    val mesas: LiveData<List<Mesa>> = _mesas

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _resultado = MutableLiveData<Boolean>()
    val resultado: LiveData<Boolean> = _resultado

    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)
    private val TAG = "MesasViewModel"
    
    fun cargarMesas() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }
                
                val response = api.getMesas("Bearer $token")
                if (response.isSuccessful) {
                    _mesas.value = response.body() ?: emptyList()
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
     * Crea una nueva mesa
     */
    fun crearMesa(numero: Int, capacidad: Int, ubicacion: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    return@launch
                }
                
                // Crear JSON para enviar al API
                val mesaJson = JSONObject().apply {
                    put("numero", numero)
                    put("capacidad", capacidad)
                    put("ubicacion", ubicacion)
                }
                
                Log.d(TAG, "Enviando mesa: ${mesaJson.toString()}")
                
                val jsonString = mesaJson.toString()
                val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
                
                // Llamar al API
                val response = api.createMesa("Bearer $token", requestBody)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Mesa creada correctamente")
                    _resultado.value = true
                    // Recargar la lista de mesas
                    cargarMesas()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al crear mesa: $errorBody")
                    _error.value = "Error al crear mesa: $errorBody"
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