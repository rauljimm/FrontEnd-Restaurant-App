package rjm.frontrestaurante.api

import com.google.gson.annotations.SerializedName

/**
 * Clase para solicitud de actualización del estado de una mesa
 */
data class MesaUpdateRequest(
    @SerializedName("estado") val estado: String,
    @SerializedName("metodo_pago") val metodoPago: String? = null
)

/**
 * Clase para solicitud de actualización del estado de un pedido
 */
data class PedidoUpdateRequest(
    @SerializedName("estado") val estado: String
)

/**
 * Clase para solicitud de creación de detalle de pedido
 */
data class DetallePedidoRequest(
    @SerializedName("producto_id") val productoId: Int,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("observaciones") val observaciones: String? = null
)

/**
 * Clase para solicitud de actualización de detalle de pedido
 */
data class DetallePedidoUpdateRequest(
    @SerializedName("cantidad") val cantidad: Int? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("observaciones") val observaciones: String? = null
)

/**
 * Clase para solicitud de actualización de cuenta
 */
data class CuentaUpdateRequest(
    @SerializedName("metodo_pago") val metodoPago: String
) 