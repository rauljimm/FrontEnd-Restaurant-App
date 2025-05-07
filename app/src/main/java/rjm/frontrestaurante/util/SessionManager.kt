package rjm.frontrestaurante.util

import android.content.Context
import android.content.SharedPreferences
import rjm.frontrestaurante.RestauranteApp
import rjm.frontrestaurante.util.AppPreferences

/**
 * Gestor de sesión para manejar el token y los datos del usuario
 */
object SessionManager {
    private const val PREF_NAME = "RestaurantePrefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ROLE = "user_role"
    
    private val prefs: SharedPreferences by lazy {
        RestauranteApp.getInstance().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Guarda el token en las preferencias
     */
    fun saveToken(token: String) {
        // Limpiar datos anteriores primero para evitar problemas
        logout()
        // Guardar el nuevo token
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    /**
     * Obtiene el token guardado
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    /**
     * Guarda la información básica del usuario
     */
    fun saveUserInfo(id: Int, name: String, role: String) {
        // Guardar datos en SessionManager
        prefs.edit()
            .putInt(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_ROLE, role)
            .apply()
            
        // Sincronizar con AppPreferences
        val appPreferences = RestauranteApp.getInstance().preferences
        appPreferences.saveUserId(id)
        appPreferences.setSecondLoginNeeded(false)
    }
    
    /**
     * Obtiene el ID del usuario
     */
    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }
    
    /**
     * Obtiene el nombre del usuario
     */
    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }
    
    /**
     * Obtiene el rol del usuario
     */
    fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, "") ?: ""
    }
    
    /**
     * Comprueba si el usuario está autenticado
     */
    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }
    
    /**
     * Cierra la sesión, borrando todos los datos guardados
     */
    fun logout() {
        // Primero, limpiar completamente las preferencias
        prefs.edit().clear().apply()
        
        // Para mayor seguridad, asegurarnos que cada valor específico esté vacío
        prefs.edit()
            .putString(KEY_TOKEN, null)
            .putInt(KEY_USER_ID, -1)
            .putString(KEY_USER_NAME, "")
            .putString(KEY_USER_ROLE, "")
            .apply()
            
        // Sincronizar con las preferencias de la aplicación
        try {
            val appPreferences = RestauranteApp.getInstance().preferences
            appPreferences.clearPreferences()
            appPreferences.setSecondLoginNeeded(false)
            
            // Verificar que realmente se han borrado los datos
            android.util.Log.d("SessionManager", "Logout: Token=${getToken()}, UserId=${getUserId()}, Role=${getUserRole()}")
        } catch (e: Exception) {
            // Ignorar errores, solo para asegurar que no falla el logout
            android.util.Log.e("SessionManager", "Error al sincronizar con AppPreferences", e)
        }
    }
} 