package rjm.frontrestaurante.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import rjm.frontrestaurante.model.*
import rjm.frontrestaurante.api.MesaUpdateRequest
import rjm.frontrestaurante.api.PedidoUpdateRequest
import rjm.frontrestaurante.api.DetallePedidoRequest
import rjm.frontrestaurante.api.DetallePedidoUpdateRequest
import rjm.frontrestaurante.api.CuentaUpdateRequest

/**
 * Interfaz que define los endpoints de la API del restaurante
 */
interface RestauranteApi {

    // Autenticación
    @POST("login")
    suspend fun login(@Body credentials: Map<String, String>): Response<TokenResponse>

    @GET("usuarios/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<Usuario>

    // Usuarios
    @GET("usuarios")
    suspend fun getUsuarios(
        @Header("Authorization") token: String
    ): Response<List<Usuario>>

    @POST("usuarios")
    suspend fun createUsuario(
        @Header("Authorization") token: String,
        @Body usuario: UsuarioRequest
    ): Response<Usuario>

    @PUT("usuarios/{id}")
    suspend fun updateUsuario(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body usuario: UsuarioUpdateRequest
    ): Response<Usuario>

    @DELETE("usuarios/{id}")
    suspend fun deleteUsuario(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    // Mesas
    @GET("mesas")
    suspend fun getMesas(
        @Header("Authorization") token: String,
        @Query("estado") estado: String? = null
    ): Response<List<Mesa>>

    @GET("mesas/{id}")
    suspend fun getMesaById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Mesa>

    @POST("mesas")
    suspend fun createMesa(
        @Header("Authorization") token: String,
        @Body mesa: RequestBody
    ): Response<Mesa>

    @PUT("mesas/{id}")
    suspend fun updateMesa(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body mesa: RequestBody
    ): Response<Mesa>

    @PUT("mesas/{id}")
    suspend fun updateMesaWithRequest(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body mesa: MesaUpdateRequest
    ): Response<Mesa>

    @DELETE("mesas/{id}")
    suspend fun deleteMesa(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    // Pedidos
    @GET("pedidos")
    suspend fun getPedidos(
        @Header("Authorization") token: String,
        @Query("estado") estado: String? = null,
        @Query("mesa_id") mesaId: Int? = null,
        @Query("activos") activos: Boolean? = null,
        @Query("fecha_inicio") fechaInicio: String? = null
    ): Response<List<Pedido>>

    @GET("pedidos/{id}")
    suspend fun getPedidoById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Pedido>

    @POST("pedidos")
    suspend fun createPedido(
        @Header("Authorization") token: String,
        @Body pedido: RequestBody
    ): Response<Pedido>

    @PUT("pedidos/{id}")
    suspend fun updatePedido(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body pedido: PedidoUpdateRequest
    ): Response<Pedido>

    @DELETE("pedidos/{id}")
    suspend fun deletePedido(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    // Detalles de Pedido
    @POST("pedidos/{pedidoId}/detalles")
    suspend fun addDetallePedido(
        @Header("Authorization") token: String,
        @Path("pedidoId") pedidoId: Int,
        @Body detalle: DetallePedidoRequest
    ): Response<DetallePedido>

    @PUT("pedidos/{pedidoId}/detalles/{detalleId}")
    suspend fun updateDetallePedido(
        @Header("Authorization") token: String,
        @Path("pedidoId") pedidoId: Int,
        @Path("detalleId") detalleId: Int,
        @Body detalle: DetallePedidoUpdateRequest
    ): Response<DetallePedido>

    @DELETE("pedidos/{pedidoId}/detalles/{detalleId}")
    suspend fun deleteDetallePedido(
        @Header("Authorization") token: String,
        @Path("pedidoId") pedidoId: Int,
        @Path("detalleId") detalleId: Int
    ): Response<Unit>

    // Productos
    @GET("productos")
    suspend fun getProductos(
        @Header("Authorization") token: String,
        @Query("categoria_id") categoriaId: Int? = null,
        @Query("tipo") tipo: String? = null,
        @Query("disponible") disponible: Boolean? = null
    ): Response<List<Producto>>

    @GET("productos/{id}")
    suspend fun getProductoById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Producto>

    @POST("productos")
    suspend fun createProducto(
        @Header("Authorization") token: String,
        @Body producto: RequestBody
    ): Response<Producto>

    @DELETE("productos/{id}")
    suspend fun deleteProducto(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    // Categorías
    @GET("categorias")
    suspend fun getCategorias(
        @Header("Authorization") token: String
    ): Response<List<Categoria>>

    @POST("categorias")
    suspend fun createCategoria(
        @Header("Authorization") token: String,
        @Body categoria: RequestBody
    ): Response<Categoria>

    @PUT("categorias/{id}")
    suspend fun updateCategoria(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body categoria: RequestBody
    ): Response<Categoria>

    @DELETE("categorias/{id}")
    suspend fun deleteCategoria(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    // Reservas
    @GET("reservas")
    suspend fun getReservas(
        @Header("Authorization") token: String,
        @Query("fecha") fecha: String? = null,
        @Query("mesa_id") mesaId: Int? = null,
        @Query("estado") estado: String? = null
    ): Response<List<Reserva>>

    @GET("reservas/{id}")
    suspend fun getReservaById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Reserva>

    @POST("reservas")
    suspend fun createReserva(
        @Header("Authorization") token: String,
        @Body reserva: RequestBody
    ): Response<Reserva>

    @PUT("reservas/{id}")
    suspend fun updateReserva(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body reserva: RequestBody
    ): Response<Reserva>

    @DELETE("reservas/{id}")
    suspend fun deleteReserva(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    @GET("mesas/{id}/reserva-activa")
    suspend fun getReservaActiva(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Reserva>

    // Cuentas
    @GET("cuentas")
    suspend fun getCuentas(
        @Header("Authorization") token: String,
        @Query("fecha_inicio") fechaInicio: String? = null,
        @Query("fecha_fin") fechaFin: String? = null,
        @Query("mesa_id") mesaId: Int? = null
    ): Response<List<Cuenta>>

    @GET("cuentas/{id}")
    suspend fun getCuentaById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Cuenta>

    @GET("cuentas/resumen")
    suspend fun getResumenCuentas(
        @Header("Authorization") token: String,
        @Query("fecha_inicio") fechaInicio: String? = null,
        @Query("fecha_fin") fechaFin: String? = null
    ): Response<Map<String, Any>>

    @GET("cuentas/generar/mesa/{mesaId}")
    suspend fun generarCuentaMesa(
        @Header("Authorization") token: String,
        @Path("mesaId") mesaId: Int
    ): Response<Map<String, Any>>

    @PUT("cuentas/{id}")
    suspend fun updateCuenta(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body cuenta: CuentaUpdateRequest
    ): Response<Cuenta>

    @DELETE("cuentas/{id}")
    suspend fun deleteCuenta(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>
}

/**
 * Respuesta de la API para el token
 */
data class TokenResponse(
    val access_token: String,
    val token_type: String
)

/**
 * Modelo para el usuario actual
 */
data class Usuario(
    val id: Int,
    val username: String,
    val email: String,
    val nombre: String,
    val apellido: String,
    val rol: String,
    val activo: Boolean,
    val fecha_creacion: String
)

/**
 * Request para crear un pedido
 */
data class PedidoRequest(
    val mesa_id: Int,
    val observaciones: String? = null,
    val detalles: List<DetallePedidoRequest>? = null
)

/**
 * Request para crear o actualizar una categoría
 */
data class CategoriaRequest(
    val nombre: String,
    val descripcion: String? = null
)

/**
 * Request para crear o actualizar una reserva
 */
data class ReservaRequest(
    val mesa_id: Int,
    val cliente_nombre: String,
    val cliente_telefono: String,
    val fecha: String,
    val hora: String,
    val num_personas: Int,
    val observaciones: String? = null,
    val estado: String? = null
)

/**
 * Request para crear un usuario
 */
data class UsuarioRequest(
    val username: String,
    val password: String,
    val email: String,
    val nombre: String,
    val apellido: String,
    val rol: String
)

/**
 * Request para actualizar un usuario
 */
data class UsuarioUpdateRequest(
    val email: String? = null,
    val nombre: String? = null,
    val apellido: String? = null,
    val password: String? = null,
    val rol: String? = null,
    val activo: Boolean? = null
) 