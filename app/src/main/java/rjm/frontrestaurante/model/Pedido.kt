package rjm.frontrestaurante.model

import java.util.Date

/**
 * Modelo de datos para representar un pedido en el restaurante
 */
data class Pedido(
    val id: Int,
    val mesaId: Int? = null,
    val camareroId: Int,
    val estado: EstadoPedido,
    val observaciones: String = "",
    val fecha: Date,
    val detalles: List<DetallePedido> = emptyList(),
    val total: Double = 0.0
)

/**
 * Modelo de datos para representar un detalle de pedido
 */
data class DetallePedido(
    val id: Int,
    val pedidoId: Int,
    val productoId: Int,
    val cantidad: Int,
    val observaciones: String = "",
    val estado: EstadoDetallePedido,
    val producto: Producto? = null
)

/**
 * Enumeración para representar los posibles estados de un pedido
 */
enum class EstadoPedido {
    RECIBIDO, EN_PREPARACION, LISTO, ENTREGADO, CANCELADO;
    
    companion object {
        fun fromString(estado: String): EstadoPedido {
            return when (estado.lowercase()) {
                "recibido" -> RECIBIDO
                "en_preparacion" -> EN_PREPARACION
                "listo" -> LISTO
                "entregado" -> ENTREGADO
                "cancelado" -> CANCELADO
                else -> RECIBIDO
            }
        }
    }
}

/**
 * Enumeración para representar los posibles estados de un detalle de pedido
 */
enum class EstadoDetallePedido {
    PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO, CANCELADO;
    
    companion object {
        fun fromString(estado: String): EstadoDetallePedido {
            return when (estado.lowercase()) {
                "pendiente" -> PENDIENTE
                "en_preparacion" -> EN_PREPARACION
                "listo" -> LISTO
                "entregado" -> ENTREGADO
                "cancelado" -> CANCELADO
                else -> PENDIENTE
            }
        }
    }
} 