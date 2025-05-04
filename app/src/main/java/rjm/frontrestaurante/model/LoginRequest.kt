package rjm.frontrestaurante.model

/**
 * Modelo para la solicitud de inicio de sesión
 */
data class LoginRequest(
    val username: String,
    val password: String
) 