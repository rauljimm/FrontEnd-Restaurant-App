package rjm.frontrestaurante.model

/**
 * Modelo de datos para representar una mesa en el restaurante
 */
data class Mesa(
    val id: Int,
    val numero: Int,
    val capacidad: Int,
    val estado: EstadoMesa,
    val ubicacion: String = ""
)

/**
 * Enumeración para representar los posibles estados de una mesa
 */
enum class EstadoMesa {
    LIBRE, OCUPADA, RESERVADA, MANTENIMIENTO;
    
    companion object {
        fun fromString(estado: String): EstadoMesa {
            return when (estado.lowercase()) {
                "libre" -> LIBRE
                "ocupada" -> OCUPADA
                "reservada" -> RESERVADA
                "mantenimiento" -> MANTENIMIENTO
                else -> LIBRE
            }
        }
    }
} 