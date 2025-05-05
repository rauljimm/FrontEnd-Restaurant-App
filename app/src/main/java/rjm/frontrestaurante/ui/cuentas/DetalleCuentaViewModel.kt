package rjm.frontrestaurante.ui.cuentas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.api.CuentaUpdateRequest
import rjm.frontrestaurante.api.RestauranteApi
import rjm.frontrestaurante.api.RetrofitClient
import rjm.frontrestaurante.model.Cuenta
import rjm.frontrestaurante.util.SessionManager
import java.io.IOException

class DetalleCuentaViewModel : ViewModel() {

    private val _cuenta = MutableLiveData<Cuenta>()
    val cuenta: LiveData<Cuenta> = _cuenta

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    private val api = RetrofitClient.getClient().create(RestauranteApi::class.java)
    private var cuentaId: Int = -1

    fun setCuentaId(id: Int) {
        cuentaId = id
        cargarCuenta()
    }

    fun cargarCuenta() {
        if (cuentaId <= 0) {
            _error.value = "ID de cuenta inválido"
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

                val response = api.getCuentaById("Bearer $token", cuentaId)
                if (response.isSuccessful) {
                    _cuenta.value = response.body()
                } else {
                    _error.value = "Error al cargar cuenta: ${response.code()} - ${response.message()}"
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

    fun actualizarMetodoPago(metodoPago: String) {
        if (cuentaId <= 0) {
            _error.value = "ID de cuenta inválido"
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

                val request = CuentaUpdateRequest(metodoPago = metodoPago)
                val response = api.updateCuenta("Bearer $token", cuentaId, request)
                
                if (response.isSuccessful) {
                    _cuenta.value = response.body()
                    _updateSuccess.value = true
                } else {
                    _error.value = "Error al actualizar cuenta: ${response.code()} - ${response.message()}"
                    _updateSuccess.value = false
                }
            } catch (e: IOException) {
                _error.value = "Error de conexión: ${e.message}"
                _updateSuccess.value = false
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                _updateSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}