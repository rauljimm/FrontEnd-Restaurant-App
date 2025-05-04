package rjm.frontrestaurante.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo para la respuesta de inicio de sesión
 */
data class LoginResponse(
    @SerializedName("access_token")
    val token: String,
    
    @SerializedName("token_type")
    val tokenType: String
) 