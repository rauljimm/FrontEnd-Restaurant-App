package rjm.frontrestaurante.ui.cuentas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.api.RestauranteApi
import rjm.frontrestaurante.api.RetrofitClient
import rjm.frontrestaurante.model.Cuenta
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CuentasViewModel : ViewModel() {

    private val _cuentas = MutableLiveData<List<Cuenta>>()
    val cuentas: LiveData<List<Cuenta>> = _cuentas

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)

    // Filtros
    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null
    private var mesaId: Int? = null

    init {
        cargarCuentas()
    }

    fun cargarCuentas() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No se ha iniciado sesión"
                    _isLoading.value = false
                    return@launch
                }

                // Formatear fechas para la API
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val fechaInicioStr = fechaInicio?.let { dateFormat.format(it) }
                val fechaFinStr = fechaFin?.let { dateFormat.format(it) }

                val response = api.getCuentas(
                    "Bearer $token",
                    fechaInicio = fechaInicioStr,
                    fechaFin = fechaFinStr,
                    mesaId = mesaId
                )

                if (response.isSuccessful) {
                    _cuentas.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Error al cargar cuentas: ${response.code()} - ${response.message()}"
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

    fun setFiltros(fechaInicio: Date? = null, fechaFin: Date? = null, mesaId: Int? = null) {
        this.fechaInicio = fechaInicio
        this.fechaFin = fechaFin
        this.mesaId = mesaId
        cargarCuentas()
    }

    fun limpiarFiltros() {
        fechaInicio = null
        fechaFin = null
        mesaId = null
        cargarCuentas()
    }
} 