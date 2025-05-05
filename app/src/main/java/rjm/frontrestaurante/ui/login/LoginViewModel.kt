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

    // Obtener la instancia de la aplicación y usar el método explícito
    private val app = RestauranteApp.getInstance()
    // Usar el método explícito en lugar de la propiedad
    private val apiService = app.obtenerServicioAPI()
    private val preferences = app.preferences

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
                // Crear mapa de credenciales
                val credentials = mapOf(
                    "username" to username,
                    "password" to password
                )
                val response = apiService.login(credentials)
                
                Log.d(TAG, "Respuesta del servidor: ${response.code()} - ${response.message()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    Log.d(TAG, "Login exitoso. Token recibido: ${loginResponse.access_token.take(10)}...")
                    
                    // Guardar el token en ambas ubicaciones
                    preferences.saveAuthToken(loginResponse.access_token)
                    SessionManager.saveToken(loginResponse.access_token)
                    
                    // Después de un login exitoso, cargaremos los datos del usuario
                    // con una llamada separada a /usuarios/me
                    loadUserInfo(loginResponse.access_token)
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
                val response = apiService.getCurrentUser(authToken)
                
                Log.d(TAG, "Respuesta de getCurrentUser: ${response.code()} - ${response.message()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!
                    Log.d(TAG, "Usuario obtenido: ID=${usuario.id}, Nombre=${usuario.nombre}, Rol=${usuario.rol}")
                    
                    // Guardar información del usuario
                    preferences.saveUserId(usuario.id)
                    // Guardar el rol del usuario utilizando SessionManager
                    // Concatenar nombre y apellido ya que esta clase Usuario no tiene el método nombreCompleto()
                    val nombreCompleto = "${usuario.nombre} ${usuario.apellido}"
                    SessionManager.saveUserInfo(usuario.id, nombreCompleto, usuario.rol)
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