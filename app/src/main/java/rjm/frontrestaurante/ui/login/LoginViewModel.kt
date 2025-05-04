package rjm.frontrestaurante.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.RestauranteApp
import rjm.frontrestaurante.model.LoginRequest
import rjm.frontrestaurante.util.SessionManager

/**
 * ViewModel para la pantalla de login
 */
class LoginViewModel : ViewModel() {
    private val TAG = "LoginViewModel"

    private val _loginState = MutableLiveData<LoginResult>()
    val loginState: LiveData<LoginResult> = _loginState

    private val apiService = RestauranteApp.getInstance().apiService
    private val preferences = RestauranteApp.getInstance().preferences

    /**
     * Realiza el inicio de sesión
     */
    fun login(username: String, password: String) {
        _loginState.value = LoginResult.Loading(true)
        Log.d(TAG, "Intentando login con username: $username")
        
        // Establecer que no necesitamos un segundo login al comenzar
        preferences.setSecondLoginNeeded(false)
        
        viewModelScope.launch {
            try {
                // Crear objeto de solicitud
                val loginRequest = LoginRequest(username, password)
                val response = apiService.login(loginRequest)
                
                Log.d(TAG, "Respuesta del servidor: ${response.code()} - ${response.message()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    Log.d(TAG, "Login exitoso. Token recibido: ${loginResponse.token.take(10)}...")
                    
                    // Guardar el token en ambas ubicaciones
                    preferences.saveAuthToken(loginResponse.token)
                    SessionManager.saveToken(loginResponse.token)
                    
                    // Después de un login exitoso, cargaremos los datos del usuario
                    // con una llamada separada a /usuarios/me
                    loadUserInfo(loginResponse.token)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalle"
                    Log.e(TAG, "Error de autenticación: ${response.code()} - $errorBody")
                    _loginState.value = LoginResult.Error("Error de autenticación: ${response.message()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción durante login", e)
                _loginState.value = LoginResult.Error("Error de conexión: ${e.message}")
                // Asegurar que no quedamos en un estado inconsistente
                preferences.setSecondLoginNeeded(false)
            } finally {
                _loginState.value = LoginResult.Loading(false)
            }
        }
    }
    
    /**
     * Carga la información del usuario usando el token
     */
    private fun loadUserInfo(token: String) {
        viewModelScope.launch {
            try {
                val authToken = "Bearer $token"
                Log.d(TAG, "Cargando información de usuario con token: Bearer ${token.take(10)}...")
                val response = apiService.getUsuarioActual(authToken)
                
                Log.d(TAG, "Respuesta de getUsuarioActual: ${response.code()} - ${response.message()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!
                    Log.d(TAG, "Usuario obtenido: ID=${usuario.id}, Nombre=${usuario.nombre}, Rol=${usuario.rol}")
                    
                    // Guardar información del usuario
                    preferences.saveUserId(usuario.id)
                    // Guardar el rol del usuario utilizando SessionManager
                    SessionManager.saveUserInfo(usuario.id, usuario.nombreCompleto(), usuario.rol)
                    // Establecer que no necesitamos un segundo intento de login
                    preferences.setSecondLoginNeeded(false)
                    
                    _loginState.value = LoginResult.Success
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalle"
                    Log.e(TAG, "Error al cargar datos del usuario: ${response.code()} - $errorBody")
                    _loginState.value = LoginResult.Error("Error al cargar datos del usuario: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción durante carga de usuario", e)
                _loginState.value = LoginResult.Error("Error de conexión: ${e.message}")
                // Asegurar que no quedamos en un estado inconsistente
                preferences.setSecondLoginNeeded(false)
            }
        }
    }
}

/**
 * Estados posibles del proceso de login
 */
sealed class LoginResult {
    object Success : LoginResult()
    data class Error(val message: String) : LoginResult()
    data class Loading(val isLoading: Boolean) : LoginResult()
} 