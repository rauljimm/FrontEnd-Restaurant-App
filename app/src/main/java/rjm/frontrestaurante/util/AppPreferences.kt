package rjm.frontrestaurante.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Clase para gestionar las preferencias de la aplicación
 */
class AppPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * Guarda el token de autorización
     */
    fun saveAuthToken(token: String) {
        // Limpiar datos anteriores para evitar inconsistencias
        clearPreferences()
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }
    
    /**
     * Obtiene el token de autorización
     */
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }
    
    /**
     * Guarda el ID de usuario
     */
    fun saveUserId(userId: Int) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply()
    }
    
    /**
     * Obtiene el ID de usuario
     */
    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, 0)
    }
    
    /**
     * Establece si se necesita un segundo intento de login
     */
    fun setSecondLoginNeeded(needed: Boolean) {
        prefs.edit().putBoolean(KEY_SECOND_LOGIN_NEEDED, needed).apply()
    }
    
    /**
     * Comprueba si se necesita un segundo intento de login
     */
    fun isSecondLoginNeeded(): Boolean {
        // Por defecto no necesitamos un segundo intento
        return prefs.getBoolean(KEY_SECOND_LOGIN_NEEDED, false)
    }
    
    /**
     * Limpia todas las preferencias (para logout)
     */
    fun clearPreferences() {
        // Primero limpiar todas las preferencias
        prefs.edit().clear().apply()
        
        // Y luego establecer explícitamente cada valor a su estado inicial
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, null)
            .putInt(KEY_USER_ID, 0)
            .putBoolean(KEY_SECOND_LOGIN_NEEDED, false)
            .apply()
    }
    
    companion object {
        private const val PREF_NAME = "restaurante_preferences"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SECOND_LOGIN_NEEDED = "second_login_needed"
    }
} 