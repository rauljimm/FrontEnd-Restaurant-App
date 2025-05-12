package rjm.frontrestaurante.ui.usuarios

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rjm.frontrestaurante.RestauranteApp
import rjm.frontrestaurante.api.Usuario
import rjm.frontrestaurante.api.UsuarioRequest
import rjm.frontrestaurante.api.UsuarioUpdateRequest
import rjm.frontrestaurante.util.SessionManager

/**
 * ViewModel para el fragmento de gestión de usuarios
 */
class UsuariosViewModel : ViewModel() {
    private val TAG = "UsuariosViewModel"
    
    private val _usuarios = MutableLiveData<List<Usuario>>()
    val usuarios: LiveData<List<Usuario>> = _usuarios
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _usuarioCreado = MutableLiveData<Boolean>()
    val usuarioCreado: LiveData<Boolean> = _usuarioCreado
    
    private val _usuarioActualizado = MutableLiveData<Boolean>()
    val usuarioActualizado: LiveData<Boolean> = _usuarioActualizado
    
    private val _usuarioEliminado = MutableLiveData<Boolean>()
    val usuarioEliminado: LiveData<Boolean> = _usuarioEliminado
    
    /**
     * Carga los usuarios desde la API
     */
    fun cargarUsuarios() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = ""
            
            try {
                val token = "Bearer ${SessionManager.getToken()}"
                val response = RestauranteApp.getInstance().obtenerServicioAPI().getUsuarios(token)
                
                if (response.isSuccessful) {
                    _usuarios.value = response.body() ?: emptyList()
                    Log.d(TAG, "Usuarios cargados correctamente: ${_usuarios.value?.size}")
                } else {
                    _error.value = "Error al cargar usuarios: ${response.code()}"
                    Log.e(TAG, "Error al cargar usuarios: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                Log.e(TAG, "Error al cargar usuarios", e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Validar email con formato básico
     */
    private fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val emailRegex = Regex("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return emailRegex.matches(email)
    }
    
    /**
     * Crea un nuevo usuario
     */
    fun crearUsuario(username: String, password: String, email: String, nombre: String, apellido: String, rol: String) {
        // Validar email
        if (!isValidEmail(email)) {
            _error.value = "El formato del email no es válido"
            return
        }
        
        viewModelScope.launch {
            _loading.value = true
            _error.value = ""
            _usuarioCreado.value = false
            
            try {
                val token = "Bearer ${SessionManager.getToken()}"
                val usuarioRequest = UsuarioRequest(
                    username = username,
                    password = password,
                    email = email,
                    nombre = nombre,
                    apellido = apellido,
                    rol = rol
                )
                
                val response = RestauranteApp.getInstance().obtenerServicioAPI().createUsuario(token, usuarioRequest)
                
                if (response.isSuccessful) {
                    _usuarioCreado.value = true
                    cargarUsuarios() // Recargar lista de usuarios
                    Log.d(TAG, "Usuario creado correctamente")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalle"
                    _error.value = "Error al crear usuario: ${response.code()} - $errorBody"
                    Log.e(TAG, "Error al crear usuario: ${response.code()}")
                    Log.e(TAG, "Cuerpo del error: $errorBody")
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                Log.e(TAG, "Error al crear usuario", e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Actualiza un usuario existente
     */
    fun actualizarUsuario(userId: Int, email: String?, nombre: String?, apellido: String?, password: String?, rol: String?) {
        // Validar email si se proporciona
        if (email != null && !isValidEmail(email)) {
            _error.value = "El formato del email no es válido"
            return
        }
        
        viewModelScope.launch {
            _loading.value = true
            _error.value = ""
            _usuarioActualizado.value = false
            
            try {
                val token = "Bearer ${SessionManager.getToken()}"
                val usuarioRequest = UsuarioUpdateRequest(
                    email = email,
                    nombre = nombre,
                    apellido = apellido,
                    password = if (password.isNullOrBlank()) null else password,
                    rol = rol
                )
                
                // Logear todos los datos que se envían
                Log.d(TAG, "Actualizando usuario ID: $userId")
                Log.d(TAG, "Datos enviados: email='$email', nombre='$nombre', apellido='$apellido', rol='$rol'")
                Log.d(TAG, "Password enviado: ${if (password.isNullOrBlank()) "No (null)" else "Sí (nuevo)"}")
                
                val response = RestauranteApp.getInstance().obtenerServicioAPI().updateUsuario(token, userId, usuarioRequest)
                
                if (response.isSuccessful) {
                    _usuarioActualizado.value = true
                    cargarUsuarios() // Recargar lista de usuarios
                    Log.d(TAG, "Usuario actualizado correctamente")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalle"
                    _error.value = "Error al actualizar usuario: ${response.code()} - $errorBody"
                    Log.e(TAG, "Error al actualizar usuario: ${response.code()}")
                    Log.e(TAG, "Cuerpo del error: $errorBody")
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                Log.e(TAG, "Error al actualizar usuario", e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Elimina un usuario
     */
    fun eliminarUsuario(userId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = ""
            _usuarioEliminado.value = false
            
            try {
                val token = "Bearer ${SessionManager.getToken()}"
                val response = RestauranteApp.getInstance().obtenerServicioAPI().deleteUsuario(token, userId)
                
                if (response.isSuccessful) {
                    _usuarioEliminado.value = true
                    cargarUsuarios() // Recargar lista de usuarios
                    Log.d(TAG, "Usuario eliminado correctamente")
                } else {
                    _error.value = "Error al eliminar usuario: ${response.code()}"
                    Log.e(TAG, "Error al eliminar usuario: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                Log.e(TAG, "Error al eliminar usuario", e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Verifica si el usuario actual es administrador
     */
    fun isAdmin(): Boolean {
        val userRole = SessionManager.getUserRole()
        Log.d(TAG, "Verificando rol de administrador: $userRole")
        return userRole.equals("ADMIN", ignoreCase = true)
    }
} 