package rjm.frontrestaurante.model

import java.util.Date

/**
 * Modelo de datos para representar una cuenta cobrada
 */
data class Cuenta(
    val id: Int,
    val mesaId: Int?,
    val numeroMesa: Int,
    val camareroId: Int?,
    val nombreCamarero: String,
    val fechaCobro: Date,
    val total: Double,
    val metodoPago: String?,
    val detalles: List<DetalleCuenta> = emptyList()
)

/**
 * Modelo de datos para representar un detalle de cuenta
 */
data class DetalleCuenta(
    val pedidoId: Int,
    val productoId: Int,
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double,
    val observaciones: String? = null
) 