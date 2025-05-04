package rjm.frontrestaurante.model

import java.util.Date

/**
 * Modelo de datos para representar una reserva en el restaurante
 */
data class Reserva(
    val id: Int,
    val mesaId: Int,
    val clienteNombre: String,
    val clienteTelefono: String,
    val fecha: Date,
    val hora: String,
    val numPersonas: Int,
    val observaciones: String = "",
    val estado: EstadoReserva = EstadoReserva.PENDIENTE
)

/**
 * Enumeración para representar los estados de una reserva
 */
enum class EstadoReserva {
    PENDIENTE, CONFIRMADA, CANCELADA, COMPLETADA;
    
    companion object {
        fun fromString(estado: String): EstadoReserva {
            return when (estado.lowercase()) {
                "pendiente" -> PENDIENTE
                "confirmada" -> CONFIRMADA
                "cancelada" -> CANCELADA
                "completada" -> COMPLETADA
                else -> PENDIENTE
            }
        }
    }
} 